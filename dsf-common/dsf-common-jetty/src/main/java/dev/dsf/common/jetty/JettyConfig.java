package dev.dsf.common.jetty;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;

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

	String PROPERTY_JETTY_CLIENT_TRUSTSTORE_PEM = "jetty.client.truststore.pem";
	String PROPERTY_JETTY_SERVER_TRUSTSTORE_PEM = "jetty.server.truststore.pem";
	String PROPERTY_JETTY_SERVER_KEYSTORE_P12 = "jetty.server.keystore.p12";
	String PROPERTY_JETTY_SERVER_KEYSTORE_PASSWORD = "jetty.server.keystore.password";

	String PROPERTY_JETTY_CLIENT_CERT_HEADER = "jetty.clientcertheader";
	String PROPERTY_JETTY_CLIENT_CERT_HEADER_DEFAULT = "X-ClientCert";

	String PROPERTY_JETTY_LOG4J_CONFIG = "jetty.log4j.config";
	String PROPERTY_JETTY_LOG4J_CONFIG_DEFAULT = "conf/log4j2.xml";

	static Supplier<RuntimeException> propertyNotDefined(String propertyName)
	{
		return () -> new RuntimeException("Property " + propertyName + " not defined");
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

	Optional<Path> getClientTrustStorePath();

	Optional<KeyStore> getClientTrustStore();

	Optional<Path> getServerTrustStorePath();

	Optional<KeyStore> getServerTrustStore();

	Optional<Path> getServerKeyStorePath();

	Optional<char[]> getServerKeyStorePassword();

	Optional<KeyStore> getServerKeyStore();

	default Optional<String> getClientCertHeaderName()
	{
		return Optional.of(PROPERTY_JETTY_CLIENT_CERT_HEADER_DEFAULT);
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
