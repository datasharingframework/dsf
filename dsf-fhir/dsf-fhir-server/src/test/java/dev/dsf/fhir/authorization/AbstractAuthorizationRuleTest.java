/*
 * Copyright 2018-2025 Heilbronn University of Applied Sciences
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.dsf.fhir.authorization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.util.UUID;
import java.util.function.Predicate;

import org.hl7.fhir.r4.model.Resource;
import org.junit.Before;
import org.junit.Test;

import dev.dsf.common.auth.conf.DsfRole;
import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.authentication.FhirServerRoleImpl;
import dev.dsf.fhir.authentication.OrganizationProvider;
import dev.dsf.fhir.authorization.read.ReadAccessHelper;
import dev.dsf.fhir.dao.provider.DaoProvider;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.service.ReferenceResolver;

public abstract class AbstractAuthorizationRuleTest<R extends Resource>
{
	protected static final String SERVER_BASE = "https://dsf.test/fhir";
	protected static final UUID RESOURCE_UUID = UUID.fromString("d1e2f3a4-b5c6-4d7e-8f90-123456789abc");

	protected DaoProvider daoProvider;
	protected ReferenceResolver referenceResolver;
	protected OrganizationProvider organizationProvider;
	protected ReadAccessHelper readAccessHelper;
	protected ParameterConverter parameterConverter;

	protected AbstractAuthorizationRule<R, ?> rule;

	@Before
	public void setUpBase()
	{
		daoProvider = mock(DaoProvider.class);
		referenceResolver = mock(ReferenceResolver.class);
		organizationProvider = mock(OrganizationProvider.class);
		readAccessHelper = mock(ReadAccessHelper.class);
		parameterConverter = mock(ParameterConverter.class);
		lenient().when(parameterConverter.toUuid(any(), any())).thenReturn(RESOURCE_UUID);

		rule = createRule();
	}

	protected abstract AbstractAuthorizationRule<R, ?> createRule();

	protected abstract Class<R> expectedResourceType();

	protected abstract R newResource();

	protected static Identity identity(boolean localIdentity, boolean hasRole)
	{
		return identity(localIdentity, _ -> hasRole);
	}

	protected static Identity identity(boolean localIdentity, Predicate<DsfRole> hasRole)
	{
		Identity identity = mock(Identity.class);
		lenient().when(identity.isLocalIdentity()).thenReturn(localIdentity);
		lenient().when(identity.hasDsfRole(any())).then(invocation -> hasRole.test(invocation.getArgument(0)));

		return identity;
	}

	@Test
	public void getResourceTypeMatchesExpected()
	{
		assertEquals(expectedResourceType(), rule.getResourceType());
	}

	@Test
	public void afterPropertiesSetSucceedsWithAllDependencies() throws Exception
	{
		rule.afterPropertiesSet();
	}

	@Test
	public void searchAllowedWhenIdentityHasSearchRole()
	{
		assertTrue(rule.reasonSearchAllowed(identity(true, true)).isPresent());
	}

	@Test
	public void searchDeniedWhenIdentityHasNoSearchRole()
	{
		assertTrue(rule.reasonSearchAllowed(identity(true, false)).isEmpty());
	}

	@Test
	public void historyAllowedWhenIdentityHasHistoryRole()
	{
		assertTrue(rule.reasonHistoryAllowed(identity(true, true)).isPresent());
	}

	@Test
	public void historyDeniedWhenIdentityHasNoHistoryRole()
	{
		assertTrue(rule.reasonHistoryAllowed(identity(true, false)).isEmpty());
	}

	@Test
	public void websocketAllowedForLocalIdentityWithWebsocketRole() throws Exception
	{
		when(daoProvider.newReadOnlyAutoCommitTransaction()).thenReturn(mock(Connection.class));

		assertTrue(rule.reasonWebsocketAllowed(identity(true, true), newResource()).isPresent());
	}

	@Test
	public void websocketDeniedForNonLocalIdentity() throws Exception
	{
		when(daoProvider.newReadOnlyAutoCommitTransaction()).thenReturn(mock(Connection.class));

		assertTrue(rule.reasonWebsocketAllowed(identity(false, true), newResource()).isEmpty());
	}

	@Test
	public void websocketDeniedWithoutWebsocketRole() throws Exception
	{
		when(daoProvider.newReadOnlyAutoCommitTransaction()).thenReturn(mock(Connection.class));

		assertTrue(rule.reasonWebsocketAllowed(identity(true, false), newResource()).isEmpty());
	}

	@Test
	public void permanentDeleteDeniedForNonLocalIdentity()
	{
		Connection connection = mock(Connection.class);

		assertTrue(rule.reasonPermanentDeleteAllowed(connection, identity(false, true), newResource()).isEmpty());
		verifyNoInteractions(connection);
	}

	@Test
	public void permanentDeleteDeniedWithoutPermanentDeleteRole()
	{
		Connection connection = mock(Connection.class);

		assertTrue(rule.reasonPermanentDeleteAllowed(connection, identity(true, false), newResource()).isEmpty());
		verifyNoInteractions(connection);
	}

	@Test
	public void permanentDeleteForLocalIdentityWithPermanentDeleteRoleAndDeleteRoleAllowed() throws Exception
	{
		when(daoProvider.newReadOnlyAutoCommitTransaction()).thenReturn(mock(Connection.class));

		assertTrue(rule.reasonPermanentDeleteAllowed(identity(true, true), newResource()).isPresent());
	}

	@Test
	public void permanentDeleteForLocalIdentityWithPermanentDeleteRoleAndNoDeleteRoleDenied() throws Exception
	{
		when(daoProvider.newReadOnlyAutoCommitTransaction()).thenReturn(mock(Connection.class));

		assertTrue(rule.reasonPermanentDeleteAllowed(identity(true, role ->
		{
			if (FhirServerRoleImpl.delete(expectedResourceType()).equals(role))
				return false;
			else
				return true;
		}), newResource()).isEmpty());
	}
}
