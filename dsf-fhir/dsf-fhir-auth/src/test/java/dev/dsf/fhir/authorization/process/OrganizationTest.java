package dev.dsf.fhir.authorization.process;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.stream.Stream;

import org.hl7.fhir.r4.model.Coding;
import org.junit.Test;

import dev.dsf.common.auth.conf.Identity;

public class OrganizationTest
{
	private static final String IDENTIFIER = "organization.com";

	private static org.hl7.fhir.r4.model.Organization createFhirOrganization(String identifier)
	{
		var o = new org.hl7.fhir.r4.model.Organization();
		o.setActive(true);
		o.getIdentifierFirstRep().setSystem(ProcessAuthorizationHelper.ORGANIZATION_IDENTIFIER_SYSTEM)
				.setValue(identifier);
		return o;
	}

	private static final Identity LOCAL_ORG_ACTIVE = TestOrganizationIdentity.local(createFhirOrganization(IDENTIFIER));
	private static final Identity LOCAL_ORG_NOT_ACTIVE = TestOrganizationIdentity
			.local(createFhirOrganization(IDENTIFIER).setActive(false));
	private static final Identity LOCAL_NO_ORG = TestOrganizationIdentity.local(null);
	private static final Identity LOCAL_ORG_BAD_IDENTIFIER_ACTIVE = TestOrganizationIdentity
			.local(createFhirOrganization("bad.identifier"));
	private static final Identity REMOTE_ORG_ACTIVE = TestOrganizationIdentity
			.remote(createFhirOrganization(IDENTIFIER));
	private static final Identity REMOTE_ORG_NOT_ACTIVE = TestOrganizationIdentity
			.remote(createFhirOrganization(IDENTIFIER).setActive(false));
	private static final Identity REMOTE_NO_ORG = TestOrganizationIdentity
			.remote((org.hl7.fhir.r4.model.Organization) null);
	private static final Identity REMOTE_ORG_BAD_IDENTIFIER_ACTIVE = TestOrganizationIdentity
			.remote(createFhirOrganization("bad.identifier"));

	private static final Identity LOCAL_PRACTITIONER_ORG_ACTIVE = TestPractitionerIdentity.practitioner(
			createFhirOrganization(IDENTIFIER).setActive(true),
			new Coding("http://dsf.dev/fhir/CodeSystem/practitioner-role", "DIC_USER", null));
	private static final Identity LOCAL_PRACTITIONER_ORG_ACTIVE_BAD_ROLE1 = TestPractitionerIdentity.practitioner(
			createFhirOrganization(IDENTIFIER).setActive(true),
			new Coding("http://dsf.dev/fhir/CodeSystem/practitioner-role", "UAC_USER", null));
	private static final Identity LOCAL_PRACTITIONER_ORG_ACTIVE_BAD_ROLE2 = TestPractitionerIdentity.practitioner(
			createFhirOrganization(IDENTIFIER).setActive(true),
			new Coding("http://dsf.dev/fhir/CodeSystem/bad-system", "DIC_USER", null));
	private static final Identity LOCAL_PRACTITIONER_ORG_NOT_ACTIVE = TestPractitionerIdentity.practitioner(
			createFhirOrganization(IDENTIFIER).setActive(false),
			new Coding("http://dsf.dev/fhir/CodeSystem/practitioner-role", "DIC_USER", null));
	private static final Identity LOCAL_PRACTITIONER_ORG_ACTIVE_NO_ROLES = TestPractitionerIdentity
			.practitioner(createFhirOrganization(IDENTIFIER).setActive(true));
	private static final Identity LOCAL_PRACTITIONER_ORG_BAD_IDENTIFIER_ACTIVE = TestPractitionerIdentity.practitioner(
			createFhirOrganization("bad.identifier").setActive(true),
			new Coding("http://dsf.dev/fhir/CodeSystem/practitioner-role", "DIC_USER", null));

	private static final Organization local = new Organization(true, IDENTIFIER, null, null);
	private static final Organization remote = new Organization(false, IDENTIFIER, null, null);

	private static final Organization localPractitioner = new Organization(true, IDENTIFIER,
			"http://dsf.dev/fhir/CodeSystem/practitioner-role", "DIC_USER");

	@Test
	public void testLocalOrganizationRecipientOk() throws Exception
	{
		assertTrue(local.isRecipientAuthorized(LOCAL_ORG_ACTIVE, Stream.empty()));
	}

	@Test
	public void testLocalOrganizationRecipientNotOkOrganizationNotActive() throws Exception
	{
		assertFalse(local.isRecipientAuthorized(LOCAL_ORG_NOT_ACTIVE, Stream.empty()));
	}

	@Test
	public void testLocalOrganizationRecipientNotOkNoOrganization() throws Exception
	{
		assertFalse(local.isRecipientAuthorized(LOCAL_NO_ORG, Stream.empty()));
	}

	@Test
	public void testLocalOrganizationRecipientNotOkNoUser() throws Exception
	{
		assertFalse(local.isRecipientAuthorized(null, Stream.empty()));
	}

	@Test
	public void testLocalOrganizationRecipientNotOkRemoteOrganization() throws Exception
	{
		assertFalse(local.isRecipientAuthorized(REMOTE_ORG_ACTIVE, Stream.empty()));
	}

	@Test
	public void testLocalOrganizationRecipientNotOkIdentifierWrong() throws Exception
	{
		assertFalse(local.isRecipientAuthorized(LOCAL_ORG_BAD_IDENTIFIER_ACTIVE, Stream.empty()));
	}

	@Test
	public void testRemoteOrganizationRecipientOk() throws Exception
	{
		assertTrue(remote.isRecipientAuthorized(REMOTE_ORG_ACTIVE, Stream.empty()));
	}

	@Test
	public void testRemoteOrganizationRecipientNotOkOrganizationNotActive() throws Exception
	{
		assertFalse(remote.isRecipientAuthorized(REMOTE_ORG_NOT_ACTIVE, Stream.empty()));
	}

	@Test
	public void testRemoteOrganizationRecipientNotOkNoOrganization() throws Exception
	{
		assertFalse(remote.isRecipientAuthorized(REMOTE_NO_ORG, Stream.empty()));
	}

	@Test
	public void testRemoteOrganizationRecipientNotOkNoUser() throws Exception
	{
		assertFalse(remote.isRecipientAuthorized(null, Stream.empty()));
	}

	@Test
	public void testRemoteOrganizationRecipientNotOkLocalOrganization() throws Exception
	{
		assertFalse(remote.isRecipientAuthorized(LOCAL_ORG_ACTIVE, Stream.empty()));
	}

	@Test
	public void testRemoteOrganizationRecipientNotOkIdentifierWrong() throws Exception
	{
		assertFalse(remote.isRecipientAuthorized(REMOTE_ORG_BAD_IDENTIFIER_ACTIVE, Stream.empty()));
	}

	@Test
	public void testLocalOrganizationRequesterOk() throws Exception
	{
		assertTrue(local.isRequesterAuthorized(LOCAL_ORG_ACTIVE, Stream.empty()));
	}

	@Test
	public void testLocalOrganizationRequesterNotOkOrganizationNotActive() throws Exception
	{
		assertFalse(local.isRequesterAuthorized(LOCAL_ORG_NOT_ACTIVE, Stream.empty()));
	}

	@Test
	public void testLocalOrganizationRequesterNotOkNoOrganization() throws Exception
	{
		assertFalse(local.isRequesterAuthorized(LOCAL_NO_ORG, Stream.empty()));
	}

	@Test
	public void testLocalOrganizationRequesterNotOkNoUser() throws Exception
	{
		assertFalse(local.isRequesterAuthorized(null, Stream.empty()));
	}

	@Test
	public void testLocalOrganizationRequesterNotOkRemoteOrganization() throws Exception
	{
		assertFalse(local.isRequesterAuthorized(REMOTE_ORG_ACTIVE, Stream.empty()));
	}

	@Test
	public void testLocalOrganizationRequesterNotOkIdentifierWrong() throws Exception
	{
		assertFalse(local.isRequesterAuthorized(LOCAL_ORG_BAD_IDENTIFIER_ACTIVE, Stream.empty()));
	}

	@Test
	public void testRemoteOrganizationRequesterOk() throws Exception
	{
		assertTrue(remote.isRequesterAuthorized(REMOTE_ORG_ACTIVE, Stream.empty()));
	}

	@Test
	public void testRemoteOrganizationRequesterNotOkOrganizationNotActive() throws Exception
	{
		assertFalse(remote.isRequesterAuthorized(REMOTE_ORG_NOT_ACTIVE, Stream.empty()));
	}

	@Test
	public void testRemoteOrganizationRequesterNotOkNoOrganization() throws Exception
	{
		assertFalse(remote.isRequesterAuthorized(REMOTE_NO_ORG, Stream.empty()));
	}

	@Test
	public void testRemoteOrganizationRequesterNotOkNoUser() throws Exception
	{
		assertFalse(remote.isRequesterAuthorized(null, Stream.empty()));
	}

	@Test
	public void testRemoteOrganizationRequesterNotOkLocalOrganization() throws Exception
	{
		assertFalse(remote.isRequesterAuthorized(LOCAL_ORG_ACTIVE, Stream.empty()));
	}

	@Test
	public void testRemoteOrganizationRequesterNotOkIdentifierWrong() throws Exception
	{
		assertFalse(remote.isRequesterAuthorized(REMOTE_ORG_BAD_IDENTIFIER_ACTIVE, Stream.empty()));
	}

	@Test
	public void testLocalOrganizationPractitionerRequesterOk() throws Exception
	{
		assertTrue(localPractitioner.isRequesterAuthorized(LOCAL_PRACTITIONER_ORG_ACTIVE, Stream.empty()));
	}

	@Test
	public void testLocalOrganizationPractitionerRequesterNotOkOrganizationNotActive() throws Exception
	{
		assertFalse(localPractitioner.isRequesterAuthorized(LOCAL_PRACTITIONER_ORG_NOT_ACTIVE, Stream.empty()));
	}

	@Test
	public void testLocalOrganizationPractitionerRequesterNotOkPractitionerNoRoles() throws Exception
	{
		assertFalse(localPractitioner.isRequesterAuthorized(LOCAL_PRACTITIONER_ORG_ACTIVE_NO_ROLES, Stream.empty()));
	}

	@Test
	public void testLocalOrganizationPractitionerRequesterNotOkPractitionerBadRole1() throws Exception
	{
		assertFalse(localPractitioner.isRequesterAuthorized(LOCAL_PRACTITIONER_ORG_ACTIVE_BAD_ROLE1, Stream.empty()));
	}

	@Test
	public void testLocalOrganizationPractitionerRequesterNotOkPractitionerBadRole2() throws Exception
	{
		assertFalse(localPractitioner.isRequesterAuthorized(LOCAL_PRACTITIONER_ORG_ACTIVE_BAD_ROLE2, Stream.empty()));
	}

	@Test
	public void testLocalOrganizationPractitionerRequesterNotOkNotAPractitioner() throws Exception
	{
		assertFalse(localPractitioner.isRequesterAuthorized(LOCAL_ORG_ACTIVE, Stream.empty()));
	}

	@Test
	public void testLocalOrganizationPractitionerRequesterNotOkBadOrganizationIdentifier() throws Exception
	{
		assertFalse(
				localPractitioner.isRequesterAuthorized(LOCAL_PRACTITIONER_ORG_BAD_IDENTIFIER_ACTIVE, Stream.empty()));
	}
}
