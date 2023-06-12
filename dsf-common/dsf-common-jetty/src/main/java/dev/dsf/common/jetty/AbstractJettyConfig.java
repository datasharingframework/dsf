package dev.dsf.common.jetty;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;

import org.bouncycastle.pkcs.PKCSException;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
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
import de.rwh.utils.crypto.io.PemIo;

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
		ServerConnector connector = new ServerConnector(server, httpConnectionFactory());
		connector.setHost(getStatusHost().orElseThrow(JettyConfig.propertyNotDefined(PROPERTY_JETTY_STATUS_HOST)));
		connector.setPort(getStatusPort().orElseThrow(JettyConfig.propertyNotDefined(PROPERTY_JETTY_STATUS_PORT)));

		return connector;
	}

	@Override
	public Map<String, String> getAllProperties()
	{
		Map<String, String> properties = new HashMap<>();
		properties.put(PROPERTY_JETTY_HOST, getHost().orElse(null));
		properties.put(PROPERTY_JETTY_PORT, getPort().map(String::valueOf).orElse(null));
		properties.put(PROPERTY_JETTY_CONTEXT_PATH, getContextPath().orElse(null));
		properties.put(PROPERTY_JETTY_STATUS_HOST, getStatusHost().orElse(null));
		properties.put(PROPERTY_JETTY_STATUS_PORT, getStatusPort().map(String::valueOf).orElse(null));

		properties.put(PROPERTY_JETTY_SERVER_CERTIFICATE, getServerCertificatePath().map(Path::toString).orElse(null));
		properties.put(PROPERTY_JETTY_SERVER_CERTIFICATE_CHAIN,
				getServerCertificateChainPath().map(Path::toString).orElse(null));
		properties.put(PROPERTY_JETTY_SERVER_CERTIFICATE_PRIVATE_KEY,
				getServerCertificatePrivateKeyPath().map(Path::toString).orElse(null));
		properties.put(PROPERTY_JETTY_SERVER_CERTIFICATE_PRIVATE_KEY_PASSWORD,
				getServerCertificatePrivateKeyPassword().map(String::valueOf).orElse(null));

		properties.put(PROPERTY_JETTY_AUTH_CLIENT_TRUST_CERTIFICATES,
				getClientTrustCertificatesPath().map(Path::toString).orElse(null));
		properties.put(PROPERTY_JETTY_AUTH_CLIENT_CERTIFICATE_HEADER_NAME,
				getClientCertificateHeaderName().orElse(null));

		properties.put(PROPERTY_JETTY_AUTH_OIDC_AUTHORIZATION_CODE_FLOW,
				String.valueOf(getOidcAuthorizationCodeFlowEndabled()));
		properties.put(PROPERTY_JETTY_AUTH_OIDC_BEARER_TOKEN, String.valueOf(getOidcBearerTokenEnabled()));

		properties.put(PROPERTY_JETTY_AUTH_OIDC_PROVIDER_BASE_URL, getOidcProviderBaseUrl().orElse(null));
		properties.put(PROPERTY_JETTY_AUTH_OIDC_PROVIDER_CLIENT_CONNECT_TIMEOUT,
				getOidcProviderClientConnectTimeout().map(String::valueOf).orElse(null));
		properties.put(PROPERTY_JETTY_AUTH_OIDC_PROVIDER_CLIENT_IDLE_TIMEOUT,
				getOidcProviderClientIdleTimeout().map(String::valueOf).orElse(null));
		properties.put(PROPERTY_JETTY_AUTH_OIDC_PROVIDER_CLIENT_TRUST_CERTIFICATES,
				getOidcProviderClientTrustCertificatesPath().map(Path::toString).orElse(null));
		properties.put(PROPERTY_JETTY_AUTH_OIDC_PROVIDER_CLIENT_CERTIFICATE,
				getOidcProviderClientCertificatePath().map(Path::toString).orElse(null));
		properties.put(PROPERTY_JETTY_AUTH_OIDC_PROVIDER_CLIENT_CERTIFICATE_PRIVATE_KEY,
				getOidcProviderClientCertificatePrivateKeyPath().map(Path::toString).orElse(null));
		properties.put(PROPERTY_JETTY_AUTH_OIDC_PROVIDER_CLIENT_CERTIFICATE_PRIVATE_KEY_PASSWORD,
				getOidcProviderClientCertificatePrivateKeyPassword().map(String::valueOf).orElse(null));
		properties.put(PROPERTY_JETTY_AUTH_OIDC_PROVIDER_CLIENT_PROXY_URL,
				getOidcProviderClientProxyUrl().map(URL::toString).orElse(null));

		properties.put(PROPERTY_JETTY_AUTH_OIDC_CLIENT_ID, getOidcClientId().orElse(null));
		properties.put(PROPERTY_JETTY_AUTH_OIDC_CLIENT_SECRET, getOidcClientSecret().orElse(null));

		properties.put(PROPERTY_JETTY_AUTH_OIDC_BACK_CHANNEL_LOGOUT, String.valueOf(getOidcBackChannelLogoutEnabled()));
		properties.put(PROPERTY_JETTY_AUTH_OIDC_BACK_CHANNEL_LOGOUT_PATH, getOidcBackChannelPath().orElse(null));

		properties.put(PROPERTY_JETTY_LOG4J_CONFIG, getLog4JConfigPath().map(Path::toString).orElse(null));
		return properties;
	}

	public static final BiFunction<JettyConfig, Server, Connector> httpConnector()
	{
		return (config, server) ->
		{
			ServerConnector connector = new ServerConnector(server, httpConnectionFactory(
					new ForwardedRequestCustomizer(),
					new ForwardedSecureRequestCustomizer(config.getClientCertificateHeaderName().orElseThrow(
							JettyConfig.propertyNotDefined(PROPERTY_JETTY_AUTH_CLIENT_CERTIFICATE_HEADER_NAME)))));
			connector.setHost(config.getHost().orElseThrow(JettyConfig.propertyNotDefined(PROPERTY_JETTY_HOST)));
			connector.setPort(config.getPort().orElseThrow(JettyConfig.propertyNotDefined(PROPERTY_JETTY_PORT)));

			return connector;
		};
	}

	private static HttpConnectionFactory httpConnectionFactory(Customizer... customizers)
	{
		HttpConfiguration httpConfiguration = new HttpConfiguration();
		httpConfiguration.setSendServerVersion(false);
		httpConfiguration.setSendXPoweredBy(false);
		httpConfiguration.setSendDateHeader(false);

		Arrays.stream(customizers).forEach(httpConfiguration::addCustomizer);

		return new HttpConnectionFactory(httpConfiguration);
	}

	public static final BiFunction<JettyConfig, Server, Connector> httpsConnector()
	{
		char[] keyStorePassword = UUID.randomUUID().toString().toCharArray();

		return (config, server) ->
		{
			ServerConnector connector = new ServerConnector(server,
					sslConnectionFactory(
							config.getClientTrustStore().orElseThrow(
									JettyConfig.propertyNotDefined(PROPERTY_JETTY_AUTH_CLIENT_TRUST_CERTIFICATES)),
							config.getServerKeyStore(keyStorePassword)
									.orElseThrow(JettyConfig.propertiesNotDefined(PROPERTY_JETTY_SERVER_CERTIFICATE,
											PROPERTY_JETTY_SERVER_CERTIFICATE_PRIVATE_KEY)),
							keyStorePassword, config.getOidcConfig() == null),
					httpConnectionFactory(new SecureRequestCustomizer()));
			connector.setHost(config.getHost().orElseThrow(JettyConfig.propertyNotDefined(PROPERTY_JETTY_HOST)));
			connector.setPort(config.getPort().orElseThrow(JettyConfig.propertyNotDefined(PROPERTY_JETTY_PORT)));

			return connector;
		};
	}

	private static SslConnectionFactory sslConnectionFactory(KeyStore trustStore, KeyStore keyStore,
			char[] keyStorePassword, boolean needClientAuth)
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

		sslContextFactory.setKeyStore(keyStore);
		sslContextFactory.setKeyStorePassword(String.valueOf(keyStorePassword));

		sslContextFactory.setTrustStore(trustStore);
		if (needClientAuth)
			sslContextFactory.setNeedClientAuth(true);
		else
			sslContextFactory.setWantClientAuth(true);

		return new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString());
	}

	private KeyStore readTrustStore(Path trustStorePath)
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

	private KeyStore readKeyStore(Path certificatePath, Path certificateChainPath, Path keyPath, char[] keyPassword,
			char[] keyStorePassword)
	{
		try
		{
			PrivateKey privateKey = PemIo.readPrivateKeyFromPem(keyPath, keyPassword);
			X509Certificate certificate = PemIo.readX509CertificateFromPem(certificatePath);

			List<Certificate> certificateChain = new ArrayList<>();
			certificateChain.add(certificate);

			if (certificateChainPath != null)
			{
				try (InputStream chainStream = Files.newInputStream(certificateChainPath))
				{
					CertificateFactory certificateFactory = CertificateFactory.getInstance("X509");
					certificateChain.addAll(certificateFactory.generateCertificates(chainStream));
				}
			}

			return CertificateHelper.toJksKeyStore(privateKey, certificateChain.toArray(Certificate[]::new),
					UUID.randomUUID().toString(), keyStorePassword);
		}
		catch (NoSuchAlgorithmException | CertificateException | KeyStoreException | IOException | PKCSException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public Optional<KeyStore> getClientTrustStore()
	{
		return getClientTrustCertificatesPath().map(this::readTrustStore);
	}

	@Override
	public Optional<KeyStore> getServerKeyStore(char[] keyStorePassword)
	{
		if (getServerCertificatePath().isEmpty() || getServerCertificatePrivateKeyPath().isEmpty())
			return Optional.empty();

		return Optional.of(readKeyStore(getServerCertificatePath().get(), getServerCertificateChainPath().orElse(null),
				getServerCertificatePrivateKeyPath().get(), getServerCertificatePrivateKeyPassword().orElse(null),
				keyStorePassword));
	}

	@Override
	public Optional<KeyStore> getOidcProviderClientTrustStore()
	{
		return getOidcProviderClientTrustCertificatesPath().map(this::readTrustStore);
	}

	@Override
	public Optional<KeyStore> getOidcProviderClientKeyStore(char[] keyStorePassword)
	{
		if (getOidcProviderClientCertificatePath().isEmpty()
				|| getOidcProviderClientCertificatePrivateKeyPath().isEmpty())
			return Optional.empty();

		return Optional.of(readKeyStore(getOidcProviderClientCertificatePath().get(),
				getServerCertificateChainPath().orElse(null), getOidcProviderClientCertificatePrivateKeyPath().get(),
				getOidcProviderClientCertificatePrivateKeyPassword().orElse(null), keyStorePassword));
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
