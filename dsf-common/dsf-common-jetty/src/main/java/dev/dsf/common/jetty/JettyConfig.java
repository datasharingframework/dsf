package dev.dsf.common.jetty;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.eclipse.jetty.client.HttpProxy;
import org.eclipse.jetty.client.Origin.Address;
import org.eclipse.jetty.client.ProxyConfiguration.Proxy;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;

public interface JettyConfig
{
	String JETTY_PROPERTIES_FILE = "conf/jetty.properties";

	String PROPERTY_JETTY_HOST = "jetty.host";
	String PROPERTY_JETTY_HOST_DEFAULT = "127.0.0.1";
	String PROPERTY_JETTY_PORT = "jetty.port";
	String PROPERTY_JETTY_CONTEXT_PATH = "jetty.context.path";
	String PROPERTY_JETTY_STATUS_HOST = "jetty.status.host";
	String PROPERTY_JETTY_STATUS_HOST_DEFAULT = "127.0.0.1";
	String PROPERTY_JETTY_STATUS_PORT = "jetty.status.port";

	String PROPERTY_JETTY_SERVER_CERTIFICATE = "jetty.server.server.certificate";
	String PROPERTY_JETTY_SERVER_CERTIFICATE_CHAIN = "jetty.server.server.certificate.chain";
	String PROPERTY_JETTY_SERVER_CERTIFICATE_PRIVATE_KEY = "jetty.server.certificate.private.key";
	String PROPERTY_JETTY_SERVER_CERTIFICATE_PRIVATE_KEY_PASSWORD = "jetty.server.certificate.private.key.password";

	String PROPERTY_JETTY_AUTH_CLIENT_TRUST_CERTIFICATES = "jetty.auth.client.trust.certificates";
	String PROPERTY_JETTY_AUTH_CLIENT_CERTIFICATE_HEADER_NAME = "jetty.auth.client.certificate.header.name";
	String PROPERTY_JETTY_AUTH_CLIENT_CERTIFICATE_HEADER_NAME_DEFAULT = "X-ClientCert";

	String PROPERTY_JETTY_AUTH_OIDC_AUTHORIZATION_CODE_FLOW = "jetty.auth.oidc.authorization.code.flow";
	String PROPERTY_JETTY_AUTH_OIDC_BEARER_TOKEN = "jetty.auth.oidc.bearer.token";

	String PROPERTY_JETTY_AUTH_OIDC_PROVIDER_BASE_URL = "jetty.auth.oidc.provider.base.url";
	String PROPERTY_JETTY_AUTH_OIDC_PROVIDER_CLIENT_CONNECT_TIMEOUT = "jetty.auth.oidc.provider.client.connectTimeout";
	String PROPERTY_JETTY_AUTH_OIDC_PROVIDER_CLIENT_IDLE_TIMEOUT = "jetty.auth.oidc.provider.client.idleTimeout";
	String PROPERTY_JETTY_AUTH_OIDC_PROVIDER_CLIENT_TRUST_CERTIFICATES = "jetty.auth.oidc.provider.client.trust.certificates";
	String PROPERTY_JETTY_AUTH_OIDC_PROVIDER_CLIENT_CERTIFICATE = "jetty.auth.oidc.provider.client.certificate";
	String PROPERTY_JETTY_AUTH_OIDC_PROVIDER_CLIENT_CERTIFICATE_PRIVATE_KEY = "jetty.auth.oidc.provider.client.certificate.private.key";
	String PROPERTY_JETTY_AUTH_OIDC_PROVIDER_CLIENT_CERTIFICATE_PRIVATE_KEY_PASSWORD = "jetty.auth.oidc.provider.client.certificate.private.key.password";
	String PROPERTY_JETTY_AUTH_OIDC_PROVIDER_CLIENT_PROXY_URL = "jetty.auth.oidc.provider.client.proxy_url";

	String PROPERTY_JETTY_AUTH_OIDC_CLIENT_ID = "jetty.auth.oidc.client.id";
	String PROPERTY_JETTY_AUTH_OIDC_CLIENT_SECRET = "jetty.auth.oidc.client.secret";

	String PROPERTY_JETTY_AUTH_OIDC_BACK_CHANNEL_LOGOUT = "jetty.auth.oidc.back.channel.logout";
	String PROPERTY_JETTY_AUTH_OIDC_BACK_CHANNEL_LOGOUT_PATH = "jetty.auth.oidc.back.channel.logout.path";
	String PROPERTY_JETTY_AUTH_OIDC_BACK_CHANNEL_LOGOUT_PATH_DEFAULT = "/back-channel-logout";

	String PROPERTY_JETTY_LOG4J_CONFIG = "jetty.log4j.config";
	String PROPERTY_JETTY_LOG4J_CONFIG_DEFAULT = "conf/log4j2.xml";

	static String propertyToEnvironmentVariableName(String propertyName)
	{
		return propertyName.toUpperCase(Locale.ENGLISH).replace('.', '_');
	}

	static Supplier<RuntimeException> propertyNotDefined(String propertyName)
	{
		return () -> new RuntimeException("Property " + propertyName + " not defined (environment variable "
				+ propertyToEnvironmentVariableName(propertyName) + ")");
	}

	static Supplier<RuntimeException> propertyNotDefinedTrue(String propertyName)
	{
		return () -> new RuntimeException("Property " + propertyName + " not defined as 'true' (environment variable "
				+ propertyToEnvironmentVariableName(propertyName) + ")");
	}

	static Supplier<RuntimeException> propertiesNotDefined(String propertyName1, String propertyName2)
	{
		return () -> new RuntimeException("Property " + propertyName1 + " or " + propertyName2
				+ " not defined (environment variables " + propertyToEnvironmentVariableName(propertyName1) + " or "
				+ propertyToEnvironmentVariableName(propertyName2) + ")");
	}

	default Optional<String> getStatusHost()
	{
		return Optional.of(PROPERTY_JETTY_STATUS_HOST_DEFAULT);
	}

	Optional<Integer> getStatusPort();

	default Optional<String> getHost()
	{
		return Optional.of(PROPERTY_JETTY_HOST_DEFAULT);
	}

	Optional<Integer> getPort();

	Optional<String> getContextPath();


	Optional<Path> getServerCertificatePath();

	default Optional<Path> getServerCertificateChainPath()
	{
		return Optional.empty();
	}

	Optional<Path> getServerCertificatePrivateKeyPath();

	Optional<char[]> getServerCertificatePrivateKeyPassword();

	Optional<KeyStore> getServerKeyStore(char[] keyStorePassword);


	Optional<Path> getClientTrustCertificatesPath();

	Optional<KeyStore> getClientTrustStore();

	default Optional<String> getClientCertificateHeaderName()
	{
		return Optional.of(PROPERTY_JETTY_AUTH_CLIENT_CERTIFICATE_HEADER_NAME_DEFAULT);
	}

	default boolean getOidcAuthorizationCodeFlowEndabled()
	{
		return false;
	}

	default boolean getOidcBearerTokenEnabled()
	{
		return false;
	}

	default Optional<String> getOidcProviderBaseUrl()
	{
		return Optional.empty();
	}

	default Optional<Long> getOidcProviderClientConnectTimeout()
	{
		return Optional.empty();
	}

	default Optional<Long> getOidcProviderClientIdleTimeout()
	{
		return Optional.empty();
	}

	default Optional<Path> getOidcProviderClientTrustCertificatesPath()
	{
		return Optional.empty();
	}

	Optional<KeyStore> getOidcProviderClientTrustStore();

	default Optional<Path> getOidcProviderClientCertificatePath()
	{
		return Optional.empty();
	}

	default Optional<Path> getOidcProviderClientCertificatePrivateKeyPath()
	{
		return Optional.empty();
	}

	default Optional<char[]> getOidcProviderClientCertificatePrivateKeyPassword()
	{
		return Optional.empty();
	}

	Optional<KeyStore> getOidcProviderClientKeyStore(char[] keyStorePassword);

	default Optional<URL> getOidcProviderClientProxyUrl()
	{
		return Optional.empty();
	}

	default Optional<Proxy> getOidcClientProxy()
	{
		return getOidcProviderClientProxyUrl().map(clientProxyUrl ->
		{
			Address address = new Address(clientProxyUrl.getHost(),
					clientProxyUrl.getPort() < 0 ? clientProxyUrl.getDefaultPort() : clientProxyUrl.getPort());
			return new HttpProxy(address, "https".equals(clientProxyUrl.getProtocol()));
		});
	}

	default Optional<String> getOidcClientId()
	{
		return Optional.empty();
	}

	default Optional<String> getOidcClientSecret()
	{
		return Optional.empty();
	}

	default boolean getOidcBackChannelLogoutEnabled()
	{
		return false;
	}

	default Optional<String> getOidcBackChannelPath()
	{
		return Optional.of(PROPERTY_JETTY_AUTH_OIDC_BACK_CHANNEL_LOGOUT_PATH_DEFAULT);
	}

	default Optional<OidcConfig> getOidcConfig()
	{
		if (!getOidcAuthorizationCodeFlowEndabled() && !getOidcBearerTokenEnabled() && !getOidcBackChannelLogoutEnabled())
			return Optional.empty();

		Duration clientIdleTimeout = getOidcProviderClientIdleTimeout()
				.map(timeout -> Duration.of(timeout, ChronoUnit.MILLIS)).orElse(null);
		Duration clientConnectTimeout = getOidcProviderClientConnectTimeout()
				.map(timeout -> Duration.of(timeout, ChronoUnit.MILLIS)).orElse(null);

		char[] keyStorePassword = UUID.randomUUID().toString().toCharArray();

		return Optional.of(new OidcConfig(getOidcAuthorizationCodeFlowEndabled(), getOidcBearerTokenEnabled(),
				getOidcProviderBaseUrl().get(), getOidcClientId().orElse(null), getOidcClientSecret().orElse(null),
				getOidcBackChannelLogoutEnabled(), getOidcBackChannelPath().orElse(null), clientIdleTimeout,
				clientConnectTimeout, getOidcProviderClientTrustStore().orElse(null),
				getOidcProviderClientKeyStore(keyStorePassword).orElse(null), keyStorePassword,
				getOidcClientProxy().orElse(null)));
	}

	default Optional<Path> getLog4JConfigPath()
	{
		return Optional.of(Paths.get(PROPERTY_JETTY_LOG4J_CONFIG_DEFAULT));
	}

	Map<String, String> getAllProperties();

	Connector createStatusConnector(Server server);

	BiFunction<JettyConfig, Server, Connector> getConnectorFactory();

	default Connector createConnector(Server server)
	{
		return getConnectorFactory().apply(this, server);
	}
}
