package dev.dsf.bpe.v2.service;

import java.io.IOException;
import java.io.InputStream;
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
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import javax.crypto.DecapsulateException;
import javax.crypto.NoSuchPaddingException;
import javax.net.ssl.SSLContext;

import de.hsheilbronn.mi.utils.crypto.cert.CertificateValidator;
import de.hsheilbronn.mi.utils.crypto.context.SSLContextFactory;
import de.hsheilbronn.mi.utils.crypto.io.KeyStoreReader;
import de.hsheilbronn.mi.utils.crypto.io.PemReader;
import de.hsheilbronn.mi.utils.crypto.kem.AbstractKemAesGcm;
import de.hsheilbronn.mi.utils.crypto.kem.EcDhKemAesGcm;
import de.hsheilbronn.mi.utils.crypto.kem.RsaKemAesGcm;
import de.hsheilbronn.mi.utils.crypto.keypair.KeyPairGeneratorFactory;
import de.hsheilbronn.mi.utils.crypto.keypair.KeyPairValidator;
import de.hsheilbronn.mi.utils.crypto.keystore.KeyStoreCreator;

public class CryptoServiceImpl implements CryptoService
{
	public static final class KemDelegate implements Kem
	{
		private final AbstractKemAesGcm delegate;

		public KemDelegate(AbstractKemAesGcm delegate)
		{
			this.delegate = delegate;
		}

		@Override
		public InputStream encrypt(InputStream data, PublicKey publicKey) throws NoSuchAlgorithmException,
				InvalidKeyException, NoSuchPaddingException, InvalidAlgorithmParameterException
		{
			return delegate.encrypt(data, publicKey);
		}

		@Override
		public InputStream decrypt(InputStream encrypted, PrivateKey privateKey)
				throws IOException, NoSuchAlgorithmException, InvalidKeyException, DecapsulateException,
				NoSuchPaddingException, InvalidAlgorithmParameterException
		{
			return delegate.decrypt(encrypted, privateKey);
		}
	}

	@Override
	public Kem createRsaKem()
	{
		return new KemDelegate(new RsaKemAesGcm());
	}

	@Override
	public Kem createEcDhKem()
	{
		return new KemDelegate(new EcDhKemAesGcm());
	}

	@Override
	public KeyPairGenerator createKeyPairGeneratorRsa4096AndInitialize()
	{
		return KeyPairGeneratorFactory.rsa4096().initialize();
	}

	@Override
	public KeyPairGenerator createKeyPairGeneratorSecp256r1AndInitialize()
	{
		return KeyPairGeneratorFactory.secp256r1().initialize();
	}

	@Override
	public KeyPairGenerator createKeyPairGeneratorSecp384r1AndInitialize()
	{
		return KeyPairGeneratorFactory.secp384r1().initialize();
	}

	@Override
	public KeyPairGenerator createKeyPairGeneratorSecp521r1AndInitialize()
	{
		return KeyPairGeneratorFactory.secp521r1().initialize();
	}

	@Override
	public KeyPairGenerator createKeyPairGeneratorX25519AndInitialize()
	{
		return KeyPairGeneratorFactory.x25519().initialize();
	}

	@Override
	public KeyPairGenerator createKeyPairGeneratorX448AndInitialize()
	{
		return KeyPairGeneratorFactory.x448().initialize();
	}

	@Override
	public X509Certificate readCertificate(InputStream pem) throws IOException
	{
		return PemReader.readCertificate(pem);
	}

	@Override
	public List<X509Certificate> readCertificates(InputStream pem) throws IOException
	{
		return PemReader.readCertificates(pem);
	}

	@Override
	public PrivateKey readPrivateKey(InputStream pem, char[] password) throws IOException
	{
		return PemReader.readPrivateKey(pem, password);
	}

	@Override
	public boolean isKeyPair(PrivateKey privateKey, PublicKey publicKey)
	{
		return KeyPairValidator.matches(privateKey, publicKey);
	}

	@Override
	public boolean isCertificateExpired(X509Certificate certificate)
	{
		return CertificateValidator.isCertificateExpired(certificate);
	}

	@Override
	public boolean isClientCertificate(X509Certificate certificate)
	{
		return CertificateValidator.isClientCertificate(certificate);
	}

	@Override
	public boolean isServerCertificate(X509Certificate certificate)
	{
		return CertificateValidator.isServerCertificate(certificate);
	}

	@Override
	public void validateClientCertificate(KeyStore trustStore, Collection<? extends X509Certificate> certificateChain)
			throws CertificateException
	{
		Objects.requireNonNull(trustStore, "trustStore");
		Objects.requireNonNull(certificateChain, "certificateChain");

		CertificateValidator.vaildateClientCertificate(trustStore, certificateChain);
	}

	@Override
	public void validateServerCertificate(KeyStore trustStore, Collection<? extends X509Certificate> certificateChain)
			throws CertificateException
	{
		Objects.requireNonNull(trustStore, "trustStore");
		Objects.requireNonNull(certificateChain, "certificateChain");

		CertificateValidator.vaildateServerCertificate(trustStore, certificateChain);
	}

	@Override
	public KeyStore createKeyStoreForPrivateKeyAndCertificateChain(PrivateKey key, char[] password,
			Collection<? extends X509Certificate> chain)
	{
		return KeyStoreCreator.jksForPrivateKeyAndCertificateChain(key, password, chain);
	}

	@Override
	public KeyStore createKeyStoreForTrustedCertificates(Collection<? extends X509Certificate> certificates)
	{
		return KeyStoreCreator.jksForTrustedCertificates(certificates);
	}

	@Override
	public KeyStore readKeyStoreJks(InputStream stream, char[] password) throws IOException
	{
		return KeyStoreReader.readJks(stream, password);
	}

	@Override
	public KeyStore readKeyStorePkcs12(InputStream stream, char[] password) throws IOException
	{
		return KeyStoreReader.readPkcs12(stream, password);
	}

	@Override
	public SSLContext createSSLContext(KeyStore trustStore)
			throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyManagementException
	{
		Objects.requireNonNull(trustStore, "trustStore");

		return SSLContextFactory.createSSLContext(trustStore);
	}

	@Override
	public SSLContext createSSLContext(KeyStore trustStore, KeyStore keyStore, char[] keyStorePassword)
			throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyManagementException
	{
		Objects.requireNonNull(trustStore, "trustStore");
		Objects.requireNonNull(keyStore, "keyStore");
		Objects.requireNonNull(keyStorePassword, "keyStorePassword");

		return SSLContextFactory.createSSLContext(trustStore, keyStore, keyStorePassword);
	}
}
