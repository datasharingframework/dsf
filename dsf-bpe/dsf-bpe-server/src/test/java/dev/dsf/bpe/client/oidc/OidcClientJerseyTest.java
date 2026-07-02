/*
 * Copyright 2018-2025 Heilbronn University of Applied Sciences
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.dsf.bpe.client.oidc;

import static java.nio.charset.StandardCharsets.US_ASCII;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

import de.hsheilbronn.mi.utils.crypto.context.SSLContextFactory;
import de.hsheilbronn.mi.utils.crypto.io.PemReader;
import de.hsheilbronn.mi.utils.crypto.keystore.KeyStoreCreator;
import dev.dsf.bpe.api.client.oidc.OidcClientException;
import dev.dsf.bpe.integration.X509Certificates;
import dev.dsf.common.oidc.Jwks;
import dev.dsf.common.oidc.OidcConfiguration;
import jakarta.ws.rs.core.HttpHeaders;

public class OidcClientJerseyTest
{
	@Rule
	public final X509Certificates certificates = new X509Certificates();

	@Rule
	public final TokenEndpointServerResource tokenEndpointServerResource = new TokenEndpointServerResource();

	// Needs keycloak service from 3dic-ttp dev setup, "Service accounts roles" needs to be activated for dic1-fhir
	// client
	@Ignore
	@Test
	public void getAccessToken() throws Exception
	{
		KeyStore trustStore = KeyStoreCreator.jksForTrustedCertificates(PemReader
				.readCertificate(Paths.get("../../dsf-tools/dsf-tools-test-data-generator/cert/DSF_Dev_Root_CA.pem")));

		OidcClientJersey client = new OidcClientJersey("https://keycloak:8443/realms/dic1",
				"/.well-known/openid-configuration", "dic1-fhir", "mF0GEtjFoyWIM3in4VCwifGI3azb4DTn".toCharArray(),
				trustStore, null, null, null, null, null, "Test Client", Duration.ofSeconds(10), Duration.ofSeconds(5),
				true, Duration.ofSeconds(10), null, false);

		char[] accessToken = client.asOidcClientWithDecodedJwt().getAccessToken();
		assertNotNull(accessToken);
	}

	@Test
	public void basicAuthorizationClientIdAndSecretShouldBeUrlEncoded() throws Exception
	{
		var clientId = "client/id:+";
		var clientSecret = "client/secret:+";
		var expectedAuthorizationHeaderValue = "Basic ".concat(clientId.transform(id -> URLEncoder.encode(id, US_ASCII))
				.concat(":").concat(clientSecret.transform(secret -> URLEncoder.encode(secret, US_ASCII)))
				.transform(s -> Base64.getEncoder().encodeToString(s.getBytes(US_ASCII))));
		var client = new OidcClientJersey("https://localhost:" + tokenEndpointServerResource.getPort(),
				"/.well-known/openid-configuration", clientId, clientSecret.toCharArray(),
				certificates.getFhirServerCertificate().trustStore(), null, null, null, null, null, "Test Client",
				Duration.ofSeconds(10), Duration.ofSeconds(5), true, Duration.ofSeconds(10), null, false);
		var config = new OidcConfiguration("https://localhost:" + tokenEndpointServerResource.getPort(),
				"https://localhost:" + tokenEndpointServerResource.getPort() + "/token", "https://localhost/jwks",
				Set.of("client_credentials"));

		// token is intentionally invalid as it is not the focus of this test
		// and to simplify test code
		assertThrows(OidcClientException.class, () -> client.getAccessTokenDecoded(config, new Jwks(List.of())));

		assertEquals(expectedAuthorizationHeaderValue, tokenEndpointServerResource.getAuthorizationHeaderValue());
	}

	private class TokenEndpointServerResource extends ExternalResource
	{
		private HttpsServer server;
		private String authorizationHeaderValue;

		@Override
		protected void before() throws Throwable
		{
			// Server will be created lazily when getPort() is first called
		}

		@Override
		protected void after()
		{
			if (server != null)
			{
				server.stop(0);
			}
		}

		int getPort() throws Exception
		{
			if (server == null)
			{
				server = createServer();
				server.start();
			}
			return server.getAddress().getPort();
		}

		String getAuthorizationHeaderValue()
		{
			return authorizationHeaderValue;
		}

		private HttpsServer createServer() throws Exception
		{
			var newServer = HttpsServer.create(new InetSocketAddress("localhost", 0), 0);
			var serverCertificate = certificates.getFhirServerCertificate();

			var keyStorePassword = "server-password".toCharArray();
			KeyStore keyStore = KeyStoreCreator.jksForPrivateKeyAndCertificateChain(serverCertificate.privateKey(),
					keyStorePassword, serverCertificate.certificate(), serverCertificate.caCertificate());

			newServer.setHttpsConfigurator(
					new HttpsConfigurator(SSLContextFactory.createSSLContext(null, keyStore, keyStorePassword, "TLS")));

			newServer.createContext("/token", exchange ->
			{
				authorizationHeaderValue = exchange.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION);

				byte[] response = "{\"access_token\":\"invalid\",\"expires_in\":300}".getBytes(US_ASCII);
				exchange.getResponseHeaders().set(HttpHeaders.CONTENT_TYPE, "application/json");
				exchange.sendResponseHeaders(200, response.length);
				try (OutputStream out = exchange.getResponseBody())
				{
					out.write(response);
				}
				catch (IOException e)
				{
					throw new RuntimeException(e);
				}
				exchange.close();
			});

			return newServer;
		}
	}
}
