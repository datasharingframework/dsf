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

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.Connection;

import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseStatus;
import org.junit.Ignore;
import org.junit.Test;

import dev.dsf.common.auth.conf.OrganizationIdentity;

public class QuestionnaireResponseAuthorizationRuleTest extends AbstractAuthorizationRuleTest<QuestionnaireResponse>
{
	@Override
	protected QuestionnaireResponseAuthorizationRule createRule()
	{
		return new QuestionnaireResponseAuthorizationRule(daoProvider, SERVER_BASE, referenceResolver,
				organizationProvider, readAccessHelper, parameterConverter);
	}

	@Override
	protected Class<QuestionnaireResponse> expectedResourceType()
	{
		return QuestionnaireResponse.class;
	}

	@Override
	protected QuestionnaireResponse newResource()
	{
		return response(QuestionnaireResponseStatus.INPROGRESS);
	}

	private static QuestionnaireResponse response(QuestionnaireResponseStatus status)
	{
		QuestionnaireResponse questionnaireResponse = new QuestionnaireResponse();
		questionnaireResponse.setId("QuestionnaireResponse/d1e2f3a4-b5c6-4d7e-8f90-123456789abc/_history/1");
		questionnaireResponse.setStatus(status);
		return questionnaireResponse;
	}

	private static OrganizationIdentity localOrganizationIdentity()
	{
		OrganizationIdentity identity = mock(OrganizationIdentity.class);
		when(identity.isLocalIdentity()).thenReturn(true);
		when(identity.hasDsfRole(any())).thenReturn(true);
		return identity;
	}

	@Test
	public void createDeniedWhenIdentityHasNoCreateRole()
	{
		Connection connection = mock(Connection.class);

		assertTrue(rule.reasonCreateAllowed(connection, identity(true, false),
				response(QuestionnaireResponseStatus.INPROGRESS)).isEmpty());
		verifyNoInteractions(connection);
	}

	@Test
	public void createDeniedWhenIdentityNotLocalOrganizationOrDsfAdmin()
	{
		Connection connection = mock(Connection.class);

		assertTrue(rule
				.reasonCreateAllowed(connection, identity(true, true), response(QuestionnaireResponseStatus.INPROGRESS))
				.isEmpty());
		verifyNoInteractions(connection);
	}

	@Test
	public void createDeniedWhenStatusNotInProgress()
	{
		Connection connection = mock(Connection.class);

		assertTrue(rule.reasonCreateAllowed(connection, localOrganizationIdentity(),
				response(QuestionnaireResponseStatus.STOPPED)).isEmpty());
		verifyNoInteractions(connection);
	}

	@Test
	public void readAllowedForLocalOrganization()
	{
		Connection connection = mock(Connection.class);

		assertTrue(rule.reasonReadAllowed(connection, localOrganizationIdentity(),
				response(QuestionnaireResponseStatus.COMPLETED)).isPresent());
		verifyNoInteractions(connection);
	}

	@Test
	public void readDeniedWhenIdentityHasNoReadRole()
	{
		Connection connection = mock(Connection.class);

		assertTrue(rule
				.reasonReadAllowed(connection, identity(true, false), response(QuestionnaireResponseStatus.COMPLETED))
				.isEmpty());
		verifyNoInteractions(connection);
	}

	@Test
	public void readDeniedForIdentityNotLocalOrganizationAndNotAuthorizedPractitioner()
	{
		Connection connection = mock(Connection.class);

		assertTrue(rule
				.reasonReadAllowed(connection, identity(true, true), response(QuestionnaireResponseStatus.COMPLETED))
				.isEmpty());
		verifyNoInteractions(connection);
	}

	@Test
	public void updateDeniedWhenIdentityHasNoUpdateRole()
	{
		Connection connection = mock(Connection.class);

		assertTrue(rule.reasonUpdateAllowed(connection, identity(true, false),
				response(QuestionnaireResponseStatus.INPROGRESS), response(QuestionnaireResponseStatus.COMPLETED))
				.isEmpty());
		verifyNoInteractions(connection);
	}

	@Test
	public void updateDeniedWhenIdentityNotLocal()
	{
		Connection connection = mock(Connection.class);

		assertTrue(rule.reasonUpdateAllowed(connection, identity(false, true),
				response(QuestionnaireResponseStatus.INPROGRESS), response(QuestionnaireResponseStatus.COMPLETED))
				.isEmpty());
		verifyNoInteractions(connection);
	}

	@Test
	public void deleteAllowedForLocalOrganization()
	{
		Connection connection = mock(Connection.class);

		assertTrue(rule.reasonDeleteAllowed(connection, localOrganizationIdentity(),
				response(QuestionnaireResponseStatus.COMPLETED)).isPresent());
		verifyNoInteractions(connection);
	}

	@Test
	public void deleteDeniedWhenIdentityHasNoDeleteRole()
	{
		Connection connection = mock(Connection.class);

		assertTrue(rule
				.reasonDeleteAllowed(connection, identity(true, false), response(QuestionnaireResponseStatus.COMPLETED))
				.isEmpty());
		verifyNoInteractions(connection);
	}

	@Test
	public void deleteDeniedWhenIdentityNotLocalOrganizationOrDsfAdmin()
	{
		Connection connection = mock(Connection.class);

		assertTrue(rule
				.reasonDeleteAllowed(connection, identity(true, true), response(QuestionnaireResponseStatus.COMPLETED))
				.isEmpty());
		verifyNoInteractions(connection);
	}

	@Test
	@Ignore
	@Override
	public void permanentDeleteForLocalIdentityWithPermanentDeleteRoleAndDeleteRoleAllowed() throws Exception
	{
		super.permanentDeleteForLocalIdentityWithPermanentDeleteRoleAndDeleteRoleAllowed();
	}
}
