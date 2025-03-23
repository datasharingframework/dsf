package dev.dsf.bpe.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.Map;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class FhirClientConfigYamlTest
{
	private static final Logger logger = LoggerFactory.getLogger(FhirClientConfigYamlTest.class);

	private static final YAMLMapper mapper = YAMLMapper.builder().addModule(new JavaTimeModule()).build();

	private static final String TEST_YAML = """
			some-fhir-server-id:
			  base-url: https://bar:443/fhir/baz
			  test-connection-on-startup: yes
			  enable-debug-logging: yes
			  connect-timeout: PT0.5S
			  read-timeout: PT10M
			  trusted-root-certificates-file: /does/not/exist/trust-store.pem
			  cert-auth:
			    p12-file: /does/not/exist/fhir_client_certificate.p12
			    private-key-file: /does/not/exist/fhir_client_private-key.pem
			    certificate-file: /does/not/exist/fhir_client_certificate.pem
			    password: pa55w0rd
			    password-file: /does/not/exist/fhir_client_certificate.p12.password
			  basic-auth:
			    username: user
			    password: pa55w0rd
			    password-file: /does/not/exist/basic_auth.password
			  bearer-auth:
			    token: bearer...token
			    token-file: /does/not/exist/bearer.token
			  oidc-auth:
			    base-url: https://foo/bar
			    discovery-path: /.well-known/openid-configuration
			    test-connection-on-startup: no
			    enable-debug-logging: yes
			    connect-timeout: PT5S
			    read-timeout: PT10M
			    trusted-root-certificates-file: /does/not/exist/trust-store.pem
			    client-id: some_client_id
			    client-secret: s3cr3t
			    client-secret-file: /does/not/exist/oidc_client.secret
			some-other-fhir-server-id:
			  base-url: http://bar/fhir
			  oidc-auth:
			    base-url: https://foo/bar
			    client-id: some_other_client_id
			    client-secret: s3cr3t2
			""";

	@Test
	public void readValue() throws Exception
	{
		Map<String, FhirClientConfigYaml> configs = mapper.readValue(TEST_YAML, FhirClientConfigYaml.MAP_OF_CONFIGS);

		assertNotNull(configs);
		assertEquals(2, configs.size());
		assertTrue(configs.containsKey("some-fhir-server-id"));
		assertTrue(configs.containsKey("some-other-fhir-server-id"));

		assertNotNull(configs.get("some-fhir-server-id"));
		assertEquals(Duration.ofMillis(500), configs.get("some-fhir-server-id").connectTimeout());
		assertEquals(Duration.ofMinutes(10), configs.get("some-fhir-server-id").readTimeout());
		assertNotNull(configs.get("some-fhir-server-id").oidcAuth());
		assertNotNull(configs.get("some-fhir-server-id").oidcAuth().testConnectionOnStartup());
		assertFalse(configs.get("some-fhir-server-id").oidcAuth().testConnectionOnStartup());
		assertNotNull(configs.get("some-fhir-server-id").oidcAuth().enableDebugLogging());
		assertTrue(configs.get("some-fhir-server-id").oidcAuth().enableDebugLogging());

		assertNotNull(configs.get("some-other-fhir-server-id"));
		assertNotNull(configs.get("some-other-fhir-server-id").oidcAuth());
		assertNull(configs.get("some-other-fhir-server-id").oidcAuth().testConnectionOnStartup());
		assertNull(configs.get("some-other-fhir-server-id").oidcAuth().enableDebugLogging());
	}

	@Test
	public void validate() throws Exception
	{
		Map<String, FhirClientConfigYaml> configs = mapper.readValue(TEST_YAML, FhirClientConfigYaml.MAP_OF_CONFIGS);

		configs.get("some-fhir-server-id").validate("some-fhir-server-id").forEach(e -> logger.debug(e.toString()));

		assertEquals(13, configs.get("some-fhir-server-id").validate("some-fhir-server-id").count());
		assertEquals(0, configs.get("some-other-fhir-server-id").validate("some-fhir-server-id").count());
	}
}
