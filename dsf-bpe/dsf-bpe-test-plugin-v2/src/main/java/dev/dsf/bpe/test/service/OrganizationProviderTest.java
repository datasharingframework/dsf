package dev.dsf.bpe.test.service;

import static dev.dsf.bpe.test.PluginTestExecutor.expectNotNull;
import static dev.dsf.bpe.test.PluginTestExecutor.expectSame;
import static dev.dsf.bpe.test.PluginTestExecutor.expectTrue;

import java.util.List;
import java.util.Optional;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;

import dev.dsf.bpe.test.PluginTest;
import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.constants.NamingSystems.OrganizationIdentifier;

public class OrganizationProviderTest extends AbstractTest
{
	private static final String LOCAL_ORGANIZATION_IDENTIFIER_VALUE = "Test_Organization";
	private static final Identifier LOCAL_ORGANIZATION_IDENTIFIER = OrganizationIdentifier
			.withValue(LOCAL_ORGANIZATION_IDENTIFIER_VALUE);
	private static final String PARENT_ORGANIZATION_IDENTIFIER_VALUE = "Parent_Organization";
	private static final Identifier PARENT_ORGANIZATION_IDENTIFIER = OrganizationIdentifier
			.withValue(PARENT_ORGANIZATION_IDENTIFIER_VALUE);
	private static final String EXTERNAL_ORGANIZATION_IDENTIFIER_VALUE = "External_Test_Organization";

	public OrganizationProviderTest(ProcessPluginApi api)
	{
		super(api);
	}

	private void testOrganization(Optional<Organization> oO, String identifierValue)
	{
		expectNotNull(oO);
		expectTrue(oO.isPresent());
		expectNotNull(oO.get());

		Optional<Identifier> identifier = OrganizationIdentifier.findFirst(oO);
		expectNotNull(identifier);
		expectTrue(identifier.isPresent());
		expectSame(identifierValue, identifier.get().getValue());
	}

	private void testOrganization(Organization o, String identifierValue)
	{
		expectNotNull(o);

		Optional<Identifier> identifier = OrganizationIdentifier.findFirst(o);
		expectNotNull(identifier);
		expectTrue(identifier.isPresent());
		expectSame(identifierValue, identifier.get().getValue());
	}

	@PluginTest
	public void getLocalOrganization() throws Exception
	{
		Optional<Organization> oO = api.getOrganizationProvider().getLocalOrganization();
		testOrganization(oO, LOCAL_ORGANIZATION_IDENTIFIER_VALUE);
	}

	@PluginTest
	public void getLocalOrganizationIdentifier() throws Exception
	{
		expectNotNull(api.getOrganizationProvider().getLocalOrganizationIdentifier());
		expectTrue(api.getOrganizationProvider().getLocalOrganizationIdentifier().isPresent());
		expectNotNull(api.getOrganizationProvider().getLocalOrganizationIdentifier().get());
		expectSame(LOCAL_ORGANIZATION_IDENTIFIER_VALUE,
				api.getOrganizationProvider().getLocalOrganizationIdentifier().get().getValue());
	}

	@PluginTest
	public void getLocalOrganizationIdentifierValue() throws Exception
	{
		expectNotNull(api.getOrganizationProvider().getLocalOrganizationIdentifierValue());
		expectTrue(api.getOrganizationProvider().getLocalOrganizationIdentifierValue().isPresent());
		expectSame(LOCAL_ORGANIZATION_IDENTIFIER_VALUE,
				api.getOrganizationProvider().getLocalOrganizationIdentifierValue().get());
	}

	@PluginTest
	public void getOrganizationByIdentifier() throws Exception
	{
		Optional<Organization> oO = api.getOrganizationProvider().getOrganization(LOCAL_ORGANIZATION_IDENTIFIER);
		testOrganization(oO, LOCAL_ORGANIZATION_IDENTIFIER_VALUE);
	}

	@PluginTest
	public void getOrganizationByIdentifierValue() throws Exception
	{
		Optional<Organization> oO = api.getOrganizationProvider().getOrganization(LOCAL_ORGANIZATION_IDENTIFIER_VALUE);
		testOrganization(oO, LOCAL_ORGANIZATION_IDENTIFIER_VALUE);
	}

	@PluginTest
	public void getOrganizationsByParentIdentifier() throws Exception
	{
		List<Organization> os = api.getOrganizationProvider().getOrganizations(PARENT_ORGANIZATION_IDENTIFIER);
		expectNotNull(os);
		expectSame(2, os.size());

		List<String> memberIdentifiers = os.stream().filter(o -> o.getIdentifier().size() == 1)
				.map(OrganizationIdentifier::findFirst).flatMap(Optional::stream).map(Identifier::getValue).toList();
		expectSame(2, memberIdentifiers.size());

		int localOrgIndex = memberIdentifiers.indexOf(LOCAL_ORGANIZATION_IDENTIFIER_VALUE);
		expectTrue(localOrgIndex >= 0);
		testOrganization(os.get(localOrgIndex), LOCAL_ORGANIZATION_IDENTIFIER_VALUE);

		int externalOrgIndex = memberIdentifiers.indexOf(EXTERNAL_ORGANIZATION_IDENTIFIER_VALUE);
		expectTrue(externalOrgIndex >= 0);
		testOrganization(os.get(externalOrgIndex), EXTERNAL_ORGANIZATION_IDENTIFIER_VALUE);
	}

	@PluginTest
	public void getOrganizationsByParentIdentifierValue() throws Exception
	{
		List<Organization> os = api.getOrganizationProvider().getOrganizations(PARENT_ORGANIZATION_IDENTIFIER_VALUE);
		expectNotNull(os);
		expectSame(2, os.size());

		List<String> memberIdentifiers = os.stream().filter(o -> o.getIdentifier().size() == 1)
				.map(OrganizationIdentifier::findFirst).flatMap(Optional::stream).map(Identifier::getValue).toList();
		expectSame(2, memberIdentifiers.size());

		int localOrgIndex = memberIdentifiers.indexOf(LOCAL_ORGANIZATION_IDENTIFIER_VALUE);
		expectTrue(localOrgIndex >= 0);
		testOrganization(os.get(localOrgIndex), LOCAL_ORGANIZATION_IDENTIFIER_VALUE);

		int externalOrgIndex = memberIdentifiers.indexOf(EXTERNAL_ORGANIZATION_IDENTIFIER_VALUE);
		expectTrue(externalOrgIndex >= 0);
		testOrganization(os.get(externalOrgIndex), EXTERNAL_ORGANIZATION_IDENTIFIER_VALUE);
	}

	@PluginTest
	public void getOrganizationsByParentIdentifierAndRole() throws Exception
	{
		List<Organization> lO = api.getOrganizationProvider().getOrganizations(PARENT_ORGANIZATION_IDENTIFIER,
				new Coding("http://dsf.dev/fhir/CodeSystem/organization-role", "DIC", null));
		expectNotNull(lO);
		expectSame(1, lO.size());
		testOrganization(lO.get(0), LOCAL_ORGANIZATION_IDENTIFIER_VALUE);

		List<Organization> eO = api.getOrganizationProvider().getOrganizations(PARENT_ORGANIZATION_IDENTIFIER,
				new Coding("http://dsf.dev/fhir/CodeSystem/organization-role", "TTP", null));
		expectNotNull(eO);
		expectSame(1, eO.size());
		testOrganization(eO.get(0), EXTERNAL_ORGANIZATION_IDENTIFIER_VALUE);
	}

	@PluginTest
	public void getOrganizationsByParentIdentifierValueAndRole() throws Exception
	{
		List<Organization> lO = api.getOrganizationProvider().getOrganizations(PARENT_ORGANIZATION_IDENTIFIER_VALUE,
				"DIC");
		expectNotNull(lO);
		expectSame(1, lO.size());
		testOrganization(lO.get(0), LOCAL_ORGANIZATION_IDENTIFIER_VALUE);

		List<Organization> eO = api.getOrganizationProvider().getOrganizations(PARENT_ORGANIZATION_IDENTIFIER_VALUE,
				"TTP");
		expectNotNull(eO);
		expectSame(1, eO.size());
		testOrganization(eO.get(0), EXTERNAL_ORGANIZATION_IDENTIFIER_VALUE);
	}

	@PluginTest
	public void getRemoteOrganizations() throws Exception
	{
		List<Organization> os = api.getOrganizationProvider().getRemoteOrganizations();
		expectSame(1, os.size());
		testOrganization(os.get(0), EXTERNAL_ORGANIZATION_IDENTIFIER_VALUE);
	}

	@PluginTest
	public void getParentOrganizations() throws Exception
	{
		List<Organization> os = api.getOrganizationProvider().getParentOrganizations();
		expectSame(1, os.size());
		testOrganization(os.get(0), PARENT_ORGANIZATION_IDENTIFIER_VALUE);
	}
}
