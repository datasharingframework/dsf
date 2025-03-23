package dev.dsf.tools.generator;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigGenerator
{
	private static final Logger logger = LoggerFactory.getLogger(ConfigGenerator.class);

	private static final String BPE_CONFIG_TEMPLATE_FILE = "/config-templates/java-test-bpe-config.properties";
	private static final String FHIR_CONFIG_TEMPLATE_FILE = "/config-templates/java-test-fhir-config.properties";

	private static final String P_KEY_BPE_ROLE_CONFIG = "dev.dsf.bpe.server.roleConfig";
	private static final String P_KEY_FHIR_ROLE_CONFIG = "dev.dsf.fhir.server.roleConfig";

	private final CertificateGenerator certificateGenerator;

	public ConfigGenerator(CertificateGenerator certificateGenerator)
	{
		Objects.requireNonNull(certificateGenerator, "certificateGenerator");

		this.certificateGenerator = certificateGenerator;
	}

	private Properties readProperties(String propertiesFile)
	{
		@SuppressWarnings("serial")
		Properties properties = new Properties()
		{
			// making sure entries are sorted when storing properties
			@Override
			public Set<Entry<Object, Object>> entrySet()
			{
				return Collections.synchronizedSet(
						super.entrySet().stream().sorted(Comparator.comparing(e -> e.getKey().toString()))
								.collect(Collectors.toCollection(LinkedHashSet::new)));
			}
		};
		try (InputStream in = CertificateGenerator.class.getResourceAsStream(propertiesFile);
				InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
		{
			properties.load(reader);
		}
		catch (IOException e)
		{
			logger.error("Unable to read properties from {}", propertiesFile.toString(), e);
			throw new RuntimeException(e);
		}
		return properties;
	}

	public Properties getJavaTestBpeConfigProperties()
	{
		Properties properties = readProperties(BPE_CONFIG_TEMPLATE_FILE);
		properties.setProperty(P_KEY_BPE_ROLE_CONFIG, String.format("""
				- webbrowser_test_user:
				    thumbprint: %s
				    token-role: admin
				    dsf-role:
				      - ADMIN
				""", certificateGenerator.getCertificateThumbprintsByCommonNameAsHex()
				.get(CertificateGenerator.SUBJECT_CN_WEBBROWSER_TEST_USER)));

		return properties;
	}

	public Properties getJavaTestFhirConfigProperties()
	{
		Properties properties = readProperties(FHIR_CONFIG_TEMPLATE_FILE);
		properties.setProperty(P_KEY_FHIR_ROLE_CONFIG, String.format("""
				- webbrowser_test_user:
				    thumbprint: %s
				    token-role: admin
				    dsf-role:
				      - CREATE
				      - READ
				      - UPDATE
				      - DELETE
				      - SEARCH
				      - HISTORY
				      - PERMANENT_DELETE
				    practitioner-role:
				      - http://dsf.dev/fhir/CodeSystem/practitioner-role|DSF_ADMIN
				""", certificateGenerator.getCertificateThumbprintsByCommonNameAsHex()
				.get(CertificateGenerator.SUBJECT_CN_WEBBROWSER_TEST_USER)));

		return properties;
	}
}
