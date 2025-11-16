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
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.Constants;
import de.hsheilbronn.mi.utils.crypto.context.SSLContextFactory;
import de.hsheilbronn.mi.utils.crypto.keystore.KeyStoreCreator;
import dev.dsf.bpe.integration.X509Certificates.CertificateAndPrivateKey;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response.Status;

public class TestFhirDataServer
{
	private static final Logger logger = LoggerFactory.getLogger(TestFhirDataServer.class);

	private final HttpsServer server;

	public TestFhirDataServer(CertificateAndPrivateKey serverCertificate)
	{
		FhirContext context = FhirContext.forR4();

		try
		{
			char[] keyStorePassword = UUID.randomUUID().toString().toCharArray();
			KeyStore keyStore = KeyStoreCreator.jksForPrivateKeyAndCertificateChain(serverCertificate.privateKey(),
					keyStorePassword, serverCertificate.certificate(), serverCertificate.caCertificate());

			server = HttpsServer.create(new InetSocketAddress("localhost", 0), 0);
			server.setHttpsConfigurator(
					new HttpsConfigurator(SSLContextFactory.createSSLContext(null, keyStore, keyStorePassword, "TLS")));

			AtomicReference<Integer> counter = new AtomicReference<>(0);

			server.createContext("/Patient", exchange ->
			{
				logger.info("GET /Patient");

				counter.set(0);

				exchange.getResponseHeaders().set(HttpHeaders.LOCATION,
						"https://localhost:" + server.getAddress().getPort() + "/async");
				exchange.getResponseHeaders().set(HttpHeaders.RETRY_AFTER,
						DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss O").withZone(ZoneOffset.UTC)
								.format(ZonedDateTime.now().plusSeconds(2)));
				exchange.sendResponseHeaders(Status.ACCEPTED.getStatusCode(), 0);
				exchange.close();
			});

			server.createContext("/async", exchange ->
			{
				logger.info("GET /async");

				Integer c = counter.updateAndGet(i -> ++i);
				if (c <= 2)
				{
					exchange.getResponseHeaders().set(HttpHeaders.RETRY_AFTER, "1");
					exchange.sendResponseHeaders(Status.ACCEPTED.getStatusCode(), 0);
					exchange.close();
				}

				Bundle response = new Bundle().setType(BundleType.BATCHRESPONSE);
				response.addEntry().setResource(new Bundle().setType(BundleType.SEARCHSET).setTotal(0)).getResponse()
						.setStatus("200 OK").setLocation("Patient");
				String jsonResponse = context.newJsonParser().encodeResourceToString(response);

				exchange.getResponseHeaders().set(HttpHeaders.CONTENT_TYPE, Constants.CT_FHIR_JSON_NEW);
				exchange.sendResponseHeaders(Status.OK.getStatusCode(), jsonResponse.getBytes().length);

				try (OutputStream os = exchange.getResponseBody())
				{
					os.write(jsonResponse.getBytes());
				}

				exchange.close();
			});

			server.createContext("/Observation", exchange ->
			{
				logger.info("GET /Observation");

				exchange.sendResponseHeaders(Status.GATEWAY_TIMEOUT.getStatusCode(), 0);
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
