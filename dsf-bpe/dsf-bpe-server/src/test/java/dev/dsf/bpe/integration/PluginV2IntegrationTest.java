package dev.dsf.bpe.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.X509Certificate;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import de.hsheilbronn.mi.utils.crypto.ca.CertificateAuthority;
import de.hsheilbronn.mi.utils.crypto.ca.CertificationRequest;
import de.hsheilbronn.mi.utils.crypto.ca.CertificationRequest.CertificationRequestAndPrivateKey;
import de.hsheilbronn.mi.utils.crypto.io.KeyStoreWriter;
import de.hsheilbronn.mi.utils.crypto.io.PemWriter;
import de.hsheilbronn.mi.utils.crypto.keystore.KeyStoreCreator;

public class PluginV2IntegrationTest extends AbstractPluginIntegrationTest
{
	private static final String PROCESS_VERSION = "2.0";

	public PluginV2IntegrationTest()
	{
		super(PROCESS_VERSION);
	}

	@BeforeClass
	public static void verifyProcessPluginResourcesExist() throws Exception
	{
		verifyProcessPluginResourcesExistForVersion(PROCESS_VERSION);
	}

	@Test
	public void startApiTest() throws Exception
	{
		executePluginTest(createTestTask("Api"));
	}

	@Test
	public void startProxyTest() throws Exception
	{
		executePluginTest(createTestTask("Proxy"));
	}

	@Test
	public void startOrganizationProviderTest() throws Exception
	{
		executePluginTest(createTestTask("OrganizationProvider"));
	}

	@Test
	public void startEndpointProviderTest() throws Exception
	{
		executePluginTest(createTestTask("EndpointProvider"));
	}

	@Test
	public void startFhirClientProviderTest() throws Exception
	{
		executePluginTest(createTestTask("FhirClientProvider"));
	}

	@Test
	public void startSendTaskTest() throws Exception
	{
		executePluginTest(createTestTask("SendTaskTest"));
	}

	@Test
	public void startFieldInjectionTest() throws Exception
	{
		executePluginTest(createTestTask("FieldInjectionTest"));
	}

	@Test
	public void startErrorBoundaryEventTest() throws Exception
	{
		executePluginTest(createTestTask("ErrorBoundaryEventTest"));
	}

	@Test
	public void startExceptionTest() throws Exception
	{
		executePluginTest(createTestTask("ExceptionTest"));
	}

	@Test
	public void startContinueSendTest() throws Exception
	{
		executePluginTest(createTestTask("ContinueSendTest"));
	}

	@Test
	public void startJsonVariableTest() throws Exception
	{
		executePluginTest(createTestTask("JsonVariableTest"));
	}

	@Test
	public void startCryptoServiceTest() throws Exception
	{
		List<Path> filesToDelete = null;
		try
		{
			filesToDelete = createCaCerKeyFiles();
			executePluginTest(createTestTask("CryptoServiceTest"));
		}
		finally
		{
			if (filesToDelete != null)
				filesToDelete.forEach(this::deleteFile);
		}
	}

	private List<Path> createCaCerKeyFiles() throws Exception
	{
		CertificateAuthority ca = CertificateAuthority
				.builderSha384EcdsaSecp384r1("DE", null, null, "DSF", "Test", "Plugin V2 Integration Test CA").build();
		CertificationRequestAndPrivateKey clientReq = CertificationRequest
				.builder(ca, "DE", null, null, "DSF", "Test", "client").generateKeyPair().build();
		CertificationRequestAndPrivateKey serverReq = CertificationRequest
				.builder(ca, "DE", null, null, "DSF", "Test", "server").generateKeyPair().build();
		X509Certificate clientCert = ca.signClientCertificate(clientReq);
		X509Certificate serverCert = ca.signServerCertificate(serverReq);

		char[] password = "password".toCharArray();

		Path caCertFile = Paths.get("target/plugin_v2_ca.crt");
		PemWriter.writeCertificate(ca.getCertificate(), caCertFile);

		Path caTrustStoreJksFile = Paths.get("target/plugin_v2_ca.jks");
		KeyStoreWriter.write(KeyStoreCreator.jksForTrustedCertificates(ca.getCertificate()), password,
				caTrustStoreJksFile);

		Path caTrustStoreP12File = Paths.get("target/plugin_v2_ca.p12");
		KeyStoreWriter.write(KeyStoreCreator.pkcs12ForTrustedCertificates(ca.getCertificate()), password,
				caTrustStoreP12File);

		Path clientCertFile = Paths.get("target/plugin_v2_client.crt");
		PemWriter.writeCertificate(clientCert, clientCertFile);

		Path clientKeyFile = Paths.get("target/plugin_v2_client.key");
		PemWriter.writePrivateKey(clientReq.getPrivateKey()).asPkcs8().encryptedAes128(password).toFile(clientKeyFile);

		Path clientKeyStoreJksFile = Paths.get("target/plugin_v2_client.jks");
		KeyStoreWriter.write(
				KeyStoreCreator.jksForPrivateKeyAndCertificateChain(clientReq.getPrivateKey(), password, clientCert),
				password, clientKeyStoreJksFile);

		Path clientKeyStoreP12File = Paths.get("target/plugin_v2_client.p12");
		KeyStoreWriter.write(
				KeyStoreCreator.pkcs12ForPrivateKeyAndCertificateChain(clientReq.getPrivateKey(), password, clientCert),
				password, clientKeyStoreP12File);

		Path serverCertFile = Paths.get("target/plugin_v2_server.crt");
		PemWriter.writeCertificate(serverCert, serverCertFile);

		Path serverKeyFile = Paths.get("target/plugin_v2_server.key");
		PemWriter.writePrivateKey(serverReq.getPrivateKey()).asPkcs8().encryptedAes128(password).toFile(serverKeyFile);

		Path serverKeyStoreJksFile = Paths.get("target/plugin_v2_server.jks");
		KeyStoreWriter.write(
				KeyStoreCreator.jksForPrivateKeyAndCertificateChain(serverReq.getPrivateKey(), password, serverCert),
				password, serverKeyStoreJksFile);

		Path serverKeyStoreP12File = Paths.get("target/plugin_v2_server.p12");
		KeyStoreWriter.write(
				KeyStoreCreator.pkcs12ForPrivateKeyAndCertificateChain(serverReq.getPrivateKey(), password, serverCert),
				password, serverKeyStoreP12File);

		return List.of(caCertFile, caTrustStoreJksFile, caTrustStoreP12File, clientCertFile, clientKeyFile,
				clientKeyStoreJksFile, clientKeyStoreP12File, serverCertFile, serverKeyFile, serverKeyStoreJksFile,
				serverKeyStoreP12File);
	}

	private void deleteFile(Path path)
	{
		try
		{
			Files.deleteIfExists(path);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Test
	public void startMimetypeServiceTest() throws Exception
	{
		executePluginTest(createTestTask("MimetypeServiceTest"));
	}

	@Test
	public void startFhirBinaryVariableTest() throws Exception
	{
		executePluginTest(createTestTask("FhirBinaryVariableTest"));
	}
}