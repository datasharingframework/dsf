package dev.dsf.fhir.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.rwh.utils.crypto.CertificateAuthority;
import de.rwh.utils.crypto.CertificateHelper;
import de.rwh.utils.crypto.CertificationRequestBuilder;
import de.rwh.utils.crypto.io.CertificateWriter;
import de.rwh.utils.crypto.io.PemIo;

public class X509Certificates extends ExternalResource
{
	public static final class ClientCertificate
	{
		private final X509Certificate certificate;
		private final KeyStore trustStore;
		private final KeyStore keyStore;
		private final char[] keyStorePassword;

		ClientCertificate(X509Certificate certificate, KeyStore trustStore, KeyStore keyStore, char[] keyStorePassword)
		{
			this.certificate = certificate;
			this.trustStore = trustStore;
			this.keyStore = keyStore;
			this.keyStorePassword = keyStorePassword;
		}

		public X509Certificate getCertificate()
		{
			return certificate;
		}

		public KeyStore getTrustStore()
		{
			return trustStore;
		}

		public KeyStore getKeyStore()
		{
			return keyStore;
		}

		public char[] getKeyStorePassword()
		{
			return keyStorePassword;
		}

		public String getCertificateSha512ThumbprintHex()
		{
			try
			{
				return Hex.encodeHexString(MessageDigest.getInstance("SHA-512").digest(getCertificate().getEncoded()));
			}
			catch (CertificateEncodingException | NoSuchAlgorithmException e)
			{
				logger.error("Error while calculating SHA-512 certificate thumbprint", e);
				throw new RuntimeException(e);
			}
		}
	}

	private static final Logger logger = LoggerFactory.getLogger(X509Certificates.class);
	private static final BouncyCastleProvider provider = new BouncyCastleProvider();
	public static final char[] PASSWORD = "password".toCharArray();

	private boolean beforeRun;

	private final X509Certificates parent;

	private ClientCertificate clientCertificate;
	private ClientCertificate practitionerClientCertificate;
	private ClientCertificate externalClientCertificate;

	private Path caCertificateFile;
	private Path serverCertificateFile;
	private Path serverCertificatePrivateKeyFile;
	private Path clientCertificateFile;
	private Path clientCertificatePrivateKeyFile;
	private Path externalClientCertificateFile;
	private Path externalClientCertificatePrivateKeyFile;
	private Path practitionerClientCertificateFile;
	private Path practitionerClientCertificatePrivateKeyFile;

	private List<Path> filesToDelete;

	public X509Certificates()
	{
		this(null);
	}

	public X509Certificates(X509Certificates parent)
	{
		this.parent = parent;
	}

	private boolean parentBeforeRan()
	{
		return parent != null && parent.beforeRun;
	}

	@Override
	protected void before() throws Throwable
	{
		if (parentBeforeRan())
			logger.debug("X509Certificates created by parent");
		else
			createX509Certificates();

		beforeRun = true;
	}

	@Override
	protected void after()
	{
		if (parentBeforeRan())
			logger.debug("X509Certificates will be deleted by parent");
		else
			deleteX509Certificates();
	}

	public ClientCertificate getClientCertificate()
	{
		if (parentBeforeRan())
			return parent.getClientCertificate();
		else
			return clientCertificate;
	}

	public ClientCertificate getExternalClientCertificate()
	{
		if (parentBeforeRan())
			return parent.getExternalClientCertificate();
		else
			return externalClientCertificate;
	}

	public ClientCertificate getPractitionerClientCertificate()
	{
		if (parentBeforeRan())
			return parent.getPractitionerClientCertificate();
		else
			return practitionerClientCertificate;
	}

	public Path getCaCertificateFile()
	{
		if (parentBeforeRan())
			return parent.getCaCertificateFile();

		return caCertificateFile;
	}

	public Path getServerCertificateFile()
	{
		if (parentBeforeRan())
			return parent.getServerCertificateFile();

		return serverCertificateFile;
	}

	public Path getServerCertificatePrivateKeyFile()
	{
		if (parentBeforeRan())
			return parent.getServerCertificatePrivateKeyFile();

		return serverCertificatePrivateKeyFile;
	}

	public Path getClientCertificateFile()
	{
		if (parentBeforeRan())
			return parent.getClientCertificateFile();

		return clientCertificateFile;
	}

	public Path getClientCertificatePrivateKeyFile()
	{
		if (parentBeforeRan())
			return parent.getClientCertificatePrivateKeyFile();

		return clientCertificatePrivateKeyFile;
	}

	public Path getExternalClientCertificateFile()
	{
		if (parentBeforeRan())
			return parent.getExternalClientCertificateFile();

		return externalClientCertificateFile;
	}

	public Path getExternalClientCertificatePrivateKeyFile()
	{
		if (parentBeforeRan())
			return parent.getExternalClientCertificatePrivateKeyFile();

		return externalClientCertificatePrivateKeyFile;
	}

	public Path getPractitionerClientCertificateFile()
	{
		if (parentBeforeRan())
			return parent.getPractitionerClientCertificateFile();

		return practitionerClientCertificateFile;
	}

	public Path getPractitionerClientCertificatePrivateKeyFile()
	{
		if (parentBeforeRan())
			return parent.getPractitionerClientCertificatePrivateKeyFile();

		return practitionerClientCertificatePrivateKeyFile;
	}

	private void createX509Certificates() throws InvalidKeyException, NoSuchAlgorithmException, KeyStoreException,
			CertificateException, OperatorCreationException, IllegalStateException, IOException, InvalidKeySpecException
	{
		logger.info("Creating certificates ...");

		Path caCertificateFile = Paths.get("target", UUID.randomUUID().toString() + ".pem");
		Path serverCertificateFile = Paths.get("target", UUID.randomUUID().toString() + ".pem");
		Path serverCertificatePrivateKeyFile = Paths.get("target", UUID.randomUUID().toString() + ".pem");
		Path clientCertificateFile = Paths.get("target", UUID.randomUUID().toString() + ".pem");
		Path clientCertificatePrivateKeyFile = Paths.get("target", UUID.randomUUID().toString() + ".pem");
		Path externalClientCertificateFile = Paths.get("target", UUID.randomUUID().toString() + ".pem");
		Path externalClientCertificatePrivateKeyFile = Paths.get("target", UUID.randomUUID().toString() + ".pem");
		Path practitionerClientCertificateFile = Paths.get("target", UUID.randomUUID().toString() + ".pem");
		Path practitionerClientCertificatePrivateKeyFile = Paths.get("target", UUID.randomUUID().toString() + ".pem");

		CertificateAuthority.registerBouncyCastleProvider();

		CertificateAuthority ca = new CertificateAuthority("DE", null, null, null, null, "test-ca");
		ca.initialize();
		X509Certificate caCertificate = ca.getCertificate();

		PemIo.writeX509CertificateToPem(caCertificate, caCertificateFile);

		// -- server
		X500Name serverSubject = CertificationRequestBuilder.createSubject("DE", null, null, null, null, "test-server");
		KeyPair serverRsaKeyPair = CertificationRequestBuilder.createRsaKeyPair4096Bit();
		JcaPKCS10CertificationRequest serverRequest = CertificationRequestBuilder
				.createServerCertificationRequest(serverSubject, serverRsaKeyPair, null, "localhost");

		X509Certificate serverCertificate = ca.signWebServerCertificate(serverRequest);

		CertificateWriter.toPkcs12(serverCertificateFile, serverRsaKeyPair.getPrivate(), PASSWORD, serverCertificate,
				caCertificate, "test-server");

		PemIo.writeX509CertificateToPem(serverCertificate, serverCertificateFile);
		PemIo.writeAes128EncryptedPrivateKeyToPkcs8(provider, serverCertificatePrivateKeyFile,
				serverRsaKeyPair.getPrivate(), PASSWORD);

		// server --

		// -- client
		X500Name clientSubject = CertificationRequestBuilder.createSubject("DE", null, null, null, null, "test-client");
		KeyPair clientRsaKeyPair = CertificationRequestBuilder.createRsaKeyPair4096Bit();
		JcaPKCS10CertificationRequest clientRequest = CertificationRequestBuilder
				.createClientCertificationRequest(clientSubject, clientRsaKeyPair);

		X509Certificate clientCertificate = ca.signWebClientCertificate(clientRequest);

		KeyStore clientKeyStore = CertificateHelper.toPkcs12KeyStore(clientRsaKeyPair.getPrivate(),
				new Certificate[] { clientCertificate, caCertificate }, "test-client", PASSWORD);

		PemIo.writeX509CertificateToPem(clientCertificate, clientCertificateFile);
		PemIo.writeAes128EncryptedPrivateKeyToPkcs8(provider, clientCertificatePrivateKeyFile,
				clientRsaKeyPair.getPrivate(), PASSWORD);
		// client --

		// -- external client
		X500Name externalClientSubject = CertificationRequestBuilder.createSubject("DE", null, null, null, null,
				"external-client");
		KeyPair externalClientRsaKeyPair = CertificationRequestBuilder.createRsaKeyPair4096Bit();
		JcaPKCS10CertificationRequest externalClientRequest = CertificationRequestBuilder
				.createClientCertificationRequest(externalClientSubject, externalClientRsaKeyPair);

		X509Certificate externalClientCertificate = ca.signWebClientCertificate(externalClientRequest);

		KeyStore externalClientKeyStore = CertificateHelper.toPkcs12KeyStore(externalClientRsaKeyPair.getPrivate(),
				new Certificate[] { externalClientCertificate, caCertificate }, "external-client", PASSWORD);

		CertificateWriter.toPkcs12(externalClientCertificateFile, externalClientRsaKeyPair.getPrivate(), PASSWORD,
				externalClientCertificate, caCertificate, "client");

		PemIo.writeX509CertificateToPem(externalClientCertificate, externalClientCertificateFile);
		PemIo.writeAes128EncryptedPrivateKeyToPkcs8(provider, externalClientCertificatePrivateKeyFile,
				externalClientRsaKeyPair.getPrivate(), PASSWORD);
		// external client --

		// -- practitioner client
		X500Name practitionerClientSubject = CertificationRequestBuilder.createSubject("DE", null, null, null, null,
				"practitioner-client");
		KeyPair practitionerClientRsaKeyPair = CertificationRequestBuilder.createRsaKeyPair4096Bit();
		JcaPKCS10CertificationRequest practitionerClientRequest = CertificationRequestBuilder
				.createClientCertificationRequest(practitionerClientSubject, practitionerClientRsaKeyPair,
						"practitioner@test.org");

		X509Certificate practitionerClientCertificate = ca.signWebClientCertificate(practitionerClientRequest);

		KeyStore practitionerClientKeyStore = CertificateHelper.toPkcs12KeyStore(
				practitionerClientRsaKeyPair.getPrivate(),
				new Certificate[] { practitionerClientCertificate, caCertificate }, "practitioner-client", PASSWORD);

		CertificateWriter.toPkcs12(practitionerClientCertificateFile, practitionerClientRsaKeyPair.getPrivate(),
				PASSWORD, practitionerClientCertificate, caCertificate, "client");

		PemIo.writeX509CertificateToPem(practitionerClientCertificate, practitionerClientCertificateFile);
		PemIo.writeAes128EncryptedPrivateKeyToPkcs8(provider, practitionerClientCertificatePrivateKeyFile,
				practitionerClientRsaKeyPair.getPrivate(), PASSWORD);
		// practitioner client --

		this.clientCertificate = new ClientCertificate(clientCertificate,
				CertificateHelper.extractTrust(clientKeyStore), clientKeyStore, PASSWORD);
		this.externalClientCertificate = new ClientCertificate(externalClientCertificate,
				CertificateHelper.extractTrust(externalClientKeyStore), externalClientKeyStore, PASSWORD);
		this.practitionerClientCertificate = new ClientCertificate(practitionerClientCertificate,
				CertificateHelper.extractTrust(practitionerClientKeyStore), practitionerClientKeyStore, PASSWORD);

		this.caCertificateFile = caCertificateFile;
		this.serverCertificateFile = serverCertificateFile;
		this.serverCertificatePrivateKeyFile = serverCertificatePrivateKeyFile;
		this.clientCertificateFile = clientCertificateFile;
		this.clientCertificatePrivateKeyFile = clientCertificatePrivateKeyFile;
		this.externalClientCertificateFile = externalClientCertificateFile;
		this.externalClientCertificatePrivateKeyFile = externalClientCertificatePrivateKeyFile;
		this.practitionerClientCertificateFile = practitionerClientCertificateFile;
		this.practitionerClientCertificatePrivateKeyFile = practitionerClientCertificatePrivateKeyFile;

		this.filesToDelete = Arrays.asList(caCertificateFile, serverCertificateFile, serverCertificatePrivateKeyFile,
				clientCertificateFile, clientCertificatePrivateKeyFile, externalClientCertificateFile,
				externalClientCertificatePrivateKeyFile, practitionerClientCertificateFile,
				practitionerClientCertificatePrivateKeyFile);
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
