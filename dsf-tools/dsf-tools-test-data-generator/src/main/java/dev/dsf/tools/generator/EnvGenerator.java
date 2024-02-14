package dev.dsf.tools.generator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
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

		EnvEntry(String userThumbprintVariableName, String userThumbprint)
		{
			this.userThumbprintVariableName = userThumbprintVariableName;
			this.userThumbprint = userThumbprint;
		}
	}

	public void generateAndWriteDockerTestFhirEnvFile(Map<String, CertificateFiles> clientCertificateFilesByCommonName)
	{
		String bundleUserThumbprint = filterAndMapToThumbprint(clientCertificateFilesByCommonName, "test-client")
				.findFirst().get();
		String webbroserTestUserThumbprint = filterAndMapToThumbprint(clientCertificateFilesByCommonName,
				"Webbrowser Test User").findFirst().get();

		writeEnvFile(Paths.get("../../dsf-docker-test-setup/bpe/.env"),
				List.of(new EnvEntry(WEBBROSER_TEST_USER_THUMBPRINT, webbroserTestUserThumbprint)));

		writeEnvFile(Paths.get("../../dsf-docker-test-setup/fhir/.env"),
				List.of(new EnvEntry(BUNDLE_USER_THUMBPRINT, bundleUserThumbprint),
						new EnvEntry(WEBBROSER_TEST_USER_THUMBPRINT, webbroserTestUserThumbprint)));
	}

	public void generateAndWriteDockerTest3DicTtpDockerFhirEnvFiles(
			Map<String, CertificateFiles> clientCertificateFilesByCommonName)
	{
		String webbroserTestUserThumbprint = filterAndMapToThumbprint(clientCertificateFilesByCommonName,
				"Webbrowser Test User").findFirst().get();

		String bundleDic1UserThumbprint = filterAndMapToThumbprint(clientCertificateFilesByCommonName, "dic1-client")
				.findFirst().get();

		String bundleDic2UserThumbprint = filterAndMapToThumbprint(clientCertificateFilesByCommonName, "dic2-client")
				.findFirst().get();

		String bundleDic3UserThumbprint = filterAndMapToThumbprint(clientCertificateFilesByCommonName, "dic3-client")
				.findFirst().get();

		String bundleTtpUserThumbprint = filterAndMapToThumbprint(clientCertificateFilesByCommonName, "ttp-client")
				.findFirst().get();

		List<EnvEntry> entries = List.of(new EnvEntry(WEBBROSER_TEST_USER_THUMBPRINT, webbroserTestUserThumbprint),
				new EnvEntry("DIC1_" + BUNDLE_USER_THUMBPRINT, bundleDic1UserThumbprint),
				new EnvEntry("DIC2_" + BUNDLE_USER_THUMBPRINT, bundleDic2UserThumbprint),
				new EnvEntry("DIC3_" + BUNDLE_USER_THUMBPRINT, bundleDic3UserThumbprint),
				new EnvEntry("TTP_" + BUNDLE_USER_THUMBPRINT, bundleTtpUserThumbprint));

		writeEnvFile(Paths.get("../../dsf-docker-test-setup-3dic-ttp/.env"), entries);
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
			logger.error("Error while writing .env file to {}", target.toString(), e);
			throw new RuntimeException(e);
		}
	}
}
