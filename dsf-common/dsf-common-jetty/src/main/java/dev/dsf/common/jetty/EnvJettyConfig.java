package dev.dsf.common.jetty;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class EnvJettyConfig extends AbstractJettyConfig implements JettyConfig
{
	private final JettyConfig delegate;

	public EnvJettyConfig(JettyConfig defaultConfig)
	{
		super(defaultConfig.getConnectorFactory());

		this.delegate = defaultConfig;
	}

	private Optional<String> getString(String propertyName, Supplier<Optional<String>> defaultValue)
	{
		final String envVariableName = propertyToEnvironmentVariableName(propertyName);
		return Optional.ofNullable(System.getenv(envVariableName)).or(defaultValue);
	}

	private Optional<char[]> getCharArray(String propertyName, Supplier<Optional<char[]>> defaultValue)
	{
		final String envVariableName = propertyToEnvironmentVariableName(propertyName);
		return Optional.ofNullable(System.getenv(envVariableName)).map(String::toCharArray).or(defaultValue);
	}

	private Optional<Integer> getPort(String propertyName, Supplier<Optional<Integer>> defaultValue)
	{
		final String envVariableName = propertyToEnvironmentVariableName(propertyName);
		return getString(envVariableName, Optional::empty).map(value ->
		{
			try
			{
				int intValue = Integer.parseInt(value);

				if (intValue < 0 || intValue > 0xFFFF)
					throw new RuntimeException(
							"Environment variable " + envVariableName + ", value " + value + " < 0 or > " + 0xFFFF);

				return intValue;
			}
			catch (NumberFormatException e)
			{
				throw new RuntimeException(
						"Environment variable " + envVariableName + ", value " + value + " not a number");
			}
		}).or(defaultValue);
	}

	private Optional<Long> getTimeout(String propertyName, Supplier<Optional<Long>> defaultValue)
	{
		final String envVariableName = propertyToEnvironmentVariableName(propertyName);
		return getString(envVariableName, Optional::empty).map(value ->
		{
			try
			{
				return Long.parseLong(value);
			}
			catch (NumberFormatException e)
			{
				throw new RuntimeException(
						"Environment variable " + envVariableName + ", value " + value + " not a number");
			}
		}).or(defaultValue);
	}

	private Optional<Path> getPath(String propertyName, Supplier<Optional<Path>> defaultValue)
	{
		final String envVariableName = propertyToEnvironmentVariableName(propertyName);
		return getString(envVariableName, Optional::empty).map(value ->
		{
			Path pathValue = Paths.get(value);

			if (!Files.isReadable(pathValue))
				throw new RuntimeException(
						"Environment variable " + envVariableName + ", value " + value + " file not readable");

			return pathValue;
		}).or(defaultValue);
	}

	private Optional<URL> getUrl(String propertyName, Supplier<Optional<URL>> defaultValue)
	{
		final String envVariableName = propertyToEnvironmentVariableName(propertyName);
		return getString(envVariableName, Optional::empty).map(value ->
		{
			try
			{
				return new URL(value);
			}
			catch (MalformedURLException e)
			{
				throw new RuntimeException(
						"Environment variable " + envVariableName + ", value " + value + " not a valid URL");
			}
		}).or(defaultValue);
	}

	private boolean getBoolean(String propertyName, Supplier<Boolean> defaultValue)
	{
		final String envVariableName = propertyToEnvironmentVariableName(propertyName);
		return getString(envVariableName, Optional::empty).map(Boolean::parseBoolean).orElseGet(defaultValue);
	}

	private String propertyToEnvironmentVariableName(String propertyName)
	{
		return propertyName.toUpperCase(Locale.ENGLISH).replace('.', '_');
	}

	@Override
	public Optional<String> getStatusHost()
	{
		return getString(PROPERTY_JETTY_STATUS_HOST, delegate::getStatusHost);
	}

	@Override
	public Optional<Integer> getStatusPort()
	{
		return getPort(PROPERTY_JETTY_STATUS_PORT, delegate::getStatusPort);
	}

	@Override
	public Optional<String> getHost()
	{
		return getString(PROPERTY_JETTY_HOST, delegate::getHost);
	}

	@Override
	public Optional<Integer> getPort()
	{
		return getPort(PROPERTY_JETTY_PORT, delegate::getPort);
	}

	@Override
	public Optional<String> getContextPath()
	{
		return getString(PROPERTY_JETTY_CONTEXT_PATH, delegate::getContextPath);
	}

	@Override
	public Optional<Path> getServerCertificatePath()
	{
		return getPath(PROPERTY_JETTY_SERVER_CERTIFICATE, delegate::getServerCertificatePath);
	}

	@Override
	public Optional<Path> getServerCertificateChainPath()
	{
		return getPath(PROPERTY_JETTY_SERVER_CERTIFICATE_CHAIN, delegate::getServerCertificateChainPath);
	}

	@Override
	public Optional<Path> getServerCertificatePrivateKeyPath()
	{
		return getPath(PROPERTY_JETTY_SERVER_CERTIFICATE_PRIVATE_KEY, delegate::getServerCertificatePrivateKeyPath);
	}

	@Override
	public Optional<char[]> getServerCertificatePrivateKeyPassword()
	{
		return getCharArray(PROPERTY_JETTY_SERVER_CERTIFICATE_PRIVATE_KEY_PASSWORD,
				delegate::getServerCertificatePrivateKeyPassword);
	}

	@Override
	public Optional<Path> getClientTrustCertificatesPath()
	{
		return getPath(PROPERTY_JETTY_AUTH_CLIENT_TRUST_CERTIFICATES, delegate::getClientTrustCertificatesPath);
	}

	@Override
	public Optional<String> getClientCertificateHeaderName()
	{
		return getString(PROPERTY_JETTY_AUTH_CLIENT_CERTIFICATE_HEADER_NAME, delegate::getClientCertificateHeaderName);
	}

	@Override
	public Optional<String> getOidcProviderBaseUrl()
	{
		return getString(PROPERTY_JETTY_AUTH_OIDC_PROVIDER_BASE_URL, delegate::getOidcProviderBaseUrl);
	}

	@Override
	public Optional<Long> getOidcProviderClientConnectTimeout()
	{
		return getTimeout(PROPERTY_JETTY_AUTH_OIDC_PROVIDER_CLIENT_CONNECT_TIMEOUT,
				delegate::getOidcProviderClientConnectTimeout);
	}

	@Override
	public Optional<Long> getOidcProviderClientIdleTimeout()
	{
		return getTimeout(PROPERTY_JETTY_AUTH_OIDC_PROVIDER_CLIENT_IDLE_TIMEOUT,
				delegate::getOidcProviderClientIdleTimeout);
	}

	@Override
	public Optional<Path> getOidcProviderClientTrustCertificatesPath()
	{
		return getPath(PROPERTY_JETTY_AUTH_OIDC_PROVIDER_CLIENT_TRUST_CERTIFICATES,
				delegate::getOidcProviderClientTrustCertificatesPath);
	}

	@Override
	public Optional<Path> getOidcProviderClientCertificatePath()
	{
		return getPath(PROPERTY_JETTY_AUTH_OIDC_PROVIDER_CLIENT_CERTIFICATE,
				delegate::getOidcProviderClientCertificatePath);
	}

	@Override
	public Optional<Path> getOidcProviderClientCertificatePrivateKeyPath()
	{
		return getPath(PROPERTY_JETTY_AUTH_OIDC_PROVIDER_CLIENT_CERTIFICATE_PRIVATE_KEY,
				delegate::getOidcProviderClientCertificatePrivateKeyPath);
	}

	@Override
	public Optional<char[]> getOidcProviderClientCertificatePrivateKeyPassword()
	{
		return getCharArray(PROPERTY_JETTY_AUTH_OIDC_PROVIDER_CLIENT_CERTIFICATE_PRIVATE_KEY_PASSWORD,
				delegate::getOidcProviderClientCertificatePrivateKeyPassword);
	}

	@Override
	public Optional<URL> getOidcProviderClientProxyUrl()
	{
		return getUrl(PROPERTY_JETTY_AUTH_OIDC_PROVIDER_CLIENT_PROXY_URL, delegate::getOidcProviderClientProxyUrl);
	}

	@Override
	public Optional<String> getOidcClientId()
	{
		return getString(PROPERTY_JETTY_AUTH_OIDC_CLIENT_ID, delegate::getOidcClientId);
	}

	@Override
	public Optional<String> getOidcClientSecret()
	{
		return getString(PROPERTY_JETTY_AUTH_OIDC_CLIENT_SECRET, delegate::getOidcClientSecret);
	}

	@Override
	public boolean getOidcSsoBackChannelLogoutEnabled()
	{
		return getBoolean(PROPERTY_JETTY_AUTH_OIDC_SSO_BACK_CHANNEL_LOGOUT,
				delegate::getOidcSsoBackChannelLogoutEnabled);
	}

	@Override
	public Optional<String> getOidcSsoBackChannelPath()
	{
		return getString(PROPERTY_JETTY_AUTH_OIDC_SSO_BACK_CHANNEL_LOGOUT_PATH, delegate::getOidcSsoBackChannelPath);
	}

	@Override
	public Optional<Path> getLog4JConfigPath()
	{
		return getPath(PROPERTY_JETTY_LOG4J_CONFIG, delegate::getLog4JConfigPath);
	}

	@Override
	public Map<String, String> getAllProperties()
	{
		Map<String, String> allProperties = new HashMap<>(super.getAllProperties());
		delegate.getAllProperties().forEach(allProperties::putIfAbsent);
		return allProperties;
	}
}
