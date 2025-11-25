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

	private static final String LOG_MESSAGE_SERVER_ROOT_CAS = "server root CAs";
	private static final String LOG_MESSAGE_CLIENT_CA_CHAINS = "client CA chains";
	private static final String LOG_MESSAGE_CLIENT_ISSUING_CAS = "client issuing CAs";

	private static final class X509CertificateHolder
	{
		final X509Certificate certificate;
		final Predicate<String> isClientOnly;
		final Predicate<String> isServerOnly;
		final JcaX509CertificateHolder certificateHolder;

		final List<X509CertificateHolder> children = new ArrayList<>();
		X509CertificateHolder parent;

		X509CertificateHolder(X509Certificate certificate, Predicate<String> isClientOnly,
				Predicate<String> isServerOnly)
		{
			this.certificate = certificate;
			this.isClientOnly = isClientOnly;
			this.isServerOnly = isServerOnly;

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

		boolean isServerOnly()
		{
			return getChildren().isEmpty() ? isServerOnly.test(getSubjectCommonName())
					: getChildren().stream().allMatch(X509CertificateHolder::isServerOnly);
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
	private final List<String> serverOnlyCaCommonNames;

	public DefaultCaFilesGenerator(Path projectBasedir, Path certFolder, List<String> clientOnlyCaCommonNames,
			List<String> serverOnlyCaCommonNames) throws MojoExecutionException
	{
		this.projectBasedir = projectBasedir;
		this.certFolder = certFolder;
		this.clientOnlyCaCommonNames = clientOnlyCaCommonNames;
		this.serverOnlyCaCommonNames = serverOnlyCaCommonNames;

		if (projectBasedir == null)
			throw new MojoExecutionException("projectBasedir not defined");
		if (certFolder == null)
			throw new MojoExecutionException("certFolder not defined");
		if (!Files.isReadable(certFolder))
			throw new MojoExecutionException("certFolder '" + certFolder.toString() + "' not readable");
		if (clientOnlyCaCommonNames == null)
			throw new MojoExecutionException("clientOnlyCaCommonNames not defined");
		if (serverOnlyCaCommonNames == null)
			throw new MojoExecutionException("serverOnlyCaCommonNames not defined");
	}

	public void createFiles(Stream<Path> clientIssuingCas, Stream<Path> clientCaChains, Stream<Path> serverRootCas)
			throws IOException, MojoExecutionException
	{
		if (clientIssuingCas == null)
			throw new MojoExecutionException("clientIssuingCas not defined");
		if (clientCaChains == null)
			throw new MojoExecutionException("clientCaChains not defined");
		if (serverRootCas == null)
			throw new MojoExecutionException("serverRootCas not defined");

		List<Path> iToWrite = clientIssuingCas.filter(isDirectory(LOG_MESSAGE_CLIENT_ISSUING_CAS)).toList();
		List<Path> cToWrite = clientCaChains.filter(isDirectory(LOG_MESSAGE_CLIENT_CA_CHAINS)).toList();
		List<Path> sWrite = serverRootCas.filter(isDirectory(LOG_MESSAGE_SERVER_ROOT_CAS)).toList();

		if (iToWrite.isEmpty() && cToWrite.isEmpty() && sWrite.isEmpty())
			return;

		List<X509CertificateHolder> certificates = readCertificates();

		try
		{
			iToWrite.forEach(writeClientIssuingCas(certificates, LOG_MESSAGE_CLIENT_ISSUING_CAS));

			cToWrite.forEach(writeClientCaChains(certificates, LOG_MESSAGE_CLIENT_CA_CHAINS));

			sWrite.forEach(writeServerRootCas(certificates, LOG_MESSAGE_SERVER_ROOT_CAS));
		}
		catch (RuntimeIOException e)
		{
			throw e.getCause();
		}
	}

	private Predicate<Path> isDirectory(String logMessage)
	{
		return folder ->
		{
			boolean isDirectory = Files.isDirectory(folder);

			if (!isDirectory)
				logger.info("Default {} folder at {} does not exist", logMessage, projectBasedir.relativize(folder));

			return isDirectory;
		};
	}

	private List<X509CertificateHolder> readCertificates() throws IOException
	{
		List<X509CertificateHolder> certificates = new ArrayList<>();
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(certFolder,
				entry -> entry.getFileName().toString().endsWith(".pem")))
		{
			directoryStream.forEach(readCertificate(certificates));
		}

		Map<X500Name, X509CertificateHolder> certificatesBySubject = certificates.stream()
				.collect(Collectors.toMap(X509CertificateHolder::getSubject, Function.identity()));
		certificates.forEach(c -> c.setParent(certificatesBySubject));

		return certificates;
	}

	private Consumer<? super Path> readCertificate(List<X509CertificateHolder> certificates)
	{
		return file ->
		{
			try
			{
				logger.info("Reading certificate from {}", projectBasedir.relativize(file));
				X509CertificateHolder certificate = new X509CertificateHolder(PemReader.readCertificate(file),
						clientOnlyCaCommonNames::contains, serverOnlyCaCommonNames::contains);

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
		List<X509CertificateHolder> issuingCas = certificates.stream().filter(X509CertificateHolder::isIssuingCa)
				.filter(Predicate.not(X509CertificateHolder::isServerOnly))
				.sorted(Comparator.comparing(X509CertificateHolder::getSubjectCommonName)).toList();

		return folder -> issuingCas.forEach(writeCert(folder, logMessage));
	}

	private Consumer<Path> writeClientCaChains(List<X509CertificateHolder> certificates, String logMessage)
	{
		List<X509CertificateHolder> caChains = certificates.stream().filter(X509CertificateHolder::isRoot)
				.filter(Predicate.not(X509CertificateHolder::isServerOnly))
				.sorted(Comparator.comparing(X509CertificateHolder::getSubjectCommonName)).flatMap(childern()).toList();

		return folder -> caChains.forEach(writeCert(folder, logMessage));
	}

	private Function<X509CertificateHolder, Stream<X509CertificateHolder>> childern()
	{
		return cert -> Stream.concat(Stream.of(cert), cert.getChildren().stream()
				.sorted(Comparator.comparing(X509CertificateHolder::getSubjectCommonName)).flatMap(childern()));
	}

	private Consumer<Path> writeServerRootCas(List<X509CertificateHolder> certificates, String logMessage)
	{
		List<X509CertificateHolder> rootCas = certificates.stream().filter(X509CertificateHolder::isRoot)
				.filter(Predicate.not(X509CertificateHolder::isClientOnly)).toList();

		return folder -> rootCas.forEach(writeCert(folder, logMessage));
	}

	private Consumer<X509CertificateHolder> writeCert(Path folder, String logMessage)
	{
		return holder ->
		{
			try
			{
				Path target = folder.resolve(holder.getSubjectCommonName().replaceAll("[/\\- ]+", "_") + ".crt");

				if (Files.isReadable(target))
					logger.info("Not writing default {} file to {}, file exists", logMessage,
							projectBasedir.relativize(target));
				else
				{
					logger.info("Writing default {} file to {}", logMessage, projectBasedir.relativize(target));
					PemWriter.writeCertificate(holder.getCertificate(), true, target);
				}
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		};
	}
}
