package dev.dsf.maven.dev;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.asn1.x500.AttributeTypeAndValue;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.hsheilbronn.mi.utils.crypto.ca.CertificateAuthority;
import de.hsheilbronn.mi.utils.crypto.ca.CertificationRequest;
import de.hsheilbronn.mi.utils.crypto.ca.CertificationRequest.CertificationRequestAndPrivateKey;
import de.hsheilbronn.mi.utils.crypto.ca.CertificationRequest.CertificationRequestBuilder;
import de.hsheilbronn.mi.utils.crypto.io.PemReader;
import de.hsheilbronn.mi.utils.crypto.io.PemWriter;
import de.hsheilbronn.mi.utils.crypto.keypair.KeyPairValidator;

public class CertificateGenerator
{
	private static final Logger logger = LoggerFactory.getLogger(CertificateGenerator.class);

	private static record ReadOrCreated<T>(T element, boolean created)
	{
		public static <T> ReadOrCreated<T> read(T element)
		{
			return new ReadOrCreated<>(element, false);
		}

		public static <T> ReadOrCreated<T> created(T element)
		{
			return new ReadOrCreated<>(element, true);
		}
	}

	public static record CertificateAndPrivateKey(X509Certificate certificate, PrivateKey privateKey)
	{
	}

	public static record CertificationRequestConfig(
			BiFunction<CertificateAuthority, CertificationRequest, X509Certificate> signer, String commonName,
			String mail, List<String> dnsNames)
	{
		public CertificationRequestConfig(
				BiFunction<CertificateAuthority, CertificationRequest, X509Certificate> signer, String commonName,
				String mail, String... dnsNames)
		{
			this(signer, commonName, mail, List.of(dnsNames));
		}

		public CertificationRequestConfig(
				BiFunction<CertificateAuthority, CertificationRequest, X509Certificate> signer, String commonName,
				String mail)
		{
			this(signer, commonName, mail, List.of());
		}

		public CertificateAndPrivateKey sign(CertificateAuthority ca)
		{
			CertificationRequestBuilder reqBuilder = CertificationRequest
					.builder(ca, SUBJECT_C, null, null, SUBJECT_O, null, commonName).generateKeyPair()
					.setDnsNames(dnsNames);

			if (mail != null && !mail.isBlank())
				reqBuilder.setEmail(mail);

			CertificationRequestAndPrivateKey req = reqBuilder.build();

			X509Certificate crt = signer.apply(ca, req);

			return new CertificateAndPrivateKey(crt, req.getPrivateKey());
		}
	}

	public static final String POSTFIX_PRIVATE_KEY = ".key";
	public static final String POSTFIX_CERTIFICATE = ".crt";

	private static final String SUBJECT_C = "DE";
	private static final String SUBJECT_O = "DSF";

	public static final String SUBJECT_CN_ROOT_CA = "DSF Dev Root CA";
	public static final String SUBJECT_CN_ISSUING_CA = "DSF Dev Issuing CA";

	private static final CertificationRequestConfig CERTIFICATION_REQUEST_ISSUING_CA = new CertificationRequestConfig(
			CertificateAuthority::signClientServerIssuingCaCertificate, SUBJECT_CN_ISSUING_CA, null);

	private final Path certDir;
	private final char[] privateKeyPassword;
	private final List<CertificationRequestConfig> certificationRequestConfigs = new ArrayList<>();

	private CertificateAuthority rootCa;
	private CertificateAuthority issuingCa;
	private Map<String, CertificateAndPrivateKey> certificatesByCommonName;

	public CertificateGenerator(Path certDir, char[] privateKeyPassword,
			List<CertificationRequestConfig> certificationRequestConfigs)
	{
		Objects.requireNonNull(certDir, "certDir");
		Objects.requireNonNull(privateKeyPassword, "privateKeyPassword");

		this.certDir = certDir;
		this.privateKeyPassword = privateKeyPassword;

		if (certificationRequestConfigs != null)
			this.certificationRequestConfigs.addAll(certificationRequestConfigs);
	}

	public void initialize()
	{
		logger.info("Initializing certificate generator ...");

		ReadOrCreated<CertificateAuthority> initRootCa = initRootCa();
		rootCa = initRootCa.element();

		ReadOrCreated<CertificateAuthority> initIssuingCa = initIssuingCa(initRootCa);
		issuingCa = initIssuingCa.element();

		certificatesByCommonName = initCertificates(initIssuingCa);
	}

	public boolean isInitialized()
	{
		return rootCa != null && issuingCa != null && certificatesByCommonName != null;
	}

	private void checkInitialized()
	{
		if (!isInitialized())
			throw new IllegalStateException("not initialized");
	}

	public X509Certificate getRootCaCertificate()
	{
		checkInitialized();

		return rootCa.getCertificate();
	}

	public X509Certificate getIssuingCaCertificate()
	{
		checkInitialized();

		return issuingCa.getCertificate();
	}

	public Map<String, CertificateAndPrivateKey> getCertificatesAndPrivateKeysByCommonName()
	{
		checkInitialized();

		return Collections.unmodifiableMap(certificatesByCommonName);
	}

	public Optional<CertificateAndPrivateKey> getCertificateAndPrivateKey(String commonName)
	{
		checkInitialized();

		return Optional.ofNullable(certificatesByCommonName.get(commonName));
	}

	public Map<String, String> getCertificateThumbprintsByCommonNameAsHex()
	{
		checkInitialized();

		return certificatesByCommonName.entrySet().stream()
				.collect(Collectors.toUnmodifiableMap(Entry::getKey, e -> toHexThumbprint(e.getValue().certificate())));
	}

	private String toHexThumbprint(X509Certificate certificate)
	{
		try
		{
			return Hex.encodeHexString(MessageDigest.getInstance("SHA-512").digest(certificate.getEncoded()));
		}
		catch (CertificateEncodingException | NoSuchAlgorithmException e)
		{
			logger.error("Unable to calculating SHA-512 certificate thumbprint: {} - {}", e.getClass().getName(),
					e.getMessage());
			throw new RuntimeException(e);
		}
	}

	private Path toPath(String commonName, String postFix)
	{
		return certDir.resolve(commonName.replaceAll(" ", "_") + postFix);
	}

	private Optional<X509Certificate> readCertificate(String commonName)
	{
		Path file = toPath(commonName, POSTFIX_CERTIFICATE);

		if (!Files.isReadable(file))
			return Optional.empty();

		try
		{
			return Optional.of(PemReader.readCertificate(file));
		}
		catch (IOException e)
		{
			logger.error("Unable to read certificate {}: {} - {}", file.toAbsolutePath().normalize(),
					e.getClass().getName(), e.getMessage());

			throw new RuntimeException(e);
		}
	}

	private Optional<PrivateKey> readPrivateKey(String commonName)
	{
		Path file = toPath(commonName, POSTFIX_PRIVATE_KEY);

		if (!Files.isReadable(file))
			return Optional.empty();

		try
		{
			return Optional.of(PemReader.readPrivateKey(file, privateKeyPassword));
		}
		catch (IOException e)
		{
			logger.error("Unable to read private-key {}: {} - {}", file.toAbsolutePath().normalize(),
					e.getClass().getName(), e.getMessage());

			throw new RuntimeException(e);
		}
	}

	private Optional<CertificateAndPrivateKey> readCertificateAndPrivateKey(String commonName)
	{
		Optional<X509Certificate> crt = readCertificate(commonName);
		if (crt.isEmpty())
		{
			logger.debug("Certificate for '{}' not found", commonName);
			return Optional.empty();
		}

		if (getCommonName(crt.get()).filter(cn -> cn.equals(commonName)).isEmpty())
		{
			logger.warn("Found certificate for '{}' subject common-name not matching", commonName);
			return Optional.empty();
		}

		Optional<PrivateKey> key = readPrivateKey(commonName);
		if (key.isEmpty())
		{
			logger.debug("Private-Key for '{}' not found", commonName);
			return Optional.empty();
		}

		if (!KeyPairValidator.matches(key.get(), crt.map(X509Certificate::getPublicKey).get()))
		{
			logger.warn("Found certificate and private-key for '{}' not matching", commonName);
			return Optional.empty();
		}

		try
		{
			crt.get().checkValidity();
		}
		catch (CertificateExpiredException | CertificateNotYetValidException e)
		{
			logger.warn("Found certificate not valid: {}", e.getMessage());
			return Optional.empty();
		}

		logger.info("Using existing certificate and private-key for '{}'", commonName);
		return Optional.of(new CertificateAndPrivateKey(crt.get(), key.get()));
	}

	private Optional<String> getCommonName(X509Certificate crt)
	{
		try
		{
			return Arrays.stream(new JcaX509CertificateHolder(crt).getSubject().getRDNs(BCStyle.CN))
					.filter(Objects::nonNull).map(RDN::getFirst).map(AttributeTypeAndValue::getValue)
					.map(IETFUtils::valueToString).findFirst();
		}
		catch (CertificateEncodingException e)
		{
			return Optional.empty();
		}
	}

	private void writeCertificate(String commonName, X509Certificate crt)
	{
		Path file = toPath(commonName, POSTFIX_CERTIFICATE);

		try
		{
			PemWriter.writeCertificate(crt, true, file);
		}
		catch (IOException e)
		{
			logger.error("Unable to write certificate {}: {} - {}", file.toAbsolutePath().normalize(),
					e.getClass().getName(), e.getMessage());
			throw new RuntimeException(e);
		}
	}

	private void writePrivateKey(String commonName, PrivateKey privateKey)
	{
		Path file = toPath(commonName, POSTFIX_PRIVATE_KEY);

		try
		{
			PemWriter.writePrivateKey(privateKey).asPkcs8().encryptedAes128(privateKeyPassword).toFile(file);
		}
		catch (IOException e)
		{
			logger.error("Unable to write private-key {}: {} - {}", file.toAbsolutePath().normalize(),
					e.getClass().getName(), e.getMessage());
			throw new RuntimeException(e);
		}
	}

	private void writeCertificateAndPrivateKey(String commonName, CertificateAndPrivateKey certificateAndPrivateKey)
	{
		writeCertificate(commonName, certificateAndPrivateKey.certificate());
		writePrivateKey(commonName, certificateAndPrivateKey.privateKey());
	}

	private ReadOrCreated<CertificateAuthority> initRootCa()
	{
		return readCa(SUBJECT_CN_ROOT_CA).orElseGet(() -> createRootCa());
	}

	private Optional<ReadOrCreated<CertificateAuthority>> readCa(String commonName)
	{
		return readCertificateAndPrivateKey(commonName)
				.map(cK -> ReadOrCreated.read(CertificateAuthority.existingCa(cK.certificate(), cK.privateKey())));
	}

	private ReadOrCreated<CertificateAuthority> createRootCa()
	{
		logger.info("Creating '{}'", SUBJECT_CN_ROOT_CA);

		CertificateAuthority ca = CertificateAuthority
				.builderSha384EcdsaSecp384r1(SUBJECT_C, null, null, SUBJECT_O, null, SUBJECT_CN_ROOT_CA).build();

		writeCertificateAndPrivateKey(SUBJECT_CN_ROOT_CA,
				new CertificateAndPrivateKey(ca.getCertificate(), ca.getKeyPair().getPrivate()));

		return ReadOrCreated.created(ca);
	}

	private ReadOrCreated<CertificateAuthority> initIssuingCa(ReadOrCreated<CertificateAuthority> rootCa)
	{
		if (rootCa.created())
			return createIssuingCa(rootCa.element());
		else
			return readCa(SUBJECT_CN_ISSUING_CA).orElseGet(() -> createIssuingCa(rootCa.element()));
	}

	private ReadOrCreated<CertificateAuthority> createIssuingCa(CertificateAuthority rootCa)
	{
		logger.info("Creating private-key and signing certificate for '{}'", SUBJECT_CN_ISSUING_CA);

		CertificateAndPrivateKey cK = CERTIFICATION_REQUEST_ISSUING_CA.sign(rootCa);
		CertificateAuthority ca = CertificateAuthority.existingCa(cK.certificate(), cK.privateKey());

		writeCertificateAndPrivateKey(SUBJECT_CN_ISSUING_CA, cK);

		return ReadOrCreated.created(ca);
	}

	private Map<String, CertificateAndPrivateKey> initCertificates(ReadOrCreated<CertificateAuthority> issuingCa)
	{
		return certificationRequestConfigs.stream()
				.collect(Collectors.toMap(CertificationRequestConfig::commonName,
						(c -> issuingCa.created() ? createCertificate(c, issuingCa)
								: readCertificateAndPrivateKey(c.commonName())
										.orElseGet(() -> createCertificate(c, issuingCa)))));
	}

	private CertificateAndPrivateKey createCertificate(CertificationRequestConfig c,
			ReadOrCreated<CertificateAuthority> issuingCa)
	{
		logger.info("Creating private-key and signing certificate for '{}'", c.commonName());

		CertificateAndPrivateKey cK = c.sign(issuingCa.element());

		writeCertificateAndPrivateKey(c.commonName, cK);

		return cK;
	}
}
