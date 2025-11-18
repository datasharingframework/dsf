/*
 * Copyright 2018-2025 Heilbronn University of Applied Sciences
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.dsf.common.config;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpProxy;
import org.eclipse.jetty.client.Origin.Address;
import org.eclipse.jetty.client.ProxyConfiguration.Proxy;
import org.eclipse.jetty.client.transport.HttpClientTransportOverHTTP;
import org.eclipse.jetty.ee10.servlet.SessionHandler;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.openid.OpenIdAuthenticator;
import org.eclipse.jetty.security.openid.OpenIdConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.ConfigurableEnvironment;

import de.hsheilbronn.mi.utils.crypto.cert.CertificateFormatter.X500PrincipalFormat;
import de.hsheilbronn.mi.utils.crypto.cert.CertificateValidator;
import de.hsheilbronn.mi.utils.crypto.io.PemReader;
import de.hsheilbronn.mi.utils.crypto.keypair.KeyPairValidator;
import de.hsheilbronn.mi.utils.crypto.keystore.KeyStoreCreator;
import de.hsheilbronn.mi.utils.crypto.keystore.KeyStoreFormatter;
import dev.dsf.common.auth.BackChannelLogoutAuthenticator;
import dev.dsf.common.auth.BearerTokenAuthenticator;
import dev.dsf.common.auth.ClientCertificateAuthenticator;
import dev.dsf.common.auth.DelegatingAuthenticator;
import dev.dsf.common.auth.DsfLoginService;
import dev.dsf.common.auth.DsfOpenIdLoginService;
import dev.dsf.common.auth.DsfSecurityHandler;
import dev.dsf.common.auth.StatusPortAuthenticator;
import dev.dsf.common.buildinfo.BuildInfoReader;
import dev.dsf.common.buildinfo.BuildInfoReaderImpl;
import dev.dsf.common.docker.secrets.DockerSecretsPropertySourceFactory;
import dev.dsf.common.documentation.Documentation;
import dev.dsf.common.jetty.HttpClientWithGetRetry;
import dev.dsf.common.jetty.JettyServer;
import dev.dsf.common.oidc.BaseOidcClient;
import dev.dsf.common.oidc.BaseOidcClientJersey;
import dev.dsf.common.oidc.BaseOidcClientWithCache;
import dev.dsf.common.oidc.JwtVerifier;
import dev.dsf.common.oidc.JwtVerifierImpl;
import jakarta.servlet.ServletContainerInitializer;

@Configuration
@PropertySource(value = "file:conf/jetty.properties", encoding = "UTF-8", ignoreResourceNotFound = true)
public abstract class AbstractJettyConfig extends AbstractCertificateConfig
{
	private static final Logger logger = LoggerFactory.getLogger(AbstractJettyConfig.class);

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

	@Documentation(description = "Folder with PEM encoded files (*.crt, *.pem) or a single PEM encoded file with one or more trusted full CA chains to validate client certificates for https connections from local and remote clients", recommendation = "Add file to default folder via bind mount or use docker secret file to configure", example = "/run/secrets/app_client_trust_certificates.pem")
	@Value("${dev.dsf.server.auth.trust.client.certificate.cas:ca/client_ca_chains}")
	private String clientCertificateTrustStoreFileOrFolder;

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

	@Documentation(description = "Set to `true` to enable OIDC authorization code flow", recommendation = "Requires *DEV_DSF_SERVER_AUTH_OIDC_PROVIDER_REALM_BASE_URL*, *DEV_DSF_SERVER_AUTH_OIDC_CLIENT_ID* and *DEV_DSF_SERVER_AUTH_OIDC_CLIENT_SECRET* or *DEV_DSF_SERVER_AUTH_OIDC_CLIENT_SECRET_FILE* to be specified")
	@Value("${dev.dsf.server.auth.oidc.authorization.code.flow:false}")
	private boolean oidcAuthorizationCodeFlowEnabled;

	@Documentation(description = "Set to `true` to enable OIDC bearer token authentication", recommendation = "Requires *DEV_DSF_SERVER_AUTH_OIDC_PROVIDER_REALM_BASE_URL* to be specified")
	@Value("${dev.dsf.server.auth.oidc.bearer.token:false}")
	private boolean oidcBearerTokenEnabled;

	@Documentation(description = "Audience (aud) value to verify before accepting OIDC bearer tokens, uses value from `DEV_DSF_SERVER_AUTH_OIDC_CLIENT_ID` by default, set blank string e.g. `''` to disable", recommendation = "Requires *DEV_DSF_SERVER_AUTH_OIDC_PROVIDER_REALM_BASE_URL* to be specified and *DEV_DSF_SERVER_AUTH_OIDC_BEARER_TOKEN* set tor `true`")
	@Value("${dev.dsf.server.auth.oidc.bearer.token.audience:#{null}}")
	private String oidcBearerTokenAudience;

	@Documentation(description = "OIDC provider realm base url", example = "https://keycloak.test.com:8443/realms/example-realm-name")
	@Value("${dev.dsf.server.auth.oidc.provider.realm.base.url:#{null}}")
	private String oidcProviderRealmBaseUrl;

	@Documentation(description = "OIDC provider discovery path")
	@Value("${dev.dsf.server.auth.oidc.provider.discovery.path:/.well-known/openid-configuration}")
	private String oidcProviderDiscoveryPath;

	@Documentation(description = "OIDC provider client connect timeout")
	@Value("${dev.dsf.server.auth.oidc.provider.client.timeout.connect:PT5S}")
	private String oidcProviderClientTimeoutConnect;

	@Documentation(description = "OIDC provider client read timeout")
	@Value("${dev.dsf.server.auth.oidc.provider.client.timeout.read:PT30S}")
	private String oidcProviderClientTimeoutRead;

	@Documentation(description = "Folder with PEM encoded files (*.crt, *.pem) or a single PEM encoded file with one or more trusted root certificates to validate server certificates for https connections to the OIDC provider", recommendation = "Add file to default folder via bind mount or use docker secret file to configure", example = "/run/secrets/oidc_provider_trust_certificates.pem")
	@Value("${dev.dsf.server.auth.oidc.provider.client.trust.server.certificate.cas:ca/server_root_cas}")
	private String oidcProviderClientTrustCertificatesFileOrFolder;

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

	@Documentation(description = "Forward proxy no-proxy list, entries will match exactly or against (one level) sub-domains, if no port is specified - all ports are matched; comma or space separated list, YAML block scalars supported", example = "foo.bar, test.com:8080")
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

		KeyStore serverCertificateKeyStore = serverCertificateKeyStore(keyStorePassword);

		return JettyServer.httpsConnector(apiHost, apiPort, clientCertificateTrustStore(), serverCertificateKeyStore,
				keyStorePassword, !oidcAuthorizationCodeFlowEnabled && !oidcBearerTokenEnabled);
	}

	private KeyStore serverCertificateKeyStore(char[] keyStorePassword)
	{
		try
		{
			Path certificatePath = checkFile(serverCertificateFile, "dev.dsf.server.certificate");
			Path certificateChainPath = checkOptionalFile(serverCertificateChainFile,
					"dev.dsf.server.certificate.chain");
			Path keyPath = checkFile(serverCertificateKeyFile, "dev.dsf.server.certificate.key");

			PrivateKey privateKey = PemReader.readPrivateKey(keyPath, serverCertificateKeyFilePassword);

			List<X509Certificate> certificates = new ArrayList<>();
			certificates.add(PemReader.readCertificate(certificatePath));
			certificates.addAll(PemReader.readCertificates(certificateChainPath));

			if (!CertificateValidator.isServerCertificate(certificates.get(0)))
				throw new IOException(errorMessage("dev.dsf.server.certificate", "Certificate from '"
						+ certificatePath.normalize().toAbsolutePath().toString() + "' not a server certificate"));
			else if (!KeyPairValidator.matches(privateKey, certificates.get(0).getPublicKey()))
				throw new IOException(errorMessage("dev.dsf.server.certificate", "dev.dsf.server.certificate.key",
						"Private-key at '" + keyPath.normalize().toAbsolutePath().toString()
								+ "' not matching Public-key from certificate at '"
								+ certificatePath.normalize().toAbsolutePath().toString() + "'"));

			return KeyStoreCreator.jksForPrivateKeyAndCertificateChain(privateKey, keyStorePassword, certificates);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
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
		Map<String, String> initParameters = jettyProperties == null ? Map.of()
				: ((Properties) jettyProperties.getSource()).entrySet().stream().collect(
						Collectors.toMap(e -> Objects.toString(e.getKey()), e -> Objects.toString(e.getValue())));

		return new JettyServer(apiConnector(), statusConnector(), mavenServerModuleName(), contextPath,
				servletContainerInitializers(), initParameters, this::configureSecurityHandler);
	}

	@Bean
	private KeyStore clientCertificateTrustStore()
	{
		return createTrustStore(clientCertificateTrustStoreFileOrFolder,
				"dev.dsf.server.auth.trust.client.certificate.cas");
	}

	private void configureSecurityHandler(WebAppContext webAppContext, Supplier<Integer> statusPortSupplier)
	{
		SessionHandler sessionHandler = webAppContext.getSessionHandler();
		DsfLoginService dsfLoginService = new DsfLoginService(webAppContext);

		OpenIdConfiguration openIdConfiguration = null;
		OpenIdAuthenticator openIdAuthenticator = null;
		DsfOpenIdLoginService openIdLoginService = null;
		BearerTokenAuthenticator bearerTokenAuthenticator = null;
		BackChannelLogoutAuthenticator backChannelLogoutAuthenticator = null;

		if (oidcAuthorizationCodeFlowEnabled || oidcBearerTokenEnabled || oidcBackChannelLogoutEnabled)
		{
			openIdConfiguration = new OpenIdConfiguration.Builder(oidcProviderRealmBaseUrl, oidcClientId,
					oidcClientSecret).httpClient(createOidcClient()).build();

			if (oidcAuthorizationCodeFlowEnabled)
			{
				if (oidcProviderRealmBaseUrl == null)
					throw propertyNotDefined("dev.dsf.server.auth.oidc.provider.realm.base.url");
				else if (oidcClientId == null)
					throw propertyNotDefined("dev.dsf.server.auth.oidc.client.id");
				else if (oidcClientSecret == null)
					throw propertyNotDefined("dev.dsf.server.auth.oidc.client.secret");
				else
				{
					openIdAuthenticator = new OpenIdAuthenticator(openIdConfiguration);
					logger.info("OIDC authorization code flow enabled");
				}
			}

			if (oidcBearerTokenEnabled)
			{
				if (oidcProviderRealmBaseUrl == null)
					throw propertyNotDefined("dev.dsf.server.auth.oidc.provider.realm.base.url");
				else
				{
					bearerTokenAuthenticator = new BearerTokenAuthenticator(jwtVerifier());
					logger.info("OIDC bearer token enabled");
				}
			}

			if (oidcBackChannelLogoutEnabled)
			{
				if (!oidcAuthorizationCodeFlowEnabled)
					throw propertyNotDefined("dev.dsf.server.auth.oidc.authorization.code.flow");
				else if (oidcClientId == null)
					throw propertyNotDefined("dev.dsf.server.auth.oidc.client.id");
				else if (oidcBackChannelPath == null)
					throw propertyNotDefined("dev.dsf.server.auth.oidc.back.channel.logout.path");
				else
				{
					backChannelLogoutAuthenticator = new BackChannelLogoutAuthenticator(jwtVerifier(),
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

	@Bean
	@Lazy
	public JwtVerifier jwtVerifier()
	{
		return new JwtVerifierImpl(oidcProviderRealmBaseUrl, oidcClientId, oidcBearerTokenAudience, baseOidcClient());
	}

	@Bean
	@Lazy
	public BaseOidcClient baseOidcClient()
	{
		String proxyUrl = null, proxyUsername = null;
		char[] proxyPassword = null;

		ProxyConfig proxy = proxyConfig();
		if (proxy.isEnabled(oidcProviderRealmBaseUrl))
		{
			proxyUrl = proxy.getUrl();
			proxyUsername = proxy.getUsername();
			proxyPassword = proxy.getPassword();
		}

		KeyStore trustStore = oidcProviderClientTrustStore();
		char[] keyStorePassword = UUID.randomUUID().toString().toCharArray();
		KeyStore keyStore = oidcProviderClientKeyStore(keyStorePassword);

		return new BaseOidcClientWithCache(new BaseOidcClientJersey(oidcProviderRealmBaseUrl, oidcProviderDiscoveryPath,
				trustStore, keyStore, keyStore == null ? null : keyStorePassword, proxyUrl, proxyUsername,
				proxyPassword, buildInfoReader().getUserAgentValue(), oidcProviderClientTimeoutConnect(),
				oidcProviderClientTimeoutRead(), false));
	}

	@Bean
	@Lazy
	public Duration oidcProviderClientTimeoutRead()
	{
		return Duration.parse(oidcProviderClientTimeoutRead);
	}

	@Bean
	@Lazy
	public Duration oidcProviderClientTimeoutConnect()
	{
		return Duration.parse(oidcProviderClientTimeoutConnect);
	}

	private Proxy oidcClientProxy()
	{
		ProxyConfig config = proxyConfig();
		if (config.isEnabled(oidcProviderRealmBaseUrl))
		{
			try
			{
				URL proxyUrl = new URI(config.getUrl()).toURL();

				Address address = new Address(proxyUrl.getHost(),
						proxyUrl.getPort() < 0 ? proxyUrl.getDefaultPort() : proxyUrl.getPort());
				return new HttpProxy(address, "https".equals(proxyUrl.getProtocol()));
			}
			catch (MalformedURLException | URISyntaxException e)
			{
				throw new RuntimeException(e);
			}
		}
		else
			return null;
	}

	@Bean
	@Lazy
	public ProxyConfig proxyConfig()
	{
		return new ProxyConfigImpl(proxyUrl, proxyUsername, proxyPassword, proxyNoProxy);
	}

	@Bean
	@Lazy
	public KeyStore oidcProviderClientTrustStore()
	{
		return createOptionalTrustStore(oidcProviderClientTrustCertificatesFileOrFolder,
				"dev.dsf.server.auth.oidc.provider.client.trust.server.certificate.cas");
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public KeyStore oidcProviderClientKeyStore(char[] oidcClientKeyStorePassword)
	{
		return createOptionalClientKeyStore(oidcProviderClientCertificateFile,
				oidcProviderClientCertificatePrivateKeyFile, oidcProviderClientCertificatePrivateKeyPassword,
				oidcClientKeyStorePassword, "dev.dsf.server.auth.oidc.provider.client.certificate",
				"dev.dsf.server.auth.oidc.provider.client.certificate.private.key");
	}

	private HttpClient createOidcClient()
	{
		char[] oidcClientKeyStorePassword = UUID.randomUUID().toString().toCharArray();

		KeyStore oidcProviderClientTrustStore = oidcProviderClientTrustStore();
		KeyStore oidcProviderClientKeyStore = oidcProviderClientKeyStore(oidcClientKeyStorePassword);

		SslContextFactory.Client sslContextFactory = new SslContextFactory.Client(false);
		if (oidcProviderClientTrustStore != null)
		{
			sslContextFactory.setTrustStore(oidcProviderClientTrustStore);
			logger.info("Using trust-store with {} to validate OIDC provider server certificate",
					KeyStoreFormatter
							.toSubjectsFromCertificates(oidcProviderClientTrustStore, X500PrincipalFormat.RFC1779)
							.values().stream().collect(Collectors.joining("; ", "[", "]")));
		}
		if (oidcProviderClientKeyStore != null)
		{
			sslContextFactory.setKeyStore(oidcProviderClientKeyStore);
			sslContextFactory.setKeyStorePassword(String.valueOf(oidcClientKeyStorePassword));
		}

		ClientConnector connector = new ClientConnector();
		connector.setSslContextFactory(sslContextFactory);
		connector.setIdleTimeout(oidcProviderClientTimeoutRead());
		connector.setConnectTimeout(oidcProviderClientTimeoutConnect());

		HttpClient httpClient = new HttpClientWithGetRetry(new HttpClientTransportOverHTTP(connector), 5);
		if (oidcClientProxy() != null)
			httpClient.getProxyConfiguration().addProxy(oidcClientProxy());

		httpClient.setUserAgentField(new HttpField(HttpHeader.USER_AGENT, buildInfoReader().getUserAgentValue()));

		return httpClient;
	}

	@Bean
	public BuildInfoReader buildInfoReader()
	{
		return new BuildInfoReaderImpl();
	}

	@EventListener({ ContextRefreshedEvent.class })
	public void onContextRefreshedEvent()
	{
		buildInfoReader().logSystemDefaultTimezone();
		buildInfoReader().logBuildInfo();
	}
}
