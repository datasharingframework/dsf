package dev.dsf.fhir.authorization.process;

import static org.junit.Assert.*;

import java.util.stream.Stream;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Organization;
import org.junit.Test;

import dev.dsf.common.auth.conf.Identity;

public class AllTest
{
	private static final Identity REMOTE_NO_ORG = TestOrganizationIdentity.remote(null);
	private static final Identity REMOTE_ORG_NOT_ACTIVE = TestOrganizationIdentity
			.remote(new Organization().setActive(false));
	private static final Identity REMOTE_ORG_ACTIVE = TestOrganizationIdentity
			.remote(new Organization().setActive(true));
	private static final Identity LOCAL_NO_ORG = TestOrganizationIdentity.local(null);
	private static final Identity LOCAL_ORG_NOT_ACTIVE = TestOrganizationIdentity
			.local(new Organization().setActive(false));
	private static final Identity LOCAL_ORG_ACTIVE = TestOrganizationIdentity.local(new Organization().setActive(true));

	private static final Identity LOCAL_PRACTITIONER_ORG_ACTIVE = TestPractitionerIdentity.practitioner(
			new Organization().setActive(true),
			new Coding("http://dsf.dev/fhir/CodeSystem/practitioner-role", "DIC_USER", null));
	private static final Identity LOCAL_PRACTITIONER_ORG_ACTIVE_DSF_ADMIN = TestPractitionerIdentity.practitioner(
			new Organization().setActive(true),
			new Coding("http://dsf.dev/fhir/CodeSystem/practitioner-role", "DSF_ADMIN", null));
	private static final Identity LOCAL_PRACTITIONER_ORG_ACTIVE_BAD_ROLE1 = TestPractitionerIdentity.practitioner(
			new Organization().setActive(true),
			new Coding("http://dsf.dev/fhir/CodeSystem/practitioner-role", "UAC_USER", null));
	private static final Identity LOCAL_PRACTITIONER_ORG_ACTIVE_BAD_ROLE2 = TestPractitionerIdentity.practitioner(
			new Organization().setActive(true),
			new Coding("http://dsf.dev/fhir/CodeSystem/bad-system", "DIC_USER", null));
	private static final Identity LOCAL_PRACTITIONER_ORG_NOT_ACTIVE = TestPractitionerIdentity.practitioner(
			new Organization().setActive(false),
			new Coding("http://dsf.dev/fhir/CodeSystem/practitioner-role", "DIC_USER", null));
	private static final Identity LOCAL_PRACTITIONER_ORG_ACTIVE_NO_ROLES = TestPractitionerIdentity
			.practitioner(new Organization().setActive(true));

	private static final All local = new All(true, null, null);
	private static final All remote = new All(false, null, null);

	private static final All localPractitioner = new All(true, "http://dsf.dev/fhir/CodeSystem/practitioner-role",
			"DIC_USER");

	@Test
	public void testLocalAllRecipientOk() throws Exception
	{
		assertTrue(local.isRecipientAuthorized(LOCAL_ORG_ACTIVE, Stream.empty()));
	}

	@Test
	public void testLocalAllRecipientNotOkOrganizationNotActive() throws Exception
	{
		assertFalse(local.isRecipientAuthorized(LOCAL_ORG_NOT_ACTIVE, Stream.empty()));
	}

	@Test
	public void testLocalAllRecipientNotOkNoOrganization() throws Exception
	{
		assertFalse(local.isRecipientAuthorized(LOCAL_NO_ORG, Stream.empty()));
	}

	@Test
	public void testLocalAllRecipientNotOkNoUser() throws Exception
	{
		assertFalse(local.isRecipientAuthorized(null, Stream.empty()));
	}

	@Test
	public void testLocalAllRecipientNotOkRemoteOrganization() throws Exception
	{
		assertFalse(local.isRecipientAuthorized(REMOTE_ORG_ACTIVE, Stream.empty()));
	}

	@Test
	public void testRemoteAllRecipientOk() throws Exception
	{
		assertTrue(remote.isRecipientAuthorized(REMOTE_ORG_ACTIVE, Stream.empty()));
	}

	@Test
	public void testRemoteAllRecipientNotOkOrganizationNotActive() throws Exception
	{
		assertFalse(remote.isRecipientAuthorized(REMOTE_ORG_NOT_ACTIVE, Stream.empty()));
	}

	@Test
	public void testRemoteAllRecipientNotOkNoOrganization() throws Exception
	{
		assertFalse(remote.isRecipientAuthorized(REMOTE_NO_ORG, Stream.empty()));
	}

	@Test
	public void testRemoteAllRecipientNotOkNoUser() throws Exception
	{
		assertFalse(remote.isRecipientAuthorized(null, Stream.empty()));
	}

	@Test
	public void testRemoteAllRecipientNotOkLocalOrganization() throws Exception
	{
		assertFalse(remote.isRecipientAuthorized(LOCAL_ORG_ACTIVE, Stream.empty()));
	}

	@Test
	public void testRemoteAllRecipientNotOkPractitioner() throws Exception
	{
		assertFalse(remote.isRecipientAuthorized(LOCAL_PRACTITIONER_ORG_ACTIVE, Stream.empty()));
		assertFalse(remote.isRecipientAuthorized(LOCAL_PRACTITIONER_ORG_ACTIVE_BAD_ROLE1, Stream.empty()));
		assertFalse(remote.isRecipientAuthorized(LOCAL_PRACTITIONER_ORG_ACTIVE_BAD_ROLE2, Stream.empty()));
		assertFalse(remote.isRecipientAuthorized(LOCAL_PRACTITIONER_ORG_NOT_ACTIVE, Stream.empty()));
		assertFalse(remote.isRecipientAuthorized(LOCAL_PRACTITIONER_ORG_ACTIVE_NO_ROLES, Stream.empty()));
		assertFalse(remote.isRecipientAuthorized(LOCAL_PRACTITIONER_ORG_ACTIVE_DSF_ADMIN, Stream.empty()));
	}

	@Test
	public void testLocalAllRequesterOk() throws Exception
	{
		assertTrue(local.isRequesterAuthorized(LOCAL_ORG_ACTIVE, Stream.empty()));
	}

	@Test
	public void testLocalAllRequesterOkPractitionerAdmin() throws Exception
	{
		assertTrue(local.isRequesterAuthorized(LOCAL_PRACTITIONER_ORG_ACTIVE_DSF_ADMIN, Stream.empty()));
	}

	@Test
	public void testLocalAllRequesterNotOkOrganizationNotActive() throws Exception
	{
		assertFalse(local.isRequesterAuthorized(LOCAL_ORG_NOT_ACTIVE, Stream.empty()));
	}

	@Test
	public void testLocalAllRequesterNotOkNoOrganization() throws Exception
	{
		assertFalse(local.isRequesterAuthorized(LOCAL_NO_ORG, Stream.empty()));
	}

	@Test
	public void testLocalAllRequesterNotOkNoUser() throws Exception
	{
		assertFalse(local.isRequesterAuthorized(null, Stream.empty()));
	}

	@Test
	public void testLocalAllRequesterNotOkRemoteOrganization() throws Exception
	{
		assertFalse(local.isRequesterAuthorized(REMOTE_ORG_ACTIVE, Stream.empty()));
	}

	@Test
	public void testRemoteAllRequesterOk() throws Exception
	{
		assertTrue(remote.isRequesterAuthorized(REMOTE_ORG_ACTIVE, Stream.empty()));
	}

	@Test
	public void testRemoteAllRequesterNotOkOrganizationNotActive() throws Exception
	{
		assertFalse(remote.isRequesterAuthorized(REMOTE_ORG_NOT_ACTIVE, Stream.empty()));
	}

	@Test
	public void testRemoteAllRequesterNotOkNoOrganization() throws Exception
	{
		assertFalse(remote.isRequesterAuthorized(REMOTE_NO_ORG, Stream.empty()));
	}

	@Test
	public void testRemoteAllRequesterNotOkNoUser() throws Exception
	{
		assertFalse(remote.isRequesterAuthorized(null, Stream.empty()));
	}

	@Test
	public void testRemoteAllRequesterNotOkLocalOrganization() throws Exception
	{
		assertFalse(remote.isRequesterAuthorized(LOCAL_ORG_ACTIVE, Stream.empty()));
	}

	@Test
	public void testLocalAllPractitionerRequesterOk() throws Exception
	{
		assertTrue(localPractitioner.isRequesterAuthorized(LOCAL_PRACTITIONER_ORG_ACTIVE, Stream.empty()));
	}

	@Test
	public void testLocalAllPractitionerRequesterOkPractitionerAdmin() throws Exception
	{
		assertTrue(localPractitioner.isRequesterAuthorized(LOCAL_PRACTITIONER_ORG_ACTIVE_DSF_ADMIN, Stream.empty()));
	}

	@Test
	public void testLocalAllPractitionerRequesterNotOkOrganizationNotActive() throws Exception
	{
		assertFalse(localPractitioner.isRequesterAuthorized(LOCAL_PRACTITIONER_ORG_NOT_ACTIVE, Stream.empty()));
	}

	@Test
	public void testLocalAllPractitionerRequesterNotOkPractitionerNoRoles() throws Exception
	{
		assertFalse(localPractitioner.isRequesterAuthorized(LOCAL_PRACTITIONER_ORG_ACTIVE_NO_ROLES, Stream.empty()));
	}

	@Test
	public void testLocalAllPractitionerRequesterNotOkPractitionerBadRole1() throws Exception
	{
		assertFalse(localPractitioner.isRequesterAuthorized(LOCAL_PRACTITIONER_ORG_ACTIVE_BAD_ROLE1, Stream.empty()));
	}

	@Test
	public void testLocalAllPractitionerRequesterNotOkPractitionerBadRole2() throws Exception
	{
		assertFalse(localPractitioner.isRequesterAuthorized(LOCAL_PRACTITIONER_ORG_ACTIVE_BAD_ROLE2, Stream.empty()));
	}

	@Test
	public void testLocalAllPractitionerRequesterNotOkNotAPractitioner() throws Exception
	{
		assertFalse(localPractitioner.isRequesterAuthorized(LOCAL_ORG_ACTIVE, Stream.empty()));
	}
}
