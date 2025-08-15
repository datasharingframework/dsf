package dev.dsf.maven.ca;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.plugin.MojoExecutionException;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.hsheilbronn.mi.utils.crypto.io.PemReader;
import de.hsheilbronn.mi.utils.crypto.io.PemWriter;
import dev.dsf.maven.exception.RuntimeIOException;

public class DefaultCaFilesGenerator
{
	private static final Logger logger = LoggerFactory.getLogger(DefaultCaFilesGenerator.class);

	private static final String LOG_MESSAGE_SERVER_ROOT_C_AS = "server root CAs";
	private static final String LOG_MESSAGE_CLIENT_CA_CHAINS = "client CA chains";
	private static final String LOG_MESSAGE_CLIENT_ISSUING_C_AS = "client issuing CAs";

	private static final class X509CertificateHolder
	{
		final X509Certificate certificate;
		final Predicate<String> isClientOnly;
		final JcaX509CertificateHolder certificateHolder;

		final List<X509CertificateHolder> children = new ArrayList<>();
		X509CertificateHolder parent;

		X509CertificateHolder(X509Certificate certificate, Predicate<String> isClientOnly)
		{
			this.certificate = certificate;
			this.isClientOnly = isClientOnly;

			try
			{
				this.certificateHolder = new JcaX509CertificateHolder(certificate);
			}
			catch (CertificateEncodingException e)
			{
				throw new RuntimeException(e);
			}
		}

		X509Certificate getCertificate()
		{
			return certificate;
		}

		boolean isRoot()
		{
			return certificateHolder.getIssuer() != null
					&& certificateHolder.getIssuer().equals(certificateHolder.getSubject());
		}

		boolean isCa()
		{
			return certificate.getBasicConstraints() >= 0;
		}

		boolean isIssuingCa()
		{
			return getChildren().isEmpty();
		}

		boolean isClientOnly()
		{
			return getChildren().isEmpty() ? isClientOnly.test(getSubjectCommonName())
					: getChildren().stream().allMatch(X509CertificateHolder::isClientOnly);
		}

		X500Name getSubject()
		{
			return certificateHolder.getSubject();
		}

		String getSubjectCommonName()
		{
			return IETFUtils.valueToString(certificateHolder.getSubject().getRDNs(BCStyle.CN)[0].getFirst().getValue());
		}

		X500Name getIssuer()
		{
			return certificateHolder.getIssuer();
		}

		void setParent(Map<X500Name, X509CertificateHolder> certificatesBySubject)
		{
			if (isRoot())
				return;

			parent = certificatesBySubject.get(getIssuer());

			if (parent != null)
				parent.children.add(this);
		}

		List<X509CertificateHolder> getChildren()
		{
			return Collections.unmodifiableList(children);
		}

		ZonedDateTime getNotAfter()
		{
			return ZonedDateTime.ofInstant(certificate.getNotAfter().toInstant(), ZoneId.of("GMT"));
		}

		ZonedDateTime getNotBefore()
		{
			return ZonedDateTime.ofInstant(certificate.getNotBefore().toInstant(), ZoneId.of("GMT"));
		}
	}

	private final Path projectBasedir;
	private final Path certFolder;
	private final List<String> clientOnlyCaCommonNames;

	public DefaultCaFilesGenerator(Path projectBasedir, Path certFolder, List<String> clientOnlyCaCommonNames)
			throws MojoExecutionException
	{
		this.projectBasedir = projectBasedir;
		this.certFolder = certFolder;
		this.clientOnlyCaCommonNames = clientOnlyCaCommonNames;

		if (projectBasedir == null)
			throw new MojoExecutionException("projectBasedir not defined");
		if (certFolder == null)
			throw new MojoExecutionException("certFolder not defined");
		if (!Files.isReadable(certFolder))
			throw new MojoExecutionException("certFolder '" + certFolder.toString() + "' not readable");
		if (clientOnlyCaCommonNames == null || clientOnlyCaCommonNames.isEmpty())
			throw new MojoExecutionException("clientOnlyCaCommonNames not defined or empty");
	}

	public void createFiles(Stream<Path> clientCertIssuingCaFiles, Stream<Path> clientCertCaChainFiles,
			Stream<Path> serverCertRootCaFiles) throws IOException, MojoExecutionException
	{
		if (clientCertIssuingCaFiles == null)
			throw new MojoExecutionException("clientCertIssuingCaFiles not defined");
		if (clientCertCaChainFiles == null)
			throw new MojoExecutionException("clientCertCaChainFiles not defined");
		if (serverCertRootCaFiles == null)
			throw new MojoExecutionException("serverCertRootCaFiles not defined");

		List<Path> iToWrite = clientCertIssuingCaFiles.filter(ifNotExists(LOG_MESSAGE_CLIENT_ISSUING_C_AS)).toList();
		List<Path> cToWrite = clientCertCaChainFiles.filter(ifNotExists(LOG_MESSAGE_CLIENT_CA_CHAINS)).toList();
		List<Path> sWrite = serverCertRootCaFiles.filter(ifNotExists(LOG_MESSAGE_SERVER_ROOT_C_AS)).toList();

		if (iToWrite.isEmpty() && cToWrite.isEmpty() && sWrite.isEmpty())
			return;

		List<X509CertificateHolder> certificates = readCertificates(certFolder, clientOnlyCaCommonNames);

		try
		{
			iToWrite.forEach(writeClientIssuingCas(certificates, LOG_MESSAGE_CLIENT_ISSUING_C_AS));

			cToWrite.forEach(writeClientCaChains(certificates, LOG_MESSAGE_CLIENT_CA_CHAINS));

			sWrite.forEach(writeServerRootCas(certificates, LOG_MESSAGE_SERVER_ROOT_C_AS));
		}
		catch (RuntimeIOException e)
		{
			throw e.getCause();
		}
	}

	private Predicate<Path> ifNotExists(String logMessage)
	{
		return file ->
		{

			boolean exists = Files.isReadable(file);

			if (exists)
				logger.info("Default {} file exists at {}", logMessage, projectBasedir.relativize(file));

			return !exists;
		};
	}

	private List<X509CertificateHolder> readCertificates(Path certFolder, List<String> clientOnlyCaCommonNames)
			throws IOException
	{
		List<X509CertificateHolder> certificates = new ArrayList<>();
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(certFolder,
				entry -> entry.getFileName().toString().endsWith(".pem")))
		{
			directoryStream.forEach(readCertificate(certificates, clientOnlyCaCommonNames));
		}

		Map<X500Name, X509CertificateHolder> certificatesBySubject = certificates.stream()
				.collect(Collectors.toMap(X509CertificateHolder::getSubject, Function.identity()));
		certificates.forEach(c -> c.setParent(certificatesBySubject));

		return certificates;
	}

	private Consumer<? super Path> readCertificate(List<X509CertificateHolder> certificates,
			List<String> clientOnlyCaCommonNames)
	{
		return file ->
		{
			try
			{
				logger.info("Reading certificate from {}", projectBasedir.relativize(file));
				X509CertificateHolder certificate = new X509CertificateHolder(PemReader.readCertificate(file),
						clientOnlyCaCommonNames::contains);

				if (!certificate.isCa())
					throw new RuntimeException("Certificate in " + file.toString() + " is not a CA certificate");

				ZonedDateTime now = ZonedDateTime.now();
				if (now.isBefore(certificate.getNotBefore()))
					throw new RuntimeException("Certificate in " + file.toString() + " is not valid before "
							+ DateTimeFormatter.ISO_ZONED_DATE_TIME.format(certificate.getNotBefore())
							+ " current date/time "
							+ DateTimeFormatter.ISO_ZONED_DATE_TIME.format(now.withZoneSameInstant(ZoneId.of("GMT"))));
				if (now.isAfter(certificate.getNotAfter()))
					throw new RuntimeException("Certificate in " + file.toString() + " is not valid after "
							+ DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(certificate.getNotAfter())
							+ " current date/time "
							+ DateTimeFormatter.ISO_ZONED_DATE_TIME.format(now.withZoneSameInstant(ZoneId.of("GMT"))));

				if (now.plusYears(1).isAfter(certificate.getNotAfter()))
					logger.warn("Certificate in {} is not valid after {}", file.toString(),
							DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(certificate.getNotAfter()));

				certificates.add(certificate);
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		};
	}

	private Consumer<Path> writeClientIssuingCas(List<X509CertificateHolder> certificates, String logMessage)
	{
		return file ->
		{
			List<X509Certificate> certs = certificates.stream().filter(X509CertificateHolder::isIssuingCa)
					.sorted(Comparator.comparing(X509CertificateHolder::getSubjectCommonName))
					.map(X509CertificateHolder::getCertificate).toList();

			writeCas(certs, file, logMessage);
		};
	}

	private Consumer<Path> writeClientCaChains(List<X509CertificateHolder> certificates, String logMessage)
	{
		return file ->
		{
			List<X509Certificate> certs = certificates.stream().filter(X509CertificateHolder::isRoot)
					.sorted(Comparator.comparing(X509CertificateHolder::getSubjectCommonName)).flatMap(childern())
					.map(X509CertificateHolder::getCertificate).toList();

			writeCas(certs, file, logMessage);
		};
	}

	private Function<X509CertificateHolder, Stream<X509CertificateHolder>> childern()
	{
		return cert -> Stream.concat(Stream.of(cert), cert.getChildren().stream()
				.sorted(Comparator.comparing(X509CertificateHolder::getSubjectCommonName)).flatMap(childern()));
	}

	private Consumer<Path> writeServerRootCas(List<X509CertificateHolder> certificates, String logMessage)
	{
		return file ->
		{
			List<X509Certificate> certs = certificates.stream().filter(X509CertificateHolder::isRoot)
					.filter(Predicate.not(X509CertificateHolder::isClientOnly))
					.sorted(Comparator.comparing(X509CertificateHolder::getSubjectCommonName))
					.map(X509CertificateHolder::getCertificate).toList();

			writeCas(certs, file, logMessage);
		};
	}

	private void writeCas(List<X509Certificate> certs, Path file, String logMessage) throws RuntimeIOException
	{
		try
		{
			logger.info("Writing default {} file to {}", logMessage, projectBasedir.relativize(file));
			PemWriter.writeCertificates(certs, file);
		}
		catch (IOException e)
		{
			throw new RuntimeIOException(e);
		}
	}
}
