package dev.dsf.common.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.Coding;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import dev.dsf.common.auth.conf.DsfRole;
import dev.dsf.common.auth.conf.RoleConfig;
import dev.dsf.common.auth.conf.RoleConfig.Mapping;

public class RoleConfigTest
{
	private static final Logger logger = LoggerFactory.getLogger(RoleConfigTest.class);

	private static enum TestRole implements DsfRole
	{
		foo, bar, baz;

		public static boolean isValid(String s)
		{
			return Stream.of(values()).map(Enum::name).anyMatch(n -> n.equals(s));
		}
	}

	@Test
	public void read() throws Exception
	{
		String document = """
				- foo:
				    thumbprint: aabbccdd...
				    dsf-role:
				      - foo
				      - bar
				      - baz
				    invalid:
				- bar:
				    thumbprint:
				      - eeffF0011...
				      - 22334455...
				    email:
				      - one@test.com
				      - two@test.com
				    dsf-role:
				      - foo
				      - baz
				    practitioner-role: invalid
				- test1:
				    email: someone@test.com
				    dsf-role:
				      - foo
				      - bar
				      - invalid
				    practitioner-role: http://test.org/fhir/CodeSystem/foo|bar
				- test2:
				    token-role:
				      - claim_a
				      - claim_b
				    dsf-role: foo
				- test3:
				    token-group:
				      - group1
				    dsf-role: foo
				    practitioner-role:
				      - http://test.org/fhir/CodeSystem/foo|bar
				      - http://test.org/fhir/CodeSystem/foo|baz
				- invalid""";
		logger.debug("document:\n{}", document);

		Function<String, Coding> practitionerRoleFactory = role ->
		{
			if (role != null)
			{
				String[] roleParts = role.split("\\|");
				if (roleParts.length == 2)
					return new Coding().setSystem(roleParts[0]).setCode(roleParts[1]);
			}

			return null;
		};

		RoleConfig roles = new RoleConfig(new Yaml().load(document),
				s -> TestRole.isValid(s) ? TestRole.valueOf(s) : null, practitionerRoleFactory);
		roles.getEntries().forEach(e -> logger.debug(e.toString()));

		assertNotNull(roles.getEntries());
		assertEquals(5, roles.getEntries().size());

		assertMapping("foo", Arrays.asList("aabbccdd..."), Collections.emptyList(), Collections.emptyList(),
				Collections.emptyList(), Arrays.asList(TestRole.foo, TestRole.bar, TestRole.baz),
				Collections.emptyList(), roles.getEntries().get(0));

		assertMapping("bar", Arrays.asList("eeffF0011...", "22334455..."),
				Arrays.asList("one@test.com", "two@test.com"), Collections.emptyList(), Collections.emptyList(),
				Arrays.asList(TestRole.foo, TestRole.baz), Collections.emptyList(), roles.getEntries().get(1));

		assertMapping("test1", Collections.emptyList(), Arrays.asList("someone@test.com"), Collections.emptyList(),
				Collections.emptyList(), Arrays.asList(TestRole.foo, TestRole.bar),
				Collections.singletonList(new Coding().setSystem("http://test.org/fhir/CodeSystem/foo").setCode("bar")),
				roles.getEntries().get(2));

		assertMapping("test2", Collections.emptyList(), Collections.emptyList(), Arrays.asList("claim_a", "claim_b"),
				Collections.emptyList(), Arrays.asList(TestRole.foo), Collections.emptyList(),
				roles.getEntries().get(3));

		assertMapping("test3", Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
				Collections.singletonList("group1"), Arrays.asList(TestRole.foo),
				Arrays.asList(new Coding().setSystem("http://test.org/fhir/CodeSystem/foo").setCode("bar"),
						new Coding().setSystem("http://test.org/fhir/CodeSystem/foo").setCode("baz")),
				roles.getEntries().get(4));
	}

	private void assertMapping(String expectedName, List<String> expectedThumbprints, List<String> expectedEmails,
			List<String> expectedTokenRoles, List<String> expectedTokenGroups, List<DsfRole> expectedDsfRoles,
			List<Coding> expectedPractionerRole, Mapping actual)
	{
		assertNotNull(actual);
		assertEquals(expectedName, actual.getName());
		assertEquals(expectedThumbprints, actual.getThumbprints());
		assertEquals(expectedEmails, actual.getEmails());
		assertEquals(expectedTokenRoles, actual.getTokenRoles());
		assertEquals(expectedTokenGroups, actual.getTokenGroups());
		assertEquals(expectedDsfRoles, actual.getDsfRoles());

		assertNotNull(expectedPractionerRole);
		assertNotNull(actual.getPractitionerRoles());
		assertEquals(expectedPractionerRole.size(), actual.getPractitionerRoles().size());

		for (int i = 0; i < expectedPractionerRole.size(); i++)
		{
			Coding expextedCoding = expectedPractionerRole.get(i);
			Coding actualCoding = actual.getPractitionerRoles().get(i);
			assertNotNull(expextedCoding);
			assertNotNull(actualCoding);
			assertTrue(expextedCoding.equalsDeep(actualCoding));
		}
	}
}
