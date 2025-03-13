package dev.dsf.bpe.config;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import de.hsheilbronn.mi.utils.crypto.ca.CertificateAuthority;
import de.hsheilbronn.mi.utils.crypto.ca.CertificationRequest;
import de.hsheilbronn.mi.utils.crypto.ca.CertificationRequest.CertificationRequestAndPrivateKey;
import de.hsheilbronn.mi.utils.crypto.io.KeyStoreWriter;
import de.hsheilbronn.mi.utils.crypto.io.PemWriter;
import de.hsheilbronn.mi.utils.crypto.keystore.KeyStoreCreator;
import dev.dsf.bpe.api.config.FhirClientConfig;
import dev.dsf.bpe.api.config.FhirClientConfig.BasicAuthentication;
import dev.dsf.bpe.api.config.FhirClientConfig.BearerAuthentication;
import dev.dsf.bpe.api.config.FhirClientConfig.CertificateAuthentication;
import dev.dsf.bpe.api.config.FhirClientConfig.OidcAuthentication;
import dev.dsf.bpe.api.config.FhirClientConfigs;

public class FhirClientConfigYamlReaderTest
{
	private static final Logger logger = LoggerFactory.getLogger(FhirClientConfigYamlReaderTest.class);

	private static final String TEST_VALID_YAML = """
			min-server:
			  base-url: http://min.server/fhir
			no-auth-server:
			  base-url: https://no.auth.server/fhir
			  test-connection-on-startup: no
			  enable-debug-logging: no
			  connect-timeout: PT0.5S
			  read-timeout: PT10M
			cert-auth-server1:
			  base-url: https://cert.auth.server:443/fhir/foo
			  test-connection-on-startup: yes
			  enable-debug-logging: no
			  connect-timeout: PT2S
			  read-timeout: PT1H
			  trusted-root-certificates-file: '#[ca.crt]'
			  cert-auth:
			    p12-file: '#[client.p12]'
			    password: '#[password]'
			cert-auth-server2:
			  base-url: https://cert.auth.server/fhir
			  test-connection-on-startup: no
			  enable-debug-logging: yes
			  connect-timeout: PT2S
			  read-timeout: PT10M
			  trusted-root-certificates-file: '#[ca.crt]'
			  cert-auth:
			    private-key-file: '#[client.key]'
			    certificate-file: '#[client.crt]'
			    password-file: '#[password.file]'
			basic-auth-server:
			  base-url: https://basic.auth.server/fhir
			  test-connection-on-startup: yes
			  enable-debug-logging: yes
			  connect-timeout: PT2S
			  read-timeout: PT10M
			  trusted-root-certificates-file: '#[ca.crt]'
			  basic-auth:
			    username: user
			    password-file: '#[password.file]'
			bearer-auth-server:
			  base-url: https://bearer.auth.server/fhir
			  test-connection-on-startup: no
			  enable-debug-logging: no
			  connect-timeout: PT2S
			  read-timeout: PT10M
			  trusted-root-certificates-file: '#[ca.crt]'
			  bearer-auth:
			    token: bearer...token
			oidc-auth-server1:
			  base-url: https://oidcauth.server/fhir
			  connect-timeout: PT2S
			  read-timeout: PT10M
			  trusted-root-certificates-file: '#[ca.crt]'
			  oidc-auth:
			    base-url: https://oidc1.server/foo
			    discovery-path: /test/.well-known/openid-configuration
			    test-connection-on-startup: no
			    enable-debug-logging: yes
			    connect-timeout: PT5S
			    read-timeout: PT10M
			    trusted-root-certificates-file: '#[ca.crt]'
			    client-id: some_client_id
			    client-secret-file: '#[password.file]'
			oidc-auth-server2:
			  base-url: https://oidcauth.server/fhir
			  connect-timeout: PT2S
			  read-timeout: PT10M
			  trusted-root-certificates-file: '#[ca.crt]'
			  oidc-auth:
			    base-url: https://oidc2.server/foo/
			    client-id: some_client_id
			    client-secret: s3cr3t
			empty-cert-auth:
			  base-url: http://empty.cert.auth/fhir
			  cert-auth:""";

	private static record YamlAndErrorCount(String yaml, int errorCount)
	{
		public String yaml()
		{
			return replaceTestProperties(yaml);
		}
	}

	private static YamlAndErrorCount ye(String yaml, int errorCount)
	{
		return new YamlAndErrorCount(yaml, errorCount);
	}

	private static final List<YamlAndErrorCount> NOT_VALID_YAMLS = List.of(ye("no-base-url:", 1), ye("""
			cert-auth-password-file:
			  base-url: https://cert.auth/password/file
			  cert-auth:
			    password-file: '#[password.file]'
			""", 1), ye("""
			cert-auth-password:
			  base-url: https://cert.auth/password
			  cert-auth:
			    password: '#[password]'
			""", 1), ye("""
			cert-auth-p12-private-key:
			  base-url: https://cert.auth/p12/private/key
			  cert-auth:
			    p12-file: '#[client.p12]'
			    private-key-file: '#[client.key]'
			""", 2), ye("""
			cert-auth-p12-cert:
			  base-url: https://cert.auth/p12/cert
			  cert-auth:
			    p12-file: '#[client.p12]'
			    certificate-file: '#[client.crt]'
			""", 2), ye("""
			cert-auth-private-key-password:
			  base-url: https://cert.auth/private/key/password
			  cert-auth:
			    private-key-file: '#[client.key]'
			    password: '#[password]'
			""", 1), ye("""
			cert-auth-certificate-password-file:
			  base-url: https://cert.auth/certificate/password/file
			  cert-auth:
			    certificate-file: '#[client.crt]'
			    password-file: '#[password.file]'
			""", 1), ye("""
			cert-auth-server-certificate:
			  base-url: https://cert.auth/server/certificate
			  cert-auth:
			    private-key-file: '#[server.key]'
			    certificate-file: '#[server.crt]'
			    password-file: '#[password.file]'
			""", 1), ye("""
			basic-auth-password-file:
			  base-url: https://basic.auth/password/file
			  basic-auth:
			    password-file: '#[password.file]'
			""", 1), ye("""
			basic-auth-password-file:
			  base-url: https://basic.auth/password
			  basic-auth:
			    password: '#[password]'
			""", 1), ye("""
			basic-auth-password-file:
			  base-url: https://basic.auth/password
			  basic-auth:
			    username: user
			    password: '🙂'
			""", 1), ye("""
			basic-auth-username:
			  base-url: https://basic.auth/username
			  basic-auth:
			    username: user
			""", 1), ye("""
			oidc-auth-discovery-client-id-client-secret:
			  base-url: https://oidc.auth/client/id/client/secret
			  oidc-auth:
			    client-id: some_client_id
			    client-secret: s3cr3t
			""", 1), ye("""
			oidc-auth-discovery-base-url-client-id:
			  base-url: https://oidc.auth/base/url/client/id
			  oidc-auth:
			    base-url: https://oidc.server
			    client-id: some_client_id
			""", 1), ye("""
			oidc-auth-discovery-base-url-client-secret:
			  base-url: https://oidc.auth/base/url/client/secret
			  oidc-auth:
			    base-url: https://oidc.server
			    client-secret: s3cr3t
			""", 1), ye("""
			read-timeout-too-large:
			  base-url: http://min.server/fhir
			  read-timeout: P25D
			""", 1), ye("""
			connection-timeout-too-large:
			  base-url: http://min.server/fhir
			  connect-timeout: P24DT20H31M23.648S
			""", 1));

	private static final char[] PASSWORD = "pa55w0rd".toCharArray();

	private static final Path CA_CRT_FILE = Paths.get("target/" + UUID.randomUUID().toString() + ".crt");
	private static final Path CLIENT_CRT_FILE = Paths.get("target/" + UUID.randomUUID().toString() + ".crt");
	private static final Path CLIENT_KEY_FILE = Paths.get("target/" + UUID.randomUUID().toString() + ".key");
	private static final Path PASSWORD_FILE = Paths.get("target/" + UUID.randomUUID().toString() + ".password");
	private static final Path CLIENT_P12_FILE = Paths.get("target/" + UUID.randomUUID().toString() + ".p12");
	private static final Path SERVER_CRT_FILE = Paths.get("target/" + UUID.randomUUID().toString() + ".crt");
	private static final Path SERVER_KEY_FILE = Paths.get("target/" + UUID.randomUUID().toString() + ".key");

	private static final boolean DEFAULT_TEST_CONNECTION_ON_STARTUP = true;
	private static final boolean DEFAULT_ENABLE_DEBUG_LOGGING = false;
	private static final Duration DEFAULT_CONNECTION_TIMEOUT = Duration.ofSeconds(1);
	private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(20);
	private static final String DEFAULT_OIDC_DISCOVERY_PATH = "/.well-known/openid-configuration";

	private static KeyStore DEFAULT_TRUST_STORE;
	private static X509Certificate CLIENT_CERTIFICATE, SERVER_CERTIFICATE;
	private static CertificateAuthority CA;

	private FhirClientConfigYamlReaderImpl reader = new FhirClientConfigYamlReaderImpl(
			DEFAULT_TEST_CONNECTION_ON_STARTUP, DEFAULT_ENABLE_DEBUG_LOGGING, DEFAULT_CONNECTION_TIMEOUT,
			DEFAULT_READ_TIMEOUT, DEFAULT_TRUST_STORE, DEFAULT_OIDC_DISCOVERY_PATH);

	@BeforeClass
	public static void beforeClass() throws Exception
	{
		CA = CertificateAuthority.builderSha384EcdsaSecp384r1("DE", null, null, null, null, "JUnit Test Ca").build();
		PemWriter.writeCertificate(CA.getCertificate(), true, CA_CRT_FILE);
		DEFAULT_TRUST_STORE = KeyStoreCreator.jksForTrustedCertificates(CA.getCertificate());

		CertificationRequestAndPrivateKey clientReq = CertificationRequest
				.builder(CA, "DE", null, null, null, null, "JUnit Test Client").generateKeyPair().build();
		CLIENT_CERTIFICATE = CA.signClientCertificate(clientReq);
		PemWriter.writePrivateKey(clientReq.getPrivateKey()).asPkcs8().encryptedAes128(PASSWORD)
				.toFile(CLIENT_KEY_FILE);
		PemWriter.writeCertificates(List.of(CLIENT_CERTIFICATE, CA.getCertificate()), true, CLIENT_CRT_FILE);

		KeyStore clientP12 = KeyStoreCreator.pkcs12ForPrivateKeyAndCertificateChain(clientReq.getPrivateKey(), PASSWORD,
				CLIENT_CERTIFICATE, CA.getCertificate());
		KeyStoreWriter.write(clientP12, PASSWORD, CLIENT_P12_FILE);

		CertificationRequestAndPrivateKey serverReq = CertificationRequest
				.builder(CA, "DE", null, null, null, null, "junit.test.server").generateKeyPair().build();
		SERVER_CERTIFICATE = CA.signServerCertificate(serverReq);
		PemWriter.writePrivateKey(serverReq.getPrivateKey()).asPkcs8().encryptedAes128(PASSWORD)
				.toFile(SERVER_KEY_FILE);
		PemWriter.writeCertificates(List.of(SERVER_CERTIFICATE, CA.getCertificate()), true, SERVER_CRT_FILE);

		Files.writeString(PASSWORD_FILE, String.valueOf(PASSWORD), StandardCharsets.UTF_8);
	}

	@AfterClass
	public static void afterClass() throws Exception
	{
		tryToDelete(CA_CRT_FILE);
		tryToDelete(CLIENT_CRT_FILE);
		tryToDelete(CLIENT_KEY_FILE);
		tryToDelete(PASSWORD_FILE);
		tryToDelete(CLIENT_P12_FILE);
		tryToDelete(SERVER_CRT_FILE);
		tryToDelete(SERVER_KEY_FILE);
	}

	private static void tryToDelete(Path file)
	{
		try
		{
			Files.deleteIfExists(file);
		}
		catch (IOException e)
		{
			// don't care
		}
	}

	private static String replaceTestProperties(String yaml)
	{
		return yaml.replaceAll(Pattern.quote("#[password]"), Matcher.quoteReplacement(String.valueOf(PASSWORD)))
				.replaceAll(Pattern.quote("#[ca.crt]"), Matcher.quoteReplacement(CA_CRT_FILE.toString()))
				.replaceAll(Pattern.quote("#[client.crt]"), Matcher.quoteReplacement(CLIENT_CRT_FILE.toString()))
				.replaceAll(Pattern.quote("#[client.key]"), Matcher.quoteReplacement(CLIENT_KEY_FILE.toString()))
				.replaceAll(Pattern.quote("#[server.crt]"), Matcher.quoteReplacement(SERVER_CRT_FILE.toString()))
				.replaceAll(Pattern.quote("#[server.key]"), Matcher.quoteReplacement(SERVER_KEY_FILE.toString()))
				.replaceAll(Pattern.quote("#[password.file]"), Matcher.quoteReplacement(PASSWORD_FILE.toString()))
				.replaceAll(Pattern.quote("#[client.p12]"), Matcher.quoteReplacement(CLIENT_P12_FILE.toString()));
	}

	@Test(expected = NullPointerException.class)
	public void testReadNullString() throws Exception
	{
		reader.readConfigs((String) null);
	}

	@Test(expected = NullPointerException.class)
	public void testReadNullPath() throws Exception
	{
		reader.readConfigs((Path) null);

	}

	@Test(expected = NullPointerException.class)
	public void testReadNullReader() throws Exception
	{
		reader.readConfigs((Reader) null);
	}

	@Test(expected = NullPointerException.class)
	public void testReadNullInputStream() throws Exception
	{
		reader.readConfigs((InputStream) null);
	}

	@Test
	public void testReadEmptyString() throws Exception
	{
		reader.readConfigs("");
	}

	@Test(expected = MismatchedInputException.class)
	public void testReadEmptyStringReader() throws Exception
	{
		reader.readConfigs(new StringReader(""));
	}

	@Test
	public void testRead() throws Exception
	{
		String yaml = replaceTestProperties(TEST_VALID_YAML);
		logger.debug("Valid test YAML after porperty replacement:\n{}", yaml);

		FhirClientConfigs configs = logValidationErrors(() -> reader.readConfigs(yaml));
		assertNotNull(configs);
		assertNotNull(configs.getConfigs());
		assertEquals(9, configs.getConfigs().size());
		assertEquals(
				List.of("basic-auth-server", "bearer-auth-server", "cert-auth-server1", "cert-auth-server2",
						"empty-cert-auth", "min-server", "no-auth-server", "oidc-auth-server1", "oidc-auth-server2"),
				configs.getConfigs().stream().map(FhirClientConfig::fhirServerId).sorted().toList());

		testConfig(configs, "min-server", "http://min.server/fhir", DEFAULT_TEST_CONNECTION_ON_STARTUP,
				DEFAULT_ENABLE_DEBUG_LOGGING, DEFAULT_CONNECTION_TIMEOUT, DEFAULT_READ_TIMEOUT, Assert::assertNull,
				Assert::assertNull, Assert::assertNull, Assert::assertNull);
		testConfig(configs, "cert-auth-server1", "https://cert.auth.server:443/fhir/foo", true, false,
				Duration.ofSeconds(2), Duration.ofHours(1), Assert::assertNull, Assert::assertNull,
				testCertAuth(PASSWORD), Assert::assertNull);
		testConfig(configs, "cert-auth-server2", "https://cert.auth.server/fhir", false, true, Duration.ofSeconds(2),
				Duration.ofMinutes(10), Assert::assertNull, Assert::assertNull, testCertAuth(PASSWORD),
				Assert::assertNull);
		testConfig(configs, "no-auth-server", "https://no.auth.server/fhir", false, false, Duration.ofMillis(500),
				Duration.ofMinutes(10), Assert::assertNull, Assert::assertNull, Assert::assertNull, Assert::assertNull);
		testConfig(configs, "basic-auth-server", "https://basic.auth.server/fhir", true, true, Duration.ofSeconds(2),
				Duration.ofMinutes(10), testBasicAuth(PASSWORD, "user"), Assert::assertNull, Assert::assertNull,
				Assert::assertNull);
		testConfig(configs, "bearer-auth-server", "https://bearer.auth.server/fhir", false, false,
				Duration.ofSeconds(2), Duration.ofMinutes(10), Assert::assertNull,
				testBearerAuth("bearer...token".toCharArray()), Assert::assertNull, Assert::assertNull);
		testConfig(configs, "oidc-auth-server1", "https://oidcauth.server/fhir", DEFAULT_TEST_CONNECTION_ON_STARTUP,
				DEFAULT_ENABLE_DEBUG_LOGGING, Duration.ofSeconds(2), Duration.ofMinutes(10), Assert::assertNull,
				Assert::assertNull, Assert::assertNull,
				testOidcAuth("https://oidc1.server/foo", "/test/.well-known/openid-configuration", false, true,
						Duration.ofSeconds(5), Duration.ofMinutes(10), "some_client_id", PASSWORD));
		testConfig(configs, "oidc-auth-server2", "https://oidcauth.server/fhir", DEFAULT_TEST_CONNECTION_ON_STARTUP,
				DEFAULT_ENABLE_DEBUG_LOGGING, Duration.ofSeconds(2), Duration.ofMinutes(10), Assert::assertNull,
				Assert::assertNull, Assert::assertNull,
				testOidcAuth("https://oidc2.server/foo", DEFAULT_OIDC_DISCOVERY_PATH,
						DEFAULT_TEST_CONNECTION_ON_STARTUP, DEFAULT_ENABLE_DEBUG_LOGGING, DEFAULT_CONNECTION_TIMEOUT,
						DEFAULT_READ_TIMEOUT, "some_client_id", "s3cr3t".toCharArray()));
		testConfig(configs, "empty-cert-auth", "http://empty.cert.auth/fhir", DEFAULT_TEST_CONNECTION_ON_STARTUP,
				DEFAULT_ENABLE_DEBUG_LOGGING, DEFAULT_CONNECTION_TIMEOUT, DEFAULT_READ_TIMEOUT, Assert::assertNull,
				Assert::assertNull, Assert::assertNull, Assert::assertNull);
	}

	@FunctionalInterface
	private static interface BiConsumerWithException<T, U>
	{
		void accept(T t, U u) throws Exception;
	}

	private void testConfig(FhirClientConfigs configs, String expectedFhirServerId, String expectedBaseUrl,
			boolean expectedTestConnectionOnStartup, boolean expectedEnableDebugLogging,
			Duration expectedConnectionTimeout, Duration expectedReadTimeout,
			BiConsumerWithException<String, BasicAuthentication> basicAuthenticationTester,
			BiConsumerWithException<String, BearerAuthentication> bearerAuthenticationTester,
			BiConsumerWithException<String, CertificateAuthentication> certificateAuthenticationTester,
			BiConsumerWithException<String, OidcAuthentication> oidcAuthenticationTester) throws Exception
	{
		Optional<FhirClientConfig> config = configs.getConfig(expectedFhirServerId);
		assertNotNull(expectedFhirServerId, config);
		assertTrue(expectedFhirServerId, config.isPresent());
		FhirClientConfig c = config.get();

		assertEquals(expectedFhirServerId, expectedFhirServerId, c.fhirServerId());

		assertEquals(expectedFhirServerId, expectedBaseUrl, c.baseUrl());
		assertEquals(expectedFhirServerId, expectedTestConnectionOnStartup, c.testConnectionOnStartup());
		assertEquals(expectedFhirServerId, expectedEnableDebugLogging, c.enableDebugLogging());
		assertEquals(expectedFhirServerId, expectedConnectionTimeout, c.connectTimeout());
		assertEquals(expectedFhirServerId, expectedReadTimeout, c.readTimeout());
		testTrustStore(expectedFhirServerId, c.trustStore());

		basicAuthenticationTester.accept(expectedFhirServerId, c.basicAuthentication());
		bearerAuthenticationTester.accept(expectedFhirServerId, c.bearerAuthentication());
		certificateAuthenticationTester.accept(expectedFhirServerId, c.certificateAuthentication());
		oidcAuthenticationTester.accept(expectedFhirServerId, c.oidcAuthentication());
	}

	private void testTrustStore(String expectedFhirServerId, KeyStore trustStore) throws KeyStoreException
	{
		assertNotNull(trustStore);
		assertEquals(1, Collections.list(trustStore.aliases()).size());
		assertNotNull(trustStore.getCertificate(CA.getCertificate().getSubjectX500Principal().getName()));
	}

	private BiConsumerWithException<String, BasicAuthentication> testBasicAuth(char[] expectedPassword,
			String expectedUsername)
	{
		return (expectedFhirServerId, auth) ->
		{
			assertArrayEquals(expectedFhirServerId, expectedPassword, auth.password());
			assertEquals(expectedFhirServerId, expectedUsername, auth.username());
		};
	}

	private BiConsumerWithException<String, BearerAuthentication> testBearerAuth(char[] extpectedToken)
	{
		return (expectedFhirServerId, auth) -> assertArrayEquals(expectedFhirServerId, extpectedToken, auth.token());
	}

	private BiConsumerWithException<String, CertificateAuthentication> testCertAuth(char[] expectedKeyStorePassword)
	{
		return (expectedFhirServerId, auth) ->
		{
			assertNotNull(expectedFhirServerId, auth.keyStorePassword());
			assertArrayEquals(expectedFhirServerId, expectedKeyStorePassword, auth.keyStorePassword());
			assertNotNull(expectedFhirServerId, auth.keyStore());
			assertEquals(expectedFhirServerId, 1, Collections.list(auth.keyStore().aliases()).size());
			assertNotNull(expectedFhirServerId,
					auth.keyStore().getCertificate(CLIENT_CERTIFICATE.getSubjectX500Principal().getName()));
			assertNotNull(expectedFhirServerId, auth.keyStore()
					.getKey(CLIENT_CERTIFICATE.getSubjectX500Principal().getName(), auth.keyStorePassword()));
			assertEquals(expectedFhirServerId, 2,
					auth.keyStore().getCertificateChain(CLIENT_CERTIFICATE.getSubjectX500Principal().getName()).length);
		};
	}

	private BiConsumerWithException<String, OidcAuthentication> testOidcAuth(String expectedBaseUrl,
			String expectedDiscoveryPath, boolean expectedTestConnectionOnStartup, boolean expectedEnableDebugLogging,
			Duration expectedConnectionTimeout, Duration expectedReadTimeout, String expectedClientId,
			char[] expectedClientSecret)
	{
		return (expectedFhirServerId, auth) ->
		{
			assertEquals(expectedFhirServerId, expectedBaseUrl, auth.baseUrl());
			assertEquals(expectedFhirServerId, expectedDiscoveryPath, auth.discoveryPath());
			assertEquals(expectedFhirServerId, expectedTestConnectionOnStartup, auth.testConnectionOnStartup());
			assertEquals(expectedFhirServerId, expectedEnableDebugLogging, auth.enableDebugLogging());
			assertEquals(expectedFhirServerId, expectedConnectionTimeout, auth.connectTimeout());
			assertEquals(expectedFhirServerId, expectedReadTimeout, auth.readTimeout());
			testTrustStore(expectedFhirServerId, auth.trustStore());
			assertEquals(expectedFhirServerId, expectedClientId, auth.clientId());
			assertArrayEquals(expectedFhirServerId, expectedClientSecret, auth.clientSecret());
		};
	}

	@Test
	public void testReadInvalid() throws Exception
	{
		NOT_VALID_YAMLS.forEach(this::testReadInvalid);
	}

	private void testReadInvalid(YamlAndErrorCount ye)
	{
		try
		{
			logValidationErrors(() -> reader.readConfigs(ye.yaml()));
		}
		catch (ConfigInvalidException e)
		{
			assertEquals(ye.yaml, ye.errorCount(), e.getValidationErrors().size());
			return;
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}

		fail(ConfigInvalidException.class.getName() + " expected for '" + ye.yaml.replace("\n", "\\n") + "'");
	}

	@FunctionalInterface
	private static interface SupplierWithConfigInvalidExceptionAndIOException<T>
	{
		T get() throws ConfigInvalidException, IOException;
	}

	private FhirClientConfigs logValidationErrors(
			SupplierWithConfigInvalidExceptionAndIOException<FhirClientConfigs> parse)
			throws ConfigInvalidException, IOException
	{
		try
		{
			return parse.get();
		}
		catch (ConfigInvalidException e)
		{
			logger.debug("Validation errors: " + e.getValidationErrors());
			throw e;
		}
		catch (Exception e)
		{
			throw e;
		}
	}
}
