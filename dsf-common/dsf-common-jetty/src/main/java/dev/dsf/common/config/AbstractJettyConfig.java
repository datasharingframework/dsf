package dev.dsf.common.config;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pkcs.PKCSException;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpProxy;
import org.eclipse.jetty.client.Origin.Address;
import org.eclipse.jetty.client.ProxyConfiguration.Proxy;
import org.eclipse.jetty.client.http.HttpClientTransportOverHTTP;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.openid.OpenIdAuthenticator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.ConfigurableEnvironment;

import de.rwh.utils.crypto.CertificateHelper;
import de.rwh.utils.crypto.io.CertificateReader;
import de.rwh.utils.crypto.io.PemIo;
import dev.dsf.common.auth.BackChannelLogoutAuthenticator;
import dev.dsf.common.auth.BearerTokenAuthenticator;
import dev.dsf.common.auth.ClientCertificateAuthenticator;
import dev.dsf.common.auth.DelegatingAuthenticator;
import dev.dsf.common.auth.DsfLoginService;
import dev.dsf.common.auth.DsfOpenIdConfiguration;
import dev.dsf.common.auth.DsfOpenIdLoginService;
import dev.dsf.common.auth.DsfSecurityHandler;
import dev.dsf.common.auth.StatusPortAuthenticator;
import dev.dsf.common.documentation.Documentation;
import dev.dsf.common.jetty.HttpClientWithGetRetry;
import dev.dsf.common.jetty.JettyServer;
import dev.dsf.tools.docker.secrets.DockerSecretsPropertySourceFactory;
import jakarta.servlet.ServletContainerInitializer;

@Configuration
@PropertySource(value = "file:conf/jetty.properties", encoding = "UTF-8", ignoreResourceNotFound = true)
public abstract class AbstractJettyConfig
{
	private static final Logger logger = LoggerFactory.getLogger(AbstractJettyConfig.class);

	private static final BouncyCastleProvider provider = new BouncyCastleProvider();

	@Documentation(description = "Status connector host")
	@Value("${dev.dsf.server.status.host:127.0.0.1}")
	private String statusHost;

	@Documentation(description = "Status connector port, default in docker image: `10000`")
	@Value("${dev.dsf.server.status.port}")
	private int statusPort;

	@Documentation(description = "API connector host, default in docker image: `0.0.0.0`")
	@Value("${dev.dsf.server.api.host:127.0.0.1}")
	private String apiHost;

	@Documentation(description = "API connector port, default in docker image: `8080`")
	@Value("${dev.dsf.server.api.port}")
	private int apiPort;

	@Documentation(description = "Web application context path, default in `bpe` docker image: `/bpe`, default in `fhir` docker image: `/fhir`", recommendation = "Only modify for testing")
	@Value("${dev.dsf.server.context.path}")
	private String contextPath;

	@Documentation(description = "Name of HTTP header with client certificate from reverse proxy")
	@Value("${dev.dsf.server.auth.client.certificate.header:X-ClientCert}")
	private String clientCertificateHeaderName;

	@Documentation(description = "PEM encoded file with one or more trusted full CA chains to validate client certificates for https connections from local and remote clients", recommendation = "Use docker secret file to configure", example = "/run/secrets/app_client_trust_certificates.pem")
	@Value("${dev.dsf.server.auth.trust.client.certificate.cas:ca/client_cert_ca_chains.pem}")
	private String clientCertificateTrustStoreFile;

	@Documentation(description = "Server certificate file for testing", recommendation = "Only specify For testing when terminating TLS in jetty server")
	@Value("${dev.dsf.server.certificate:#{null}}")
	private String serverCertificateFile;

	@Documentation(description = "Server certificate chain file for testing", recommendation = "Only specify For testing when terminating TLS in jetty server")
	@Value("${dev.dsf.server.certificate.chain:#{null}}")
	private String serverCertificateChainFile;

	@Documentation(description = "Server certificate private key file for testing", recommendation = "Only specify For testing when terminating TLS in jetty server")
	@Value("${dev.dsf.server.certificate.key:#{null}}")
	private String serverCertificateKeyFile;

	@Documentation(description = "Server certificate private key file password for testing", recommendation = "Only specify For testing when terminating TLS in jetty server")
	@Value("${dev.dsf.server.certificate.key.password:#{null}}")
	private char[] serverCertificateKeyFilePassword;

	@Documentation(description = "Set to `true` to enable OIDC authorization code flow", recommendation = "Requires *DEV_DSF_SERVER_AUTH_OIDC_PROVIDER_REALM_BASE_URL*, *DEV_DSF_SERVER_AUTH_OIDC_CLIENT_ID* and *DEV_DSF_SERVER_AUTH_OIDC_CLIENT_SECRET* to be specified")
	@Value("${dev.dsf.server.auth.oidc.authorization.code.flow:false}")
	private boolean oidcAuthorizationCodeFlowEnabled;

	@Documentation(description = "Set to `true` to enable OIDC bearer token authentication", recommendation = "Requires *DEV_DSF_SERVER_AUTH_OIDC_PROVIDER_REALM_BASE_URL* to be specified")
	@Value("${dev.dsf.server.auth.oidc.bearer.token:false}")
	private boolean oidcBearerTokenEnabled;

	@Documentation(description = "OIDC provider realm base url", example = "https://keycloak.test.com:8443/realms/example-realm-name")
	@Value("${dev.dsf.server.auth.oidc.provider.realm.base.url:#{null}}")
	private String oidcProviderRealmBaseUrl;

	@Documentation(description = "OIDC provider client connect timeout in milliseconds")
	@Value("${dev.dsf.server.auth.oidc.provider.client.connectTimeout:5000}")
	private long oidcProviderClientConnectTimeout;

	@Documentation(description = "OIDC provider client idle timeout in milliseconds")
	@Value("${dev.dsf.server.auth.oidc.provider.client.idleTimeout:30000}")
	private long oidcProviderClientIdleTimeout;

	@Documentation(description = "PEM encoded file with one or more trusted root certificates to validate server certificates for https connections to the OIDC provider", recommendation = "Use docker secret file to configure", example = "/run/secrets/oidc_provider_trust_certificates.pem")
	@Value("${dev.dsf.server.auth.oidc.provider.client.trust.server.certificate.cas:ca/server_cert_root_cas.pem}")
	private String oidcProviderClientTrustCertificatesFile;

	@Documentation(description = "PEM encoded file with client certificate for https connections to the OIDC provider", recommendation = "Use docker secret file to configure", example = "/run/secrets/oidc_provider_client_certificate.pem")
	@Value("${dev.dsf.server.auth.oidc.provider.client.certificate:#{null}}")
	private String oidcProviderClientCertificateFile;

	@Documentation(description = "Private key corresponding to the client certificate for the OIDC provider as PEM encoded file. Use *${env_variable}_PASSWORD* or *${env_variable}_PASSWORD_FILE* if private key is encrypted", recommendation = "Use docker secret file to configure", example = "/run/secrets/oidc_provider_client_certificate_private_key.pem")
	@Value("${dev.dsf.server.auth.oidc.provider.client.certificate.private.key:#{null}}")
	private String oidcProviderClientCertificatePrivateKeyFile;

	@Documentation(description = "Password to decrypt the client certificate for the OIDC provider encrypted private key", recommendation = "Use docker secret file to configure using *${env_variable}_FILE*", example = "/run/secrets/oidc_provider_client_certificate_private_key.pem.password")
	@Value("${dev.dsf.server.auth.oidc.provider.client.certificate.private.key.password:#{null}}")
	private char[] oidcProviderClientCertificatePrivateKeyPassword;

	@Documentation(description = "OIDC provider client_id, must be specified if *DEV_DSF_SERVER_AUTH_OIDC_AUTHORIZATION_CODE_FLOW* is enabled")
	@Value("${dev.dsf.server.auth.oidc.client.id:#{null}}")
	private String oidcClientId;

	@Documentation(description = "OIDC provider client_secret, must be specified if *DEV_DSF_SERVER_AUTH_OIDC_AUTHORIZATION_CODE_FLOW* is enabled")
	@Value("${dev.dsf.server.auth.oidc.client.secret:#{null}}")
	private String oidcClientSecret;

	@Documentation(description = "Set to `true` to enable OIDC back-channel logout", recommendation = "Requires *DEV_DSF_SERVER_AUTH_OIDC_AUTHORIZATION_CODE_FLOW* to be set to `true` (enabled), *DEV_DSF_SERVER_AUTH_OIDC_CLIENT_ID* and *DEV_DSF_SERVER_AUTH_OIDC_BACK_CHANNEL_LOGOUT_PATH* to be specified")
	@Value("${dev.dsf.server.auth.oidc.back.channel.logout:false}")
	private boolean oidcBackChannelLogoutEnabled;

	@Documentation(description = "Path called by the OIDC provide to request back-channel logout")
	@Value("${dev.dsf.server.auth.oidc.back.channel.logout.path:/back-channel-logout}")
	private String oidcBackChannelPath;

	@Documentation(description = "Forward (http/https) proxy url, use *DEV_DSF_BPE_PROXY_NOPROXY* to list domains that do not require a forward proxy", example = "http://proxy.foo:8080")
	@Value("${dev.dsf.proxy.url:#{null}}")
	private String proxyUrl;

	@Documentation(description = "Forward proxy username", recommendation = "Configure username if proxy requires authentication")
	@Value("${dev.dsf.proxy.username:#{null}}")
	private String proxyUsername;

	@Documentation(description = "Forward Proxy password", recommendation = "Configure password if proxy requires authentication, use docker secret file to configure using *${env_variable}_FILE*")
	@Value("${dev.dsf.proxy.password:#{null}}")
	private char[] proxyPassword;

	@Documentation(description = "Forward proxy no-proxy list, entries will match exactly or agianst (one level) sub-domains, if no port is specified - all ports are matched; comma or space separated list, YAML block scalars supported", example = "foo.bar, test.com:8080")
	@Value("#{'${dev.dsf.proxy.noProxy:}'.trim().split('(,[ ]?)|(\\\\n)')}")
	private List<String> proxyNoProxy;

	protected abstract Function<Server, ServerConnector> apiConnector();

	protected abstract String mavenServerModuleName();

	protected abstract List<Class<? extends ServletContainerInitializer>> servletContainerInitializers();

	protected final Function<Server, ServerConnector> httpApiConnector()
	{
		return JettyServer.httpConnector(apiHost, apiPort, clientCertificateHeaderName);
	}

	protected final Function<Server, ServerConnector> httpsApiConnector()
	{
		final char[] keyStorePassword = UUID.randomUUID().toString().toCharArray();
		return JettyServer.httpsConnector(apiHost, apiPort, clientCertificateTrustStore(),
				serverCertificateKeyStore(keyStorePassword), keyStorePassword,
				!oidcAuthorizationCodeFlowEnabled && !oidcBearerTokenEnabled);
	}

	protected final Function<Server, ServerConnector> statusConnector()
	{
		return JettyServer.statusConnector(statusHost, statusPort);
	}

	@Bean // static in order to initialize before @Configuration classes
	public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer(
			ConfigurableEnvironment environment)
	{
		new DockerSecretsPropertySourceFactory(environment).readDockerSecretsAndAddPropertiesToEnvironment();
		return new PropertySourcesPlaceholderConfigurer();
	}

	@Bean
	public JettyServer jettyServer(ConfigurableEnvironment environment)
	{
		org.springframework.core.env.PropertySource<?> jettyProperties = environment.getPropertySources()
				.get("URL [file:conf/jetty.properties]");
		Map<String, String> initParameters = jettyProperties == null ? Collections.emptyMap()
				: ((Properties) jettyProperties.getSource()).entrySet().stream().collect(
						Collectors.toMap(e -> Objects.toString(e.getKey()), e -> Objects.toString(e.getValue())));

		return new JettyServer(apiConnector(), statusConnector(), mavenServerModuleName(), contextPath,
				servletContainerInitializers(), initParameters, this::configureSecurityHandler);
	}

	private KeyStore serverCertificateKeyStore(char[] keyStorePassword)
	{
		try
		{
			Path serverCertificatePath = checkFile(serverCertificateFile, "Server certificate file");
			Path serverCertificateChainPath = checkOptionalFile(serverCertificateChainFile,
					"Server certificate chain file");
			Path serverCertificateKeyPath = checkFile(serverCertificateKeyFile, "Server certificate key file");

			return readKeyStore(serverCertificatePath, serverCertificateChainPath, serverCertificateKeyPath,
					serverCertificateKeyFilePassword, keyStorePassword);
		}
		catch (CertificateException | KeyStoreException | NoSuchAlgorithmException | IOException | PKCSException e)
		{
			throw new RuntimeException(e);
		}
	}

	private KeyStore readKeyStore(Path certificatePath, Path certificateChainPath, Path keyPath, char[] keyPassword,
			char[] keyStorePassword)
			throws IOException, PKCSException, CertificateException, KeyStoreException, NoSuchAlgorithmException
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

	private KeyStore clientCertificateTrustStore()
	{
		try
		{
			Path clientCertificateTrustStorePath = checkFile(clientCertificateTrustStoreFile,
					"Client certificate trust store file");

			return CertificateReader.allFromCer(clientCertificateTrustStorePath);
		}
		catch (NoSuchAlgorithmException | CertificateException | KeyStoreException | IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	private Path checkFile(String file, String fileDescription) throws IOException
	{
		if (file == null || file.isBlank())
			throw new RuntimeException(fileDescription + " not defined");

		Path path = Paths.get(file);

		if (!Files.isReadable(path))
			throw new IOException(fileDescription + " '" + path.toAbsolutePath().toString() + "' not readable");

		return path;
	}

	private Path checkOptionalFile(String file, String fileDescription) throws IOException
	{
		if (file == null || file.isBlank())
			return null;
		else
		{
			Path path = Paths.get(file);

			if (!Files.isReadable(path))
				throw new IOException(fileDescription + " '" + path.toAbsolutePath().toString() + "' not readable");

			return path;
		}
	}

	private void configureSecurityHandler(WebAppContext webAppContext, Supplier<Integer> statusPortSupplier)
	{
		SessionHandler sessionHandler = webAppContext.getSessionHandler();
		DsfLoginService dsfLoginService = new DsfLoginService(webAppContext);

		DsfOpenIdConfiguration openIdConfiguration = null;
		OpenIdAuthenticator openIdAuthenticator = null;
		DsfOpenIdLoginService openIdLoginService = null;
		BearerTokenAuthenticator bearerTokenAuthenticator = null;
		BackChannelLogoutAuthenticator backChannelLogoutAuthenticator = null;

		if (oidcAuthorizationCodeFlowEnabled || oidcBearerTokenEnabled || oidcBackChannelLogoutEnabled)
		{
			openIdConfiguration = new DsfOpenIdConfiguration(oidcProviderRealmBaseUrl, oidcClientId, oidcClientSecret,
					createOidcClient(), oidcBackChannelLogoutEnabled, oidcBearerTokenEnabled);

			if (oidcAuthorizationCodeFlowEnabled)
			{
				if (oidcProviderRealmBaseUrl == null)
					throw propertyNotDefined("dev.dsf.server.auth.oidc.provider.realm.base.url").get();
				else if (oidcClientId == null)
					throw propertyNotDefined("dev.dsf.server.auth.oidc.client.id").get();
				else if (oidcClientSecret == null)
					throw propertyNotDefined("dev.dsf.server.auth.oidc.client.secret").get();
				else
				{
					openIdAuthenticator = new OpenIdAuthenticator(openIdConfiguration);
					logger.info("OIDC authorization code flow enabled");
				}
			}

			if (oidcBearerTokenEnabled)
			{
				if (oidcProviderRealmBaseUrl == null)
					throw propertyNotDefined("dev.dsf.server.auth.oidc.provider.realm.base.url").get();
				else
				{
					bearerTokenAuthenticator = new BearerTokenAuthenticator(openIdConfiguration);
					logger.info("OIDC bearer token enabled");
				}
			}

			if (oidcBackChannelLogoutEnabled)
			{
				if (!oidcAuthorizationCodeFlowEnabled)
					throw propertyNotDefinedTrue("dev.dsf.server.auth.oidc.authorization.code.flow").get();
				else if (oidcClientId == null)
					throw propertyNotDefined("dev.dsf.server.auth.oidc.client.id").get();
				else if (oidcBackChannelPath == null)
					throw propertyNotDefined("dev.dsf.server.auth.oidc.back.channel.logout.path").get();
				else
				{
					backChannelLogoutAuthenticator = new BackChannelLogoutAuthenticator(openIdConfiguration,
							oidcBackChannelPath);
					logger.info("OIDC back-channel logout enabled");
				}
			}

			openIdLoginService = new DsfOpenIdLoginService(openIdConfiguration, dsfLoginService);
		}

		StatusPortAuthenticator statusPortAuthenticator = new StatusPortAuthenticator(statusPortSupplier);
		ClientCertificateAuthenticator clientCertificateAuthenticator = new ClientCertificateAuthenticator(
				clientCertificateTrustStore());
		DelegatingAuthenticator delegatingAuthenticator = new DelegatingAuthenticator(sessionHandler,
				statusPortAuthenticator, clientCertificateAuthenticator, bearerTokenAuthenticator, openIdAuthenticator,
				openIdLoginService, backChannelLogoutAuthenticator);

		SecurityHandler securityHandler = new DsfSecurityHandler(dsfLoginService, delegatingAuthenticator,
				openIdConfiguration);
		securityHandler.setSessionRenewedOnAuthentication(true);

		webAppContext.setSecurityHandler(securityHandler);

		sessionHandler.addEventListener(backChannelLogoutAuthenticator);
	}

	private Supplier<RuntimeException> propertyNotDefined(String propertyName)
	{
		return () -> new RuntimeException("Property " + propertyName + " not defined (environment variable "
				+ propertyToEnvironmentVariableName(propertyName) + ")");
	}

	private Supplier<RuntimeException> propertyNotDefinedTrue(String propertyName)
	{
		return () -> new RuntimeException("Property " + propertyName + " not defined as 'true' (environment variable "
				+ propertyToEnvironmentVariableName(propertyName) + ")");
	}

	private String propertyToEnvironmentVariableName(String propertyName)
	{
		return propertyName.toUpperCase(Locale.ENGLISH).replace('.', '_');
	}

	private Duration oidcClientIdleTimeout()
	{
		return oidcProviderClientIdleTimeout >= 0 ? Duration.of(oidcProviderClientIdleTimeout, ChronoUnit.MILLIS)
				: null;
	}

	private Duration oidcClientConnectTimeout()
	{
		return oidcProviderClientConnectTimeout >= 0 ? Duration.of(oidcProviderClientConnectTimeout, ChronoUnit.MILLIS)
				: null;
	}

	private Proxy oidcClientProxy()
	{
		ProxyConfig config = new ProxyConfigImpl(proxyUrl, proxyUsername, proxyPassword, proxyNoProxy);
		if (config.getUrl() != null && !config.isNoProxyUrl(oidcProviderRealmBaseUrl))
		{
			try
			{
				URL proxyUrl = new URL(config.getUrl());

				Address address = new Address(proxyUrl.getHost(),
						proxyUrl.getPort() < 0 ? proxyUrl.getDefaultPort() : proxyUrl.getPort());
				return new HttpProxy(address, "https".equals(proxyUrl.getProtocol()));
			}
			catch (MalformedURLException e)
			{
				throw new RuntimeException(e);
			}
		}
		else
			return null;
	}

	private HttpClient createOidcClient()
	{
		char[] oidcClientKeyStorePassword = UUID.randomUUID().toString().toCharArray();
		KeyStore oidcProviderClientKeyStore = oidcProviderClientKeyStore(oidcClientKeyStorePassword);

		SslContextFactory.Client sslContextFactory = new SslContextFactory.Client(false);
		if (oidcProviderClientTrustStore() != null)
			sslContextFactory.setTrustStore(oidcProviderClientTrustStore());
		if (oidcProviderClientKeyStore != null)
		{
			sslContextFactory.setKeyStore(oidcProviderClientKeyStore);
			sslContextFactory.setKeyStorePassword(String.valueOf(oidcClientKeyStorePassword));
		}

		ClientConnector connector = new ClientConnector();
		connector.setSslContextFactory(sslContextFactory);
		if (oidcClientIdleTimeout() != null)
			connector.setIdleTimeout(oidcClientIdleTimeout());
		if (oidcClientConnectTimeout() != null)
			connector.setConnectTimeout(oidcClientConnectTimeout());

		HttpClient httpClient = new HttpClientWithGetRetry(new HttpClientTransportOverHTTP(connector), 5);
		if (oidcClientProxy() != null)
			httpClient.getProxyConfiguration().addProxy(oidcClientProxy());

		return httpClient;
	}

	private KeyStore oidcProviderClientTrustStore()
	{
		try
		{
			Path clientCertificateTrustStorePath = checkOptionalFile(oidcProviderClientTrustCertificatesFile,
					"OIDC provider client certificate trust store file");

			return clientCertificateTrustStorePath == null ? null
					: CertificateReader.allFromCer(clientCertificateTrustStorePath);
		}
		catch (NoSuchAlgorithmException | CertificateException | KeyStoreException | IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	private KeyStore oidcProviderClientKeyStore(char[] keyStorePassword)
	{
		try
		{
			Path certificatePath = checkOptionalFile(oidcProviderClientCertificateFile,
					"OIDC provider client certificate file");
			Path privateKeyPath = checkOptionalFile(oidcProviderClientCertificatePrivateKeyFile,
					"OIDC provider client certificate key file");

			if (certificatePath == null && privateKeyPath != null)
				throw new IOException(
						"OIDC provider client certificate key file defined but OIDC provider client certificate file not defined");
			else if (certificatePath != null && privateKeyPath == null)
				throw new IOException(
						"OIDC provider client certificate file defined but OIDC provider client certificate key file not defined");
			else if (certificatePath != null && privateKeyPath != null)
			{
				X509Certificate certificate = PemIo.readX509CertificateFromPem(certificatePath);
				PrivateKey privateKey = PemIo.readPrivateKeyFromPem(provider, privateKeyPath,
						oidcProviderClientCertificatePrivateKeyPassword);

				String subjectCommonName = CertificateHelper.getSubjectCommonName(certificate);
				return CertificateHelper.toJksKeyStore(privateKey, new Certificate[] { certificate }, subjectCommonName,
						keyStorePassword);
			}
			else
				return null;
		}
		catch (CertificateException | KeyStoreException | NoSuchAlgorithmException | IOException | PKCSException e)
		{
			throw new RuntimeException(e);
		}
	}
}
