package dev.dsf.bpe.spring.config;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.UUID;

import org.bouncycastle.pkcs.PKCSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.dsf.bpe.client.LocalFhirClientProvider;
import dev.dsf.bpe.client.LocalFhirClientProviderImpl;

@Configuration
public class FhirClientConfig extends AbstractConfig implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(FhirClientConfig.class);

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
				"Local webservice client config: {trustStorePath: {}, certificatePath: {}, privateKeyPath: {}, privateKeyPassword: {},"
						+ " url: {}, proxy: {}}",
				propertiesConfig.getClientCertificateTrustStoreFile(), propertiesConfig.getClientCertificateFile(),
				propertiesConfig.getClientCertificatePrivateKeyFile(),
				propertiesConfig.getClientCertificatePrivateKeyFilePassword() != null ? "***" : "null",
				propertiesConfig.getFhirServerBaseUrl(),
				propertiesConfig.proxyConfig().isEnabled(propertiesConfig.getFhirServerBaseUrl()) ? "enabled"
						: "disabled");
		logger.info(
				"Local websocket client config: {trustStorePath: {}, certificatePath: {}, privateKeyPath: {}, privateKeyPassword: {},"
						+ " url: {}, proxy: {}}",
				propertiesConfig.getClientCertificateTrustStoreFile(), propertiesConfig.getClientCertificateFile(),
				propertiesConfig.getClientCertificatePrivateKeyFile(),
				propertiesConfig.getClientCertificatePrivateKeyFilePassword() != null ? "***" : "null",
				getWebsocketUrl(),
				propertiesConfig.proxyConfig().isEnabled(getWebsocketUrl()) ? "enabled" : "disabled");
		logger.info(
				"Remote webservice client config: {trustStorePath: {}, certificatePath: {}, privateKeyPath: {}, privateKeyPassword: {},"
						+ " proxy: {}}",
				propertiesConfig.getClientCertificateTrustStoreFile(), propertiesConfig.getClientCertificateFile(),
				propertiesConfig.getClientCertificatePrivateKeyFile(),
				propertiesConfig.getClientCertificatePrivateKeyFilePassword() != null ? "***" : "null",
				propertiesConfig.proxyConfig().isEnabled()
						? "enabled if remote server not in " + propertiesConfig.proxyConfig().getNoProxyUrls()
						: "disabled");
	}

	@Bean
	public LocalFhirClientProvider clientProvider()
	{
		char[] keyStorePassword = UUID.randomUUID().toString().toCharArray();

		try
		{
			KeyStore webserviceKeyStore = createKeyStore(propertiesConfig.getClientCertificateFile(),
					propertiesConfig.getClientCertificatePrivateKeyFile(),
					propertiesConfig.getClientCertificatePrivateKeyFilePassword(), keyStorePassword);
			KeyStore webserviceTrustStore = createTrustStore(propertiesConfig.getClientCertificateTrustStoreFile());

			return new LocalFhirClientProviderImpl(fhirConfig.fhirContext(), propertiesConfig.getFhirServerBaseUrl(),
					propertiesConfig.getWebserviceClientLocalReadTimeout(),
					propertiesConfig.getWebserviceClientLocalConnectTimeout(),
					propertiesConfig.getWebserviceClientLocalVerbose(), webserviceTrustStore, webserviceKeyStore,
					keyStorePassword, getWebsocketUrl(), webserviceTrustStore, webserviceKeyStore, keyStorePassword,
					propertiesConfig.proxyConfig(), buildInfoReaderConfig.buildInfoReader());
		}
		catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException | PKCSException e)
		{
			throw new RuntimeException(e);
		}
	}

	private String getWebsocketUrl()
	{
		String baseUrl = propertiesConfig.getFhirServerBaseUrl();

		if (baseUrl.startsWith("https://"))
			return baseUrl.replace("https://", "wss://") + "/ws";
		else if (baseUrl.startsWith("http://"))
			return baseUrl.replace("http://", "ws://") + "/ws";
		else
			throw new RuntimeException("server base url (" + baseUrl + ") does not start with https:// or http://");
	}
}
