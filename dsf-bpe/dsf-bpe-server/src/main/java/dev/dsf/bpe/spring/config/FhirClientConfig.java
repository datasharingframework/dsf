package dev.dsf.bpe.spring.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.UUID;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pkcs.PKCSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.rwh.utils.crypto.CertificateHelper;
import de.rwh.utils.crypto.io.CertificateReader;
import de.rwh.utils.crypto.io.PemIo;
import dev.dsf.bpe.client.FhirClientProvider;
import dev.dsf.bpe.client.FhirClientProviderImpl;
import dev.dsf.fhir.service.ReferenceCleaner;
import dev.dsf.fhir.service.ReferenceCleanerImpl;
import dev.dsf.fhir.service.ReferenceExtractor;
import dev.dsf.fhir.service.ReferenceExtractorImpl;

@Configuration
public class FhirClientConfig implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(FhirClientConfig.class);
	private static final BouncyCastleProvider provider = new BouncyCastleProvider();

	@Autowired
	private PropertiesConfig propertiesConfig;

	@Autowired
	private FhirConfig fhirConfig;

	@Override
	public void afterPropertiesSet() throws Exception
	{
		logger.info(
				"Local webservice client config: {trustStorePath: {}, certificatePath: {}, privateKeyPath: {}, privateKeyPassword: {},"
						+ " url: {}, proxy: {}}",
				propertiesConfig.getClientCertificateTrustStoreFile(), propertiesConfig.getClientCertificateFile(),
				propertiesConfig.getClientCertificatePrivateKeyFile(),
				propertiesConfig.getClientCertificatePrivateKeyFilePassword() != null ? "***" : "null",
				propertiesConfig.getServerBaseUrl(),
				propertiesConfig.proxyConfig().isEnabled(propertiesConfig.getServerBaseUrl()) ? "enabled" : "disabled");
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
	public ReferenceCleaner referenceCleaner()
	{
		return new ReferenceCleanerImpl(referenceExtractor());
	}

	@Bean
	public ReferenceExtractor referenceExtractor()
	{
		return new ReferenceExtractorImpl();
	}

	@Bean
	public FhirClientProvider clientProvider()
	{
		char[] keyStorePassword = UUID.randomUUID().toString().toCharArray();

		try
		{
			KeyStore webserviceKeyStore = createKeyStore(propertiesConfig.getClientCertificateFile(),
					propertiesConfig.getClientCertificatePrivateKeyFile(),
					propertiesConfig.getClientCertificatePrivateKeyFilePassword(), keyStorePassword);
			KeyStore webserviceTrustStore = createTrustStore(propertiesConfig.getClientCertificateTrustStoreFile());

			return new FhirClientProviderImpl(fhirConfig.fhirContext(), referenceCleaner(),
					propertiesConfig.getServerBaseUrl(), propertiesConfig.getWebserviceClientLocalReadTimeout(),
					propertiesConfig.getWebserviceClientLocalConnectTimeout(),
					propertiesConfig.getWebserviceClientLocalVerbose(), webserviceTrustStore, webserviceKeyStore,
					keyStorePassword, propertiesConfig.getWebserviceClientRemoteReadTimeout(),
					propertiesConfig.getWebserviceClientRemoteConnectTimeout(),
					propertiesConfig.getWebserviceClientRemoteVerbose(), getWebsocketUrl(), webserviceTrustStore,
					webserviceKeyStore, keyStorePassword, propertiesConfig.proxyConfig());
		}
		catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException | PKCSException e)
		{
			throw new RuntimeException(e);
		}
	}

	private String getWebsocketUrl()
	{
		String baseUrl = propertiesConfig.getServerBaseUrl();

		if (baseUrl.startsWith("https://"))
			return baseUrl.replace("https://", "wss://") + "/ws";
		else if (baseUrl.startsWith("http://"))
			return baseUrl.replace("http://", "ws://") + "/ws";
		else
			throw new RuntimeException("server base url (" + baseUrl + ") does not start with https:// or http://");
	}

	private KeyStore createTrustStore(String trustStoreFile)
			throws IOException, NoSuchAlgorithmException, CertificateException, KeyStoreException
	{
		Path trustStorePath = Paths.get(trustStoreFile);

		if (!Files.isReadable(trustStorePath))
			throw new IOException("Trust store file '" + trustStorePath.toString() + "' not readable");

		return CertificateReader.allFromCer(trustStorePath);
	}

	private KeyStore createKeyStore(String certificateFile, String privateKeyFile, char[] privateKeyPassword,
			char[] keyStorePassword)
			throws IOException, PKCSException, CertificateException, KeyStoreException, NoSuchAlgorithmException
	{
		Path certificatePath = Paths.get(certificateFile);
		Path privateKeyPath = Paths.get(privateKeyFile);

		if (!Files.isReadable(certificatePath))
			throw new IOException("Certificate file '" + certificatePath.toString() + "' not readable");
		if (!Files.isReadable(certificatePath))
			throw new IOException("Private key file '" + privateKeyPath.toString() + "' not readable");

		X509Certificate certificate = PemIo.readX509CertificateFromPem(certificatePath);
		PrivateKey privateKey = PemIo.readPrivateKeyFromPem(provider, privateKeyPath, privateKeyPassword);

		String subjectCommonName = CertificateHelper.getSubjectCommonName(certificate);
		return CertificateHelper.toJksKeyStore(privateKey, new Certificate[] { certificate }, subjectCommonName,
				keyStorePassword);
	}
}
