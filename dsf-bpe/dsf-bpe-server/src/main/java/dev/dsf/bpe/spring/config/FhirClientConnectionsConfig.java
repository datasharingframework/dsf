package dev.dsf.bpe.spring.config;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.dsf.bpe.api.client.oidc.OidcClient;
import dev.dsf.bpe.api.client.oidc.OidcClientException;
import dev.dsf.bpe.api.config.FhirClientConfig;
import dev.dsf.bpe.api.config.FhirClientConfigs;
import dev.dsf.bpe.client.fhir.FhirConnectionTestClient;
import dev.dsf.bpe.client.fhir.FhirConnectionTestClientJersey;
import dev.dsf.bpe.config.ConfigInvalidException;
import dev.dsf.bpe.config.FhirClientConfigImpl;
import dev.dsf.bpe.config.FhirClientConfigImpl.CertificateAuthenticationImpl;
import dev.dsf.bpe.config.FhirClientConfigYamlReaderImpl;
import dev.dsf.bpe.config.FhirClientConfigsImpl;

@Configuration
public class FhirClientConnectionsConfig implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(FhirClientConnectionsConfig.class);

	@Autowired
	private PropertiesConfig propertiesConfig;

	@Autowired
	private BuildInfoReaderConfig buildInfoReaderConfig;

	@Autowired
	private FhirConfig fhirConfig;

	@Autowired
	private OidcClientProviderConfig oidcClientProviderConfig;

	@Autowired
	private DsfClientConfig dsfClientConfig;

	@Bean
	public FhirClientConfigYamlReaderImpl fhirClientYamlConfigReader()
			throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException
	{
		boolean defaultTestConnectionOnStartup = propertiesConfig
				.getFhirClientConnectionsConfigDefaultTestConnectionOnStartup();
		boolean defaultEnableDebugLogging = propertiesConfig.getFhirClientConnectionsConfigDefaultEnableDebugLogging();
		Duration defaultConnectTimeout = propertiesConfig.getFhirClientConnectionsConfigDefaultConnectTimeout();
		Duration defaultReadTimeout = propertiesConfig.getFhirClientConnectionsConfigDefaultReadTimeout();
		KeyStore defaultTrustStore = propertiesConfig.getFhirClientConnectionsConfigDefaultTrustStore();
		String defaultOidcDiscoveryPath = propertiesConfig.getFhirClientConnectionsConfigDefaultOidcDiscoveryPath();

		return new FhirClientConfigYamlReaderImpl(defaultTestConnectionOnStartup, defaultEnableDebugLogging,
				defaultConnectTimeout, defaultReadTimeout, defaultTrustStore, defaultOidcDiscoveryPath);
	}

	@Bean
	public FhirClientConfigs fhirClientConfigs()
			throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException
	{
		FhirClientConfigYamlReaderImpl reader = fhirClientYamlConfigReader();

		try
		{
			FhirClientConfigs configs = reader.readConfigs(propertiesConfig.getFhirClientConnectionsConfig());

			logger.debug("Configured FHIR server connections: {}", configs);
			logger.info("Configured FHIR server connections ids: {}", configs.getConfigs().isEmpty() ? "none"
					: configs.getConfigs().stream().map(FhirClientConfig::fhirServerId).sorted().toList());

			return configs.addConfig(createDsfConfig());
		}
		catch (ConfigInvalidException e)
		{
			logger.error("FHIR server connections configuration YAML not valid: {}", e.getValidationErrors());

			return FhirClientConfigsImpl.empty();
		}
		catch (IOException e)
		{
			logger.debug("Unable to parse FHIR server connections configuration", e);
			logger.warn("Unable to parse FHIR server connections configuration: {} - {}", e.getClass().getName(),
					e.getMessage());

			return FhirClientConfigsImpl.empty();
		}
	}

	private FhirClientConfig createDsfConfig()
	{
		return new FhirClientConfigImpl(FhirClientConfig.DSF_CLIENT_FHIR_SERVER_ID, "" /* intentionally empty string */,
				false, propertiesConfig.getFhirClientConnectionsConfigDefaultEnableDebugLogging(),
				propertiesConfig.getFhirClientConnectionsConfigDefaultConnectTimeout(),
				propertiesConfig.getFhirClientConnectionsConfigDefaultReadTimeout(),
				dsfClientConfig.clientProvider().getWebserviceTrustStore(),
				new CertificateAuthenticationImpl(dsfClientConfig.clientProvider().getWebserviceKeyStore(),
						dsfClientConfig.clientProvider().getWebserviceKeyStorePassword()),
				null, null, null);
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		fhirClientConfigs().getConfigs().stream().filter(FhirClientConfig::hasOidcAuthentication)
				.forEach(this::testConnection);

		fhirClientConfigs().getConfigs().stream().filter(FhirClientConfig::testConnectionOnStartup)
				.map(this::createClient).forEach(FhirConnectionTestClient::testConnection);
	}

	private FhirConnectionTestClient createClient(FhirClientConfig config)
	{
		return new FhirConnectionTestClientJersey(config, propertiesConfig.proxyConfig(),
				buildInfoReaderConfig.buildInfoReader().getUserAgentValue(), fhirConfig.fhirContext(),
				oidcClientProviderConfig.bpeOidcClientProvider());
	}

	private void testConnection(FhirClientConfig config)
	{
		OidcClient oidcClient = oidcClientProviderConfig.bpeOidcClientProvider()
				.getOidcClient(config.oidcAuthentication());

		try
		{
			logger.info("Testing connection with OIDC provider at {} for '{}' ...",
					config.oidcAuthentication().baseUrl(), config.fhirServerId());

			char[] accessToken = oidcClient.getAccessToken();

			logger.info("Testing connection with OIDC provider at {} for '{}' [OK] -> Token: {}...",
					config.oidcAuthentication().baseUrl(), config.fhirServerId(),
					String.valueOf(accessToken).substring(0, 20));
		}
		catch (OidcClientException e)
		{
			logger.debug("Testing connection with OIDC provider at {} for '{}' [Failed] -> {}",
					config.oidcAuthentication().baseUrl(), config.fhirServerId(), e);
			logger.warn("Testing connection with OIDC provider at {} for '{}' [Failed] -> {}",
					config.oidcAuthentication().baseUrl(), config.fhirServerId(), e.getMessage());
		}
	}
}
