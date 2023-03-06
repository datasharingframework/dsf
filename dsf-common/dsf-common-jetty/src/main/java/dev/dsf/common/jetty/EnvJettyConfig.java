package dev.dsf.common.jetty;

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

	private String propertyToEnvironmentVariableName(String propertyName)
	{
		return propertyName.toUpperCase(Locale.GERMAN).replace('.', '_');
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
	public Optional<Path> getClientTrustStorePath()
	{
		return getPath(PROPERTY_JETTY_CLIENT_TRUSTSTORE_PEM, delegate::getClientTrustStorePath);
	}

	@Override
	public Optional<Path> getServerTrustStorePath()
	{
		return getPath(PROPERTY_JETTY_SERVER_TRUSTSTORE_PEM, delegate::getServerTrustStorePath);
	}

	@Override
	public Optional<Path> getServerKeyStorePath()
	{
		return getPath(PROPERTY_JETTY_SERVER_KEYSTORE_P12, delegate::getServerKeyStorePath);
	}

	@Override
	public Optional<char[]> getServerKeyStorePassword()
	{
		return getString(propertyToEnvironmentVariableName(PROPERTY_JETTY_SERVER_KEYSTORE_PASSWORD),
				() -> delegate.getServerKeyStorePassword().map(String::valueOf)).map(String::toCharArray);
	}

	@Override
	public Optional<String> getClientCertHeaderName()
	{
		return getString(PROPERTY_JETTY_CLIENT_CERT_HEADER, delegate::getClientCertHeaderName);
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
