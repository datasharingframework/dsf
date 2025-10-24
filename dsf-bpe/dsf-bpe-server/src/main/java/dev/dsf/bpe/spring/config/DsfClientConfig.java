package dev.dsf.bpe.spring.config;

import java.security.KeyStore;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.hsheilbronn.mi.utils.crypto.cert.CertificateFormatter.X500PrincipalFormat;
import de.hsheilbronn.mi.utils.crypto.keystore.KeyStoreFormatter;
import dev.dsf.bpe.client.dsf.ClientProvider;
import dev.dsf.bpe.client.dsf.ClientProviderImpl;

@Configuration
public class DsfClientConfig implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(DsfClientConfig.class);

	@Autowired
	private PropertiesConfig propertiesConfig;

	@Autowired
	private FhirConfig fhirConfig;

	@Autowired
	private BuildInfoReaderConfig buildInfoReaderConfig;

	@Override
	public void afterPropertiesSet() throws Exception
	{
		logger.info(
				"Local DSF webservice client config: {trustStorePath: {}, certificatePath: {}, privateKeyPath: {}, privateKeyPassword: {},"
						+ " url: {}, proxy: {}}",
				propertiesConfig.getDsfClientTrustedServerCasFileOrFolder(),
				propertiesConfig.getDsfClientCertificateFile(),
				propertiesConfig.getDsfClientCertificatePrivateKeyFile(),
				propertiesConfig.getDsfClientCertificatePrivateKeyFilePassword() != null ? "***" : "null",
				propertiesConfig.getDsfServerBaseUrl(),
				propertiesConfig.proxyConfig().isEnabled(propertiesConfig.getDsfServerBaseUrl()) ? "enabled"
						: "disabled");
		logger.info(
				"Local DSF websocket client config: {trustStorePath: {}, certificatePath: {}, privateKeyPath: {}, privateKeyPassword: {},"
						+ " url: {}, proxy: {}}",
				propertiesConfig.getDsfClientTrustedServerCasFileOrFolder(),
				propertiesConfig.getDsfClientCertificateFile(),
				propertiesConfig.getDsfClientCertificatePrivateKeyFile(),
				propertiesConfig.getDsfClientCertificatePrivateKeyFilePassword() != null ? "***" : "null",
				getWebsocketUrl(),
				propertiesConfig.proxyConfig().isEnabled(getWebsocketUrl()) ? "enabled" : "disabled");

		logger.info(
				"Remote DSF webservice client config: {trustStorePath: {}, certificatePath: {}, privateKeyPath: {}, privateKeyPassword: {},"
						+ " proxy: {}}",
				propertiesConfig.getDsfClientTrustedServerCasFileOrFolder(),
				propertiesConfig.getDsfClientCertificateFile(),
				propertiesConfig.getDsfClientCertificatePrivateKeyFile(),
				propertiesConfig.getDsfClientCertificatePrivateKeyFilePassword() != null ? "***" : "null",
				propertiesConfig.proxyConfig().isEnabled()
						? "enabled if remote server not in " + propertiesConfig.proxyConfig().getNoProxyUrls()
						: "disabled");

		logger.info("Using trust-store with {} to validate local and remote DSF server certificates",
				KeyStoreFormatter
						.toSubjectsFromCertificates(propertiesConfig.getDsfClientTrustedServerCas(),
								X500PrincipalFormat.RFC1779)
						.values().stream().collect(Collectors.joining("; ", "[", "]")));
	}

	@Bean
	public ClientProvider clientProvider()
	{
		char[] keyStorePassword = UUID.randomUUID().toString().toCharArray();
		KeyStore keyStore = propertiesConfig.getDsfClientKeyStore(keyStorePassword);
		KeyStore trustStore = propertiesConfig.getDsfClientTrustedServerCas();

		return new ClientProviderImpl(fhirConfig.fhirContext(), propertiesConfig.getDsfServerBaseUrl(),
				propertiesConfig.getDsfClientReadTimeoutLocal(), propertiesConfig.getDsfClientConnectTimeoutLocal(),
				propertiesConfig.getDsfClientVerboseLocal(), trustStore, keyStore, keyStorePassword, getWebsocketUrl(),
				trustStore, keyStore, keyStorePassword, propertiesConfig.proxyConfig(),
				buildInfoReaderConfig.buildInfoReader().getUserAgentValue());
	}

	private String getWebsocketUrl()
	{
		String baseUrl = propertiesConfig.getDsfServerBaseUrl();

		if (baseUrl.startsWith("https://"))
			return baseUrl.replace("https://", "wss://") + "/ws";
		else if (baseUrl.startsWith("http://"))
			return baseUrl.replace("http://", "ws://") + "/ws";
		else
			throw new RuntimeException("server base url (" + baseUrl + ") does not start with https:// or http://");
	}
}
