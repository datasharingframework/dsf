package dev.dsf.common.oidc;

import java.security.KeyStore;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.net.ssl.SSLContext;

import org.glassfish.jersey.SslConfigurator;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.logging.LoggingFeature.Verbosity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public class BaseOidcClientJersey implements BaseOidcClient
{
	private static final Logger logger = LoggerFactory.getLogger(BaseOidcClientJersey.class);

	private static final java.util.logging.Logger requestDebugLogger;
	static
	{
		requestDebugLogger = java.util.logging.Logger.getLogger(BaseOidcClientJersey.class.getName());
		requestDebugLogger.setLevel(Level.INFO);
	}

	private final String baseUrl;
	private final String discoveryPath;

	protected final Client client;

	/**
	 * @param baseUrl
	 *            not <code>null</code>
	 * @param discoveryPath
	 *            not <code>null</code>
	 * @param trustStore
	 *            may be <code>null</code>
	 * @param keyStore
	 *            may be <code>null</code>
	 * @param keyStorePassword
	 *            may be <code>null</code>
	 * @param proxySchemeHostPort
	 *            may be <code>null</code>
	 * @param proxyUserName
	 *            may be <code>null</code>
	 * @param proxyPassword
	 *            may be <code>null</code>
	 * @param userAgentValue
	 *            may be <code>null</code>
	 * @param connectTimeout
	 *            not <code>null</code>
	 * @param readTimeout
	 *            not <code>null</code>
	 * @param logRequestsAndResponses
	 */
	public BaseOidcClientJersey(String baseUrl, String discoveryPath, KeyStore trustStore, KeyStore keyStore,
			char[] keyStorePassword, String proxySchemeHostPort, String proxyUserName, char[] proxyPassword,
			String userAgentValue, Duration connectTimeout, Duration readTimeout, boolean logRequestsAndResponses)
	{
		Objects.requireNonNull(baseUrl, "baseUrl");
		Objects.requireNonNull(discoveryPath, "discoveryPath");

		this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
		this.discoveryPath = discoveryPath.startsWith("/") ? discoveryPath : ("/" + discoveryPath);

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

		builder = builder.connectTimeout(connectTimeout.toMillis(), TimeUnit.MILLISECONDS)
				.readTimeout(readTimeout.toMillis(), TimeUnit.MILLISECONDS);

		if (logRequestsAndResponses)
		{
			builder = builder.register(new LoggingFeature(requestDebugLogger, Level.INFO, Verbosity.PAYLOAD_ANY,
					LoggingFeature.DEFAULT_MAX_ENTITY_SIZE));
		}

		client = builder.build();
	}

	private void logUnexpectedResponseAndClose(Response response)
	{
		String message = response.readEntity(String.class);
		logger.debug("Unexpected response, status: {} {}, message: {}", response.getStatusInfo().getStatusCode(),
				response.getStatusInfo().getReasonPhrase(), message);
	}

	@Override
	public OidcConfiguration getConfiguration() throws OidcClientException
	{
		Response response = client.target(baseUrl).path(discoveryPath).request(MediaType.APPLICATION_JSON_TYPE).get();

		if (response.getStatus() == Status.OK.getStatusCode())
		{
			OidcConfiguration config = response.readEntity(OidcConfiguration.class);

			if (baseUrl.equals(config.issuer()))
				return config;
			else
				throw new OidcClientException("Invalid response: Issuer does not match base-url (" + config.issuer()
						+ " not equal to " + baseUrl + ")");
		}
		else
		{
			logUnexpectedResponseAndClose(response);
			throw new OidcClientException("Unexpected response status code " + response.getStatusInfo().getStatusCode()
					+ " " + response.getStatusInfo().getReasonPhrase());
		}
	}

	@Override
	public Jwks getJwks() throws OidcClientException
	{
		return getJwks(getConfiguration());
	}

	@Override
	public Jwks getJwks(OidcConfiguration configuration) throws OidcClientException
	{
		Objects.requireNonNull(configuration, "configuration");

		Response response = client.target(configuration.jwksUri()).request(MediaType.APPLICATION_JSON_TYPE).get();

		if (response.getStatus() == Status.OK.getStatusCode())
		{
			Jwks jwks = response.readEntity(Jwks.class);
			return jwks;
		}
		else
		{
			logUnexpectedResponseAndClose(response);
			throw new OidcClientException("Unexpected response status code " + response.getStatusInfo().getStatusCode()
					+ " " + response.getStatusInfo().getReasonPhrase());
		}
	}
}