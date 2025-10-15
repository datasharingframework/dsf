package dev.dsf.common.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.hl7.fhir.r4.model.Coding;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import dev.dsf.common.auth.conf.DsfRole;
import dev.dsf.common.auth.conf.RoleConfig;
import dev.dsf.common.auth.conf.RoleConfig.Mapping;
import dev.dsf.common.auth.conf.RoleConfig.RoleKeyAndValues;

public class RoleConfigTest
{
	private static final Logger logger = LoggerFactory.getLogger(RoleConfigTest.class);

	private static interface TestRole extends DsfRole
	{
		List<String> resourceTypes();
	}

	private static record TestRoleImpl(String name, List<String> resourceTypes) implements TestRole
	{
		private static final Set<String> VALID_ROLES = Set.of("foo", "bar", "baz");
		private static final Set<String> VALID_RESOURCES = Set.of("Task", "QuestionnaireResponse");

		public static TestRoleImpl create(RoleKeyAndValues keyAndValues)
		{
			if (VALID_ROLES.contains(keyAndValues.key())
					&& keyAndValues.values().stream().allMatch(VALID_RESOURCES::contains))
				return new TestRoleImpl(keyAndValues.key(), keyAndValues.values());
			else
				return null;
		}

		@Override
		public boolean matches(DsfRole role)
		{
			return false;
		}
	}

	@Test
	public void testRead() throws Exception
	{
		String document = """
				- foo:
				    thumbprint: f7f9ef095c5c246d3e8149729221e668b6ffd9a117fe23e2687658f6a203d31a0e769fb20dc2af6361306717116c700c5905a895a7311057af461c5d78a257b5
				    dsf-role:
				      - foo
				      - bar
				      - baz
				    invalid:
				- bar:
				    thumbprint:
				      - 2d259cc15ee2fe57bc11e1322040ee9e045dd3efb83ed1cb0f393c3bdfecaf3f6506e5573fbc213a1025a7c3dfef101fc8d85ab069e5662d666ea970c7e0cbb6
				      - b52a8b63b030181b8b6bc9ca1e47279da4842ef7ab46c08de6c5713a4e8ecc2c1d7f8cd5c17fe4eb0fe43838ee4b020a88634ea47c520dcc7f5f966b66e69190
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
				      - bar: [Task, QuestionnaireResponse]
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

		RoleConfig<TestRoleImpl> roles = new RoleConfig<TestRoleImpl>(new Yaml().load(document), TestRoleImpl::create,
				practitionerRoleFactory);
		roles.getEntries().forEach(e -> logger.debug(e.toString()));

		assertNotNull(roles.getEntries());
		assertEquals(5, roles.getEntries().size());

		assertMapping("foo", List.of(
				"f7f9ef095c5c246d3e8149729221e668b6ffd9a117fe23e2687658f6a203d31a0e769fb20dc2af6361306717116c700c5905a895a7311057af461c5d78a257b5"),
				List.of(), List.of(), List.of(), List.of(new TestRoleImpl("foo", List.of()),
						new TestRoleImpl("bar", List.of()), new TestRoleImpl("baz", List.of())),
				List.of(), roles.getEntries().get(0));

		assertMapping("bar", List.of(
				"2d259cc15ee2fe57bc11e1322040ee9e045dd3efb83ed1cb0f393c3bdfecaf3f6506e5573fbc213a1025a7c3dfef101fc8d85ab069e5662d666ea970c7e0cbb6",
				"b52a8b63b030181b8b6bc9ca1e47279da4842ef7ab46c08de6c5713a4e8ecc2c1d7f8cd5c17fe4eb0fe43838ee4b020a88634ea47c520dcc7f5f966b66e69190"),
				List.of("one@test.com", "two@test.com"), List.of(), List.of(),
				List.of(new TestRoleImpl("foo", List.of()), new TestRoleImpl("baz", List.of())), List.of(),
				roles.getEntries().get(1));

		assertMapping("test1", List.of(), List.of("someone@test.com"), List.of(), List.of(),
				List.of(new TestRoleImpl("foo", List.of()),
						new TestRoleImpl("bar", List.of("Task", "QuestionnaireResponse"))),
				List.of(new Coding().setSystem("http://test.org/fhir/CodeSystem/foo").setCode("bar")),
				roles.getEntries().get(2));

		assertMapping("test2", List.of(), List.of(), List.of("claim_a", "claim_b"), List.of(),
				List.of(new TestRoleImpl("foo", List.of())), List.of(), roles.getEntries().get(3));

		assertMapping("test3", List.of(), List.of(), List.of(), List.of("group1"),
				List.of(new TestRoleImpl("foo", List.of())),
				List.of(new Coding().setSystem("http://test.org/fhir/CodeSystem/foo").setCode("bar"),
						new Coding().setSystem("http://test.org/fhir/CodeSystem/foo").setCode("baz")),
				roles.getEntries().get(4));
	}

	private void assertMapping(String expectedName, List<String> expectedThumbprints, List<String> expectedEmails,
			List<String> expectedTokenRoles, List<String> expectedTokenGroups, List<DsfRole> expectedDsfRoles,
			List<Coding> expectedPractionerRole, Mapping<TestRoleImpl> actual)
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
