package dev.dsf.fhir.spring.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.UUID;

import org.bouncycastle.pkcs.PKCSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.hsheilbronn.mi.utils.crypto.cert.CertificateValidator;
import de.hsheilbronn.mi.utils.crypto.io.PemReader;
import de.hsheilbronn.mi.utils.crypto.keypair.KeyPairValidator;
import de.hsheilbronn.mi.utils.crypto.keystore.KeyStoreCreator;
import dev.dsf.fhir.client.ClientProvider;
import dev.dsf.fhir.client.ClientProviderImpl;

@Configuration
public class ClientConfig implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(ClientConfig.class);

	@Autowired
	private PropertiesConfig propertiesConfig;

	@Autowired
	private FhirConfig fhirConfig;

	@Autowired
	private DaoConfig daoConfig;

	@Autowired
	private HelperConfig helperConfig;

	@Autowired
	private ReferenceConfig referenceConfig;

	@Autowired
	private BuildInfoReaderConfig buildInfoReaderConfig;

	@Bean
	public ClientProvider clientProvider()
	{
		char[] keyStorePassword = UUID.randomUUID().toString().toCharArray();

		try
		{
			KeyStore keyStore = createKeyStore(propertiesConfig.getDsfClientCertificateFile(),
					propertiesConfig.getDsfClientCertificatePrivateKeyFile(),
					propertiesConfig.getDsfClientCertificatePrivateKeyFilePassword(), keyStorePassword);
			KeyStore trustStore = propertiesConfig.getDsfClientTrustedServerCas();

			return new ClientProviderImpl(trustStore, keyStore, keyStorePassword,
					propertiesConfig.getDsfClientReadTimeout(), propertiesConfig.getDsfClientConnectTimeout(),
					propertiesConfig.proxyConfig(), propertiesConfig.getDsfClientVerbose(), fhirConfig.fhirContext(),
					referenceConfig.referenceCleaner(), daoConfig.endpointDao(), helperConfig.exceptionHandler(),
					buildInfoReaderConfig.buildInfoReader());
		}
		catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException | PKCSException e)
		{
			throw new RuntimeException(e);
		}
	}

	private KeyStore createKeyStore(String certificateFile, String privateKeyFile, char[] privateKeyPassword,
			char[] keyStorePassword)
			throws IOException, PKCSException, CertificateException, KeyStoreException, NoSuchAlgorithmException
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
			throw new IOException("First certificate from '" + certificatePath.normalize().toAbsolutePath().toString()
					+ "' not a client certificate");
		else if (!KeyPairValidator.matches(privateKey, certificates.get(0).getPublicKey()))
			throw new IOException("Private-key at '" + privateKeyPath.normalize().toAbsolutePath().toString()
					+ "' not matching Public-key from " + (certificates.size() > 1 ? "first " : "") + "certificate at '"
					+ certificatePath.normalize().toAbsolutePath().toString() + "'");

		return KeyStoreCreator.jksForPrivateKeyAndCertificateChain(privateKey, keyStorePassword, certificates);
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		logger.info(
				"Remote webservice client config: {trustStorePath: {}, certificatePath: {}, privateKeyPath: {}, privateKeyPassword: {},"
						+ " proxy: {}, no_proxy: {}}",
				propertiesConfig.getDsfClientTrustedServerCasFileOrFolder(),
				propertiesConfig.getDsfClientCertificateFile(),
				propertiesConfig.getDsfClientCertificatePrivateKeyFile(),
				propertiesConfig.getDsfClientCertificatePrivateKeyFilePassword() != null ? "***" : "null",
				propertiesConfig.proxyConfig().isEnabled() ? "enabled" : "disabled",
				propertiesConfig.proxyConfig().getNoProxyUrls());
	}
}
