package dev.dsf.tools.generator;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class EnvGenerator
{
	private static final String BUNDLE_USER_THUMBPRINT = "BUNDLE_USER_THUMBPRINT";
	private static final String WEBBROWSER_TEST_USER_THUMBPRINT = "WEBBROWSER_TEST_USER_THUMBPRINT";

	private static record EnvEntry(String userThumbprintVariableName, String userThumbprint)
	{
	}

	private final CertificateGenerator certificateGenerator;

	public EnvGenerator(CertificateGenerator certificateGenerator)
	{
		Objects.requireNonNull(certificateGenerator, "certificateGenerator");

		this.certificateGenerator = certificateGenerator;
	}

	public String generateDockerDevSetupBpeEnvFile()
	{
		Map<String, String> thumbprints = certificateGenerator.getCertificateThumbprintsByCommonNameAsHex();

		return generateEnvFile(List.of(new EnvEntry(WEBBROWSER_TEST_USER_THUMBPRINT,
				thumbprints.get(CertificateGenerator.SUBJECT_CN_WEBBROWSER_TEST_USER))));
	}

	public String generateDockerDevSetupFhirEnvFile()
	{
		Map<String, String> thumbprints = certificateGenerator.getCertificateThumbprintsByCommonNameAsHex();

		return generateEnvFile(
				List.of(new EnvEntry(BUNDLE_USER_THUMBPRINT, thumbprints.get(CertificateGenerator.SUBJECT_CN_BPE)),
						new EnvEntry(WEBBROWSER_TEST_USER_THUMBPRINT,
								thumbprints.get(CertificateGenerator.SUBJECT_CN_WEBBROWSER_TEST_USER))));
	}

	public String generateDockerDevSetup3DicTtpEnvFile()
	{
		Map<String, String> thumbprints = certificateGenerator.getCertificateThumbprintsByCommonNameAsHex();

		return generateEnvFile(List.of(
				new EnvEntry(WEBBROWSER_TEST_USER_THUMBPRINT,
						thumbprints.get(CertificateGenerator.SUBJECT_CN_WEBBROWSER_TEST_USER)),
				new EnvEntry("DIC1_" + BUNDLE_USER_THUMBPRINT, thumbprints.get(CertificateGenerator.SUBJECT_CN_DIC_1)),
				new EnvEntry("DIC2_" + BUNDLE_USER_THUMBPRINT, thumbprints.get(CertificateGenerator.SUBJECT_CN_DIC_2)),
				new EnvEntry("DIC3_" + BUNDLE_USER_THUMBPRINT, thumbprints.get(CertificateGenerator.SUBJECT_CN_DIC_3)),
				new EnvEntry("TTP_" + BUNDLE_USER_THUMBPRINT, thumbprints.get(CertificateGenerator.SUBJECT_CN_TTP))));
	}

	private String generateEnvFile(List<? extends EnvEntry> entries)
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

		return builder.toString();
	}
}
