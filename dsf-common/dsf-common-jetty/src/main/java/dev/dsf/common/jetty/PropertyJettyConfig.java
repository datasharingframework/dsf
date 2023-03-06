package dev.dsf.common.jetty;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
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

	@Override
	public Optional<String> getStatusHost()
	{
		return getString(PROPERTY_JETTY_STATUS_HOST, PROPERTY_JETTY_STATUS_HOST_DEFAULT);
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

	@Override
	public Optional<Path> getClientTrustStorePath()
	{
		return getPath(PROPERTY_JETTY_CLIENT_TRUSTSTORE_PEM);
	}

	@Override
	public Optional<Path> getServerTrustStorePath()
	{
		return getPath(PROPERTY_JETTY_SERVER_TRUSTSTORE_PEM);
	}

	@Override
	public Optional<Path> getServerKeyStorePath()
	{
		return getPath(PROPERTY_JETTY_SERVER_KEYSTORE_P12);
	}

	@Override
	public Optional<char[]> getServerKeyStorePassword()
	{
		return getString(PROPERTY_JETTY_SERVER_KEYSTORE_PASSWORD).map(String::toCharArray);
	}

	@Override
	public Optional<String> getClientCertHeaderName()
	{
		return getString(PROPERTY_JETTY_CLIENT_CERT_HEADER, PROPERTY_JETTY_CLIENT_CERT_HEADER_DEFAULT);
	}

	@Override
	public Optional<Path> getLog4JConfigPath()
	{
		return getPath(PROPERTY_JETTY_LOG4J_CONFIG, PROPERTY_JETTY_LOG4J_CONFIG_DEFAULT);
	}
}
