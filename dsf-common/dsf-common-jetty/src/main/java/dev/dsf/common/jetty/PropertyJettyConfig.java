package dev.dsf.common.jetty;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;
import java.util.function.BiFunction;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;

public final class PropertyJettyConfig extends AbstractJettyConfig implements JettyConfig
{
	public static final class PropertyJettyConfigCreator
	{
		private final BiFunction<JettyConfig, Server, Connector> connectorFactory;

		private PropertyJettyConfigCreator(BiFunction<JettyConfig, Server, Connector> connectorFactory)
		{
			this.connectorFactory = connectorFactory;
		}

		public PropertyJettyConfig read()
		{
			Properties properties = new Properties();

			Path propertiesFile = Paths.get(JETTY_PROPERTIES_FILE);
			if (Files.isReadable(propertiesFile))
			{
				try (Reader reader = new InputStreamReader(Files.newInputStream(propertiesFile),
						StandardCharsets.UTF_8))
				{
					properties.load(reader);

				}
				catch (IOException e)
				{
					throw new RuntimeException(e);
				}
			}

			return new PropertyJettyConfig(connectorFactory, properties);
		}
	}

	public static PropertyJettyConfigCreator forHttp()
	{
		return new PropertyJettyConfigCreator(AbstractJettyConfig.httpConnector());
	}

	public static PropertyJettyConfigCreator forHttps()
	{
		return new PropertyJettyConfigCreator(AbstractJettyConfig.httpsConnector());
	}

	private final Properties properties;

	private PropertyJettyConfig(BiFunction<JettyConfig, Server, Connector> connectorFactory, Properties properties)
	{
		super(connectorFactory);

		this.properties = properties;
	}

	private Optional<String> getString(String propertyName)
	{
		return getString(propertyName, null);
	}

	private Optional<String> getString(String propertyName, String defaultValue)
	{
		return Optional.ofNullable(properties.getProperty(propertyName, defaultValue));
	}

	private Optional<Integer> getPort(String propertyName)
	{
		return getString(propertyName).map(value ->
		{
			try
			{
				int intValue = Integer.parseInt(value);
				if (intValue < 0 || intValue > 0xFFFF)
					throw new RuntimeException(JETTY_PROPERTIES_FILE + ": Property " + propertyName + ", value " + value
							+ " < 0 or > " + 0xFFFF);
				else
					return intValue;
			}
			catch (NumberFormatException e)
			{
				throw new RuntimeException(
						JETTY_PROPERTIES_FILE + ": Property " + propertyName + ", value " + value + " not a number");
			}
		});
	}

	private Optional<Long> getTimeout(String propertyName)
	{
		return getString(propertyName).map(value ->
		{
			try
			{
				return Long.parseLong(value);
			}
			catch (NumberFormatException e)
			{
				throw new RuntimeException(
						JETTY_PROPERTIES_FILE + ": Property " + propertyName + ", value " + value + " not a number");
			}
		});
	}

	private Optional<Path> getPath(String propertyName)
	{
		return getPath(propertyName, null);
	}

	private Optional<Path> getPath(String propertyName, String defaultValue)
	{
		return getString(propertyName, defaultValue).map(value ->
		{
			Path pathValue = Paths.get(value);

			if (!Files.isReadable(pathValue))
				throw new RuntimeException(JETTY_PROPERTIES_FILE + ": Property " + propertyName + ", value " + value
						+ " file not readable");

			return pathValue;
		});
	}

	private Optional<URL> getUrl(String propertyName)
	{
		return getString(propertyName).map(value ->
		{
			try
			{
				return new URL(value);
			}
			catch (MalformedURLException e)
			{
				throw new RuntimeException(
						JETTY_PROPERTIES_FILE + ": Property " + propertyName + ", value " + value + " not a valid URL");
			}
		});
	}

	private boolean getBoolean(String propertyName)
	{
		return getString(propertyName).map(Boolean::parseBoolean).orElse(Boolean.FALSE);
	}

	@Override
	public Optional<String> getStatusHost()
	{
		return getString(PROPERTY_JETTY_STATUS_HOST, PROPERTY_JETTY_STATUS_HOST_DEFAULT);
	}

	@Override
	public Optional<Integer> getStatusPort()
	{
		return getPort(PROPERTY_JETTY_STATUS_PORT);
	}

	@Override
	public Optional<String> getHost()
	{
		return getString(PROPERTY_JETTY_HOST, PROPERTY_JETTY_HOST_DEFAULT);
	}

	@Override
	public Optional<Integer> getPort()
	{
		return getPort(PROPERTY_JETTY_PORT);
	}

	@Override
	public Optional<String> getContextPath()
	{
		return getString(PROPERTY_JETTY_CONTEXT_PATH);
	}

	@Override
	public Optional<Path> getServerCertificatePath()
	{
		return getPath(PROPERTY_JETTY_SERVER_CERTIFICATE);
	}

	@Override
	public Optional<Path> getServerCertificateChainPath()
	{
		return getPath(PROPERTY_JETTY_SERVER_CERTIFICATE_CHAIN);
	}

	@Override
	public Optional<Path> getServerCertificatePrivateKeyPath()
	{
		return getPath(PROPERTY_JETTY_SERVER_CERTIFICATE_PRIVATE_KEY);
	}

	@Override
	public Optional<char[]> getServerCertificatePrivateKeyPassword()
	{
		return getString(PROPERTY_JETTY_SERVER_CERTIFICATE_PRIVATE_KEY_PASSWORD).map(String::toCharArray);
	}

	@Override
	public Optional<Path> getClientTrustCertificatesPath()
	{
		return getPath(PROPERTY_JETTY_AUTH_CLIENT_TRUST_CERTIFICATES);
	}

	@Override
	public Optional<String> getClientCertificateHeaderName()
	{
		return getString(PROPERTY_JETTY_AUTH_CLIENT_CERTIFICATE_HEADER_NAME,
				PROPERTY_JETTY_AUTH_CLIENT_CERTIFICATE_HEADER_NAME_DEFAULT);
	}

	@Override
	public boolean getOidcAuthorizationCodeFlowEndabled()
	{
		return getBoolean(PROPERTY_JETTY_AUTH_OIDC_AUTHORIZATION_CODE_FLOW);
	}

	@Override
	public boolean getOidcBearerTokenEnabled()
	{
		return getBoolean(PROPERTY_JETTY_AUTH_OIDC_BEARER_TOKEN);
	}

	@Override
	public Optional<String> getOidcProviderBaseUrl()
	{
		return getString(PROPERTY_JETTY_AUTH_OIDC_PROVIDER_BASE_URL);
	}

	@Override
	public Optional<Long> getOidcProviderClientConnectTimeout()
	{
		return getTimeout(PROPERTY_JETTY_AUTH_OIDC_PROVIDER_CLIENT_CONNECT_TIMEOUT);
	}

	@Override
	public Optional<Long> getOidcProviderClientIdleTimeout()
	{
		return getTimeout(PROPERTY_JETTY_AUTH_OIDC_PROVIDER_CLIENT_IDLE_TIMEOUT);
	}

	@Override
	public Optional<Path> getOidcProviderClientTrustCertificatesPath()
	{
		return getPath(PROPERTY_JETTY_AUTH_OIDC_PROVIDER_CLIENT_TRUST_CERTIFICATES);
	}

	@Override
	public Optional<Path> getOidcProviderClientCertificatePath()
	{
		return getPath(PROPERTY_JETTY_AUTH_OIDC_PROVIDER_CLIENT_CERTIFICATE);
	}

	@Override
	public Optional<Path> getOidcProviderClientCertificatePrivateKeyPath()
	{
		return getPath(PROPERTY_JETTY_AUTH_OIDC_PROVIDER_CLIENT_CERTIFICATE_PRIVATE_KEY);
	}

	@Override
	public Optional<char[]> getOidcProviderClientCertificatePrivateKeyPassword()
	{
		return getString(PROPERTY_JETTY_AUTH_OIDC_PROVIDER_CLIENT_CERTIFICATE_PRIVATE_KEY_PASSWORD)
				.map(String::toCharArray);
	}

	@Override
	public Optional<URL> getOidcProviderClientProxyUrl()
	{
		return getUrl(PROPERTY_JETTY_AUTH_OIDC_PROVIDER_CLIENT_PROXY_URL);
	}

	@Override
	public Optional<String> getOidcClientId()
	{
		return getString(PROPERTY_JETTY_AUTH_OIDC_CLIENT_ID);
	}

	@Override
	public Optional<String> getOidcClientSecret()
	{
		return getString(PROPERTY_JETTY_AUTH_OIDC_CLIENT_SECRET);
	}

	@Override
	public boolean getOidcBackChannelLogoutEnabled()
	{
		return getBoolean(PROPERTY_JETTY_AUTH_OIDC_BACK_CHANNEL_LOGOUT);
	}

	@Override
	public Optional<String> getOidcBackChannelPath()
	{
		return getString(PROPERTY_JETTY_AUTH_OIDC_BACK_CHANNEL_LOGOUT_PATH,
				PROPERTY_JETTY_AUTH_OIDC_BACK_CHANNEL_LOGOUT_PATH_DEFAULT);
	}

	@Override
	public Optional<Path> getLog4JConfigPath()
	{
		return getPath(PROPERTY_JETTY_LOG4J_CONFIG, PROPERTY_JETTY_LOG4J_CONFIG_DEFAULT);
	}
}
