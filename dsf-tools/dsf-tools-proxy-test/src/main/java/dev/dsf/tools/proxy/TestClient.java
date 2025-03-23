package dev.dsf.tools.proxy;

import java.time.Duration;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.fhir.client.AbstractJerseyClient;
import jakarta.ws.rs.core.Response;

public class TestClient extends AbstractJerseyClient
{
	private static final Logger logger = LoggerFactory.getLogger(TestClient.class);

	public TestClient(String baseUrl, String proxySchemeHostPort, String proxyUserName, char[] proxyPassword)
	{
		super(baseUrl, null, null, null, null, null, proxySchemeHostPort, proxyUserName, proxyPassword,
				Duration.ofSeconds(5), Duration.ofSeconds(5), true, "DSF Proxy Test Client");

		logger.info("baseUrl: {}", baseUrl);
		logger.info("proxySchemeHostPort: {}", proxySchemeHostPort);
		logger.info("proxyUserName: {}", proxyUserName);
		logger.info("proxyPassword: {}", IntStream.range(0, proxyPassword != null ? proxyPassword.length : 0)
				.mapToObj(i -> "*").collect(Collectors.joining()));
	}

	public void testBaseUrl()
	{
		logger.info("GET {} ...", getBaseUrl());
		try (Response response = getResource().request().get())
		{
			logger.info("HTTP {}: {}", response.getStatusInfo().getStatusCode(),
					response.getStatusInfo().getReasonPhrase());
		}
	}
}
