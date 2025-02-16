package dev.dsf.fhir.spring.config;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

import dev.dsf.common.config.ProxyConfig;
import dev.dsf.common.config.ProxyConfigImpl;
import dev.dsf.common.documentation.Documentation;
import dev.dsf.common.ui.theme.Theme;
import dev.dsf.tools.docker.secrets.DockerSecretsPropertySourceFactory;

@Configuration
@PropertySource(value = "file:conf/config.properties", encoding = "UTF-8", ignoreResourceNotFound = true)
public class PropertiesConfig implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(PropertiesConfig.class);

	// documentation in dev.dsf.fhir.config.FhirDbMigratorConfig
	@Value("${dev.dsf.fhir.db.url}")
	private String dbUrl;

	// documentation in dev.dsf.fhir.config.FhirDbMigratorConfig
	@Value("${dev.dsf.fhir.db.user.username:fhir_server_user}")
	private String dbUsername;

	// documentation in dev.dsf.fhir.config.FhirDbMigratorConfig
	@Value("${dev.dsf.fhir.db.user.password}")
	private char[] dbPassword;

	// documentation in dev.dsf.fhir.config.FhirDbMigratorConfig
	@Value("${dev.dsf.fhir.db.user.permanent.delete.username:fhir_server_permanent_delete_user}")
	private String dbPermanentDeleteUsername;

	// documentation in dev.dsf.fhir.config.FhirDbMigratorConfig
	@Value("${dev.dsf.fhir.db.user.permanent.delete.password}")
	private char[] dbPermanentDeletePassword;

	@Documentation(required = true, description = "Base address of this DSF FHIR server to read/store fhir resources", example = "https://foo.bar/fhir")
	@Value("${dev.dsf.fhir.server.base.url}")
	private String serverBaseUrl;

	@Documentation(description = "Page size returned by the DSF FHIR server when reading/searching fhir resources")
	@Value("${dev.dsf.fhir.server.page.count:20}")
	private int defaultPageCount;

	@Documentation(description = "UI theme parameter, adds a color indicator to the ui to distinguish `dev`, `test` and `prod` environments im configured; supported values: `dev`, `test` and `prod`")
	@Value("${dev.dsf.fhir.server.ui.theme:}")
	private String uiTheme;

	@Documentation(description = "Role config YAML as defined in [FHIR Server: Access Control](access-control)")
	@Value("${dev.dsf.fhir.server.roleConfig:}")
	private String roleConfig;

	@Documentation(required = true, description = "Local identifier value used in the Allow-List", recommendation = "By convention: The shortest possible FQDN that resolve the homepage of the organization", example = "hospital.com")
	@Value("${dev.dsf.fhir.server.organization.identifier.value}")
	private String organizationIdentifierValue;

	@Documentation(description = "Fhir bundle containing the initial Allow-List, loaded on startup of the DSF FHIR server", recommendation = "Change only if you don't use the provided files from the installation guide, have local changes in the Allow-List or received an Allow-List from another source")
	@Value("${dev.dsf.fhir.server.init.bundle:conf/bundle.xml}")
	private String initBundleFile;

	@Documentation(description = "PEM encoded file with one or more trusted root certificates to validate server certificates for https connections to remote DSF FHIR servers", recommendation = "Use docker secret file to configure", example = "/run/secrets/app_client_trust_certificates.pem")
	@Value("${dev.dsf.fhir.client.trust.server.certificate.cas:ca/server_cert_root_cas.pem}")
	private String webserviceClientCertificateTrustCertificatesFile;

	@Documentation(required = true, description = "PEM encoded file with local client certificate for https connections to remote DSF FHIR servers", recommendation = "Use docker secret file to configure", example = "/run/secrets/app_client_certificate.pem")
	@Value("${dev.dsf.fhir.client.certificate}")
	private String webserviceClientCertificateFile;

	@Documentation(required = true, description = "Private key corresponding to the local client certificate as PEM encoded file. Use *${env_variable}_PASSWORD* or *${env_variable}_PASSWORD_FILE* if private key is encrypted", recommendation = "Use docker secret file to configure", example = "/run/secrets/app_client_certificate_private_key.pem")
	@Value("${dev.dsf.fhir.client.certificate.private.key}")
	private String webserviceClientCertificatePrivateKeyFile;

	@Documentation(description = "Password to decrypt the local client certificate encrypted private key", recommendation = "Use docker secret file to configure using *${env_variable}_FILE*", example = "/run/secrets/app_client_certificate_private_key.pem.password")
	@Value("${dev.dsf.fhir.client.certificate.private.key.password:#{null}}")
	private char[] webserviceClientCertificatePrivateKeyFilePassword;

	@Documentation(description = "Timeout in milliseconds until a reading a resource from a remote DSF FHIR server is aborted", recommendation = "Change default value only if timeout exceptions occur")
	@Value("${dev.dsf.fhir.client.timeout.read:10000}")
	private int webserviceClientReadTimeout;

	@Documentation(description = "Timeout in milliseconds until a connection is established between this DSF FHIR server and a remote DSF FHIR server", recommendation = "Change default value only if timeout exceptions occur")
	@Value("${dev.dsf.fhir.client.timeout.connect:2000}")
	private int webserviceClientConnectTimeout;

	@Documentation(description = "To enable verbose logging of requests to and replies from remote DSF FHIR servers, set to `true`")
	@Value("${dev.dsf.fhir.client.verbose:false}")
	private boolean webserviceClientVerbose;

	@Documentation(description = "To disable static resource caching, set to `false`", recommendation = "Only set to `false` for development")
	@Value("${dev.dsf.fhir.server.static.resource.cache:true}")
	private boolean staticResourceCacheEnabled;

	@Documentation(description = "To enable logging of webservices requests set to `true`", recommendation = "This debug function should only be activated during development; WARNING: Confidential information may be leaked via the debug log!")
	@Value("${dev.dsf.fhir.debug.log.message.webserviceRequest:false}")
	private boolean debugLogMessageWebserviceRequest;

	@Documentation(description = "To enable logging of DB queries set to `true`", recommendation = "This debug function should only be activated during development; WARNING: Confidential information may be leaked via the debug log!")
	@Value("${dev.dsf.fhir.debug.log.message.dbStatement:false}")
	private boolean debugLogMessageDbStatement;

	@Documentation(description = "To enable logging of the currently requesting user set to `true`", recommendation = "This debug function should only be activated during development; WARNING: Confidential information may be leaked via the debug log!")
	@Value("${dev.dsf.fhir.debug.log.message.currentUser:false}")
	private boolean debugLogMessageCurrentUser;

	// documentation in dev.dsf.common.config.AbstractJettyConfig
	@Value("${dev.dsf.server.status.port}")
	private int jettyStatusConnectorPort;

	// documentation in dev.dsf.common.config.AbstractJettyConfig
	@Value("${dev.dsf.proxy.url:#{null}}")
	private String proxyUrl;

	// documentation in dev.dsf.common.config.AbstractJettyConfig
	@Value("${dev.dsf.proxy.username:#{null}}")
	private String proxyUsername;

	// documentation in dev.dsf.common.config.AbstractJettyConfig
	@Value("${dev.dsf.proxy.password:#{null}}")
	private char[] proxyPassword;

	// documentation in dev.dsf.common.config.AbstractJettyConfig
	@Value("#{'${dev.dsf.proxy.noProxy:}'.trim().split('(,[ ]?)|(\\n)')}")
	private List<String> proxyNoProxy;

	// documentation in dev.dsf.common.config.AbstractJettyConfig
	@Value("${dev.dsf.server.auth.oidc.authorization.code.flow:false}")
	private boolean oidcAuthorizationCodeFlowEnabled;

	// documentation in dev.dsf.common.config.AbstractJettyConfig
	@Value("${dev.dsf.server.auth.oidc.bearer.token:false}")
	private boolean oidcBearerTokenEnabled;

	@Bean // static in order to initialize before @Configuration classes
	public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer(
			ConfigurableEnvironment environment)
	{
		new DockerSecretsPropertySourceFactory(environment).readDockerSecretsAndAddPropertiesToEnvironment();

		injectEndpointProperties(environment);

		return new PropertySourcesPlaceholderConfigurer();
	}

	private static void injectEndpointProperties(ConfigurableEnvironment environment)
	{
		try
		{
			URL baseUrl = new URI(environment.getRequiredProperty("dev.dsf.fhir.server.base.url")).toURL();
			if (baseUrl.getHost() == null || baseUrl.getHost().isBlank())
				throw new IllegalStateException("No hostname defined in FHIR server base url");

			Properties properties = new Properties();
			properties.put("dev.dsf.fhir.server.endpoint.address", baseUrl.toString());
			properties.put("dev.dsf.fhir.server.endpoint.identifier.value", baseUrl.getHost());

			environment.getPropertySources().addFirst(new PropertiesPropertySource("enpoint-properties", properties));
		}
		catch (MalformedURLException | IllegalStateException | URISyntaxException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		URL url = new URI(serverBaseUrl).toURL();
		if (!List.of("http", "https").contains(url.getProtocol()))
		{
			logger.warn("Invalid DSF FHIR server base URL: '{}', URL not starting with 'http://' or 'https://'",
					serverBaseUrl);
			throw new IllegalArgumentException("Invalid ServerBaseUrl, not starting with 'http://' or 'https://'");
		}
		else if (serverBaseUrl.endsWith("//"))
		{
			logger.warn("Invalid DSF FHIR server base URL: '{}', URL may not end in '//'", serverBaseUrl);
			throw new IllegalArgumentException("Invalid ServerBaseUrl, ending in //");
		}
		else if (!serverBaseUrl.startsWith("https://"))
		{
			logger.warn("Invalid DSF FHIR server base URL: '{}', URL must start with 'https://'", serverBaseUrl);
			throw new IllegalArgumentException("Invalid ServerBaseUrl, not starting with https://");
		}

		if (serverBaseUrl.endsWith("/"))
			logger.warn("DSF FHIR server base URL: '{}', should not end in '/', removing trailing '/'", serverBaseUrl);
	}

	public String getDbUrl()
	{
		return dbUrl;
	}

	public String getDbUsername()
	{
		return dbUsername;
	}

	public char[] getDbPassword()
	{
		return dbPassword;
	}

	public String getDbPermanentDeleteUsername()
	{
		return dbPermanentDeleteUsername;
	}

	public char[] getDbPermanentDeletePassword()
	{
		return dbPermanentDeletePassword;
	}

	public String getServerBaseUrl()
	{
		return serverBaseUrl.endsWith("/") ? serverBaseUrl.substring(serverBaseUrl.length() - 1) : serverBaseUrl;
	}

	public Theme getUiTheme()
	{
		return Theme.fromString(uiTheme);
	}

	public int getDefaultPageCount()
	{
		return defaultPageCount;
	}

	public String getRoleConfig()
	{
		return roleConfig;
	}

	public String getOrganizationIdentifierValue()
	{
		return organizationIdentifierValue;
	}

	public String getInitBundleFile()
	{
		return initBundleFile;
	}

	public String getWebserviceClientCertificateTrustCertificatesFile()
	{
		return webserviceClientCertificateTrustCertificatesFile;
	}

	public String getWebserviceClientCertificateFile()
	{
		return webserviceClientCertificateFile;
	}

	public String getWebserviceClientCertificatePrivateKeyFile()
	{
		return webserviceClientCertificatePrivateKeyFile;
	}

	public char[] getWebserviceClientCertificatePrivateKeyFilePassword()
	{
		return webserviceClientCertificatePrivateKeyFilePassword;
	}

	public int getWebserviceClientReadTimeout()
	{
		return webserviceClientReadTimeout;
	}

	public int getWebserviceClientConnectTimeout()
	{
		return webserviceClientConnectTimeout;
	}

	public boolean getWebserviceClientVerbose()
	{
		return webserviceClientVerbose;
	}

	public boolean getStaticResourceCacheEnabled()
	{
		return staticResourceCacheEnabled;
	}

	public int getJettyStatusConnectorPort()
	{
		return jettyStatusConnectorPort;
	}

	public boolean getOidcAuthorizationCodeFlowEnabled()
	{
		return oidcAuthorizationCodeFlowEnabled;
	}

	public boolean getOidcBearerTokenEnabled()
	{
		return oidcBearerTokenEnabled;
	}

	@Bean
	public ProxyConfig proxyConfig()
	{
		return new ProxyConfigImpl(proxyUrl, proxyUsername, proxyPassword, proxyNoProxy);
	}

	public boolean getDebugLogMessageWebserviceRequest()
	{
		return debugLogMessageWebserviceRequest;
	}

	public boolean getDebugLogMessageDbStatement()
	{
		return debugLogMessageDbStatement;
	}

	public boolean getDebugLogMessageCurrentUser()
	{
		return debugLogMessageCurrentUser;
	}
}
