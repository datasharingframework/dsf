package dev.dsf.common.jetty;

import java.io.IOException;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConfiguration.Customizer;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.rwh.utils.crypto.CertificateHelper;
import de.rwh.utils.crypto.io.CertificateReader;

public abstract class AbstractJettyConfig implements JettyConfig
{
	private static final Logger logger = LoggerFactory.getLogger(AbstractJettyConfig.class);

	private final BiFunction<JettyConfig, Server, Connector> connectorFactory;

	public AbstractJettyConfig(BiFunction<JettyConfig, Server, Connector> connectorFactory)
	{
		this.connectorFactory = connectorFactory;
	}

	public BiFunction<JettyConfig, Server, Connector> getConnectorFactory()
	{
		return connectorFactory;
	}

	@Override
	public final Connector createStatusConnector(Server server)
	{
		ServerConnector connector = new ServerConnector(server, httpConnectionFactory(null));
		connector.setHost(getStatusHost().orElseThrow(JettyConfig.propertyNotDefined(PROPERTY_JETTY_STATUS_HOST)));
		connector.setPort(getStatusPort().orElseThrow(JettyConfig.propertyNotDefined(PROPERTY_JETTY_STATUS_PORT)));

		return connector;
	}

	@Override
	public Map<String, String> getAllProperties()
	{
		Map<String, String> properties = new HashMap<>();
		properties.put(PROPERTY_JETTY_STATUS_HOST, getStatusHost().orElse(null));
		properties.put(PROPERTY_JETTY_STATUS_PORT, getStatusPort().map(String::valueOf).orElse(null));
		properties.put(PROPERTY_JETTY_HOST, getHost().orElse(null));
		properties.put(PROPERTY_JETTY_PORT, getPort().map(String::valueOf).orElse(null));
		properties.put(PROPERTY_JETTY_CONTEXT_PATH, getContextPath().orElse(null));
		properties.put(PROPERTY_JETTY_CLIENT_TRUSTSTORE_PEM,
				getClientTrustStorePath().map(Path::toString).orElse(null));
		properties.put(PROPERTY_JETTY_SERVER_TRUSTSTORE_PEM,
				getServerTrustStorePath().map(Path::toString).orElse(null));
		properties.put(PROPERTY_JETTY_SERVER_KEYSTORE_P12, getServerKeyStorePath().map(Path::toString).orElse(null));
		properties.put(PROPERTY_JETTY_SERVER_KEYSTORE_PASSWORD,
				getServerKeyStorePassword().map(String::valueOf).orElse(null));
		properties.put(PROPERTY_JETTY_CLIENT_CERT_HEADER, getClientCertHeaderName().orElse(null));
		properties.put(PROPERTY_JETTY_LOG4J_CONFIG, getLog4JConfigPath().map(Path::toString).orElse(null));
		return properties;
	}

	@Override
	public Optional<KeyStore> getClientTrustStore()
	{
		return getClientTrustStorePath().map(AbstractJettyConfig::readTrustStore);
	}

	@Override
	public Optional<KeyStore> getServerTrustStore()
	{
		return getServerTrustStorePath().map(AbstractJettyConfig::readTrustStore);
	}

	@Override
	public Optional<KeyStore> getServerKeyStore()
	{
		if (getServerKeyStorePath().isEmpty() || getServerKeyStorePassword().isEmpty())
			return Optional.empty();

		return Optional.of(readKeyStore(getServerKeyStorePath().get(), getServerKeyStorePassword().get()));
	}

	public static final BiFunction<JettyConfig, Server, Connector> httpConnector()
	{
		return (config, server) ->
		{
			ServerConnector connector = new ServerConnector(server,
					httpConnectionFactory(new ForwardedSecureRequestCustomizer(config.getClientCertHeaderName()
							.orElseThrow(JettyConfig.propertyNotDefined(PROPERTY_JETTY_CLIENT_CERT_HEADER)))));
			connector.setHost(config.getHost().orElseThrow(JettyConfig.propertyNotDefined(PROPERTY_JETTY_HOST)));
			connector.setPort(config.getPort().orElseThrow(JettyConfig.propertyNotDefined(PROPERTY_JETTY_PORT)));

			return connector;
		};
	}

	private static HttpConnectionFactory httpConnectionFactory(Customizer customizer)
	{
		HttpConfiguration httpConfiguration = new HttpConfiguration();
		httpConfiguration.setSendServerVersion(false);
		httpConfiguration.setSendXPoweredBy(false);
		httpConfiguration.setSendDateHeader(false);

		if (customizer != null)
			httpConfiguration.addCustomizer(customizer);

		return new HttpConnectionFactory(httpConfiguration);
	}

	public static final BiFunction<JettyConfig, Server, Connector> httpsConnector()
	{
		return (config, server) ->
		{
			ServerConnector connector = new ServerConnector(server,
					sslConnectionFactory(
							config.getServerTrustStore()
									.orElseThrow(JettyConfig.propertyNotDefined(PROPERTY_JETTY_SERVER_TRUSTSTORE_PEM)),
							config.getServerKeyStore()
									.orElseThrow(JettyConfig.propertyNotDefined(PROPERTY_JETTY_SERVER_KEYSTORE_P12)),
							config.getServerKeyStorePassword().orElseThrow(
									JettyConfig.propertyNotDefined(PROPERTY_JETTY_SERVER_KEYSTORE_PASSWORD))),
					httpConnectionFactory(new SecureRequestCustomizer()));
			connector.setHost(config.getHost().orElseThrow(JettyConfig.propertyNotDefined(PROPERTY_JETTY_HOST)));
			connector.setPort(config.getPort().orElseThrow(JettyConfig.propertyNotDefined(PROPERTY_JETTY_PORT)));

			return connector;
		};
	}

	private static KeyStore readTrustStore(Path trustStorePath)
	{
		try
		{
			return CertificateReader.allFromCer(trustStorePath);
		}
		catch (NoSuchAlgorithmException | CertificateException | KeyStoreException | IOException e)
		{
			logger.warn("Error while reading trust store from {}: {} - {}", trustStorePath.toString(),
					e.getClass().getName(), e.getMessage());
			throw new RuntimeException(e);
		}
	}

	private static KeyStore readKeyStore(Path keyStorePath, char[] keyStorePassword)
	{
		try
		{
			return CertificateReader.fromPkcs12(keyStorePath, keyStorePassword);
		}
		catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e)
		{
			logger.warn("Error while reading key store from {} with password {}: {} - {}", keyStorePath.toString(),
					keyStorePassword == null ? "null" : "***", e.getClass().getName(), e.getMessage());
			throw new RuntimeException(e);
		}
	}

	private static SslConnectionFactory sslConnectionFactory(KeyStore trustStore, KeyStore keyStore,
			char[] keyStorePassword)
	{
		logCertificateConfig(trustStore, keyStore);

		SslContextFactory.Server sslContextFactory = new SslContextFactory.Server()
		{
			@Override
			protected KeyStore loadTrustStore(Resource resource) throws Exception
			{
				return getTrustStore();
			}
		};
		sslContextFactory.setTrustStore(trustStore);
		sslContextFactory.setKeyStore(keyStore);
		sslContextFactory.setKeyStorePassword(String.valueOf(keyStorePassword));
		sslContextFactory.setNeedClientAuth(true);

		return new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString());
	}

	private static void logCertificateConfig(KeyStore trustStore, KeyStore keyStore)
	{
		if (!logger.isDebugEnabled())
			return;

		try
		{
			if (trustStore != null)
				logger.debug("Using trust store for https connector with: {}",
						CertificateHelper.listCertificateSubjectNames(trustStore));

			if (keyStore != null)
				logger.debug("Using key store for https connector with: {}",
						CertificateHelper.listCertificateSubjectNames(keyStore));
		}
		catch (KeyStoreException e)
		{
			logger.warn("Error while printing trust store / key store config", e);
		}
	}
}
