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
package dev.dsf.bpe.integration;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

import de.hsheilbronn.mi.utils.crypto.context.SSLContextFactory;
import de.hsheilbronn.mi.utils.crypto.keystore.KeyStoreCreator;
import dev.dsf.bpe.integration.X509Certificates.CertificateAndPrivateKey;

public class TestFhirDataServer
{
	private static final Logger logger = LoggerFactory.getLogger(TestFhirDataServer.class);

	private final HttpsServer server;

	public TestFhirDataServer(CertificateAndPrivateKey serverCertificate)
	{
		try
		{
			char[] keyStorePassword = UUID.randomUUID().toString().toCharArray();
			KeyStore keyStore = KeyStoreCreator.jksForPrivateKeyAndCertificateChain(serverCertificate.privateKey(),
					keyStorePassword, serverCertificate.certificate(), serverCertificate.caCertificate());

			server = HttpsServer.create(new InetSocketAddress("localhost", 0), 0);
			server.setHttpsConfigurator(
					new HttpsConfigurator(SSLContextFactory.createSSLContext(null, keyStore, keyStorePassword, "TLS")));

			server.createContext("/Patient", exchange ->
			{
				logger.info("GET /Patient");

				exchange.getResponseHeaders().set("Location",
						"https://localhost:" + server.getAddress().getPort() + "/async");
				exchange.sendResponseHeaders(202, 0);
				exchange.close();
			});

			server.createContext("/async", exchange ->
			{
				logger.info("GET /async");

				String jsonResponse = "{\"resourceType\":\"Bundle\",\"type\":\"searchset\",\"total\":0}";

				exchange.getResponseHeaders().set("Content-Type", "application/fhir+json");
				exchange.sendResponseHeaders(200, jsonResponse.getBytes().length);

				try (OutputStream os = exchange.getResponseBody())
				{
					os.write(jsonResponse.getBytes());
				}

				exchange.close();
			});
		}
		catch (IOException | UnrecoverableKeyException | KeyManagementException | KeyStoreException
				| NoSuchAlgorithmException e)
		{
			throw new RuntimeException(e);
		}
	}

	public InetSocketAddress getAddress()
	{
		return server.getAddress();
	}

	public void stop()
	{
		server.stop(0);
	}

	public void start()
	{
		server.start();
	}
}
