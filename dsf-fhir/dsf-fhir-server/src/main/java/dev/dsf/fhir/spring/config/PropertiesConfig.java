package dev.dsf.fhir.spring.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.ConfigurableEnvironment;

import dev.dsf.common.config.ProxyConfig;
import dev.dsf.common.config.ProxyConfigImpl;
import dev.dsf.common.documentation.Documentation;
import dev.dsf.tools.docker.secrets.DockerSecretsPropertySourceFactory;

@Configuration
@PropertySource(value = "file:conf/config.properties", encoding = "UTF-8", ignoreResourceNotFound = true)
public class PropertiesConfig
{
	@Documentation(required = true, description = "The address of the database used for the DSF FHIR server", recommendation = "Change only if you don't use the provided docker-compose from the installation guide or made changes to the database settings/networking in the docker-compose", example = "jdbc:postgresql://db/fhir")
	@Value("${dev.dsf.fhir.db.url}")
	private String dbUrl;

	@Documentation(description = "The user name to access the database from the DSF FHIR server")
	@Value("${dev.dsf.fhir.db.user.username:fhir_server_user}")
	private String dbUsername;

	@Documentation(required = true, description = "The password to access the database from the DSF FHIR server", recommendation = "Use docker secret file to configure using *${env_variable}_FILE*", example = "/run/secrets/db_user.password")
	@Value("${dev.dsf.fhir.db.user.password}")
	private char[] dbPassword;

	@Documentation(description = "The user name to access the database from the DSF FHIR server for permanent deletes", recommendation = "Use a different user then *DEV_DSF_FHIR_DB_USER_USERNAME*")
	@Value("${dev.dsf.fhir.db.user.permanent.delete.username:fhir_server_permanent_delete_user}")
	private String dbPermanentDeleteUsername;

	@Documentation(required = true, description = "The password to access the database from the DSF FHIR server for permanent deletes", recommendation = "Use docker secret file to configure using *${env_variable}_FILE*", example = "/run/secrets/db_user_permanent_delete.password")
	@Value("${dev.dsf.fhir.db.user.permanent.delete.password}")
	private char[] dbPermanentDeletePassword;

	@Documentation(required = true, description = "The base address of this DSF FHIR server to read/store fhir resources", example = "https://foo.bar/fhir")
	@Value("${dev.dsf.fhir.server.base.url}")
	private String serverBaseUrl;

	@Documentation(description = "The page size returned by the DSF FHIR server when reading/searching fhir resources")
	@Value("${dev.dsf.fhir.server.page.count:20}")
	private int defaultPageCount;

	@Documentation(required = true, description = "Role config YAML")
	@Value("${dev.dsf.fhir.server.roleConfig}")
	private String roleConfig;

	@Documentation(required = true, description = "The local identifier value used in the Allow-List", recommendation = "By convention: The shortest possible FQDN that resolve the homepage of the organization", example = "hospital.com")
	@Value("${dev.dsf.fhir.server.organization.identifier.value}")
	private String organizationIdentifierValue;

	@Documentation(description = "The fhir bundle containing the initial Allow-List, loaded on startup of the DSF FHIR server", recommendation = "Change only if you don't use the provided files from the installation guide, have local changes in the Allow-List or received an Allow-List from another source")
	@Value("${dev.dsf.fhir.server.init.bundle:conf/bundle.xml}")
	private String initBundleFile;

	@Documentation(required = true, description = "PEM encoded file with one or more trusted root certificates to validate server certificates for https connections to remote DSF FHIR servers", recommendation = "Use docker secret file to configure", example = "/run/secrets/app_client_trust_certificates.pem")
	@Value("${dev.dsf.fhir.client.trust.certificates}")
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

	@Documentation(description = "The timeout in milliseconds until a reading a resource from a remote DSF FHIR server is aborted", recommendation = "Change default value only if timeout exceptions occur")
	@Value("${dev.dsf.fhir.client.timeout.read:10000}")
	private int webserviceClientReadTimeout;

	@Documentation(description = "The timeout in milliseconds until a connection is established between this DSF FHIR server and a remote DSF FHIR server", recommendation = "Change default value only if timeout exceptions occur")
	@Value("${dev.dsf.fhir.client.timeout.connect:2000}")
	private int webserviceClientConnectTimeout;

	@Documentation(description = "To enable verbose logging of requests to and replies from remote DSF FHIR servers, set to `true`")
	@Value("${dev.dsf.fhir.client.verbose:false}")
	private boolean webserviceClientVerbose;

	@Documentation(description = "To disable static resource caching, set to `false`", recommendation = "Only set to `false` for development")
	@Value("${dev.dsf.fhir.server.static.resource.cache:true}")
	private boolean staticResourceCacheEnabled;

	@Value("${jetty.status.port}")
	private int jettyStatusConnectorPort;

	@Documentation(description = "Forward (http/https) proxy url, use *DEV_DSF_FHIR_PROXY_NOPROXY* to list domains that do not require a forward proxy", example = "http://proxy.foo:8080")
	@Value("${dev.dsf.fhir.proxy.url:#{null}}")
	private String proxyUrl;

	@Documentation(description = "Forward proxy username", recommendation = "Configure username if proxy requires authentication")
	@Value("${dev.dsf.fhir.proxy.username:#{null}}")
	private String proxyUsername;

	@Documentation(description = "Forward Proxy password", recommendation = "Configure password if proxy requires authentication, use docker secret file to configure using *${env_variable}_FILE*")
	@Value("${dev.dsf.fhir.proxy.password:#{null}}")
	private char[] proxyPassword;

	@Documentation(description = "Forward proxy no-proxy list, entries will match exactly or agianst (one level) sub-domains, if no port is specified - all ports are matched; comma or space separated list, YAML block scalars supported", example = "foo.bar, test.com:8080")
	@Value("#{'${dev.dsf.fhir.proxy.noProxy:}'.trim().split('(,[ ]?)|(\\\\n)')}")
	private List<String> proxyNoProxy;

	@Bean // static in order to initialize before @Configuration classes
	public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer(
			ConfigurableEnvironment environment)
	{
		new DockerSecretsPropertySourceFactory(environment).readDockerSecretsAndAddPropertiesToEnvironment();

		return new PropertySourcesPlaceholderConfigurer();
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
		return serverBaseUrl;
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

	@Bean
	public ProxyConfig proxyConfig()
	{
		return new ProxyConfigImpl(proxyUrl, proxyUsername, proxyPassword, proxyNoProxy);
	}
}
