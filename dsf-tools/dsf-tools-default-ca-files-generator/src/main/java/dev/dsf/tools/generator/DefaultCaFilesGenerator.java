package dev.dsf.tools.generator;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
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

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.rwh.utils.crypto.io.PemIo;

public class DefaultCaFilesGenerator
{
	private static final Logger logger = LoggerFactory.getLogger(DefaultCaFilesGenerator.class);

	private static final List<String> CLIENT_ONLY_ISSUING_CA_COMMON_NAMES = List.of("D-TRUST Limited Basic CA 1-2 2019",
			"D-TRUST Limited Basic CA 1-3 2019", "Fraunhofer User CA - G02", "GEANT eScience Personal CA 4",
			"GEANT eScience Personal ECC CA 4", "GEANT Personal CA 4", "GEANT Personal ECC CA 4", "GEANT S/MIME ECC 1",
			"GEANT S/MIME RSA 1", "HARICA S/MIME ECC", "HARICA S/MIME RSA");

	private static final String CLIENT_CERT_ISSUING_CAS_PEM = "ClientCertIssuingCAs.pem";
	private static final String CLIENT_CERT_CA_CHAINS_PEM = "ClientCertCaChains.pem";
	private static final String SERVER_CERT_ROOT_CAS_PEM = "ServerCertRootCAs.pem";

	private static final Path CLIENT_CERT_ISSUING_CAS_FILE = Paths.get("cert", CLIENT_CERT_ISSUING_CAS_PEM);
	private static final Path CLIENT_CERT_CA_CHAINS_FILE = Paths.get("cert", CLIENT_CERT_CA_CHAINS_PEM);
	private static final Path SERVER_CERT_ROOT_CAS_FILE = Paths.get("cert", SERVER_CERT_ROOT_CAS_PEM);

	private static final Path BPE_PROXY_TARGET_FOLDER = Paths.get("../../dsf-docker/bpe_proxy/ca");
	private static final Path BPE_SERVER_TARGET_FOLDER = Paths.get("../../dsf-bpe/dsf-bpe-server-jetty/docker/ca");
	private static final Path FHIR_PROXY_TARGET_FOLDER = Paths.get("../../dsf-docker/fhir_proxy/ca");
	private static final Path FHIR_SERVER_TARGET_FOLDER = Paths.get("../../dsf-fhir/dsf-fhir-server-jetty/docker/ca");

	private static final List<Path> CLIENT_CERT_ISSUING_CAS_TARGET_FILES = List.of(
			BPE_PROXY_TARGET_FOLDER.resolve(CLIENT_CERT_ISSUING_CAS_PEM),
			FHIR_PROXY_TARGET_FOLDER.resolve(CLIENT_CERT_ISSUING_CAS_PEM));

	private static final List<Path> CLIENT_CERT_CA_CHAINS_TARGET_FILES = List.of(
			BPE_SERVER_TARGET_FOLDER.resolve(CLIENT_CERT_CA_CHAINS_PEM),
			BPE_PROXY_TARGET_FOLDER.resolve(CLIENT_CERT_CA_CHAINS_PEM),
			FHIR_SERVER_TARGET_FOLDER.resolve(CLIENT_CERT_CA_CHAINS_PEM),
			FHIR_PROXY_TARGET_FOLDER.resolve(CLIENT_CERT_CA_CHAINS_PEM));

	private static final List<Path> SERVER_CERT_ROOT_CAS_TARGET_FILES = List.of(
			BPE_SERVER_TARGET_FOLDER.resolve(SERVER_CERT_ROOT_CAS_PEM),
			FHIR_SERVER_TARGET_FOLDER.resolve(SERVER_CERT_ROOT_CAS_PEM));

	private static final class X509CertificateHolder
	{
		final X509Certificate certificate;
		final JcaX509CertificateHolder certificateHolder;

		final List<X509CertificateHolder> children = new ArrayList<>();
		X509CertificateHolder parent;

		X509CertificateHolder(X509Certificate certificate)
		{
			this.certificate = certificate;

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
			return getChildren().isEmpty() ? CLIENT_ONLY_ISSUING_CA_COMMON_NAMES.contains(getSubjectCommonName())
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

	public static void main(String[] args) throws IOException
	{
		if (!Files.isReadable(CLIENT_CERT_ISSUING_CAS_FILE) || !Files.isReadable(CLIENT_CERT_CA_CHAINS_FILE)
				|| !Files.isReadable(SERVER_CERT_ROOT_CAS_FILE))
		{
			List<X509CertificateHolder> certificates = readCertificates();

			if (!Files.isReadable(CLIENT_CERT_ISSUING_CAS_FILE))
				writeClientIssuingCas(certificates, CLIENT_CERT_ISSUING_CAS_FILE);

			if (!Files.isReadable(CLIENT_CERT_CA_CHAINS_FILE))
				writeClientCaChains(certificates, CLIENT_CERT_CA_CHAINS_FILE);

			if (!Files.isReadable(SERVER_CERT_ROOT_CAS_FILE))
				writeServerRootCas(certificates, SERVER_CERT_ROOT_CAS_FILE);
		}

		copy(CLIENT_CERT_ISSUING_CAS_FILE, CLIENT_CERT_ISSUING_CAS_TARGET_FILES);
		copy(CLIENT_CERT_CA_CHAINS_FILE, CLIENT_CERT_CA_CHAINS_TARGET_FILES);
		copy(SERVER_CERT_ROOT_CAS_FILE, SERVER_CERT_ROOT_CAS_TARGET_FILES);
	}

	private static List<X509CertificateHolder> readCertificates() throws IOException
	{
		List<X509CertificateHolder> certificates = new ArrayList<>();
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get("src/main/resources/cert"),
				entry -> entry.getFileName().toString().endsWith(".pem")))
		{
			directoryStream.forEach(readCertificate(certificates));
		}

		Map<X500Name, X509CertificateHolder> certificatesBySubject = certificates.stream()
				.collect(Collectors.toMap(X509CertificateHolder::getSubject, Function.identity()));
		certificates.forEach(c -> c.setParent(certificatesBySubject));

		return certificates;
	}

	private static Consumer<? super Path> readCertificate(List<X509CertificateHolder> certificates)
	{
		return file ->
		{
			try
			{
				logger.debug("Reading certificate from {}", file.toString());
				X509CertificateHolder certificate = new X509CertificateHolder(PemIo.readX509CertificateFromPem(file));

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
			catch (CertificateException | IOException e)
			{
				throw new RuntimeException(e);
			}
		};
	}

	private static void writeClientIssuingCas(List<X509CertificateHolder> certificates, Path file)
	{
		List<X509CertificateHolder> certs = certificates.stream().filter(X509CertificateHolder::isIssuingCa)
				.sorted(Comparator.comparing(X509CertificateHolder::getSubjectCommonName)).toList();

		logger.info("Writing default client issuing CAs file to {}", file.toString());
		writeCas(certs, file);
	}

	private static void writeClientCaChains(List<X509CertificateHolder> certificates, Path file)
	{
		List<X509CertificateHolder> certs = certificates.stream().filter(X509CertificateHolder::isRoot)
				.sorted(Comparator.comparing(X509CertificateHolder::getSubjectCommonName)).flatMap(childern()).toList();

		logger.info("Writing default client CA chains file to {}", file.toString());
		writeCas(certs, file);
	}

	private static Function<X509CertificateHolder, Stream<X509CertificateHolder>> childern()
	{
		return cert -> Stream.concat(Stream.of(cert), cert.getChildren().stream()
				.sorted(Comparator.comparing(X509CertificateHolder::getSubjectCommonName)).flatMap(childern()));
	}

	private static void writeServerRootCas(List<X509CertificateHolder> certificates, Path file)
	{
		List<X509CertificateHolder> certs = certificates.stream().filter(X509CertificateHolder::isRoot)
				.filter(Predicate.not(X509CertificateHolder::isClientOnly))
				.sorted(Comparator.comparing(X509CertificateHolder::getSubjectCommonName)).toList();

		logger.info("Writing default server root CAs file to {}", file.toString());
		writeCas(certs, file);
	}

	private static void writeCas(List<X509CertificateHolder> certificates, Path file)
	{
		try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8))
		{
			for (X509CertificateHolder c : certificates)
			{
				writer.write("Subject: " + c.getSubject().toString() + "\n");
				writer.write(PemIo.writeX509Certificate(c.getCertificate()));
			}
		}
		catch (IOException | CertificateEncodingException e)
		{
			throw new RuntimeException(e);
		}
	}

	private static void copy(Path source, List<Path> targets)
	{
		targets.forEach(copy(source));
	}

	private static Consumer<Path> copy(Path source)
	{
		return target ->
		{
			try
			{
				if (!Files.isReadable(target))
				{
					logger.info("Copying {} to {}", source, target);
					Files.copy(source, target);
				}
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		};
	}
}
