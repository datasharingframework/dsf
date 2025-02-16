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
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.constants.NamingSystems.OrganizationIdentifier;

public class OrganizationProviderTest extends AbstractTest
{
	private static final String ORGANIZATION_IDENTIFIER_LOCAL_VALUE = "Test_Organization";
	private static final Identifier ORGANIZATION_IDENTIFIER_LOCAL = OrganizationIdentifier
			.withValue(ORGANIZATION_IDENTIFIER_LOCAL_VALUE);
	private static final String ORGANIZATION_IDENTIFIER_PARENT_VALUE = "Parent_Organization";
	private static final Identifier ORGANIZATION_IDENTIFIER_PARENT = OrganizationIdentifier
			.withValue(ORGANIZATION_IDENTIFIER_PARENT_VALUE);
	private static final String ORGANIZATION_IDENTIFIER_EXTERNAL_VALUE = "External_Test_Organization";

	private static final String ORGANIZATION_IDENTIFIER_NOT_EXISTING_VALUE = "not-existing-identifier-value";
	private static final Identifier ORGANIZATION_IDENTIFIER_NOT_EXISTING = OrganizationIdentifier
			.withValue(ORGANIZATION_IDENTIFIER_NOT_EXISTING_VALUE);
	private static final String MEMBER_ROLE_NOT_EXISTING_CODE = "not-existing-role";
	private static final Coding MEMBER_ROLE_NOT_EXISTING = new Coding(
			"http://dsf.dev/fhir/CodeSystem/organization-role", MEMBER_ROLE_NOT_EXISTING_CODE, null);
	private static final Coding MEMBER_ROLE_TTP = new Coding("http://dsf.dev/fhir/CodeSystem/organization-role", "TTP",
			null);
	private static final Coding MEMBER_ROLE_DIC = new Coding("http://dsf.dev/fhir/CodeSystem/organization-role", "DIC",
			null);

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
		testOrganization(oO, ORGANIZATION_IDENTIFIER_LOCAL_VALUE);
	}

	@PluginTest
	public void getLocalOrganizationIdentifier() throws Exception
	{
		expectNotNull(api.getOrganizationProvider().getLocalOrganizationIdentifier());
		expectTrue(api.getOrganizationProvider().getLocalOrganizationIdentifier().isPresent());
		expectNotNull(api.getOrganizationProvider().getLocalOrganizationIdentifier().get());
		expectSame(ORGANIZATION_IDENTIFIER_LOCAL_VALUE,
				api.getOrganizationProvider().getLocalOrganizationIdentifier().get().getValue());
	}

	@PluginTest
	public void getLocalOrganizationIdentifierValue() throws Exception
	{
		expectNotNull(api.getOrganizationProvider().getLocalOrganizationIdentifierValue());
		expectTrue(api.getOrganizationProvider().getLocalOrganizationIdentifierValue().isPresent());
		expectSame(ORGANIZATION_IDENTIFIER_LOCAL_VALUE,
				api.getOrganizationProvider().getLocalOrganizationIdentifierValue().get());
	}

	@PluginTest
	public void getOrganizationByIdentifierNull() throws Exception
	{
		Optional<Organization> oO = api.getOrganizationProvider().getOrganization((Identifier) null);
		expectNotNull(oO);
		expectTrue(oO.isEmpty());
	}

	@PluginTest
	public void getOrganizationByIdentifierNotExisting() throws Exception
	{
		Optional<Organization> oO = api.getOrganizationProvider().getOrganization(ORGANIZATION_IDENTIFIER_NOT_EXISTING);
		expectNotNull(oO);
		expectTrue(oO.isEmpty());
	}

	@PluginTest
	public void getOrganizationByIdentifier() throws Exception
	{
		Optional<Organization> oO = api.getOrganizationProvider().getOrganization(ORGANIZATION_IDENTIFIER_LOCAL);
		testOrganization(oO, ORGANIZATION_IDENTIFIER_LOCAL_VALUE);
	}

	@PluginTest
	public void getOrganizationByIdentifierValueNull() throws Exception
	{
		Optional<Organization> oO = api.getOrganizationProvider().getOrganization((String) null);
		expectNotNull(oO);
		expectTrue(oO.isEmpty());
	}

	@PluginTest
	public void getOrganizationByIdentifierValueNotExisting() throws Exception
	{
		Optional<Organization> oO = api.getOrganizationProvider()
				.getOrganization(ORGANIZATION_IDENTIFIER_NOT_EXISTING_VALUE);
		expectNotNull(oO);
		expectTrue(oO.isEmpty());
	}

	@PluginTest
	public void getOrganizationByIdentifierValue() throws Exception
	{
		Optional<Organization> oO = api.getOrganizationProvider().getOrganization(ORGANIZATION_IDENTIFIER_LOCAL_VALUE);
		testOrganization(oO, ORGANIZATION_IDENTIFIER_LOCAL_VALUE);
	}

	@PluginTest
	public void getOrganizationsByParentIdentifierNull() throws Exception
	{
		getOrganizationsByParentIdentifierExpectEmpty(null);
	}

	@PluginTest
	public void getOrganizationsByParentIdentifierNotExisting() throws Exception
	{
		getOrganizationsByParentIdentifierExpectEmpty(ORGANIZATION_IDENTIFIER_NOT_EXISTING);
	}

	private void getOrganizationsByParentIdentifierExpectEmpty(Identifier parentIdentifier)
	{
		List<Organization> os = api.getOrganizationProvider().getOrganizations(parentIdentifier);
		expectNotNull(os);
		expectTrue(os.isEmpty());
	}

	@PluginTest
	public void getOrganizationsByParentIdentifier() throws Exception
	{
		List<Organization> os = api.getOrganizationProvider().getOrganizations(ORGANIZATION_IDENTIFIER_PARENT);
		expectNotNull(os);
		expectSame(2, os.size());

		List<String> memberIdentifiers = os.stream().filter(o -> o.getIdentifier().size() == 1)
				.map(OrganizationIdentifier::findFirst).flatMap(Optional::stream).map(Identifier::getValue).toList();
		expectSame(2, memberIdentifiers.size());

		int localOrgIndex = memberIdentifiers.indexOf(ORGANIZATION_IDENTIFIER_LOCAL_VALUE);
		expectTrue(localOrgIndex >= 0);
		testOrganization(os.get(localOrgIndex), ORGANIZATION_IDENTIFIER_LOCAL_VALUE);

		int externalOrgIndex = memberIdentifiers.indexOf(ORGANIZATION_IDENTIFIER_EXTERNAL_VALUE);
		expectTrue(externalOrgIndex >= 0);
		testOrganization(os.get(externalOrgIndex), ORGANIZATION_IDENTIFIER_EXTERNAL_VALUE);
	}

	@PluginTest
	public void getOrganizationsByParentIdentifierValueNull() throws Exception
	{
		getOrganizationsByParentIdentifierValueExpectEmpty(null);
	}

	@PluginTest
	public void getOrganizationsByParentIdentifierValueNotExisting() throws Exception
	{
		getOrganizationsByParentIdentifierValueExpectEmpty(ORGANIZATION_IDENTIFIER_NOT_EXISTING_VALUE);
	}

	private void getOrganizationsByParentIdentifierValueExpectEmpty(String parentIdentifierValue)
	{
		List<Organization> os = api.getOrganizationProvider().getOrganizations(parentIdentifierValue);
		expectNotNull(os);
		expectTrue(os.isEmpty());
	}

	@PluginTest
	public void getOrganizationsByParentIdentifierValue() throws Exception
	{
		List<Organization> os = api.getOrganizationProvider().getOrganizations(ORGANIZATION_IDENTIFIER_PARENT_VALUE);
		expectNotNull(os);
		expectSame(2, os.size());

		List<String> memberIdentifiers = os.stream().filter(o -> o.getIdentifier().size() == 1)
				.map(OrganizationIdentifier::findFirst).flatMap(Optional::stream).map(Identifier::getValue).toList();
		expectSame(2, memberIdentifiers.size());

		int localOrgIndex = memberIdentifiers.indexOf(ORGANIZATION_IDENTIFIER_LOCAL_VALUE);
		expectTrue(localOrgIndex >= 0);
		testOrganization(os.get(localOrgIndex), ORGANIZATION_IDENTIFIER_LOCAL_VALUE);

		int externalOrgIndex = memberIdentifiers.indexOf(ORGANIZATION_IDENTIFIER_EXTERNAL_VALUE);
		expectTrue(externalOrgIndex >= 0);
		testOrganization(os.get(externalOrgIndex), ORGANIZATION_IDENTIFIER_EXTERNAL_VALUE);
	}

	@PluginTest
	public void getOrganizationsByParentIdentifierAndMemberRoleNull1() throws Exception
	{
		getOrganizationsByParentIdentifierAndMemberRoleExpectEmpty(null, MEMBER_ROLE_DIC);
	}

	@PluginTest
	public void getOrganizationsByParentIdentifierAndMemberRoleNull2() throws Exception
	{
		getOrganizationsByParentIdentifierAndMemberRoleExpectEmpty(ORGANIZATION_IDENTIFIER_PARENT, null);
	}

	@PluginTest
	public void getOrganizationsByParentIdentifierAndMemberRoleNull3() throws Exception
	{
		getOrganizationsByParentIdentifierAndMemberRoleExpectEmpty(null, null);
	}

	@PluginTest
	public void getOrganizationsByParentIdentifierAndMemberRoleNotExisting1() throws Exception
	{
		getOrganizationsByParentIdentifierAndMemberRoleExpectEmpty(ORGANIZATION_IDENTIFIER_NOT_EXISTING,
				MEMBER_ROLE_DIC);
	}

	@PluginTest
	public void getOrganizationsByParentIdentifierAndMemberRoleNotExisting2() throws Exception
	{
		getOrganizationsByParentIdentifierAndMemberRoleExpectEmpty(ORGANIZATION_IDENTIFIER_PARENT,
				MEMBER_ROLE_NOT_EXISTING);
	}

	@PluginTest
	public void getOrganizationsByParentIdentifierAndMemberRoleNotExisting3() throws Exception
	{
		getOrganizationsByParentIdentifierAndMemberRoleExpectEmpty(ORGANIZATION_IDENTIFIER_NOT_EXISTING,
				MEMBER_ROLE_NOT_EXISTING);
	}

	private void getOrganizationsByParentIdentifierAndMemberRoleExpectEmpty(Identifier parentIdentifier,
			Coding memberRole)
	{
		List<Organization> os = api.getOrganizationProvider().getOrganizations(parentIdentifier, memberRole);
		expectNotNull(os);
		expectTrue(os.isEmpty());
	}

	@PluginTest
	public void getOrganizationsByParentIdentifierAndMemberRoleDic() throws Exception
	{
		getOrganizationsByParentIdentifierAndMemberRoleExpectNotEmpty(ORGANIZATION_IDENTIFIER_PARENT, MEMBER_ROLE_DIC,
				ORGANIZATION_IDENTIFIER_LOCAL_VALUE);
	}

	@PluginTest
	public void getOrganizationsByParentIdentifierAndMemberRoleTtp() throws Exception
	{
		getOrganizationsByParentIdentifierAndMemberRoleExpectNotEmpty(ORGANIZATION_IDENTIFIER_PARENT, MEMBER_ROLE_TTP,
				ORGANIZATION_IDENTIFIER_EXTERNAL_VALUE);
	}

	private void getOrganizationsByParentIdentifierAndMemberRoleExpectNotEmpty(Identifier parentIdentifier,
			Coding memberRole, String expectedOrganizationIdentifierValue)
	{
		List<Organization> os = api.getOrganizationProvider().getOrganizations(parentIdentifier, memberRole);
		expectNotNull(os);
		expectSame(1, os.size());
		testOrganization(os.get(0), expectedOrganizationIdentifierValue);
	}

	@PluginTest
	public void getOrganizationsByParentIdentifierValueAndMemberRoleNull1() throws Exception
	{
		getOrganizationsByParentIdentifierValueAndMemberRoleExpectEmpty(null, MEMBER_ROLE_DIC);
	}

	@PluginTest
	public void getOrganizationsByParentIdentifierValueAndMemberRoleNull2() throws Exception
	{
		getOrganizationsByParentIdentifierValueAndMemberRoleExpectEmpty(ORGANIZATION_IDENTIFIER_PARENT_VALUE, null);
	}

	@PluginTest
	public void getOrganizationsByParentIdentifierValueAndMemberRoleNull3() throws Exception
	{
		getOrganizationsByParentIdentifierValueAndMemberRoleExpectEmpty(null, null);
	}

	@PluginTest
	public void getOrganizationsByParentIdentifierValueAndMemberRoleNotExisting1() throws Exception
	{
		getOrganizationsByParentIdentifierValueAndMemberRoleExpectEmpty(ORGANIZATION_IDENTIFIER_NOT_EXISTING_VALUE,
				MEMBER_ROLE_DIC);
	}

	@PluginTest
	public void getOrganizationsByParentIdentifierValueAndMemberRoleNotExisting2() throws Exception
	{
		getOrganizationsByParentIdentifierValueAndMemberRoleExpectEmpty(ORGANIZATION_IDENTIFIER_PARENT_VALUE,
				MEMBER_ROLE_NOT_EXISTING);
	}

	@PluginTest
	public void getOrganizationsByParentIdentifierValueAndMemberRoleNotExisting3() throws Exception
	{
		getOrganizationsByParentIdentifierValueAndMemberRoleExpectEmpty(ORGANIZATION_IDENTIFIER_NOT_EXISTING_VALUE,
				MEMBER_ROLE_NOT_EXISTING);
	}

	private void getOrganizationsByParentIdentifierValueAndMemberRoleExpectEmpty(String parentIdentifierValue,
			Coding memberRole)
	{
		List<Organization> os = api.getOrganizationProvider().getOrganizations(parentIdentifierValue, memberRole);
		expectNotNull(os);
		expectTrue(os.isEmpty());
	}

	@PluginTest
	public void getOrganizationsByParentIdentifierValueAndMemberRoleDic() throws Exception
	{
		getOrganizationsByParentIdentifierValueAndMemberRoleExpectNotEmpty(ORGANIZATION_IDENTIFIER_PARENT_VALUE,
				MEMBER_ROLE_DIC, ORGANIZATION_IDENTIFIER_LOCAL_VALUE);
	}

	@PluginTest
	public void getOrganizationsByParentIdentifierValueAndMemberRoleTtp() throws Exception
	{
		getOrganizationsByParentIdentifierValueAndMemberRoleExpectNotEmpty(ORGANIZATION_IDENTIFIER_PARENT_VALUE,
				MEMBER_ROLE_TTP, ORGANIZATION_IDENTIFIER_EXTERNAL_VALUE);
	}

	private void getOrganizationsByParentIdentifierValueAndMemberRoleExpectNotEmpty(String parentIdentifierValue,
			Coding memberRole, String expectedOrganizationIdentifierValue)
	{
		List<Organization> os = api.getOrganizationProvider().getOrganizations(parentIdentifierValue, memberRole);
		expectNotNull(os);
		expectSame(1, os.size());
		testOrganization(os.get(0), expectedOrganizationIdentifierValue);
	}

	@PluginTest
	public void getRemoteOrganizations() throws Exception
	{
		List<Organization> os = api.getOrganizationProvider().getRemoteOrganizations();
		expectSame(2, os.size());

		List<String> oIdentifiers = os.stream().filter(o -> o.getIdentifier().size() == 1)
				.map(OrganizationIdentifier::findFirst).flatMap(Optional::stream).map(Identifier::getValue).toList();
		expectSame(2, oIdentifiers.size());

		int parentOrgIndex = oIdentifiers.indexOf(ORGANIZATION_IDENTIFIER_PARENT_VALUE);
		expectTrue(parentOrgIndex >= 0);
		testOrganization(os.get(parentOrgIndex), ORGANIZATION_IDENTIFIER_PARENT_VALUE);

		int externalOrgIndex = oIdentifiers.indexOf(ORGANIZATION_IDENTIFIER_EXTERNAL_VALUE);
		expectTrue(externalOrgIndex >= 0);
		testOrganization(os.get(externalOrgIndex), ORGANIZATION_IDENTIFIER_EXTERNAL_VALUE);
	}
}
