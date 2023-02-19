package dev.dsf.fhir.authorization.process;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.stream.Stream;

import org.hl7.fhir.r4.model.Organization;
import org.junit.Test;

import dev.dsf.common.auth.Identity;

public class AllTest
{
	private static final Identity REMOTE_NO_ORG = TestIdentity.remote(null);
	private static final Identity REMOTE_ORG_NOT_ACTIVE = TestIdentity.remote(new Organization().setActive(false));
	private static final Identity REMOTE_ORG_ACTIVE = TestIdentity.remote(new Organization().setActive(true));
	private static final Identity LOCAL_NO_ORG = TestIdentity.local(null);
	private static final Identity LOCAL_ORG_NOT_ACTIVE = TestIdentity.local(new Organization().setActive(false));
	private static final Identity LOCAL_ORG_ACTIVE = TestIdentity.local(new Organization().setActive(true));

	private static final All local = new All(true);
	private static final All remote = new All(false);

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
	public void testLocalAllRequesterOk() throws Exception
	{
		assertTrue(local.isRequesterAuthorized(LOCAL_ORG_ACTIVE, Stream.empty()));
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
}
