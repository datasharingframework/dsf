package dev.dsf.bpe.spring.config;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.Scope;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.ConfigurableEnvironment;

import de.hsheilbronn.mi.utils.crypto.cert.CertificateValidator;
import de.hsheilbronn.mi.utils.crypto.io.KeyStoreReader;
import de.hsheilbronn.mi.utils.crypto.io.PemReader;
import de.hsheilbronn.mi.utils.crypto.keypair.KeyPairValidator;
import de.hsheilbronn.mi.utils.crypto.keystore.KeyStoreCreator;
import dev.dsf.common.config.ProxyConfig;
import dev.dsf.common.config.ProxyConfigImpl;
import dev.dsf.common.docker.secrets.DockerSecretsPropertySourceFactory;
import dev.dsf.common.documentation.Documentation;
import dev.dsf.common.ui.theme.Theme;

@Configuration
@PropertySource(value = "file:conf/config.properties", encoding = "UTF-8", ignoreResourceNotFound = true)
public class PropertiesConfig implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(PropertiesConfig.class);

	private static final String API_VERSION_PATTERN_STRING = "v([1-9]+[0-9]*)";
	private static final Pattern API_VERSION_PATTERN = Pattern.compile(API_VERSION_PATTERN_STRING);

	// documentation in dev.dsf.bpe.config.BpeDbMigratorConfig
	@Value("${dev.dsf.bpe.db.url}")
	private String dbUrl;

	// documentation in dev.dsf.bpe.config.BpeDbMigratorConfig
	@Value("${dev.dsf.bpe.db.user.username:bpe_server_user}")
	private String dbUsername;

	// documentation in dev.dsf.bpe.config.BpeDbMigratorConfig
	@Value("${dev.dsf.bpe.db.user.password}")
	private char[] dbPassword;

	// documentation in dev.dsf.bpe.config.BpeDbMigratorConfig
	@Value("${dev.dsf.bpe.db.user.camunda.username:camunda_server_user}")
	private String dbCamundaUsername;

	// documentation in dev.dsf.bpe.config.BpeDbMigratorConfig
	@Value("${dev.dsf.bpe.db.user.camunda.password}")
	private char[] dbCamundaPassword;

	@Documentation(description = "UI theme parameter, adds a color indicator to the ui to distinguish `dev`, `test` and `prod` environments im configured; supported values: `dev`, `test` and `prod`")
	@Value("${dev.dsf.bpe.server.ui.theme:}")
	private String uiTheme;

	@Documentation(description = "Base address of the BPE server, configure when exposing the web-ui", example = "https://foo.bar/bpe")
	@Value("${dev.dsf.bpe.server.base.url:https://localhost/bpe}")
	private String bpeServerBaseUrl;

	@Documentation(description = "Role config YAML as defined in [FHIR Server: Access Control](access-control)")
	@Value("${dev.dsf.bpe.server.roleConfig:}")
	private String roleConfig;

	@Documentation(description = "To disable static resource caching, set to `false`", recommendation = "Only set to `false` for development")
	@Value("${dev.dsf.bpe.server.static.resource.cache:true}")
	private boolean staticResourceCacheEnabled;

	@Documentation(description = "PEM encoded file with one or more trusted root certificates to validate server certificates for https connections to local and remote DSF FHIR servers", recommendation = "Use docker secret file to configure", example = "/run/secrets/app_client_trust_certificates.pem")
	@Value("${dev.dsf.bpe.fhir.client.trust.server.certificate.cas:ca/server_cert_root_cas.pem}")
	private String dsfClientTrustedServerCasFile;

	@Documentation(required = true, description = "PEM encoded file with local client certificate for https connections to local and remote DSF FHIR servers", recommendation = "Use docker secret file to configure", example = "/run/secrets/app_client_certificate.pem")
	@Value("${dev.dsf.bpe.fhir.client.certificate}")
	private String dsfClientCertificateFile;

	@Documentation(required = true, description = "Private key corresponding to the local client certificate as PEM encoded file. Use ${env_variable}_PASSWORD* or *${env_variable}_PASSWORD_FILE* if private key is encrypted", recommendation = "Use docker secret file to configure", example = "/run/secrets/app_client_certificate_private_key.pem")
	@Value("${dev.dsf.bpe.fhir.client.certificate.private.key}")
	private String dsfClientCertificatePrivateKeyFile;

	@Documentation(description = "Password to decrypt the local client certificate encrypted private key", recommendation = "Use docker secret file to configure using *${env_variable}_FILE*", example = "/run/secrets/app_client_certificate_private_key.pem.password")
	@Value("${dev.dsf.bpe.fhir.client.certificate.private.key.password:#{null}}")
	private char[] dsfClientCertificatePrivateKeyFilePassword;

	@Documentation(description = "Timeout until a reading a resource from a remote DSF FHIR server is aborted", recommendation = "Change default value only if timeout exceptions occur")
	@Value("${dev.dsf.bpe.fhir.client.remote.timeout.read:PT60S}")
	private String dsfClientReadTimeoutRemote;

	@Documentation(description = "Timeout until a connection is established with a remote DSF FHIR server", recommendation = "Change default value only if timeout exceptions occur")
	@Value("${dev.dsf.bpe.fhir.client.remote.timeout.connect:PT5S}")
	private String dsfClientConnectTimeoutRemote;

	@Documentation(description = "To enable verbose logging of requests to and replies from remote DSF FHIR servers, set to `true`")
	@Value("${dev.dsf.bpe.fhir.client.remote.verbose:false}")
	private boolean dsfClientVerboseRemote;

	@Documentation(description = "Timeout until reading a resource from the local DSF FHIR server is aborted", recommendation = "Change default value only if timeout exceptions occur")
	@Value("${dev.dsf.bpe.fhir.client.local.timeout.read:PT60S}")
	private String dsfClientReadTimeoutLocal;

	@Documentation(description = "Timeout until a connection is established with the local DSF FHIR server", recommendation = "Change default value only if timeout exceptions occur")
	@Value("${dev.dsf.bpe.fhir.client.local.timeout.connect:PT2S}")
	private String dsfClientConnectTimeoutLocal;

	@Documentation(description = "To enable verbose logging of requests to and replies from the local DSF FHIR server, set to `true`")
	@Value("${dev.dsf.bpe.fhir.client.local.verbose:false}")
	private boolean dsfClientVerboseLocal;

	@Documentation(required = true, description = "Base address of the local DSF FHIR server to read/store fhir resources", example = "https://foo.bar/fhir")
	@Value("${dev.dsf.bpe.fhir.server.base.url}")
	private String dsfServerBaseUrl;

	@Documentation(description = "FHIR server connections YAML config for v2 process plugins")
	@Value("${dev.dsf.bpe.fhir.client.connections.config:}")
	private String fhirClientConnectionsConfig;

	@Documentation(description = "FHIR server connections YAML: Default value for properties `test-connection-on-startup` and `oidc-auth.test-connection-on-startup`", recommendation = "To perform connection tests on BPE startup to configured FHIR servers by default set to `true`")
	@Value("${dev.dsf.bpe.fhir.client.connections.config.default.test.connection.on.startup:false}")
	private boolean fhirClientConnectionsConfigDefaultTestConnectionOnStartup;

	@Documentation(description = "FHIR server connections YAML: Default value for properties `enable-debug-logging` and `oidc-auth.enable-debug-logging`", recommendation = "To enable debug logging of requests and reponses to configured FHIR servers by default set to `true`")
	@Value("${dev.dsf.bpe.fhir.client.connections.config.default.enable.debug.logging:false}")
	private boolean fhirClientConnectionsConfigDefaultEnableDebugLogging;

	@Documentation(description = "FHIR server connections YAML: Default value for properties `connect-timeout` and `oidc-auth.connect-timeout`")
	@Value("${dev.dsf.bpe.fhir.client.connections.config.default.timeout.connect:PT2S}")
	private String fhirClientConnectionsConfigDefaultConnectTimeout;

	@Documentation(description = "FHIR server connections YAML: Default value for properties `read-timeout` and `oidc-auth.read-timeout`")
	@Value("${dev.dsf.bpe.fhir.client.connections.config.default.timeout.read:PT10M}")
	private String fhirClientConnectionsConfigDefaultReadTimeout;

	@Documentation(description = "FHIR server connections YAML: Default value for properties `trusted-root-certificates-file` and `oidc-auth.trusted-root-certificates-file`")
	@Value("${dev.dsf.bpe.fhir.client.connections.config.default.trust.server.certificate.cas:ca/server_cert_root_cas.pem}")
	private String fhirClientConnectionsConfigDefaultTrustStoreFile;

	@Documentation(description = "FHIR server connections YAML: Default value for property `oidc-auth.discovery-path`")
	@Value("${dev.dsf.bpe.fhir.client.connections.config.default.oidc.discovery.path:/.well-known/openid-configuration}")
	private String fhirClientConnectionsConfigDefaultOidcDiscoveryPath;

	@Documentation(description = "Set `false` to disable caching of OIDC dicovery and jwks resources as well as access tokens in the 'Client Credentials Grant' client; access tokens are evicted 10 seconds before they expire")
	@Value("${dev.dsf.bpe.fhir.client.connections.config.oidc.cache:true}")
	private boolean fhirClientConnectionsConfigOidcClientCacheEnabled;

	@Documentation(description = "OIDC 'Client Credentials Grant' client cache timeout of the 'openid-configuration' discovery resource")
	@Value("${dev.dsf.bpe.fhir.client.connections.config.oidc.cache.timeout.configuration.resource:PT1H}")
	private String fhirClientConnectionsConfigOidcClientCacheConfigurationResourceTimeout;

	@Documentation(description = "OIDC 'Client Credentials Grant' client cache timeout of the jwks resource")
	@Value("${dev.dsf.bpe.fhir.client.connections.config.oidc.cache.timeout.jwks.resource:PT1H}")
	private String fhirClientConnectionsConfigOidcClientCacheJwksResourceTimeout;

	@Documentation(description = "OIDC 'Client Credentials Grant' client cache timeout of access tokens before they expire, duration is subtracted from the expires at value of the acess token")
	@Value("${dev.dsf.bpe.fhir.client.connections.config.oidc.cache.timeout.access.token:PT10S}")
	private String fhirClientConnectionsConfigOidcClientCacheAccessTokenBeforeExpirationTimeout;

	@Documentation(description = "OIDC 'Client Credentials Grant' client access token time validation leeway for 'Not Before', 'Issued At' and 'Expires At' values")
	@Value("${dev.dsf.bpe.fhir.client.connections.config.oidc.time.validation.leeway:PT10S}")
	private String fhirClientConnectionsConfigOidcClientNotBeforeIssuedAtExpiresAtLeeway;

	@Documentation(description = "Subscription to receive notifications about task resources from the DSF FHIR server")
	@Value("${dev.dsf.bpe.fhir.task.subscription.search.parameter:?criteria:exact=Task%3Fstatus%3Drequested&status=active&type=websocket&payload=application/fhir%2Bjson}")
	private String taskSubscriptionSearchParameter;

	@Documentation(description = "Subscription to receive notifications about questionnaire response resources from the DSF FHIR server")
	@Value("${dev.dsf.bpe.fhir.questionnaire.response.subscription.search.parameter:?criteria:exact=QuestionnaireResponse%3Fstatus%3Dcompleted&status=active&type=websocket&payload=application/fhir%2Bjson}")
	private String questionnaireResponseSubscriptionSearchParameter;

	@Documentation(description = "Number of retries until a websocket connection can be established with the DSF FHIR server, `-1` means infinite number of retries")
	@Value("${dev.dsf.bpe.fhir.task.subscription.retry.max:-1}")
	private int websocketMaxRetries;

	@Documentation(description = "Time between two retries to establish a websocket connection with the DSF FHIR server")
	@Value("${dev.dsf.bpe.fhir.task.subscription.retry.sleep:PT5S}")
	private String websocketRetrySleep;

	@Documentation(description = "Directory containing the DSF BPE process plugins for deployment on startup of the DSF BPE server", recommendation = "Change only if you don't use the provided directory structure from the installation guide or made changes to tit")
	@Value("${dev.dsf.bpe.process.plugin.directory:process}")
	private String processPluginDirectory;

	@Documentation(description = "Directories containing exploded DSF BPE process plugins for deployment on startup of the DSF BPE server; comma or space separated list, YAML block scalars supported", recommendation = "Only for testing")
	@Value("#{'${dev.dsf.bpe.process.plugin.exploded:}'.trim().split('(,[ ]?)|(\\n)')}")
	private List<String> explodedPluginDirectories;

	@Documentation(description = "Directory containing the DSF BPE process plugin api jar files", recommendation = "Change only during development")
	@Value("${dev.dsf.bpe.process.api.directory:api}")
	private String apiClassPathBaseDirectory;

	@Documentation(description = "Map with files containing qualified classs names allowed to be loaded by plugins for api versions; map key must match "
			+ API_VERSION_PATTERN_STRING, recommendation = "Change only during development", example = "{v1: 'some/example.file', v2: 'other.file'}")
	@Value("#{${dev.dsf.bpe.process.api.allowed.bpe.classes:{:}}}")
	private Map<String, String> apiAllowedBpeClasses;

	@Documentation(description = "Map with files containing api/plugin resource with priority over bpe resources for plugins for api versions; map key must match "
			+ API_VERSION_PATTERN_STRING, recommendation = "Change only during development", example = "{v1: 'some/example.file', v2: 'other.file'}")
	@Value("#{${dev.dsf.bpe.process.api.resources.with.priority:{:}}}")
	private Map<String, String> apiResourcesWithPriority;

	@Documentation(description = "Map with files containing resources allowed to be loaded by plugins for api versions; map key must match "
			+ API_VERSION_PATTERN_STRING, recommendation = "Change only during development", example = "{v1: 'some/example.file', v2: 'other.file'}")
	@Value("#{${dev.dsf.bpe.process.api.allowed.bpe.resource:{:}}}")
	private Map<String, String> apiAllowedBpeResources;

	@Documentation(description = "List of process names that should be excluded from deployment during startup of the DSF BPE server; comma or space separated list, YAML block scalars supported", recommendation = "Only deploy processes that can be started depending on your organization's roles in the Allow-List", example = "dsfdev_updateAllowList|1.0, another_process|x.y")
	@Value("#{'${dev.dsf.bpe.process.excluded:}'.trim().split('(,[ ]?)|(\\n)')}")
	private List<String> processExcluded;

	@Documentation(description = "List of already deployed process names that should be retired during startup of the DSF BPE server; comma or space separated list, YAML block scalars supported", recommendation = "Retire processes that where deployed previously but are not anymore available", example = "old_process|x.y")
	@Value("#{'${dev.dsf.bpe.process.retired:}'.trim().split('(,[ ]?)|(\\n)')}")
	private List<String> processRetired;

	@Documentation(description = "Number of parallel Task / QuestionnaireResponse threads to start new or continue existing processes, a value `<= 0` means number of cpu cores")
	@Value("${dev.dsf.bpe.process.threads:-1}")
	private int processStartOrContinueThreads;

	@Documentation(description = "Process engine job executor core pool size")
	@Value("${dev.dsf.bpe.process.engine.corePoolSize:4}")
	private int processEngineJobExecutorCorePoolSize;

	@Documentation(description = "Process engine job executor queue size, jobs are added to the queue if all core pool threads are busy")
	@Value("${dev.dsf.bpe.process.engine.queueSize:40}")
	private int processEngineJobExecutorQueueSize;

	@Documentation(description = "Process engine job executor max pool size, additional threads until max pool size are created if the queue is full")
	@Value("${dev.dsf.bpe.process.engine.maxPoolSize:10}")
	private int processEngineJobExecutorMaxPoolSize;

	@Documentation(description = "Number of retries until a connection can be established with the local DSF FHIR server during process deployment, `-1` means infinite number of retries")
	@Value("${dev.dsf.bpe.process.fhir.server.retry.max:-1}")
	private int fhirServerRequestMaxRetries;

	@Documentation(description = "Time between two retries to establish a connection with the local DSF FHIR server during process deployment")
	@Value("${dev.dsf.bpe.process.fhir.server.retry.sleep:PT5S}")
	private String fhirServerRetryDelay;

	@Documentation(description = "Mail service sender address", example = "sender@localhost")
	@Value("${dev.dsf.bpe.mail.fromAddress:}")
	private String mailFromAddress;

	@Documentation(description = "Mail service recipient addresses, configure at least one; comma or space separated list, YAML block scalars supported", example = "recipient@localhost")
	@Value("#{'${dev.dsf.bpe.mail.toAddresses:}'.trim().split('(,[ ]?)|(\\n)')}")
	private List<String> mailToAddresses;

	@Documentation(description = "Mail service CC recipient addresses; comma or space separated list, YAML block scalars supported", example = "cc.recipient@localhost")
	@Value("#{'${dev.dsf.bpe.mail.toAddressesCc:}'.trim().split('(,[ ]?)|(\\n)')}")
	private List<String> mailToAddressesCc;

	@Documentation(description = "Mail service reply to addresses; comma or space separated list, YAML block scalars supported", example = "reply.to@localhost")
	@Value("#{'${dev.dsf.bpe.mail.replyToAddresses:}'.trim().split('(,[ ]?)|(\\n)')}")
	private List<String> mailReplyToAddresses;

	@Documentation(description = "To enable SMTP over TLS (smtps), set to `true`")
	@Value("${dev.dsf.bpe.mail.useSmtps:false}")
	private boolean mailUseSmtps;

	@Documentation(description = "SMTP server hostname", example = "smtp.server.de")
	@Value("${dev.dsf.bpe.mail.host:#{null}}")
	private String mailServerHostname;

	@Documentation(description = "SMTP server port", example = "465")
	@Value("${dev.dsf.bpe.mail.port:0}")
	private int mailServerPort;

	@Documentation(description = "SMTP server authentication username", recommendation = "Configure if the SMTP server reqiures username/password authentication; enable SMTP over TLS via *DEV_DSF_BPE_MAIL_USESMTPS*")
	@Value("${dev.dsf.bpe.mail.username:#{null}}")
	private String mailServerUsername;

	@Documentation(description = "SMTP server authentication password", recommendation = "Configure if the SMTP server reqiures username/password authentication; use docker secret file to configure using *${env_variable}_FILE*; enable SMTP over TLS via *DEV_DSF_BPE_MAIL_USESMTPS*")
	@Value("${dev.dsf.bpe.mail.password:#{null}}")
	private char[] mailServerPassword;

	@Documentation(description = "PEM encoded file with one or more trusted root certificates to validate the server certificate of the SMTP server. Requires SMTP over TLS to be enabled via *DEV_DSF_BPE_MAIL_USESMTPS*", recommendation = "Use docker secret file to configure", example = "/run/secrets/smtp_server_trust_certificates.pem")
	@Value("${dev.dsf.bpe.mail.trust.server.certificate.cas:ca/server_cert_root_cas.pem}")
	private String mailServerTrustStoreFile;

	@Documentation(description = "PEM encoded file with client certificate used to authenticate against the SMTP server. Requires SMTP over TLS to be enabled via *DEV_DSF_BPE_MAIL_USESMTPS*", recommendation = "Use docker secret file to configure", example = "/run/secrets/smtp_server_client_certificate.pem")
	@Value("${dev.dsf.bpe.mail.client.certificate:#{null}}")
	private String mailServerClientCertificateFile;

	@Documentation(description = "Private key corresponging to the SMTP server client certificate as PEM encoded file. Use ${env_variable}_PASSWORD* or *${env_variable}_PASSWORD_FILE* if private key is encrypted. Requires SMTP over TLS to be enabled via *DEV_DSF_BPE_MAIL_USESMTPS*", recommendation = "Use docker secret file to configure", example = "/run/secrets/smtp_server_client_certificate_private_key.pem")
	@Value("${dev.dsf.bpe.mail.client.certificate.private.key:#{null}}")
	private String mailServerClientCertificatePrivateKeyFile;

	@Documentation(description = "Password to decrypt the local client certificate encrypted private key", recommendation = "Use docker secret file to configure using *${env_variable}_FILE*", example = "/run/secrets/smtp_server_client_certificate_private_key.pem.password")
	@Value("${dev.dsf.bpe.mail.client.certificate.private.key.password:#{null}}")
	private char[] mailServerClientCertificatePrivateKeyFilePassword;

	@Documentation(description = "PKCS12 encoded file with S/MIME certificate, private key and certificate chain to enable send mails to be S/MIME signed", recommendation = "Use docker secret file to configure", example = "/run/secrets/smime_certificate.p12")
	@Value("${dev.dsf.bpe.mail.smime.p12Keystore:#{null}}")
	private String mailSmimeSigingKeyStoreFile;

	@Documentation(description = "Password to decrypt the PKCS12 encoded S/MIMIE certificate file", recommendation = "Use docker secret file to configure using *${env_variable}_FILE*", example = "/run/secrets/smime_certificate.p12.password")
	@Value("${dev.dsf.bpe.mail.smime.p12Keystore.password:#{null}}")
	private char[] mailSmimeSigingKeyStorePassword;

	@Documentation(description = "To enable a test mail being send on startup of the BPE, set to `true`; requires SMTP server to be configured")
	@Value("${dev.dsf.bpe.mail.sendTestMailOnStartup:false}")
	private boolean sendTestMailOnStartup;

	@Documentation(description = "To enable mails being send for every ERROR logged, set to `true`; requires SMTP server to be configured")
	@Value("${dev.dsf.bpe.mail.sendMailOnErrorLogEvent:false}")
	private boolean sendMailOnErrorLogEvent;

	@Documentation(description = "Number of previous INFO, WARN log messages to include in ERROR log event mails (>=0); requires send mail on ERROR log event option to be enabled to have an effect")
	@Value("${dev.dsf.bpe.mail.mailOnErrorLogEventBufferSize:4}")
	private int mailOnErrorLogEventBufferSize;

	@Documentation(description = "Location of the BPE debug log as displayed in the footer of ERROR log event mails, does not modify the actual location of the debug log file; requires send mail on ERROR log event option to be enabled to have an effect")
	@Value("${dev.dsf.bpe.mail.mailOnErrorLogEventDebugLogLocation:/opt/bpe/log/bpe.log}")
	private String mailOnErrorLogEventDebugLogLocation;

	@Documentation(description = "To enable debug log messages for every bpmn activity start, set to `true`", recommendation = "This debug function should only be activated during process plugin development")
	@Value("${dev.dsf.bpe.debug.log.message.onActivityStart:false}")
	private boolean debugLogMessageOnActivityStart;

	@Documentation(description = "To enable debug log messages for every bpmn activity end, set to `true`", recommendation = "This debug function should only be activated during process plugin development")
	@Value("${dev.dsf.bpe.debug.log.message.onActivityEnd:false}")
	private boolean debugLogMessageOnActivityEnd;

	@Documentation(description = "To enable logging of bpmn variables for every bpmn activity start or end, when logging of these events is enabled, set to `true`", recommendation = "This debug function should only be activated during process plugin development; WARNING: Confidential information may be leaked via the debug log!")
	@Value("${dev.dsf.bpe.debug.log.message.variables:false}")
	private boolean debugLogMessageVariables;

	@Documentation(description = "To enable logging of local bpmn variables for every bpmn activity start or end, when logging of these events is enabled, set to `true`", recommendation = "This debug function should only be activated during process plugin development; WARNING: Confidential information may be leaked via the debug log!")
	@Value("${dev.dsf.bpe.debug.log.message.variablesLocal:false}")
	private boolean debugLogMessageVariablesLocal;

	@Documentation(description = "To enable logging of webservices requests set to `true`", recommendation = "This debug function should only be activated during development; WARNING: Confidential information may be leaked via the debug log!")
	@Value("${dev.dsf.bpe.debug.log.message.webserviceRequest:false}")
	private boolean debugLogMessageWebserviceRequest;

	@Documentation(description = "To enable logging of DB queries set to `true`", recommendation = "This debug function should only be activated during development; WARNING: Confidential information may be leaked via the debug log!")
	@Value("${dev.dsf.bpe.debug.log.message.dbStatement:false}")
	private boolean debugLogMessageDbStatement;

	@Documentation(description = "To enable logging of the currently requesting user set to `true`", recommendation = "This debug function should only be activated during development; WARNING: Confidential information may be leaked via the debug log!")
	@Value("${dev.dsf.bpe.debug.log.message.currentUser:false}")
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
	@Value("${dev.dsf.server.auth.trust.client.certificate.cas:ca/client_cert_ca_chains.pem}")
	private String dsfClientTrustedClientCasFile;

	@Bean // static in order to initialize before @Configuration classes
	public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer(
			ConfigurableEnvironment environment)
	{
		new DockerSecretsPropertySourceFactory(environment).readDockerSecretsAndAddPropertiesToEnvironment();

		return new PropertySourcesPlaceholderConfigurer();
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		URL url = new URI(dsfServerBaseUrl).toURL();
		if (!List.of("http", "https").contains(url.getProtocol()))
		{
			logger.warn("Invalid DSF FHIR server base URL: '{}', URL not starting with 'http://' or 'https://'",
					dsfServerBaseUrl);
			throw new IllegalArgumentException("Invalid ServerBaseUrl, not starting with 'http://' or 'https://'");
		}
		else if (dsfServerBaseUrl.endsWith("//"))
		{
			logger.warn("Invalid DSF FHIR server base URL: '{}', URL may not end in '//'", dsfServerBaseUrl);
			throw new IllegalArgumentException("Invalid ServerBaseUrl, ending in //");
		}
		else if (!dsfServerBaseUrl.startsWith("https://"))
		{
			logger.warn("Invalid DSF FHIR server base URL: '{}', URL must start with 'https://'", dsfServerBaseUrl);
			throw new IllegalArgumentException("Invalid ServerBaseUrl, not starting with https://");
		}

		if (dsfServerBaseUrl.endsWith("/"))
			logger.warn("DSF FHIR server base URL: '{}', should not end in '/', removing trailing '/'",
					dsfServerBaseUrl);

		logger.info(
				"Concurrency config: {process-threads: {}, engine-core-pool: {}, engine-queue: {}, engine-max-pool: {}}",
				getProcessStartOrContinueThreads(), processEngineJobExecutorCorePoolSize,
				processEngineJobExecutorQueueSize, processEngineJobExecutorMaxPoolSize);

		try
		{
			X509Certificate clientCertiticate = PemReader.readCertificate(Paths.get(getDsfClientCertificateFile()));
			CertificateValidator.vaildateClientCertificate(getDsfClientTrustedClientCas(), clientCertiticate);
		}
		catch (CertificateException e)
		{
			logger.warn("Unable to validate DSF client certificate against trusted client certificate CAs: {}",
					e.getMessage());
		}
	}

	private KeyStore createTrustStore(String trustStoreFile)
	{
		try
		{
			Path trustStorePath = Paths.get(trustStoreFile);

			if (!Files.isReadable(trustStorePath))
				throw new IOException("Trust store file '" + trustStorePath.normalize().toAbsolutePath().toString()
						+ "' not readable");

			return KeyStoreCreator.jksForTrustedCertificates(PemReader.readCertificates(trustStorePath));
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	private KeyStore createKeyStore(String certificateFile, String privateKeyFile, char[] privateKeyPassword,
			char[] keyStorePassword)
	{
		try
		{
			Path certificatePath = Paths.get(certificateFile);
			Path privateKeyPath = Paths.get(privateKeyFile);

			if (!Files.isReadable(certificatePath))
				throw new IOException(
						"Certificate '" + certificatePath.normalize().toAbsolutePath().toString() + "' not readable");
			if (!Files.isReadable(privateKeyPath))
				throw new IOException(
						"Private key '" + privateKeyPath.normalize().toAbsolutePath().toString() + "' not readable");

			List<X509Certificate> certificates = PemReader.readCertificates(certificatePath);
			PrivateKey privateKey = PemReader.readPrivateKey(privateKeyPath, privateKeyPassword);

			if (certificates.isEmpty())
				throw new IOException(
						"No certificates in '" + certificatePath.normalize().toAbsolutePath().toString() + "'");
			else if (!CertificateValidator.isClientCertificate(certificates.get(0)))
				throw new IOException("First certificate from '"
						+ certificatePath.normalize().toAbsolutePath().toString() + "' not a client certificate");
			else if (!KeyPairValidator.matches(privateKey, certificates.get(0).getPublicKey()))
				throw new IOException("Private-key at '" + privateKeyPath.normalize().toAbsolutePath().toString()
						+ "' not matching Public-key from " + (certificates.size() > 1 ? "first " : "")
						+ "certificate at '" + certificatePath.normalize().toAbsolutePath().toString() + "'");

			return KeyStoreCreator.jksForPrivateKeyAndCertificateChain(privateKey, keyStorePassword, certificates);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	private KeyStore createKeyStore(String keyStoreFile, char[] keyStorePassword)
	{
		try
		{
			Path keyStorePath = Paths.get(keyStoreFile);

			if (!Files.isReadable(keyStorePath))
				throw new IOException("S/MIME mail signing certificate file '"
						+ keyStorePath.normalize().toAbsolutePath().toString() + "' not readable");

			KeyStore keyStore = KeyStoreReader.readPkcs12(keyStorePath, keyStorePassword);

			List<String> aliases = Collections.list(keyStore.aliases());
			if (aliases.size() != 1)
				throw new IOException("KeyStore at '" + keyStorePath.normalize().toAbsolutePath().toString() + "' has "
						+ aliases.size() + " entries " + aliases + ", expected 1");
			if (keyStore.getCertificateChain(aliases.get(0)) == null)
				throw new IOException("KeyStore at '" + keyStorePath.normalize().toAbsolutePath().toString()
						+ "' has no certificate chain for entry " + aliases.get(0));
			if (!keyStore.isKeyEntry(aliases.get(0)))
				throw new IOException("KeyStore at '" + keyStorePath.normalize().toAbsolutePath().toString()
						+ "' has no key for entry " + aliases.get(0));

			return keyStore;
		}
		catch (IOException | KeyStoreException e)
		{
			throw new RuntimeException(e);
		}
	}

	private Duration assertPositive(Duration duration)
	{
		if (duration != null && duration.isNegative())
			throw new IllegalArgumentException("configured duration is negative");
		else
			return duration;
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

	public String getDbCamundaUsername()
	{
		return dbCamundaUsername;
	}

	public char[] getDbCamundaPassword()
	{
		return dbCamundaPassword;
	}

	public Theme getUiTheme()
	{
		return Theme.fromString(uiTheme);
	}

	public String getServerBaseUrl()
	{
		return bpeServerBaseUrl != null && bpeServerBaseUrl.endsWith("/")
				? bpeServerBaseUrl.substring(0, bpeServerBaseUrl.length() - 1)
				: bpeServerBaseUrl;
	}

	public String getRoleConfig()
	{
		return roleConfig;
	}

	public boolean getStaticResourceCacheEnabled()
	{
		return staticResourceCacheEnabled;
	}

	public String getDsfClientTrustedServerCasFile()
	{
		return dsfClientTrustedServerCasFile;
	}

	@Bean
	public KeyStore getDsfClientTrustedServerCas()
	{
		return createTrustStore(getDsfClientTrustedServerCasFile());
	}

	public String getDsfClientCertificateFile()
	{
		return dsfClientCertificateFile;
	}

	public String getDsfClientCertificatePrivateKeyFile()
	{
		return dsfClientCertificatePrivateKeyFile;
	}

	public char[] getDsfClientCertificatePrivateKeyFilePassword()
	{
		return dsfClientCertificatePrivateKeyFilePassword;
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public KeyStore getDsfClientCertificate(char[] keyStorePassword)
	{
		return createKeyStore(getDsfClientCertificateFile(), getDsfClientCertificatePrivateKeyFile(),
				getDsfClientCertificatePrivateKeyFilePassword(), keyStorePassword);
	}

	public Duration getDsfClientReadTimeoutRemote()
	{
		return Duration.parse(dsfClientReadTimeoutRemote);
	}

	public Duration getDsfClientConnectTimeoutRemote()
	{
		return Duration.parse(dsfClientConnectTimeoutRemote);
	}

	public boolean getDsfClientVerboseRemote()
	{
		return dsfClientVerboseRemote;
	}

	public String getDsfServerBaseUrl()
	{
		return dsfServerBaseUrl != null && dsfServerBaseUrl.endsWith("/")
				? dsfServerBaseUrl.substring(0, dsfServerBaseUrl.length() - 1)
				: dsfServerBaseUrl;
	}

	public Duration getDsfClientReadTimeoutLocal()
	{
		return Duration.parse(dsfClientReadTimeoutLocal);
	}

	public Duration getDsfClientConnectTimeoutLocal()
	{
		return Duration.parse(dsfClientConnectTimeoutLocal);
	}

	public boolean getDsfClientVerboseLocal()
	{
		return dsfClientVerboseLocal;
	}

	public String getDsfClientTrustedClientCasFile()
	{
		return dsfClientTrustedClientCasFile;
	}

	@Bean
	public KeyStore getDsfClientTrustedClientCas()
	{
		return createTrustStore(getDsfClientTrustedClientCasFile());
	}

	public String getFhirClientConnectionsConfig()
	{
		return fhirClientConnectionsConfig;
	}

	public boolean getFhirClientConnectionsConfigDefaultTestConnectionOnStartup()
	{
		return fhirClientConnectionsConfigDefaultTestConnectionOnStartup;
	}

	public boolean getFhirClientConnectionsConfigDefaultEnableDebugLogging()
	{
		return fhirClientConnectionsConfigDefaultEnableDebugLogging;
	}

	public Duration getFhirClientConnectionsConfigDefaultConnectTimeout()
	{
		return Duration.parse(fhirClientConnectionsConfigDefaultConnectTimeout);
	}

	public Duration getFhirClientConnectionsConfigDefaultReadTimeout()
	{
		return Duration.parse(fhirClientConnectionsConfigDefaultReadTimeout);
	}

	public String getFhirClientConnectionsConfigDefaultTrustStoreFile()
	{
		return fhirClientConnectionsConfigDefaultTrustStoreFile;
	}

	@Bean
	public KeyStore getFhirClientConnectionsConfigDefaultTrustStore()
	{
		return createTrustStore(getFhirClientConnectionsConfigDefaultTrustStoreFile());
	}

	public String getFhirClientConnectionsConfigDefaultOidcDiscoveryPath()
	{
		return fhirClientConnectionsConfigDefaultOidcDiscoveryPath;
	}

	public boolean getFhirClientConnectionsConfigOidcClientCacheEnabled()
	{
		return fhirClientConnectionsConfigOidcClientCacheEnabled;
	}

	public Duration getFhirClientConnectionsConfigOidcClientCacheConfigurationResourceTimeout()
	{
		return assertPositive(Duration.parse(fhirClientConnectionsConfigOidcClientCacheConfigurationResourceTimeout));
	}

	public Duration getFhirClientConnectionsConfigOidcClientCacheJwksResourceTimeout()
	{
		return assertPositive(Duration.parse(fhirClientConnectionsConfigOidcClientCacheJwksResourceTimeout));
	}

	public Duration getFhirClientConnectionsConfigOidcClientCacheAccessTokenBeforeExpirationTimeout()
	{
		return assertPositive(
				Duration.parse(fhirClientConnectionsConfigOidcClientCacheAccessTokenBeforeExpirationTimeout));
	}

	public Duration getFhirClientConnectionsConfigOidcClientNotBeforeIssuedAtExpiresAtLeeway()
	{
		return assertPositive(Duration.parse(fhirClientConnectionsConfigOidcClientNotBeforeIssuedAtExpiresAtLeeway));
	}

	public String getTaskSubscriptionSearchParameter()
	{
		return taskSubscriptionSearchParameter;
	}

	public String getQuestionnaireResponseSubscriptionSearchParameter()
	{
		return questionnaireResponseSubscriptionSearchParameter;
	}

	public Duration getWebsocketRetrySleepMillis()
	{
		return Duration.parse(websocketRetrySleep);
	}

	public int getWebsocketMaxRetries()
	{
		return websocketMaxRetries;
	}

	public Path getProcessPluginDirectory()
	{
		return Paths.get(processPluginDirectory);
	}

	public List<Path> getExplodedPluginDirectories()
	{
		return explodedPluginDirectories.stream().filter(s -> s != null && !s.isBlank()).map(Paths::get).toList();
	}

	public Path getApiClassPathBaseDirectory()
	{
		return Paths.get(apiClassPathBaseDirectory);
	}

	public Map<Integer, Path> getApiAllowedBpeClasses()
	{
		return apiAllowedBpeClasses.entrySet().stream().filter(this::hasVersionKeyAndNotBlankValue)
				.collect(Collectors.toMap(this::toVersion, this::toPath));
	}

	public Map<Integer, Path> getApiAllowedBpeResources()
	{
		return apiAllowedBpeResources.entrySet().stream().filter(this::hasVersionKeyAndNotBlankValue)
				.collect(Collectors.toMap(this::toVersion, this::toPath));
	}

	public Map<Integer, Path> getApiResourcesWithPriority()
	{
		return apiResourcesWithPriority.entrySet().stream().filter(this::hasVersionKeyAndNotBlankValue)
				.collect(Collectors.toMap(this::toVersion, this::toPath));
	}

	private boolean hasVersionKeyAndNotBlankValue(Entry<String, String> entry)
	{
		return toVersion(entry) > 0 && toPath(entry) != null;
	}

	private int toVersion(Entry<String, String> entry)
	{
		if (entry == null || entry.getKey() == null || entry.getKey().isBlank())
			return Integer.MIN_VALUE;

		try
		{
			Matcher matcher = API_VERSION_PATTERN.matcher(entry.getKey());
			return matcher.matches() ? Integer.parseInt(matcher.group(1)) : Integer.MIN_VALUE;
		}
		catch (NumberFormatException e)
		{
			return Integer.MIN_VALUE;
		}
	}

	private Path toPath(Entry<String, String> entry)
	{
		if (entry == null || entry.getValue() == null || entry.getValue().isBlank())
			return null;
		else
			return Paths.get(entry.getValue());
	}

	public List<String> getProcessExcluded()
	{
		return Collections.unmodifiableList(processExcluded);
	}

	public List<String> getProcessRetired()
	{
		return Collections.unmodifiableList(processRetired);
	}

	public int getProcessStartOrContinueThreads()
	{
		if (processStartOrContinueThreads <= 0)
			return Runtime.getRuntime().availableProcessors();
		else
			return processStartOrContinueThreads;
	}

	public int getProcessEngineJobExecutorCorePoolSize()
	{
		return processEngineJobExecutorCorePoolSize;
	}

	public int getProcessEngineJobExecutorQueueSize()
	{
		return processEngineJobExecutorQueueSize;
	}

	public int getProcessEngineJobExecutorMaxPoolSize()
	{
		return processEngineJobExecutorMaxPoolSize;
	}

	public int getFhirServerRequestMaxRetries()
	{
		return fhirServerRequestMaxRetries;
	}

	public Duration getFhirServerRetryDelay()
	{
		return Duration.parse(fhirServerRetryDelay);
	}

	public String getMailFromAddress()
	{
		return mailFromAddress;
	}

	public List<String> getMailToAddresses()
	{
		return mailToAddresses;
	}

	public List<String> getMailToAddressesCc()
	{
		return mailToAddressesCc;
	}

	public List<String> getMailReplyToAddresses()
	{
		return mailReplyToAddresses;
	}

	public boolean getMailUseSmtps()
	{
		return mailUseSmtps;
	}

	public String getMailServerHostname()
	{
		return mailServerHostname;
	}

	public int getMailServerPort()
	{
		return mailServerPort;
	}

	public String getMailServerUsername()
	{
		return mailServerUsername;
	}

	public char[] getMailServerPassword()
	{
		return mailServerPassword;
	}

	public String getMailServerTrustStoreFile()
	{
		return mailServerTrustStoreFile;
	}

	@Bean
	@Lazy // not always used
	public KeyStore getMailServerTrustStore()
	{
		return createTrustStore(getMailServerTrustStoreFile());
	}

	public String getMailServerClientCertificateFile()
	{
		return mailServerClientCertificateFile;
	}

	public String getMailServerClientCertificatePrivateKeyFile()
	{
		return mailServerClientCertificatePrivateKeyFile;
	}

	public char[] getMailServerClientCertificatePrivateKeyFilePassword()
	{
		return mailServerClientCertificatePrivateKeyFilePassword;
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public Optional<KeyStore> getMailServerKeyStore(char[] keyStorePassword)
	{
		if (getMailServerClientCertificateFile() == null || getMailServerClientCertificatePrivateKeyFile() == null)
			return Optional.empty();
		else
			return Optional.of(
					createKeyStore(getMailServerClientCertificateFile(), getMailServerClientCertificatePrivateKeyFile(),
							getMailServerClientCertificatePrivateKeyFilePassword(), keyStorePassword));
	}

	public String getMailSmimeSigingKeyStoreFile()
	{
		return mailSmimeSigingKeyStoreFile;
	}

	public char[] getMailSmimeSigingKeyStorePassword()
	{
		return mailSmimeSigingKeyStorePassword;
	}

	@Bean
	public Optional<KeyStore> getMailSmimeSigingKeyStore()
	{
		if (getMailSmimeSigingKeyStoreFile() == null)
			return Optional.empty();
		else
			return Optional.of(createKeyStore(getMailSmimeSigingKeyStoreFile(), getMailSmimeSigingKeyStorePassword()));
	}

	public boolean getSendTestMailOnStartup()
	{
		return sendTestMailOnStartup;
	}

	public boolean getSendMailOnErrorLogEvent()
	{
		return sendMailOnErrorLogEvent;
	}

	public int getMailOnErrorLogEventBufferSize()
	{
		return mailOnErrorLogEventBufferSize;
	}

	public String getMailOnErrorLogEventDebugLogLocation()
	{
		return mailOnErrorLogEventDebugLogLocation;
	}

	public boolean getDebugLogMessageOnActivityStart()
	{
		return debugLogMessageOnActivityStart;
	}

	public boolean getDebugLogMessageOnActivityEnd()
	{
		return debugLogMessageOnActivityEnd;
	}

	public boolean getDebugLogMessageVariables()
	{
		return debugLogMessageVariables;
	}

	public boolean getDebugLogMessageVariablesLocal()
	{
		return debugLogMessageVariablesLocal;
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
