package dev.dsf.bpe.client;

import java.security.KeyStore;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.net.ssl.SSLContext;

import org.glassfish.jersey.SslConfigurator;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJsonProvider;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.logging.LoggingFeature.Verbosity;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.HttpHeaders;

public class AbstractJerseyClient
{
	private static final java.util.logging.Logger requestDebugLogger;
	static
	{
		requestDebugLogger = java.util.logging.Logger.getLogger(AbstractJerseyClient.class.getName());
		requestDebugLogger.setLevel(Level.INFO);
	}

	private final Client client;
	private final String baseUrl;

	public AbstractJerseyClient(String baseUrl, KeyStore trustStore, KeyStore keyStore, char[] keyStorePassword,
			ObjectMapper objectMapper, Collection<?> componentsToRegister)
	{
		this(baseUrl, trustStore, keyStore, keyStorePassword, objectMapper, componentsToRegister, null, null, null, 0,
				0, false, null);
	}

	public AbstractJerseyClient(String baseUrl, KeyStore trustStore, KeyStore keyStore, char[] keyStorePassword,
			ObjectMapper objectMapper, Collection<?> componentsToRegister, String proxySchemeHostPort,
			String proxyUserName, char[] proxyPassword, int connectTimeout, int readTimeout, boolean logRequests,
			String userAgentValue)
	{
		SSLContext sslContext = null;
		if (trustStore != null && keyStore == null && keyStorePassword == null)
			sslContext = SslConfigurator.newInstance().trustStore(trustStore).createSSLContext();
		else if (trustStore != null && keyStore != null && keyStorePassword != null)
			sslContext = SslConfigurator.newInstance().trustStore(trustStore).keyStore(keyStore)
					.keyStorePassword(keyStorePassword).createSSLContext();

		ClientBuilder builder = ClientBuilder.newBuilder();

		if (sslContext != null)
			builder = builder.sslContext(sslContext);

		ClientConfig config = new ClientConfig();
		config.connectorProvider(new ApacheConnectorProvider());
		config.property(ClientProperties.PROXY_URI, proxySchemeHostPort);
		config.property(ClientProperties.PROXY_USERNAME, proxyUserName);
		config.property(ClientProperties.PROXY_PASSWORD, proxyPassword == null ? null : String.valueOf(proxyPassword));
		builder = builder.withConfig(config);

		if (userAgentValue != null && !userAgentValue.isBlank())
			builder = builder.register((ClientRequestFilter) requestContext -> requestContext.getHeaders()
					.add(HttpHeaders.USER_AGENT, userAgentValue));

		builder = builder.readTimeout(readTimeout, TimeUnit.MILLISECONDS).connectTimeout(connectTimeout,
				TimeUnit.MILLISECONDS);

		if (objectMapper != null)
		{
			JacksonJaxbJsonProvider p = new JacksonJaxbJsonProvider(JacksonJsonProvider.BASIC_ANNOTATIONS);
			p.setMapper(objectMapper);
			builder.register(p);
		}

		if (componentsToRegister != null)
			componentsToRegister.forEach(builder::register);

		if (logRequests)
		{
			builder = builder.register(new LoggingFeature(requestDebugLogger, Level.INFO, Verbosity.PAYLOAD_ANY,
					LoggingFeature.DEFAULT_MAX_ENTITY_SIZE));
		}

		client = builder.build();

		this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
		// making sure the root url works, this might be a workaround for a jersey client bug
	}

	protected WebTarget getResource()
	{
		return client.target(baseUrl);
	}

	public String getBaseUrl()
	{
		return baseUrl;
	}
}
