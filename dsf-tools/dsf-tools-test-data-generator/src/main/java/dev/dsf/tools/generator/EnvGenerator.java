package dev.dsf.tools.generator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.tools.generator.CertificateGenerator.CertificateFiles;

public class EnvGenerator
{
	private static final Logger logger = LoggerFactory.getLogger(EnvGenerator.class);

	private static final String BUNDLE_USER_THUMBPRINT = "BUNDLE_USER_THUMBPRINT";
	private static final String WEBBROSER_TEST_USER_THUMBPRINT = "WEBBROSER_TEST_USER_THUMBPRINT";

	private static final class EnvEntry
	{
		final String userThumbprintVariableName;
		final String userThumbprint;
		final String webbrowserTestUserThumbprintVariableName;
		final String webbrowserTestUserThumbprint;

		EnvEntry(String userThumbprintVariableName, String userThumbprint,
				String webbrowserTestUserThumbprintVariableName, String webbrowserTestUserThumbprint)
		{
			this.userThumbprintVariableName = userThumbprintVariableName;
			this.userThumbprint = userThumbprint;
			this.webbrowserTestUserThumbprintVariableName = webbrowserTestUserThumbprintVariableName;
			this.webbrowserTestUserThumbprint = webbrowserTestUserThumbprint;
		}
	}

	public void generateAndWriteDockerTestFhirEnvFile(Map<String, CertificateFiles> clientCertificateFilesByCommonName)
	{
		String bundleUserThumbprint = filterAndMapToThumbprint(clientCertificateFilesByCommonName, "test-client")
				.findFirst().get();
		String webbroserTestUserThumbprint = filterAndMapToThumbprint(clientCertificateFilesByCommonName,
				"Webbrowser Test User").findFirst().get();

		writeEnvFile(Paths.get("../../dsf-docker-test-setup/fhir/.env"),
				Collections.singletonList(new EnvEntry(BUNDLE_USER_THUMBPRINT, bundleUserThumbprint,
						WEBBROSER_TEST_USER_THUMBPRINT, webbroserTestUserThumbprint)));
	}

	public void generateAndWriteDockerTest3MedicTtpFhirEnvFiles(
			Map<String, CertificateFiles> clientCertificateFilesByCommonName)
	{
		String webbroserTestUserThumbprint = filterAndMapToThumbprint(clientCertificateFilesByCommonName,
				"Webbrowser Test User").findFirst().get();

		String bundleMedic1UserThumbprint = filterAndMapToThumbprint(clientCertificateFilesByCommonName,
				"medic1-client").findFirst().get();

		String bundleMedic2UserThumbprint = filterAndMapToThumbprint(clientCertificateFilesByCommonName,
				"medic2-client").findFirst().get();

		String bundleMedic3UserThumbprint = filterAndMapToThumbprint(clientCertificateFilesByCommonName,
				"medic3-client").findFirst().get();

		String bundleTtpUserThumbprint = filterAndMapToThumbprint(clientCertificateFilesByCommonName, "ttp-client")
				.findFirst().get();

		List<EnvEntry> medic1Entries = List.of(
				new EnvEntry("MEDIC1_" + BUNDLE_USER_THUMBPRINT, bundleMedic1UserThumbprint,
						WEBBROSER_TEST_USER_THUMBPRINT, webbroserTestUserThumbprint),
				new EnvEntry("TTP_" + BUNDLE_USER_THUMBPRINT, bundleTtpUserThumbprint, null, null));

		writeEnvFile(Paths.get("../../dsf-docker-test-setup-3medic-ttp/medic1/fhir/.env"), medic1Entries);

		List<EnvEntry> medic2Entries = List.of(
				new EnvEntry("MEDIC2_" + BUNDLE_USER_THUMBPRINT, bundleMedic2UserThumbprint,
						WEBBROSER_TEST_USER_THUMBPRINT, webbroserTestUserThumbprint),
				new EnvEntry("TTP_" + BUNDLE_USER_THUMBPRINT, bundleTtpUserThumbprint, null, null));

		writeEnvFile(Paths.get("../../dsf-docker-test-setup-3medic-ttp/medic2/fhir/.env"), medic2Entries);

		List<EnvEntry> medic3Entries = List.of(
				new EnvEntry("MEDIC3_" + BUNDLE_USER_THUMBPRINT, bundleMedic3UserThumbprint,
						WEBBROSER_TEST_USER_THUMBPRINT, webbroserTestUserThumbprint),
				new EnvEntry("TTP_" + BUNDLE_USER_THUMBPRINT, bundleTtpUserThumbprint, null, null));

		writeEnvFile(Paths.get("../../dsf-docker-test-setup-3medic-ttp/medic3/fhir/.env"), medic3Entries);

		List<EnvEntry> ttpEntries = List.of(
				new EnvEntry("MEDIC1_" + BUNDLE_USER_THUMBPRINT, bundleMedic1UserThumbprint,
						WEBBROSER_TEST_USER_THUMBPRINT, webbroserTestUserThumbprint),
				new EnvEntry("MEDIC2_" + BUNDLE_USER_THUMBPRINT, bundleMedic2UserThumbprint, null, null),
				new EnvEntry("MEDIC3_" + BUNDLE_USER_THUMBPRINT, bundleMedic3UserThumbprint, null, null),
				new EnvEntry("TTP_" + BUNDLE_USER_THUMBPRINT, bundleTtpUserThumbprint, null, null));

		writeEnvFile(Paths.get("../../dsf-docker-test-setup-3medic-ttp/ttp/fhir/.env"), ttpEntries);
	}

	public void generateAndWriteDockerTest3MedicTtpDockerFhirEnvFiles(
			Map<String, CertificateFiles> clientCertificateFilesByCommonName)
	{
		String webbroserTestUserThumbprint = filterAndMapToThumbprint(clientCertificateFilesByCommonName,
				"Webbrowser Test User").findFirst().get();

		String bundleMedic1UserThumbprint = filterAndMapToThumbprint(clientCertificateFilesByCommonName,
				"medic1-client").findFirst().get();

		String bundleMedic2UserThumbprint = filterAndMapToThumbprint(clientCertificateFilesByCommonName,
				"medic2-client").findFirst().get();

		String bundleMedic3UserThumbprint = filterAndMapToThumbprint(clientCertificateFilesByCommonName,
				"medic3-client").findFirst().get();

		String bundleTtpUserThumbprint = filterAndMapToThumbprint(clientCertificateFilesByCommonName, "ttp-client")
				.findFirst().get();

		List<EnvEntry> entries = List.of(
				new EnvEntry("MEDIC1_" + BUNDLE_USER_THUMBPRINT, bundleMedic1UserThumbprint,
						WEBBROSER_TEST_USER_THUMBPRINT, webbroserTestUserThumbprint),
				new EnvEntry("MEDIC2_" + BUNDLE_USER_THUMBPRINT, bundleMedic2UserThumbprint, null, null),
				new EnvEntry("MEDIC3_" + BUNDLE_USER_THUMBPRINT, bundleMedic3UserThumbprint, null, null),
				new EnvEntry("TTP_" + BUNDLE_USER_THUMBPRINT, bundleTtpUserThumbprint, null, null));

		writeEnvFile(Paths.get("../../dsf-docker-test-setup-3medic-ttp-docker/.env"), entries);
	}

	private Stream<String> filterAndMapToThumbprint(Map<String, CertificateFiles> clientCertificateFilesByCommonName,
			String... commonNames)
	{
		return clientCertificateFilesByCommonName.entrySet().stream()
				.filter(entry -> Arrays.asList(commonNames).contains(entry.getKey()))
				.sorted(Comparator.comparing(e -> Arrays.asList(commonNames).indexOf(e.getKey()))).map(Entry::getValue)
				.map(CertificateFiles::getCertificateSha512ThumbprintHex);
	}

	private void writeEnvFile(Path target, List<? extends EnvEntry> entries)
	{
		StringBuilder builder = new StringBuilder();

		for (int i = 0; i < entries.size(); i++)
		{
			EnvEntry entry = entries.get(i);

			if (entry.webbrowserTestUserThumbprintVariableName != null && entry.webbrowserTestUserThumbprint != null)
			{
				builder.append(entry.webbrowserTestUserThumbprintVariableName);
				builder.append('=');
				builder.append(entry.webbrowserTestUserThumbprint);
				builder.append('\n');
			}

			builder.append(entry.userThumbprintVariableName);
			builder.append('=');
			builder.append(entry.userThumbprint);

			if ((i + 1) < entries.size())
				builder.append("\n");
		}

		try
		{
			logger.info("Writing .env file to {}", target.toString());
			Files.writeString(target, builder.toString());
		}
		catch (IOException e)
		{
			logger.error("Error while writing .env file to " + target.toString(), e);
			throw new RuntimeException(e);
		}
	}
}
