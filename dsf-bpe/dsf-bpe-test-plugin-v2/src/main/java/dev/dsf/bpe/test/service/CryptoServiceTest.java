/*
 * Copyright 2018-2025 Heilbronn University of Applied Sciences
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.dsf.bpe.test.service;

import static dev.dsf.bpe.test.PluginTestExecutor.expectException;
import static dev.dsf.bpe.test.PluginTestExecutor.expectFalse;
import static dev.dsf.bpe.test.PluginTestExecutor.expectNotNull;
import static dev.dsf.bpe.test.PluginTestExecutor.expectSame;
import static dev.dsf.bpe.test.PluginTestExecutor.expectTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.net.ssl.SSLContext;

import dev.dsf.bpe.test.AbstractTest;
import dev.dsf.bpe.test.PluginTest;
import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.ServiceTask;
import dev.dsf.bpe.v2.error.ErrorBoundaryEvent;
import dev.dsf.bpe.v2.service.CryptoService;
import dev.dsf.bpe.v2.service.CryptoService.Kem;
import dev.dsf.bpe.v2.variables.Variables;

public class CryptoServiceTest extends AbstractTest implements ServiceTask
{
	// files created by dev.dsf.bpe.integration.PluginV2IntegrationTest
	private static final Path CA_CERT_FILE = Paths.get("target/plugin_v2_ca.crt");
	private static final Path CA_TRUST_STORE_JKS_FILE = Paths.get("target/plugin_v2_ca.jks");
	private static final Path CA_TRUST_STORE_P12_FILE = Paths.get("target/plugin_v2_ca.p12");

	private static final Path CLIENT_CERT_FILE = Paths.get("target/plugin_v2_client.crt");
	private static final Path CLIENT_KEY_FILE = Paths.get("target/plugin_v2_client.key");
	private static final Path CLIENT_KEY_STORE_JKS_FILE = Paths.get("target/plugin_v2_client.jks");
	private static final Path CLIENT_KEY_STORE_P12_FILE = Paths.get("target/plugin_v2_client.p12");

	private static final Path SERVER_CERT_FILE = Paths.get("target/plugin_v2_server.crt");
	private static final Path SERVER_KEY_FILE = Paths.get("target/plugin_v2_server.key");
	private static final Path SERVER_KEY_STORE_JKS_FILE = Paths.get("target/plugin_v2_server.jks");
	private static final Path SERVER_KEY_STORE_P12_FILE = Paths.get("target/plugin_v2_server.p12");

	private static final char[] PASSWORD = "password".toCharArray();

	@Override
	public void execute(ProcessPluginApi api, Variables variables) throws ErrorBoundaryEvent, Exception
	{
		executeTests(api, variables, api.getCryptoService());
	}

	@PluginTest
	public void createEcDhKem(CryptoService cryptoService) throws Exception
	{
		expectNotNull(cryptoService.createEcDhKem());
	}

	@PluginTest
	public void createEcDhKemCheckEncryptionDecryption(CryptoService cryptoService) throws Exception
	{
		Kem kem = cryptoService.createEcDhKem();

		KeyPair keyPair = cryptoService.createKeyPairGeneratorX25519AndInitialize().generateKeyPair();
		byte[] plainData = "Hello World".getBytes(StandardCharsets.UTF_8);

		InputStream encrypted = kem.encrypt(new ByteArrayInputStream(plainData), keyPair.getPublic());
		expectNotNull(encrypted);

		byte[] encryptedData = encrypted.readAllBytes();
		expectNotNull(encryptedData);
		expectTrue(encryptedData.length > 12 + 2 + 1 + 1);

		InputStream decryptedDataStream = kem.decrypt(new ByteArrayInputStream(encryptedData), keyPair.getPrivate());
		byte[] decryptedData = decryptedDataStream.readAllBytes();
		expectNotNull(decryptedData);
		expectSame(plainData, decryptedData);
	}

	@PluginTest
	public void createEcDhKemCheckEncryptionDecryptionByteArray(CryptoService cryptoService) throws Exception
	{
		Kem kem = cryptoService.createEcDhKem();

		KeyPair keyPair = cryptoService.createKeyPairGeneratorX25519AndInitialize().generateKeyPair();
		byte[] plainData = "Hello World".getBytes(StandardCharsets.UTF_8);

		byte[] encryptedData = kem.encrypt(plainData, keyPair.getPublic());
		expectNotNull(encryptedData);
		expectTrue(encryptedData.length > 12 + 2 + 1 + 1);

		byte[] decryptedData = kem.decrypt(encryptedData, keyPair.getPrivate());
		expectNotNull(decryptedData);
		expectSame(plainData, decryptedData);
	}

	@PluginTest
	public void createKeyPairGeneratorRsa4096AndInitialize(CryptoService cryptoService) throws Exception
	{
		expectNotNull(cryptoService.createKeyPairGeneratorRsa4096AndInitialize());
	}

	@PluginTest
	public void createKeyPairGeneratorSecp256r1AndInitialize(CryptoService cryptoService) throws Exception
	{
		expectNotNull(cryptoService.createKeyPairGeneratorSecp256r1AndInitialize());
	}

	@PluginTest
	public void createKeyPairGeneratorSecp384r1AndInitialize(CryptoService cryptoService) throws Exception
	{
		expectNotNull(cryptoService.createKeyPairGeneratorSecp384r1AndInitialize());
	}

	@PluginTest
	public void createKeyPairGeneratorSecp521r1AndInitialize(CryptoService cryptoService) throws Exception
	{
		expectNotNull(cryptoService.createKeyPairGeneratorSecp521r1AndInitialize());
	}

	@PluginTest
	public void createKeyPairGeneratorX25519AndInitialize(CryptoService cryptoService) throws Exception
	{
		expectNotNull(cryptoService.createKeyPairGeneratorX25519AndInitialize());
	}

	@PluginTest
	public void createKeyPairGeneratorX448AndInitialize(CryptoService cryptoService) throws Exception
	{
		expectNotNull(cryptoService.createKeyPairGeneratorX448AndInitialize());
	}

	@PluginTest
	public void createKeyStoreForPrivateKeyAndCertificateChainCollection(CryptoService cryptoService) throws Exception
	{
		PrivateKey key = cryptoService.readPrivateKey(CLIENT_KEY_FILE, PASSWORD);
		X509Certificate cert = cryptoService.readCertificate(CLIENT_CERT_FILE);
		KeyStore store = cryptoService.createKeyStoreForPrivateKeyAndCertificateChain(key,
				UUID.randomUUID().toString().toCharArray(), List.of(cert));
		expectNotNull(store);
		expectSame(1, Collections.list(store.aliases()).size());
	}

	@PluginTest
	public void createKeyStoreForPrivateKeyAndCertificateChainVarargs(CryptoService cryptoService) throws Exception
	{
		PrivateKey key = cryptoService.readPrivateKey(SERVER_KEY_FILE, PASSWORD);
		X509Certificate cert = cryptoService.readCertificate(SERVER_CERT_FILE);
		KeyStore store = cryptoService.createKeyStoreForPrivateKeyAndCertificateChain(key,
				UUID.randomUUID().toString().toCharArray(), cert);
		expectNotNull(store);
		expectSame(1, Collections.list(store.aliases()).size());
	}

	@PluginTest
	public void createKeyStoreForTrustedCertificatesCollection(CryptoService cryptoService) throws Exception
	{
		X509Certificate cert = cryptoService.readCertificate(CA_CERT_FILE);
		KeyStore store = cryptoService.createKeyStoreForTrustedCertificates(List.of(cert));
		expectNotNull(store);
		expectSame(1, Collections.list(store.aliases()).size());
	}

	@PluginTest
	public void createKeyStoreForTrustedCertificatesVarargs(CryptoService cryptoService) throws Exception
	{
		X509Certificate cert = cryptoService.readCertificate(CA_CERT_FILE);
		KeyStore store = cryptoService.createKeyStoreForTrustedCertificates(cert);
		expectNotNull(store);
		expectSame(1, Collections.list(store.aliases()).size());
	}

	@PluginTest
	public void createRsaKem(CryptoService cryptoService) throws Exception
	{
		expectNotNull(cryptoService.createRsaKem());
	}

	@PluginTest
	public void createRsaKemCheckEncryptionDecryption(CryptoService cryptoService) throws Exception
	{
		Kem kem = cryptoService.createRsaKem();

		KeyPair keyPair = cryptoService.createKeyPairGeneratorRsa4096AndInitialize().generateKeyPair();
		byte[] plainData = "Hello World".getBytes(StandardCharsets.UTF_8);

		InputStream encrypted = kem.encrypt(new ByteArrayInputStream(plainData), keyPair.getPublic());
		expectNotNull(encrypted);

		byte[] encryptedData = encrypted.readAllBytes();
		expectNotNull(encryptedData);
		expectTrue(encryptedData.length > 12 + 2 + 1 + 1);

		InputStream decryptedDataStream = kem.decrypt(new ByteArrayInputStream(encryptedData), keyPair.getPrivate());
		byte[] decryptedData = decryptedDataStream.readAllBytes();

		expectSame(plainData, decryptedData);
	}

	@PluginTest
	public void createSSLContextTrustStore(CryptoService cryptoService) throws Exception
	{
		X509Certificate cert = cryptoService.readCertificate(CA_CERT_FILE);
		KeyStore store = cryptoService.createKeyStoreForTrustedCertificates(cert);

		SSLContext context = cryptoService.createSSLContext(store);
		expectNotNull(context);
	}

	@PluginTest
	public void createSSLContextTrustStoreKeyStore(CryptoService cryptoService) throws Exception
	{
		X509Certificate caCert = cryptoService.readCertificate(CA_CERT_FILE);
		KeyStore trustStore = cryptoService.createKeyStoreForTrustedCertificates(caCert);

		PrivateKey clientKey = cryptoService.readPrivateKey(CLIENT_KEY_FILE, PASSWORD);
		X509Certificate clientCert = cryptoService.readCertificate(CLIENT_CERT_FILE);
		char[] keyStorePassword = UUID.randomUUID().toString().toCharArray();
		KeyStore keyStore = cryptoService.createKeyStoreForPrivateKeyAndCertificateChain(clientKey, keyStorePassword,
				List.of(clientCert));

		SSLContext context = cryptoService.createSSLContext(trustStore, keyStore, keyStorePassword);
		expectNotNull(context);
	}

	@PluginTest
	public void isCertificateExpired(CryptoService cryptoService) throws Exception
	{
		X509Certificate caCert = cryptoService.readCertificate(CA_CERT_FILE);
		expectFalse(cryptoService.isCertificateExpired(caCert));
	}

	@PluginTest
	public void isClientCertificate(CryptoService cryptoService) throws Exception
	{
		X509Certificate clientCert = cryptoService.readCertificate(CLIENT_CERT_FILE);
		X509Certificate serverCert = cryptoService.readCertificate(SERVER_CERT_FILE);

		expectTrue(cryptoService.isClientCertificate(clientCert));
		expectFalse(cryptoService.isClientCertificate(serverCert));
	}

	@PluginTest
	public void isKeyPair(CryptoService cryptoService) throws Exception
	{
		PrivateKey clientKey = cryptoService.readPrivateKey(CLIENT_KEY_FILE, PASSWORD);
		X509Certificate clientCert = cryptoService.readCertificate(CLIENT_CERT_FILE);
		PrivateKey serverKey = cryptoService.readPrivateKey(SERVER_KEY_FILE, PASSWORD);
		X509Certificate serverCert = cryptoService.readCertificate(SERVER_CERT_FILE);

		expectTrue(cryptoService.isKeyPair(clientKey, clientCert.getPublicKey()));
		expectTrue(cryptoService.isKeyPair(serverKey, serverCert.getPublicKey()));

		expectFalse(cryptoService.isKeyPair(clientKey, serverCert.getPublicKey()));
		expectFalse(cryptoService.isKeyPair(serverKey, clientCert.getPublicKey()));
	}

	@PluginTest
	public void isServerCertificate(CryptoService cryptoService) throws Exception
	{
		X509Certificate clientCert = cryptoService.readCertificate(CLIENT_CERT_FILE);
		X509Certificate serverCert = cryptoService.readCertificate(SERVER_CERT_FILE);

		expectFalse(cryptoService.isServerCertificate(clientCert));
		expectTrue(cryptoService.isServerCertificate(serverCert));
	}

	@PluginTest
	public void readCertificateInputStream(CryptoService cryptoService) throws Exception
	{
		try (InputStream in = Files.newInputStream(CA_CERT_FILE))
		{
			expectNotNull(cryptoService.readCertificate(in));
		}
	}

	@PluginTest
	public void readCertificatePath(CryptoService cryptoService) throws Exception
	{
		expectNotNull(cryptoService.readCertificate(CA_CERT_FILE));
	}

	@PluginTest
	public void readCertificatesInputStream(CryptoService cryptoService) throws Exception
	{
		try (InputStream in = Files.newInputStream(CA_CERT_FILE))
		{
			List<X509Certificate> certs = cryptoService.readCertificates(in);
			expectNotNull(certs);
			expectSame(1, certs.size());
		}
	}

	@PluginTest
	public void readCertificatesPath(CryptoService cryptoService) throws Exception
	{
		List<X509Certificate> certs = cryptoService.readCertificates(CA_CERT_FILE);
		expectNotNull(certs);
		expectSame(1, certs.size());
	}

	@PluginTest
	public void readKeyStoreJksInputStream(CryptoService cryptoService) throws Exception
	{
		try (InputStream in = Files.newInputStream(CLIENT_KEY_STORE_JKS_FILE))
		{
			expectNotNull(cryptoService.readKeyStoreJks(in, PASSWORD));
		}
	}

	@PluginTest
	public void readKeyStoreJksPath(CryptoService cryptoService) throws Exception
	{
		expectNotNull(cryptoService.readKeyStoreJks(SERVER_KEY_STORE_JKS_FILE, PASSWORD));
	}

	@PluginTest
	public void readKeyStorePkcs12InputStream(CryptoService cryptoService) throws Exception
	{
		try (InputStream in = Files.newInputStream(CLIENT_KEY_STORE_P12_FILE))
		{
			cryptoService.readKeyStorePkcs12(in, PASSWORD);
		}
	}

	@PluginTest
	public void readKeyStorePkcs12Path(CryptoService cryptoService) throws Exception
	{
		expectNotNull(cryptoService.readKeyStorePkcs12(SERVER_KEY_STORE_P12_FILE, PASSWORD));
	}

	@PluginTest
	public void readPrivateKeyInputStream(CryptoService cryptoService) throws Exception
	{
		expectException(NullPointerException.class, () ->
		{
			try (InputStream in = Files.newInputStream(CLIENT_KEY_FILE))
			{
				cryptoService.readPrivateKey(in);
			}
		});
	}

	@PluginTest
	public void readPrivateKeyPath(CryptoService cryptoService) throws Exception
	{
		expectException(NullPointerException.class, () ->
		{
			cryptoService.readPrivateKey(SERVER_KEY_FILE);
		});
	}

	@PluginTest
	public void readPrivateKeyInputStreamCharArray(CryptoService cryptoService) throws Exception
	{
		try (InputStream in = Files.newInputStream(CLIENT_KEY_FILE))
		{
			expectNotNull(cryptoService.readPrivateKey(in, PASSWORD));
		}
	}

	@PluginTest
	public void readPrivateKeyPathCharArray(CryptoService cryptoService) throws Exception
	{
		expectNotNull(cryptoService.readPrivateKey(SERVER_KEY_FILE, PASSWORD));
	}

	@PluginTest
	public void validateClientCertificateCollection(CryptoService cryptoService) throws Exception
	{
		KeyStore caTrustStore = cryptoService.readKeyStorePkcs12(CA_TRUST_STORE_P12_FILE, PASSWORD);
		X509Certificate serverCert = cryptoService.readCertificate(SERVER_CERT_FILE);
		KeyStore serverTrustStore = cryptoService.createKeyStoreForTrustedCertificates(serverCert);
		X509Certificate clientCert = cryptoService.readCertificate(CLIENT_CERT_FILE);

		cryptoService.validateClientCertificate(caTrustStore, List.of(clientCert));

		expectException(CertificateException.class,
				() -> cryptoService.validateClientCertificate(caTrustStore, List.of(serverCert)));
		expectException(CertificateException.class,
				() -> cryptoService.validateClientCertificate(serverTrustStore, List.of(clientCert)));
	}

	@PluginTest
	public void vaildateClientCertificateVarArgs(CryptoService cryptoService) throws Exception
	{
		KeyStore caTrustStore = cryptoService.readKeyStorePkcs12(CA_TRUST_STORE_JKS_FILE, PASSWORD);
		X509Certificate serverCert = cryptoService.readCertificate(SERVER_CERT_FILE);
		KeyStore serverTrustStore = cryptoService.createKeyStoreForTrustedCertificates(serverCert);
		X509Certificate clientCert = cryptoService.readCertificate(CLIENT_CERT_FILE);

		cryptoService.validateClientCertificate(caTrustStore, clientCert);

		expectException(CertificateException.class,
				() -> cryptoService.validateClientCertificate(caTrustStore, serverCert));
		expectException(CertificateException.class,
				() -> cryptoService.validateClientCertificate(serverTrustStore, clientCert));
	}

	@PluginTest
	public void vaildateServerCertificateCollection(CryptoService cryptoService) throws Exception
	{
		KeyStore caTrustStore = cryptoService.readKeyStorePkcs12(CA_TRUST_STORE_P12_FILE, PASSWORD);
		X509Certificate serverCert = cryptoService.readCertificate(SERVER_CERT_FILE);
		X509Certificate clientCert = cryptoService.readCertificate(CLIENT_CERT_FILE);
		KeyStore clientTrustStore = cryptoService.createKeyStoreForTrustedCertificates(clientCert);

		cryptoService.validateServerCertificate(caTrustStore, List.of(serverCert));

		expectException(CertificateException.class,
				() -> cryptoService.validateServerCertificate(caTrustStore, List.of(clientCert)));
		expectException(CertificateException.class,
				() -> cryptoService.validateServerCertificate(clientTrustStore, List.of(serverCert)));
	}

	@PluginTest
	public void vaildateServerCertificateVarArgs(CryptoService cryptoService) throws Exception
	{
		KeyStore caTrustStore = cryptoService.readKeyStorePkcs12(CA_TRUST_STORE_JKS_FILE, PASSWORD);
		X509Certificate serverCert = cryptoService.readCertificate(SERVER_CERT_FILE);
		X509Certificate clientCert = cryptoService.readCertificate(CLIENT_CERT_FILE);
		KeyStore clientTrustStore = cryptoService.createKeyStoreForTrustedCertificates(serverCert);

		cryptoService.validateServerCertificate(caTrustStore, serverCert);

		expectException(CertificateException.class,
				() -> cryptoService.validateServerCertificate(caTrustStore, clientCert));
		expectException(CertificateException.class,
				() -> cryptoService.validateServerCertificate(clientTrustStore, serverCert));
	}
}
