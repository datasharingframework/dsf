package dev.dsf.bpe.v2.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import javax.crypto.DecapsulateException;
import javax.crypto.NoSuchPaddingException;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

public interface CryptoService
{
	public interface Kem
	{
		/**
		 * Encrypts the given {@link InputStream} with an AES session key calculated by KEM for the given
		 * {@link PublicKey}. The returned {@link InputStream} has the form [encapsulation length (big-endian, 2 bytes),
		 * encapsulation, AES initialization vector (12 bytes), AES encrypted data].
		 *
		 * @param data
		 *            not <code>null</code>
		 * @param publicKey
		 *            not <code>null</code>
		 * @return byte array of [encapsulation length (big-endian, 2 bytes), encapsulation, iv (12 bytes), encrypted
		 *         data]
		 * @throws IOException
		 * @throws NoSuchAlgorithmException
		 * @throws InvalidKeyException
		 * @throws NoSuchPaddingException
		 * @throws InvalidAlgorithmParameterException
		 */
		default byte[] encrypt(byte[] data, PublicKey publicKey) throws IOException, NoSuchAlgorithmException,
				InvalidKeyException, NoSuchPaddingException, InvalidAlgorithmParameterException
		{
			return encrypt(new ByteArrayInputStream(data), publicKey).readAllBytes();
		}

		/**
		 * Encrypts the given {@link InputStream} with an AES session key calculated by KEM for the given
		 * {@link PublicKey}. The returned {@link InputStream} has the form [encapsulation length (big-endian, 2 bytes),
		 * encapsulation, AES initialization vector (12 bytes), AES encrypted data].
		 *
		 * @param data
		 *            not <code>null</code>
		 * @param publicKey
		 *            not <code>null</code>
		 * @return {@link InputStream} of [encapsulation length (big-endian, 2 bytes), encapsulation, iv (12 bytes),
		 *         encrypted data]
		 * @throws IOException
		 * @throws NoSuchAlgorithmException
		 * @throws InvalidKeyException
		 * @throws NoSuchPaddingException
		 * @throws InvalidAlgorithmParameterException
		 */
		InputStream encrypt(InputStream data, PublicKey publicKey) throws IOException, NoSuchAlgorithmException,
				InvalidKeyException, NoSuchPaddingException, InvalidAlgorithmParameterException;

		/**
		 * @param encrypted
		 *            not <code>null</code>, {@link InputStream} of [encapsulation length (big-endian, 2 bytes),
		 *            encapsulation, iv (12 bytes), encrypted data]
		 * @param privateKey
		 *            not <code>null</code>
		 * @return decrypted data
		 * @throws IOException
		 * @throws NoSuchAlgorithmException
		 * @throws InvalidKeyException
		 * @throws DecapsulateException
		 * @throws NoSuchPaddingException
		 * @throws InvalidAlgorithmParameterException
		 */
		default byte[] decrypt(byte[] encrypted, PrivateKey privateKey) throws IOException, NoSuchAlgorithmException,
				InvalidKeyException, DecapsulateException, NoSuchPaddingException, InvalidAlgorithmParameterException
		{
			return decrypt(new ByteArrayInputStream(encrypted), privateKey).readAllBytes();
		}

		/**
		 * @param encrypted
		 *            not <code>null</code>, {@link InputStream} of [encapsulation length (big-endian, 2 bytes),
		 *            encapsulation, iv (12 bytes), encrypted data]
		 * @param privateKey
		 *            not <code>null</code>
		 * @return decrypted data
		 * @throws IOException
		 * @throws NoSuchAlgorithmException
		 * @throws InvalidKeyException
		 * @throws DecapsulateException
		 * @throws NoSuchPaddingException
		 * @throws InvalidAlgorithmParameterException
		 */
		InputStream decrypt(InputStream encrypted, PrivateKey privateKey) throws IOException, NoSuchAlgorithmException,
				InvalidKeyException, DecapsulateException, NoSuchPaddingException, InvalidAlgorithmParameterException;
	}

	/**
	 * @return key encapsulation mechanism with RSA key exchange using KDF2 SHA-512 for AES-256, use with RSA key pairs
	 */
	Kem createRsaKem();

	/**
	 * @return key encapsulation mechanism with Diffie–Hellman key exchange for AES-256, use with elliptic curve key
	 *         pairs like X25519, X448, secp256r1, secp384r1 and secp521r1
	 */
	Kem createEcDhKem();

	/**
	 * @return created and initialized RSA (4096 bit) key pair generator
	 * @see KeyPairGenerator#generateKeyPair()
	 */
	KeyPairGenerator createKeyPairGeneratorRsa4096AndInitialize();

	/**
	 * @return created and initialized secp256r1 key pair generator
	 * @see KeyPairGenerator#generateKeyPair()
	 */
	KeyPairGenerator createKeyPairGeneratorSecp256r1AndInitialize();

	/**
	 * @return created and initialized secp384r1 key pair generator
	 * @see KeyPairGenerator#generateKeyPair()
	 */
	KeyPairGenerator createKeyPairGeneratorSecp384r1AndInitialize();

	/**
	 * @return created and initialized secp521r1 key pair generator
	 * @see KeyPairGenerator#generateKeyPair()
	 */
	KeyPairGenerator createKeyPairGeneratorSecp521r1AndInitialize();

	/**
	 * @return created and initialized x25519 key pair generator
	 * @see KeyPairGenerator#generateKeyPair()
	 */
	KeyPairGenerator createKeyPairGeneratorX25519AndInitialize();

	/**
	 * @return created and initialized x448 key pair generator
	 * @see KeyPairGenerator#generateKeyPair()
	 */
	KeyPairGenerator createKeyPairGeneratorX448AndInitialize();

	/**
	 * @param pem
	 *            not <code>null</code>
	 * @return certificate
	 * @throws IOException
	 *             if the given file does not contain a pem encoded certificate, more than one or is not readable or
	 *             parsable
	 */
	default X509Certificate readCertificate(Path pem) throws IOException
	{
		Objects.requireNonNull(pem, "pem");

		try (InputStream in = Files.newInputStream(pem))
		{
			return readCertificate(in);
		}
	}

	/**
	 * @param pem
	 *            not <code>null</code>
	 * @return certificate
	 * @throws IOException
	 *             if the given {@link InputStream} does not contain a pem encoded certificate, more than one or is not
	 *             readable or parsable
	 */
	X509Certificate readCertificate(InputStream pem) throws IOException;

	/**
	 * @param pem
	 *            not <code>null</code>
	 * @return list of certificates
	 * @throws IOException
	 *             if the given file does not contain pem encoded certificates or is not readable or one is not parsable
	 */
	default List<X509Certificate> readCertificates(Path pem) throws IOException
	{
		Objects.requireNonNull(pem, "pem");

		try (InputStream in = Files.newInputStream(pem))
		{
			return readCertificates(in);
		}
	}

	/**
	 * @param pem
	 * @return list of certificates
	 * @throws IOException
	 *             if the given {@link InputStream} does not contain pem encoded certificates or is not readable or one
	 *             is not parsable
	 */
	List<X509Certificate> readCertificates(InputStream pem) throws IOException;

	/**
	 * @param pem
	 *            not <code>null</code>
	 * @return private key
	 * @throws IOException
	 *             if the given file does not contain a pem encoded, unencrypted private key, more than one or is not
	 *             readable or parsable
	 */
	default PrivateKey readPrivateKey(Path pem) throws IOException
	{
		return readPrivateKey(pem, null);
	}

	/**
	 * @param pem
	 *            not <code>null</code>
	 * @return private key
	 * @throws IOException
	 *             if the given {@link InputStream} does not contain a pem encoded, unencrypted private key, more than
	 *             one or is not readable or parsable
	 */
	default PrivateKey readPrivateKey(InputStream pem) throws IOException
	{
		return readPrivateKey(pem, null);
	}

	/**
	 * @param pem
	 *            not <code>null</code>
	 * @param password
	 *            if key encrypted not <code>null</code>
	 * @return private key
	 * @throws IOException
	 *             if the given file does not contain a pem encoded private key, more than one or is not readable or
	 *             parsable
	 */
	default PrivateKey readPrivateKey(Path pem, char[] password) throws IOException
	{
		Objects.requireNonNull(pem, "pem");

		try (InputStream in = Files.newInputStream(pem))
		{
			return readPrivateKey(in, password);
		}
	}

	/**
	 * @param pem
	 *            not <code>null</code>
	 * @param password
	 *            if key encrypted not <code>null</code>
	 * @return private key
	 * @throws IOException
	 *             if the given {@link InputStream} does not contain a pem encoded private key, more than one or is not
	 *             readable or parsable
	 */
	PrivateKey readPrivateKey(InputStream pem, char[] password) throws IOException;

	/**
	 * Checks if the given <b>privateKey</b> and <b>publicKey</b> match by checking if a generated signature can be
	 * verified for RSA, EC and EdDSA key pairs or a Diffie-Hellman key agreement produces the same secret key for a XDH
	 * key pair. If the <b>privateKey</b> is a {@link RSAPrivateCrtKey} and the <b>publicKey</b> is a
	 * {@link RSAPublicKey} modulus and public-exponent will be compared.
	 *
	 * @param privateKey
	 *            may be <code>null</code>
	 * @param publicKey
	 *            may be <code>null</code>
	 * @return <code>true</code> if the given keys are not <code>null</code> and match
	 */
	boolean isKeyPair(PrivateKey privateKey, PublicKey publicKey);

	/**
	 * @param certificate
	 *            not <code>null</code>
	 * @return <code>true</code> if the given <b>certificate</b> not-after field is after {@link ZonedDateTime#now()}
	 */
	boolean isCertificateExpired(X509Certificate certificate);

	/**
	 * @param certificate
	 *            not <code>null</code>
	 * @return <code>true</code> if given <b>certificate</b> has extended key usage extension "TLS Web Client
	 *         Authentication"
	 */
	boolean isClientCertificate(X509Certificate certificate);

	/**
	 * @param certificate
	 *            not <code>null</code>
	 * @return <code>true</code> if given <b>certificate</b> has extended key usage extension "TLS Web Server
	 *         Authentication"
	 */
	boolean isServerCertificate(X509Certificate certificate);

	/**
	 * @param trustStore
	 *            not <code>null</code>
	 * @param certificateChain
	 *            not <code>null</code>
	 * @throws CertificateException
	 *             if the the given certificate or certificate chain is not trusted as a client certificate by a PKIX
	 *             trust manager created for the given trust store
	 */
	default void validateClientCertificate(KeyStore trustStore, X509Certificate... certificateChain)
			throws CertificateException
	{
		validateClientCertificate(trustStore, List.of(certificateChain));
	}

	/**
	 * @param trustStore
	 *            not <code>null</code>
	 * @param certificateChain
	 *            not <code>null</code>
	 * @throws CertificateException
	 *             if the the given certificate or certificate chain is not trusted as a client certificate by a PKIX
	 *             trust manager created for the given trust store
	 */
	void validateClientCertificate(KeyStore trustStore, Collection<? extends X509Certificate> certificateChain)
			throws CertificateException;

	/**
	 * @param trustStore
	 *            not <code>null</code>
	 * @param certificateChain
	 *            not <code>null</code>
	 * @throws CertificateException
	 *             if the the given certificate or certificate chain is not trusted as a server certificate by a PKIX
	 *             trust manager created for the given trust store
	 */
	default void validateServerCertificate(KeyStore trustStore, X509Certificate... certificateChain)
			throws CertificateException
	{
		validateServerCertificate(trustStore, List.of(certificateChain));
	}

	/**
	 * @param trustStore
	 *            not <code>null</code>
	 * @param certificateChain
	 *            not <code>null</code>
	 * @throws CertificateException
	 *             if the the given certificate or certificate chain is not trusted as a server certificate by a PKIX
	 *             trust manager created for the given trust store
	 */
	void validateServerCertificate(KeyStore trustStore, Collection<? extends X509Certificate> certificateChain)
			throws CertificateException;

	/**
	 * @param key
	 *            not <code>null</code>
	 * @param password
	 *            not <code>null</code>
	 * @param chain
	 *            not <code>null</code>, at least one
	 * @return jks {@link KeyStore} for the given key and chain
	 */
	default KeyStore createKeyStoreForPrivateKeyAndCertificateChain(PrivateKey key, char[] password,
			X509Certificate... chain)
	{
		return createKeyStoreForPrivateKeyAndCertificateChain(key, password, Arrays.asList(chain));
	}

	/**
	 * @param key
	 *            not <code>null</code>
	 * @param password
	 *            not <code>null</code>
	 * @param chain
	 *            not <code>null</code>, at least one
	 * @return jks {@link KeyStore} for the given key and chain
	 */
	KeyStore createKeyStoreForPrivateKeyAndCertificateChain(PrivateKey key, char[] password,
			Collection<? extends X509Certificate> chain);

	/**
	 * @param certificates
	 *            not <code>null</code>, at least one
	 * @return jks {@link KeyStore} for the given certificates
	 */
	default KeyStore createKeyStoreForTrustedCertificates(X509Certificate... certificates)
	{
		return createKeyStoreForTrustedCertificates(List.of(certificates));
	}

	/**
	 * @param certificates
	 *            not <code>null</code>, at least one
	 * @return jks {@link KeyStore} for the given certificates
	 */
	KeyStore createKeyStoreForTrustedCertificates(Collection<? extends X509Certificate> certificates);

	/**
	 * @param file
	 *            not <code>null</code>
	 * @param password
	 *            if not <code>null</code> used to check the integrity of the keystore
	 * @return jks {@link KeyStore}
	 * @throws IOException
	 * @see KeyStore#load(InputStream, char[])
	 */
	default KeyStore readKeyStoreJks(Path file, char[] password) throws IOException
	{
		Objects.requireNonNull(file, "file");

		try (InputStream in = Files.newInputStream(file))
		{
			return readKeyStoreJks(in, password);
		}
	}

	/**
	 * @param stream
	 *            not <code>null</code>
	 * @param password
	 *            if not <code>null</code> used to check the integrity of the keystore
	 * @return jks {@link KeyStore}
	 * @throws IOException
	 * @see KeyStore#load(InputStream, char[])
	 */
	KeyStore readKeyStoreJks(InputStream stream, char[] password) throws IOException;

	/**
	 * @param file
	 *            not <code>null</code>
	 * @param password
	 *            if not <code>null</code> used to check the integrity of the keystore
	 * @return pkcs12 {@link KeyStore}
	 * @throws IOException
	 * @see KeyStore#load(InputStream, char[])
	 */
	default KeyStore readKeyStorePkcs12(Path file, char[] password) throws IOException
	{
		Objects.requireNonNull(file, "file");

		try (InputStream in = Files.newInputStream(file))
		{
			return readKeyStorePkcs12(in, password);
		}
	}

	/**
	 * @param stream
	 *            not <code>null</code>
	 * @param password
	 *            if not <code>null</code> used to check the integrity of the keystore
	 * @return pkcs12 {@link KeyStore}
	 * @throws IOException
	 * @see KeyStore#load(InputStream, char[])
	 */
	KeyStore readKeyStorePkcs12(InputStream stream, char[] password) throws IOException;

	/**
	 * @param trustStore
	 *            not <code>null</code>
	 * @return {@link SSLContext} with {@link TrustManager} for the given <b>trustStore</b>
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws UnrecoverableKeyException
	 * @throws KeyManagementException
	 */
	SSLContext createSSLContext(KeyStore trustStore)
			throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyManagementException;

	/**
	 * @param trustStore
	 *            not <code>null</code>
	 * @param keyStore
	 *            not <code>null</code>
	 * @param keyStorePassword
	 *            not <code>null</code>
	 * @return {@link SSLContext} with {@link TrustManager} for the given <b>trustStore</b> and {@link KeyManager} for
	 *         the given <b>keyStore</b> / <b>keyStorePassword</b>
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws UnrecoverableKeyException
	 * @throws KeyManagementException
	 */
	SSLContext createSSLContext(KeyStore trustStore, KeyStore keyStore, char[] keyStorePassword)
			throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyManagementException;
}
