package dev.dsf.fhir.authorization.process;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.stream.Stream;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.OrganizationAffiliation;
import org.junit.Test;

import dev.dsf.common.auth.conf.Identity;

public class RoleTest
{
	private static final String PARENT_ORGANIZATION_IDENTIFIER = "parent.org";
	private static final String MEMBER_IDENTIFIER = "member.com";
	private static final String MEMBER_ROLE_SYSTEM = "roleSystem";
	private static final String MEMBER_ROLE_CODE = "roleCode";

	private static final Role local = new Role(true, PARENT_ORGANIZATION_IDENTIFIER, MEMBER_ROLE_SYSTEM,
			MEMBER_ROLE_CODE, null, null);
	private static final Role remote = new Role(false, PARENT_ORGANIZATION_IDENTIFIER, MEMBER_ROLE_SYSTEM,
			MEMBER_ROLE_CODE, null, null);

	private static final Role localPractitioner = new Role(true, PARENT_ORGANIZATION_IDENTIFIER, MEMBER_ROLE_SYSTEM,
			MEMBER_ROLE_CODE, "http://dsf.dev/fhir/CodeSystem/practitioner-role", "DIC_USER");

	private static org.hl7.fhir.r4.model.Organization createFhirOrganization(String identifierValue)
	{
		return createFhirOrganization(identifierValue, ProcessAuthorizationHelper.ORGANIZATION_IDENTIFIER_SYSTEM);
	}

	private static org.hl7.fhir.r4.model.Organization createFhirOrganization(String identifierValue,
			String identifierSystem)
	{
		var o = new org.hl7.fhir.r4.model.Organization();
		o.setActive(true);
		o.getIdentifierFirstRep().setSystem(identifierSystem).setValue(identifierValue);
		return o;
	}

	private static final Identity LOCAL_ORG_ACTIVE = TestOrganizationIdentity
			.local(createFhirOrganization(MEMBER_IDENTIFIER));
	private static final Identity LOCAL_ORG_NOT_ACTIVE = TestOrganizationIdentity
			.local(createFhirOrganization(MEMBER_IDENTIFIER).setActive(false));
	private static final Identity LOCAL_NO_ORG = TestOrganizationIdentity.local(null);
	private static final Identity LOCAL_ORG_BAD_IDENTIFIER = TestOrganizationIdentity
			.local(createFhirOrganization("wrong.identifier"));
	private static final Identity LOCAL_ORG_BAD_IDENTIFIER_SYSTEM = TestOrganizationIdentity
			.local(createFhirOrganization(MEMBER_IDENTIFIER, "bad.system"));
	private static final Identity REMOTE_ORG_ACTIVE = TestOrganizationIdentity
			.remote(createFhirOrganization(MEMBER_IDENTIFIER));
	private static final Identity REMOTE_ORG_NOT_ACTIVE = TestOrganizationIdentity
			.remote(createFhirOrganization(MEMBER_IDENTIFIER).setActive(false));
	private static final Identity REMOTE_NO_ORG = TestOrganizationIdentity.remote((Organization) null);
	private static final Identity REMOTE_ORG_BAD_IDENTIFIER = TestOrganizationIdentity
			.remote(createFhirOrganization("wrong.identifier"));
	private static final Identity REMOTE_ORG_BAD_IDENTIFIER_SYSTEM = TestOrganizationIdentity
			.remote(createFhirOrganization(MEMBER_IDENTIFIER, "bad.system"));

	private static final Identity LOCAL_PRACTITIONER_ORG_ACTIVE = TestPractitionerIdentity.practitioner(
			createFhirOrganization(MEMBER_IDENTIFIER).setActive(true),
			new Coding("http://dsf.dev/fhir/CodeSystem/practitioner-role", "DIC_USER", null));
	private static final Identity LOCAL_PRACTITIONER_ORG_ACTIVE_DSF_ADMIN = TestPractitionerIdentity.practitioner(
			createFhirOrganization(MEMBER_IDENTIFIER).setActive(true),
			new Coding("http://dsf.dev/fhir/CodeSystem/practitioner-role", "DSF_ADMIN", null));
	private static final Identity LOCAL_PRACTITIONER_ORG_ACTIVE_BAD_ROLE1 = TestPractitionerIdentity.practitioner(
			createFhirOrganization(MEMBER_IDENTIFIER).setActive(true),
			new Coding("http://dsf.dev/fhir/CodeSystem/practitioner-role", "UAC_USER", null));
	private static final Identity LOCAL_PRACTITIONER_ORG_ACTIVE_BAD_ROLE2 = TestPractitionerIdentity.practitioner(
			createFhirOrganization(MEMBER_IDENTIFIER).setActive(true),
			new Coding("http://dsf.dev/fhir/CodeSystem/bad-system", "DIC_USER", null));
	private static final Identity LOCAL_PRACTITIONER_ORG_NOT_ACTIVE = TestPractitionerIdentity.practitioner(
			createFhirOrganization(MEMBER_IDENTIFIER).setActive(false),
			new Coding("http://dsf.dev/fhir/CodeSystem/practitioner-role", "DIC_USER", null));
	private static final Identity LOCAL_PRACTITIONER_ORG_ACTIVE_NO_ROLES = TestPractitionerIdentity
			.practitioner(createFhirOrganization(MEMBER_IDENTIFIER).setActive(true));

	private static OrganizationAffiliation createOrganizationAffiliation(String parentOrganizationIdentifier,
			String memberIdentifier, String memberRoleSystem, String memberRoleCode)
	{
		var a = new OrganizationAffiliation();
		a.setActive(true);
		a.getOrganization().setType("Organization").getIdentifier()
				.setSystem(ProcessAuthorizationHelper.ORGANIZATION_IDENTIFIER_SYSTEM)
				.setValue(parentOrganizationIdentifier);
		a.getParticipatingOrganization().setType("Organization").getIdentifier()
				.setSystem(ProcessAuthorizationHelper.ORGANIZATION_IDENTIFIER_SYSTEM).setValue(memberIdentifier);
		a.getCodeFirstRep().getCodingFirstRep().setSystem(memberRoleSystem).setCode(memberRoleCode);

		return a;
	}

	private static final OrganizationAffiliation OK_AFFILIATION = createOrganizationAffiliation(
			PARENT_ORGANIZATION_IDENTIFIER, MEMBER_IDENTIFIER, MEMBER_ROLE_SYSTEM, MEMBER_ROLE_CODE);

	private static Stream<OrganizationAffiliation> okAffiliation()
	{
		return Stream.of(OK_AFFILIATION);
	}

	@Test
	public void testLocalRoleRecipientOk() throws Exception
	{
		assertTrue(local.isRecipientAuthorized(LOCAL_ORG_ACTIVE, okAffiliation()));
	}

	@Test
	public void testLocalRoleRecipientNotOkOrganizationNotActive() throws Exception
	{
		assertFalse(local.isRecipientAuthorized(LOCAL_ORG_NOT_ACTIVE, okAffiliation()));
	}

	@Test
	public void testLocalRoleRecipientNotOkNoOrganization() throws Exception
	{
		assertFalse(local.isRecipientAuthorized(LOCAL_NO_ORG, okAffiliation()));
	}

	@Test
	public void testLocalRoleRecipientNotOkNoUser() throws Exception
	{
		assertFalse(local.isRecipientAuthorized(null, okAffiliation()));
	}

	@Test
	public void testLocalRoleRecipientNotOkRemoteOrganization() throws Exception
	{
		assertFalse(local.isRecipientAuthorized(REMOTE_ORG_ACTIVE, okAffiliation()));
	}

	@Test
	public void testLocalRoleRecipientNotOkNoAffiliations() throws Exception
	{
		assertFalse(local.isRecipientAuthorized(LOCAL_ORG_ACTIVE, Stream.empty()));
	}

	@Test
	public void testLocalRoleRecipientNotOkAffiliationsNull() throws Exception
	{
		assertFalse(local.isRecipientAuthorized(LOCAL_ORG_ACTIVE, (Stream<OrganizationAffiliation>) null));
	}

	@Test
	public void testLocalRoleRecipientNotOkBadMemberIdentifier() throws Exception
	{
		assertFalse(local.isRecipientAuthorized(LOCAL_ORG_BAD_IDENTIFIER, okAffiliation()));
	}

	@Test
	public void testLocalRoleRecipientNotOkBadMemberIdentifierSystem() throws Exception
	{
		assertFalse(local.isRecipientAuthorized(LOCAL_ORG_BAD_IDENTIFIER_SYSTEM, okAffiliation()));
	}

	@Test
	public void testLocalRoleRecipientNotOkBadMemberRoleCode() throws Exception
	{
		Stream<OrganizationAffiliation> affiliations = Stream.of(createOrganizationAffiliation(
				PARENT_ORGANIZATION_IDENTIFIER, MEMBER_IDENTIFIER, MEMBER_ROLE_SYSTEM, "bad.roleCode"));

		assertFalse(local.isRecipientAuthorized(LOCAL_ORG_ACTIVE, affiliations));
	}

	@Test
	public void testLocalRoleRecipientNotOkBadMemberRoleSystem() throws Exception
	{
		Stream<OrganizationAffiliation> affiliations = Stream.of(createOrganizationAffiliation(
				PARENT_ORGANIZATION_IDENTIFIER, MEMBER_IDENTIFIER, "bad.roleSystem", MEMBER_ROLE_CODE));

		assertFalse(local.isRecipientAuthorized(LOCAL_ORG_ACTIVE, affiliations));
	}

	// ---

	@Test
	public void testRemoteRoleRecipientOk() throws Exception
	{
		assertTrue(remote.isRecipientAuthorized(REMOTE_ORG_ACTIVE, okAffiliation()));
	}

	@Test
	public void testRemoteRoleRecipientNotOkOrganizationNotActive() throws Exception
	{
		assertFalse(remote.isRecipientAuthorized(REMOTE_ORG_NOT_ACTIVE, okAffiliation()));
	}

	@Test
	public void testRemoteRoleRecipientNotOkNoOrganization() throws Exception
	{
		assertFalse(remote.isRecipientAuthorized(REMOTE_NO_ORG, okAffiliation()));
	}

	@Test
	public void testRemoteRoleRecipientNotOkNoUser() throws Exception
	{
		assertFalse(remote.isRecipientAuthorized(null, okAffiliation()));
	}

	@Test
	public void testRemoteRoleRecipientNotOkLocalOrganization() throws Exception
	{
		assertFalse(remote.isRecipientAuthorized(LOCAL_ORG_ACTIVE, okAffiliation()));
	}

	@Test
	public void testRemoteRoleRecipientNotOkNoAffiliations() throws Exception
	{
		assertFalse(remote.isRecipientAuthorized(REMOTE_ORG_ACTIVE, Stream.empty()));
	}

	@Test
	public void testRemoteRoleRecipientNotOkAffiliationsNull() throws Exception
	{
		assertFalse(remote.isRecipientAuthorized(REMOTE_ORG_ACTIVE, (Stream<OrganizationAffiliation>) null));
	}

	@Test
	public void testRemoteRoleRecipientNotOkBadMemberIdentifier() throws Exception
	{
		assertFalse(remote.isRecipientAuthorized(REMOTE_ORG_BAD_IDENTIFIER, okAffiliation()));
	}

	@Test
	public void testRemoteRoleRecipientNotOkBadMemberIdentifierSystem() throws Exception
	{
		assertFalse(remote.isRecipientAuthorized(REMOTE_ORG_BAD_IDENTIFIER_SYSTEM, okAffiliation()));
	}

	@Test
	public void testRemoteRoleRecipientNotOkBadMemberRoleCode() throws Exception
	{
		Stream<OrganizationAffiliation> affiliations = Stream.of(createOrganizationAffiliation(
				PARENT_ORGANIZATION_IDENTIFIER, MEMBER_IDENTIFIER, MEMBER_ROLE_SYSTEM, "bad.roleCode"));

		assertFalse(remote.isRecipientAuthorized(REMOTE_ORG_ACTIVE, affiliations));
	}

	@Test
	public void testRemoteRoleRecipientNotOkMemberRoleSystem() throws Exception
	{
		Stream<OrganizationAffiliation> affiliations = Stream.of(createOrganizationAffiliation(
				PARENT_ORGANIZATION_IDENTIFIER, MEMBER_IDENTIFIER, "bad.roleSystem", MEMBER_ROLE_CODE));

		assertFalse(remote.isRecipientAuthorized(REMOTE_ORG_ACTIVE, affiliations));
	}

	@Test
	public void testRemoteRoleRecipientNotOkPractitioner() throws Exception
	{
		assertFalse(remote.isRecipientAuthorized(LOCAL_PRACTITIONER_ORG_ACTIVE, okAffiliation()));
		assertFalse(remote.isRecipientAuthorized(LOCAL_PRACTITIONER_ORG_ACTIVE_DSF_ADMIN, okAffiliation()));
		assertFalse(remote.isRecipientAuthorized(LOCAL_PRACTITIONER_ORG_ACTIVE_BAD_ROLE1, okAffiliation()));
		assertFalse(remote.isRecipientAuthorized(LOCAL_PRACTITIONER_ORG_ACTIVE_BAD_ROLE2, okAffiliation()));
		assertFalse(remote.isRecipientAuthorized(LOCAL_PRACTITIONER_ORG_NOT_ACTIVE, okAffiliation()));
		assertFalse(remote.isRecipientAuthorized(LOCAL_PRACTITIONER_ORG_ACTIVE_NO_ROLES, okAffiliation()));
	}

	// --- --- ---

	@Test
	public void testLocalRoleRequesterOk() throws Exception
	{
		assertTrue(local.isRequesterAuthorized(LOCAL_ORG_ACTIVE, okAffiliation()));
	}

	@Test
	public void testLocalRoleRequesterOkPractitionerAdmin() throws Exception
	{
		assertTrue(local.isRequesterAuthorized(LOCAL_PRACTITIONER_ORG_ACTIVE_DSF_ADMIN, okAffiliation()));
	}

	@Test
	public void testLocalRoleRequesterNotOkOrganizationNotActive() throws Exception
	{
		assertFalse(local.isRequesterAuthorized(LOCAL_ORG_NOT_ACTIVE, okAffiliation()));
	}

	@Test
	public void testLocalRoleRequesterNotOkNoOrganization() throws Exception
	{
		assertFalse(local.isRequesterAuthorized(LOCAL_NO_ORG, okAffiliation()));
	}

	@Test
	public void testLocalRoleRequesterNotOkNoUser() throws Exception
	{
		assertFalse(local.isRequesterAuthorized(null, okAffiliation()));
	}

	@Test
	public void testLocalRoleRequesterNotOkRemoteOrganization() throws Exception
	{
		assertFalse(local.isRequesterAuthorized(REMOTE_ORG_ACTIVE, okAffiliation()));
	}

	@Test
	public void testLocalRoleRequesterNotOkNoAffiliations() throws Exception
	{
		assertFalse(local.isRequesterAuthorized(LOCAL_ORG_ACTIVE, Stream.empty()));
	}

	@Test
	public void testLocalRoleRequesterNotOkAffiliationsNull() throws Exception
	{
		assertFalse(local.isRequesterAuthorized(LOCAL_ORG_ACTIVE, (Stream<OrganizationAffiliation>) null));
	}

	@Test
	public void testLocalRoleRequesterNotOkBadMemberIdentifier() throws Exception
	{
		assertFalse(local.isRequesterAuthorized(LOCAL_ORG_BAD_IDENTIFIER, okAffiliation()));
	}

	@Test
	public void testLocalRoleRequesterNotOkBadMemberIdentifierSystem() throws Exception
	{
		assertFalse(local.isRequesterAuthorized(LOCAL_ORG_BAD_IDENTIFIER_SYSTEM, okAffiliation()));
	}

	@Test
	public void testLocalRoleRequesterNotOkBadMemberRoleCode() throws Exception
	{
		Stream<OrganizationAffiliation> affiliations = Stream.of(createOrganizationAffiliation(
				PARENT_ORGANIZATION_IDENTIFIER, MEMBER_IDENTIFIER, MEMBER_ROLE_SYSTEM, "bad.roleCode"));

		assertFalse(local.isRequesterAuthorized(LOCAL_ORG_ACTIVE, affiliations));
	}

	@Test
	public void testLocalRoleRequesterNotOkMemberRoleSystem() throws Exception
	{
		Stream<OrganizationAffiliation> affiliations = Stream.of(createOrganizationAffiliation(
				PARENT_ORGANIZATION_IDENTIFIER, MEMBER_IDENTIFIER, "bad.roleSystem", MEMBER_ROLE_CODE));

		assertFalse(local.isRequesterAuthorized(LOCAL_ORG_ACTIVE, affiliations));
	}

	// ---

	@Test
	public void testRemoteRoleRequesterOk() throws Exception
	{
		assertTrue(remote.isRequesterAuthorized(REMOTE_ORG_ACTIVE, okAffiliation()));
	}

	@Test
	public void testRemoteRoleRequesterNotOkOrganizationNotActive() throws Exception
	{
		assertFalse(remote.isRequesterAuthorized(REMOTE_ORG_NOT_ACTIVE, okAffiliation()));
	}

	@Test
	public void testRemoteRoleRequesterNotOkNoOrganization() throws Exception
	{
		assertFalse(remote.isRequesterAuthorized(REMOTE_NO_ORG, okAffiliation()));
	}

	@Test
	public void testRemoteRoleRequesterNotOkNoUser() throws Exception
	{
		assertFalse(remote.isRequesterAuthorized(null, okAffiliation()));
	}

	@Test
	public void testRemoteRoleRequesterNotOkLocalOrganization() throws Exception
	{
		assertFalse(remote.isRequesterAuthorized(LOCAL_ORG_ACTIVE, okAffiliation()));
	}

	@Test
	public void testRemoteRoleRequesterNotOkNoAffiliations() throws Exception
	{
		assertFalse(remote.isRequesterAuthorized(REMOTE_ORG_ACTIVE, Stream.empty()));
	}

	@Test
	public void testRemoteRoleRequesterNotOkAffiliationsNull() throws Exception
	{
		assertFalse(remote.isRequesterAuthorized(REMOTE_ORG_ACTIVE, (Stream<OrganizationAffiliation>) null));
	}

	@Test
	public void testRemoteRoleRequesterNotOkBadMemberIdentifier() throws Exception
	{
		assertFalse(remote.isRequesterAuthorized(REMOTE_ORG_BAD_IDENTIFIER, okAffiliation()));
	}

	@Test
	public void testRemoteRoleRequesterNotOkBadMemberIdentifierSystem() throws Exception
	{
		assertFalse(remote.isRequesterAuthorized(REMOTE_ORG_BAD_IDENTIFIER_SYSTEM, okAffiliation()));
	}

	@Test
	public void testRemoteRoleRequesterNotOkBadMemberRoleCode() throws Exception
	{
		Stream<OrganizationAffiliation> affiliations = Stream.of(createOrganizationAffiliation(
				PARENT_ORGANIZATION_IDENTIFIER, MEMBER_IDENTIFIER, MEMBER_ROLE_SYSTEM, "bad.roleCode"));

		assertFalse(remote.isRequesterAuthorized(REMOTE_ORG_ACTIVE, affiliations));
	}

	@Test
	public void testRemoteRoleRequesterNotOkMemberRoleSystem() throws Exception
	{
		Stream<OrganizationAffiliation> affiliations = Stream.of(createOrganizationAffiliation(
				PARENT_ORGANIZATION_IDENTIFIER, MEMBER_IDENTIFIER, "bad.roleSystem", MEMBER_ROLE_CODE));

		assertFalse(remote.isRequesterAuthorized(REMOTE_ORG_ACTIVE, affiliations));
	}

	@Test
	public void testLocalRolePractitionerRequesterOk() throws Exception
	{
		assertTrue(localPractitioner.isRequesterAuthorized(LOCAL_PRACTITIONER_ORG_ACTIVE, okAffiliation()));
	}

	@Test
	public void testLocalRolePractitionerAdminRequesterOk() throws Exception
	{
		assertTrue(localPractitioner.isRequesterAuthorized(LOCAL_PRACTITIONER_ORG_ACTIVE_DSF_ADMIN, okAffiliation()));
	}

	@Test
	public void testLocalRolePractitionerRequesterNotOkOrganizationNotActive() throws Exception
	{
		assertFalse(localPractitioner.isRequesterAuthorized(LOCAL_PRACTITIONER_ORG_NOT_ACTIVE, okAffiliation()));
	}

	@Test
	public void testLocalRolePractitionerRequesterNotOkPractitionerNoRoles() throws Exception
	{
		assertFalse(localPractitioner.isRequesterAuthorized(LOCAL_PRACTITIONER_ORG_ACTIVE_NO_ROLES, okAffiliation()));
	}

	@Test
	public void testLocalRolePractitionerRequesterNotOkPractitionerBadRole1() throws Exception
	{
		assertFalse(localPractitioner.isRequesterAuthorized(LOCAL_PRACTITIONER_ORG_ACTIVE_BAD_ROLE1, okAffiliation()));
	}

	@Test
	public void testLocalRolePractitionerRequesterNotOkPractitionerBadRole2() throws Exception
	{
		assertFalse(localPractitioner.isRequesterAuthorized(LOCAL_PRACTITIONER_ORG_ACTIVE_BAD_ROLE2, okAffiliation()));
	}

	@Test
	public void testLocalRolePractitionerRequesterNotOkNotAPractitioner() throws Exception
	{
		assertFalse(localPractitioner.isRequesterAuthorized(LOCAL_ORG_ACTIVE, okAffiliation()));
	}
}
