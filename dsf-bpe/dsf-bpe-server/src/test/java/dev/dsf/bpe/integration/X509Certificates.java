package dev.dsf.bpe.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.time.Period;
import java.util.List;
import java.util.UUID;

import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.operator.OperatorCreationException;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.hsheilbronn.mi.utils.crypto.ca.CertificateAuthority;
import de.hsheilbronn.mi.utils.crypto.ca.CertificationRequest;
import de.hsheilbronn.mi.utils.crypto.ca.CertificationRequest.CertificationRequestAndPrivateKey;
import de.hsheilbronn.mi.utils.crypto.io.PemWriter;
import de.hsheilbronn.mi.utils.crypto.keystore.KeyStoreCreator;

public class X509Certificates extends ExternalResource
{
	public static record CertificateAndPrivateKey(X509Certificate caCertificate, X509Certificate certificate,
			PrivateKey privateKey)
	{
		public KeyStore trustStore()
		{
			return KeyStoreCreator.jksForTrustedCertificates(caCertificate);
		}

		public KeyStore keyStore()
		{
			return KeyStoreCreator.jksForPrivateKeyAndCertificateChain(privateKey, PASSWORD, certificate);
		}

		public char[] keyStorePassword()
		{
			return PASSWORD;
		}

		public String certificateSha512ThumbprintHex()
		{
			try
			{
				return Hex.encodeHexString(MessageDigest.getInstance("SHA-512").digest(certificate().getEncoded()));
			}
			catch (CertificateEncodingException | NoSuchAlgorithmException e)
			{
				logger.error("Error while calculating SHA-512 certificate thumbprint", e);
				throw new RuntimeException(e);
			}
		}
	}

	private static final Logger logger = LoggerFactory.getLogger(X509Certificates.class);

	public static final char[] PASSWORD = "password".toCharArray();

	private CertificateAndPrivateKey bpeServerCertificate;
	private CertificateAndPrivateKey fhirServerCertificate;
	private CertificateAndPrivateKey clientCertificate;
	private CertificateAndPrivateKey dicUserClientCertificate;
	private CertificateAndPrivateKey uacUserClientCertificate;
	private CertificateAndPrivateKey externalClientCertificate;

	private X509Certificate caCertificate;
	private Path caCertificateFile;
	private Path clientCertificateFile;
	private Path clientCertificatePrivateKeyFile;
	private Path externalClientCertificateFile;
	private Path externalClientCertificatePrivateKeyFile;
	private Path dicUserClientCertificateFile;
	private Path dicUserClientCertificatePrivateKeyFile;
	private Path uacUserClientCertificateFile;
	private Path uacUserClientCertificatePrivateKeyFile;

	private List<Path> filesToDelete;

	@Override
	protected void before() throws Throwable
	{
		createX509Certificates();
	}

	@Override
	protected void after()
	{
		deleteX509Certificates();
	}

	public CertificateAndPrivateKey getBpeServerCertificate()
	{
		return bpeServerCertificate;
	}

	public CertificateAndPrivateKey getFhirServerCertificate()
	{
		return fhirServerCertificate;
	}

	public CertificateAndPrivateKey getClientCertificate()
	{
		return clientCertificate;
	}

	public CertificateAndPrivateKey getExternalClientCertificate()
	{
		return externalClientCertificate;
	}

	public CertificateAndPrivateKey getDicUserClientCertificate()
	{
		return dicUserClientCertificate;
	}

	public CertificateAndPrivateKey getUacUserClientCertificate()
	{
		return uacUserClientCertificate;
	}

	public X509Certificate getCaCertificate()
	{
		return caCertificate;
	}

	public Path getCaCertificateFile()
	{
		return caCertificateFile;
	}

	public Path getClientCertificateFile()
	{
		return clientCertificateFile;
	}

	public Path getClientCertificatePrivateKeyFile()
	{
		return clientCertificatePrivateKeyFile;
	}

	public Path getExternalClientCertificateFile()
	{
		return externalClientCertificateFile;
	}

	public Path getExternalClientCertificatePrivateKeyFile()
	{
		return externalClientCertificatePrivateKeyFile;
	}

	public Path getDicUserClientCertificateFile()
	{
		return dicUserClientCertificateFile;
	}

	public Path getDicUserClientCertificatePrivateKeyFile()
	{
		return dicUserClientCertificatePrivateKeyFile;
	}

	public Path getUacUserClientCertificateFile()
	{
		return uacUserClientCertificateFile;
	}

	public Path getUacUserClientCertificatePrivateKeyFile()
	{
		return uacUserClientCertificatePrivateKeyFile;
	}

	private void createX509Certificates() throws InvalidKeyException, NoSuchAlgorithmException, KeyStoreException,
			CertificateException, OperatorCreationException, IllegalStateException, IOException, InvalidKeySpecException
	{
		logger.info("Creating certificates ...");

		Path caCertificateFile = Paths.get("target", UUID.randomUUID().toString() + ".pem");
		Path clientCertificateFile = Paths.get("target", UUID.randomUUID().toString() + ".pem");
		Path clientCertificatePrivateKeyFile = Paths.get("target", UUID.randomUUID().toString() + ".pem");
		Path externalClientCertificateFile = Paths.get("target", UUID.randomUUID().toString() + ".pem");
		Path externalClientCertificatePrivateKeyFile = Paths.get("target", UUID.randomUUID().toString() + ".pem");
		Path dicUserClientCertificateFile = Paths.get("target", UUID.randomUUID().toString() + ".pem");
		Path dicUserClientCertificatePrivateKeyFile = Paths.get("target", UUID.randomUUID().toString() + ".pem");
		Path uacUserClientCertificateFile = Paths.get("target", UUID.randomUUID().toString() + ".pem");
		Path uacUserClientCertificatePrivateKeyFile = Paths.get("target", UUID.randomUUID().toString() + ".pem");

		CertificateAuthority ca = CertificateAuthority
				.builderSha384EcdsaSecp384r1("DE", null, null, null, null, "Junit Test CA")
				.setValidityPeriod(Period.ofDays(1)).build();
		X509Certificate caCertificate = ca.getCertificate();
		PemWriter.writeCertificate(caCertificate, caCertificateFile);

		// -- bpe server
		CertificationRequestAndPrivateKey bpeServerRequest = CertificationRequest
				.builder(ca, "DE", null, null, null, null, "bpe-server").generateKeyPair().addDnsName("localhost")
				.build();
		X509Certificate bpeServerCertificate = ca.signServerCertificate(bpeServerRequest, Period.ofDays(1));
		// bpe server --

		// -- fhir server
		CertificationRequestAndPrivateKey fhirServerRequest = CertificationRequest
				.builder(ca, "DE", null, null, null, null, "fhir-server").generateKeyPair().addDnsName("localhost")
				.build();
		X509Certificate fhirServerCertificate = ca.signServerCertificate(fhirServerRequest, Period.ofDays(1));
		// fhir server --

		// -- client
		CertificationRequestAndPrivateKey clientRequest = CertificationRequest
				.builder(ca, "DE", null, null, null, null, "test-client").generateKeyPair().build();
		X509Certificate clientCertificate = ca.signClientCertificate(clientRequest, Period.ofDays(1));
		PemWriter.writeCertificate(clientCertificate, clientCertificateFile);
		PemWriter.writePrivateKey(clientRequest.getPrivateKey()).asPkcs8().encryptedAes128(PASSWORD)
				.toFile(clientCertificatePrivateKeyFile);
		// client --

		// -- external client
		CertificationRequestAndPrivateKey externalClientRequest = CertificationRequest
				.builder(ca, "DE", null, null, null, null, "external-client").generateKeyPair().build();
		X509Certificate externalClientCertificate = ca.signClientCertificate(externalClientRequest, Period.ofDays(1));
		PemWriter.writeCertificate(externalClientCertificate, externalClientCertificateFile);
		PemWriter.writePrivateKey(externalClientRequest.getPrivateKey()).asPkcs8().encryptedAes128(PASSWORD)
				.toFile(externalClientCertificatePrivateKeyFile);
		// external client --

		// -- dic user client
		CertificationRequestAndPrivateKey dicUserClientRequest = CertificationRequest
				.builder(ca, "DE", null, null, null, null, "dic-user-client").generateKeyPair()
				.setEmail("dic-user@test.org").build();
		X509Certificate dicUserClientCertificate = ca.signClientCertificate(dicUserClientRequest);
		PemWriter.writeCertificate(dicUserClientCertificate, dicUserClientCertificateFile);
		PemWriter.writePrivateKey(dicUserClientRequest.getPrivateKey()).asPkcs8().encryptedAes128(PASSWORD)
				.toFile(dicUserClientCertificatePrivateKeyFile);
		// dic user client --

		// -- uac user client
		CertificationRequestAndPrivateKey uacUserClientRequest = CertificationRequest
				.builder(ca, "DE", null, null, null, null, "uac-user-client").generateKeyPair()
				.setEmail("uac-user@test.org").build();
		X509Certificate uacUserClientCertificate = ca.signClientCertificate(uacUserClientRequest);
		PemWriter.writeCertificate(uacUserClientCertificate, uacUserClientCertificateFile);
		PemWriter.writePrivateKey(uacUserClientRequest.getPrivateKey()).asPkcs8().encryptedAes128(PASSWORD)
				.toFile(uacUserClientCertificatePrivateKeyFile);
		// uac user client --

		this.caCertificate = caCertificate;
		this.bpeServerCertificate = new CertificateAndPrivateKey(caCertificate, bpeServerCertificate,
				bpeServerRequest.getPrivateKey());
		this.fhirServerCertificate = new CertificateAndPrivateKey(caCertificate, fhirServerCertificate,
				fhirServerRequest.getPrivateKey());
		this.clientCertificate = new CertificateAndPrivateKey(caCertificate, clientCertificate,
				clientRequest.getPrivateKey());
		this.externalClientCertificate = new CertificateAndPrivateKey(caCertificate, externalClientCertificate,
				externalClientRequest.getPrivateKey());
		this.dicUserClientCertificate = new CertificateAndPrivateKey(caCertificate, dicUserClientCertificate,
				dicUserClientRequest.getPrivateKey());
		this.uacUserClientCertificate = new CertificateAndPrivateKey(caCertificate, uacUserClientCertificate,
				uacUserClientRequest.getPrivateKey());

		this.caCertificateFile = caCertificateFile;
		this.clientCertificateFile = clientCertificateFile;
		this.clientCertificatePrivateKeyFile = clientCertificatePrivateKeyFile;
		this.externalClientCertificateFile = externalClientCertificateFile;
		this.externalClientCertificatePrivateKeyFile = externalClientCertificatePrivateKeyFile;
		this.dicUserClientCertificateFile = dicUserClientCertificateFile;
		this.dicUserClientCertificatePrivateKeyFile = dicUserClientCertificatePrivateKeyFile;
		this.uacUserClientCertificateFile = uacUserClientCertificateFile;
		this.uacUserClientCertificatePrivateKeyFile = uacUserClientCertificatePrivateKeyFile;

		filesToDelete = List.of(caCertificateFile, clientCertificateFile, clientCertificatePrivateKeyFile,
				externalClientCertificateFile, externalClientCertificatePrivateKeyFile, dicUserClientCertificateFile,
				dicUserClientCertificatePrivateKeyFile, uacUserClientCertificateFile,
				uacUserClientCertificatePrivateKeyFile);
	}

	private void deleteX509Certificates()
	{
		logger.info("Deleting certificate files {} ...", filesToDelete);
		filesToDelete.forEach(this::deleteFile);
	}

	private void deleteFile(Path file)
	{
		try
		{
			Files.delete(file);
		}
		catch (IOException e)
		{
			logger.error("Error while deleting certificate file {}, error: {}", file.toString(), e.toString());
		}
	}
}
