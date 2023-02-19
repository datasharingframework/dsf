package dev.dsf.common.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import dev.dsf.common.auth.RoleConfig.Mapping;

public class RoleConfigTest
{
	private static final Logger logger = LoggerFactory.getLogger(RoleConfigTest.class);

	private static enum TestRole implements Role
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
				    role:
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
				    role:
				      - foo
				      - baz
				- test1:
				    email: someone@test.com
				    role:
				      - foo
				      - bar
				      - invalid
				- test2:
				    claim:
				      - claim_a
				      - claim_b
				    role: foo
				- invalid""";
		logger.debug("document:\n{}", document);

		RoleConfig roles = new RoleConfig(new Yaml().load(document),
				s -> TestRole.isValid(s) ? TestRole.valueOf(s) : null);
		roles.getEntries().forEach(e -> logger.debug(e.toString()));

		assertNotNull(roles.getEntries());
		assertEquals(4, roles.getEntries().size());

		assertMapping("foo", Arrays.asList("aabbccdd..."), Collections.emptyList(), Collections.emptyList(),
				Arrays.asList(TestRole.foo, TestRole.bar, TestRole.baz), roles.getEntries().get(0));

		assertMapping("bar", Arrays.asList("eeffF0011...", "22334455..."),
				Arrays.asList("one@test.com", "two@test.com"), Collections.emptyList(),
				Arrays.asList(TestRole.foo, TestRole.baz), roles.getEntries().get(1));

		assertMapping("test1", Collections.emptyList(), Arrays.asList("someone@test.com"), Collections.emptyList(),
				Arrays.asList(TestRole.foo, TestRole.bar), roles.getEntries().get(2));

		assertMapping("test2", Collections.emptyList(), Collections.emptyList(), Arrays.asList("claim_a", "claim_b"),
				Arrays.asList(TestRole.foo), roles.getEntries().get(3));
	}

	private void assertMapping(String expectedName, List<String> expectedThumbprints, List<String> expectedEmails,
			List<String> expectedClaims, List<Role> expectedRoles, Mapping actual)
	{
		assertNotNull(actual);
		assertEquals(expectedName, actual.getName());
		assertEquals(expectedThumbprints, actual.getThumbprints());
		assertEquals(expectedEmails, actual.getEmails());
		assertEquals(expectedClaims, actual.getClaims());
		assertEquals(expectedRoles, actual.getRoles());
	}
}
