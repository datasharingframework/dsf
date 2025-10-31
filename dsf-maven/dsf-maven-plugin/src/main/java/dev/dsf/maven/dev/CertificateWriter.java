package dev.dsf.maven.dev;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.hsheilbronn.mi.utils.crypto.io.KeyStoreWriter;
import de.hsheilbronn.mi.utils.crypto.io.PemWriter;
import de.hsheilbronn.mi.utils.crypto.keystore.KeyStoreCreator;
import dev.dsf.maven.dev.CertificateGenerator.CertificateAndPrivateKey;

public class CertificateWriter extends AbstractIo
{
	private static final Logger logger = LoggerFactory.getLogger(CertificateWriter.class);

	private final Path projectBasedir;
	private final CertificateGenerator generator;
	private final char[] privateKeyPassword;

	public CertificateWriter(Path projectBasedir, CertificateGenerator generator, char[] privateKeyPassword)
	{
		this.projectBasedir = Objects.requireNonNull(projectBasedir, "projectBasedir");
		this.generator = Objects.requireNonNull(generator, "generator");
		this.privateKeyPassword = Objects.requireNonNull(privateKeyPassword, "privateKeyPassword");
	}

	public void write(List<Cert> certs)
	{
		if (certs != null)
			certs.forEach(this::write);
	}

	private void write(Cert cert)
	{
		Optional<CertificateAndPrivateKey> certificateAndPrivateKey = generator
				.getCertificateAndPrivateKey(cert.getCn());
		certificateAndPrivateKey.ifPresent(capk -> cert.getTargets().stream().map(File::toPath).forEach(target ->
		{
			if (target.getFileName().toString().endsWith(".chain.crt"))
				toRuntimeException(() -> writeCertificateChain(cert.getCn(), capk, target));
			else if (target.getFileName().toString().endsWith(".crt"))
				toRuntimeException(() -> writeCertificate(cert.getCn(), capk, target));
			else if (target.getFileName().toString().endsWith(".key"))
				toRuntimeException(() -> writePrivateKey(cert.getCn(), capk, target));
			else if (target.getFileName().toString().endsWith(".key.plain"))
				toRuntimeException(() -> writePrivateKeyPlain(cert.getCn(), capk, target));
			else if (target.getFileName().toString().endsWith(".key.password"))
				toRuntimeException(() -> writePassword(cert.getCn(), capk, target));
			else if (target.getFileName().toString().endsWith(".p12"))
				toRuntimeException(() -> writePkcs12(cert.getCn(), capk, target));
			else
				logger.warn("Cert (cn: {}) target filetype not supported: {}", cert.getCn(), target.getFileName());
		}));
	}

	public void write(RootCa rootCa)
	{
		if (rootCa == null)
			return;

		rootCa.getTargets().stream().filter(Objects::nonNull).map(File::toPath).forEach(target ->
		{
			if (target.getFileName().toString().endsWith(".crt"))
				toRuntimeException(() -> writeRootCa(target));
			else if (target.getFileName().toString().endsWith(".jks"))
				toRuntimeException(() -> writeRootCaJks(target));
			else
				logger.warn("RootCa target filetype not supported: {}", target.getFileName());
		});
	}

	public void write(IssuingCa issuingCa)
	{
		if (issuingCa == null)
			return;

		issuingCa.getTargets().stream().filter(Objects::nonNull).map(File::toPath).forEach(target ->
		{
			if (target.getFileName().toString().endsWith(".crt"))
				toRuntimeException(() -> writeIssuingCa(target));
			else if (target.getFileName().toString().endsWith(".jks"))
				toRuntimeException(() -> writeIssuingCaJks(target));
			else
				logger.warn("IssuingCa target filetype not supported: {}", target.getFileName());
		});
	}

	public void write(CaChain caChain)
	{
		if (caChain == null)
			return;

		caChain.getTargets().stream().filter(Objects::nonNull).map(File::toPath).forEach(target ->
		{
			if (target.getFileName().toString().endsWith(".crt"))
				toRuntimeException(() -> writeCaChain(target));
			else if (target.getFileName().toString().endsWith(".jks"))
				toRuntimeException(() -> writeCaChainJks(target));
			else
				logger.warn("CaChain target filetype not supported: {}", target.getFileName());
		});
	}

	private void writeCertificate(String cn, CertificateAndPrivateKey capk, Path target) throws IOException
	{
		logger.info("Writing certificate (cn: {}) to {}", cn, projectBasedir.relativize(target));

		PemWriter.writeCertificate(capk.certificate(), target);
	}

	private void writeCertificateChain(String cn, CertificateAndPrivateKey capk, Path target) throws IOException
	{
		logger.info("Writing certificate (cn: {}) and issuingCa to {}", cn, projectBasedir.relativize(target));

		PemWriter.writeCertificates(List.of(capk.certificate(), generator.getIssuingCaCertificate()), true, target);
	}

	private void writePrivateKey(String cn, CertificateAndPrivateKey capk, Path target) throws IOException
	{
		logger.info("Writing private-key encrypted (cn: {}) to {}", cn, projectBasedir.relativize(target));

		PemWriter.writePrivateKey(capk.privateKey()).asPkcs8().encryptedAes128(privateKeyPassword).toFile(target);
	}

	private void writePrivateKeyPlain(String cn, CertificateAndPrivateKey capk, Path target) throws IOException
	{
		logger.info("Writing private-key unencrypted (cn: {}) to {}", cn, projectBasedir.relativize(target));

		PemWriter.writePrivateKey(capk.privateKey()).asPkcs8().notEncrypted().toFile(target);
	}

	private void writePassword(String cn, CertificateAndPrivateKey capk, Path target) throws IOException
	{
		logger.info("Writing key password (cn: {}) to {}", cn, projectBasedir.relativize(target));

		Files.writeString(target, new String(privateKeyPassword));
	}

	private void writePkcs12(String cn, CertificateAndPrivateKey capk, Path target) throws IOException
	{
		logger.info("Writing pkcs12 key-store (cn: {}) to {}", cn, projectBasedir.relativize(target));

		KeyStore keyStore = KeyStoreCreator.pkcs12ForPrivateKeyAndCertificateChain(capk.privateKey(),
				privateKeyPassword, capk.certificate(), generator.getIssuingCaCertificate(),
				generator.getRootCaCertificate());

		KeyStoreWriter.write(keyStore, privateKeyPassword, target);
	}

	private void writeRootCa(Path target) throws IOException
	{
		logger.info("Writing rootCa to {}", projectBasedir.relativize(target));

		PemWriter.writeCertificate(generator.getRootCaCertificate(), target);
	}

	private void writeIssuingCa(Path target) throws IOException
	{
		logger.info("Writing issuingCa to {}", projectBasedir.relativize(target));

		PemWriter.writeCertificate(generator.getIssuingCaCertificate(), target);
	}

	private void writeCaChain(Path target) throws IOException
	{
		logger.info("Writing caChain to {}", projectBasedir.relativize(target));

		PemWriter.writeCertificates(List.of(generator.getIssuingCaCertificate(), generator.getRootCaCertificate()),
				true, target);
	}

	private void writeRootCaJks(Path target) throws IOException
	{
		KeyStore keyStore = KeyStoreCreator.jksForTrustedCertificates(generator.getRootCaCertificate());

		logger.info("Writing rootCa to {}", projectBasedir.relativize(target));

		KeyStoreWriter.write(keyStore, privateKeyPassword, target);
	}

	private void writeIssuingCaJks(Path target) throws IOException
	{
		KeyStore keyStore = KeyStoreCreator.jksForTrustedCertificates(generator.getIssuingCaCertificate());

		logger.info("Writing issuingCa to {}", projectBasedir.relativize(target));

		KeyStoreWriter.write(keyStore, privateKeyPassword, target);
	}

	private void writeCaChainJks(Path target) throws IOException
	{
		KeyStore keyStore = KeyStoreCreator.jksForTrustedCertificates(generator.getIssuingCaCertificate(),
				generator.getRootCaCertificate());

		logger.info("Writing caChain to {}", projectBasedir.relativize(target));

		KeyStoreWriter.write(keyStore, privateKeyPassword, target);
	}
}