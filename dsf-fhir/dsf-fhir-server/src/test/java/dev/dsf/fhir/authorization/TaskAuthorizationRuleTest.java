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

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.Connection;

import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.junit.Ignore;
import org.junit.Test;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.common.auth.conf.OrganizationIdentity;
import dev.dsf.fhir.authentication.EndpointProvider;
import dev.dsf.fhir.authorization.process.ProcessAuthorizationHelper;

public class TaskAuthorizationRuleTest extends AbstractAuthorizationRuleTest<Task>
{
	private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();

	private final ProcessAuthorizationHelper processAuthorizationHelper = mock(ProcessAuthorizationHelper.class);
	private final EndpointProvider endpointProvider = mock(EndpointProvider.class);

	@Override
	protected TaskAuthorizationRule createRule()
	{
		return newRule(processAuthorizationHelper, endpointProvider);
	}

	private TaskAuthorizationRule newRule(ProcessAuthorizationHelper processAuthorizationHelper,
			EndpointProvider endpointProvider)
	{
		return new TaskAuthorizationRule(daoProvider, SERVER_BASE, referenceResolver, organizationProvider,
				readAccessHelper, parameterConverter, processAuthorizationHelper, FHIR_CONTEXT, endpointProvider);
	}

	@Override
	protected Class<Task> expectedResourceType()
	{
		return Task.class;
	}

	@Override
	protected Task newResource()
	{
		return task(TaskStatus.DRAFT);
	}

	private static Task task(TaskStatus status)
	{
		Task task = new Task();
		task.setId("Task/d1e2f3a4-b5c6-4d7e-8f90-123456789abc/_history/1");
		task.setStatus(status);
		return task;
	}

	private static OrganizationIdentity localOrganizationIdentity()
	{
		OrganizationIdentity identity = mock(OrganizationIdentity.class);
		when(identity.isLocalIdentity()).thenReturn(true);
		when(identity.hasDsfRole(any())).thenReturn(true);
		return identity;
	}

	@Test
	public void afterPropertiesSetThrowsWhenProcessAuthorizationHelperNull()
	{
		TaskAuthorizationRule ruleWithNull = newRule(null, endpointProvider);
		assertThrows(NullPointerException.class, ruleWithNull::afterPropertiesSet);
	}

	@Test
	public void afterPropertiesSetThrowsWhenEndpointProviderNull()
	{
		TaskAuthorizationRule ruleWithNull = newRule(processAuthorizationHelper, null);
		assertThrows(NullPointerException.class, ruleWithNull::afterPropertiesSet);
	}

	@Test
	public void createDeniedWhenIdentityHasNoCreateRole()
	{
		Connection connection = mock(Connection.class);

		assertTrue(rule.reasonCreateAllowed(connection, identity(true, false), task(TaskStatus.REQUESTED)).isEmpty());
		verifyNoInteractions(connection);
	}

	@Test
	public void createDeniedWhenTaskStatusNotDraftOrRequested()
	{
		Connection connection = mock(Connection.class);

		assertTrue(rule.reasonCreateAllowed(connection, identity(true, true), task(TaskStatus.COMPLETED)).isEmpty());
		verifyNoInteractions(connection);
	}

	@Test
	public void createDraftDeniedWhenIdentityNotLocalOrganizationOrDsfAdmin()
	{
		Connection connection = mock(Connection.class);

		assertTrue(rule.reasonCreateAllowed(connection, identity(true, true), task(TaskStatus.DRAFT)).isEmpty());
		verifyNoInteractions(connection);
	}

	@Test
	public void deleteAllowedForLocalOrganizationWhenTaskStatusDraft()
	{
		Connection connection = mock(Connection.class);

		assertTrue(
				rule.reasonDeleteAllowed(connection, localOrganizationIdentity(), task(TaskStatus.DRAFT)).isPresent());
		verifyNoInteractions(connection);
	}

	@Test
	public void deleteDeniedWhenIdentityHasNoDeleteRole()
	{
		Connection connection = mock(Connection.class);

		assertTrue(rule.reasonDeleteAllowed(connection, identity(true, false), task(TaskStatus.DRAFT)).isEmpty());
		verifyNoInteractions(connection);
	}

	@Test
	public void deleteDeniedWhenIdentityNotLocalOrganizationOrDsfAdmin()
	{
		Connection connection = mock(Connection.class);

		assertTrue(rule.reasonDeleteAllowed(connection, identity(true, true), task(TaskStatus.DRAFT)).isEmpty());
		verifyNoInteractions(connection);
	}

	@Test
	public void deleteDeniedWhenTaskStatusNotDraft()
	{
		Connection connection = mock(Connection.class);

		assertTrue(rule.reasonDeleteAllowed(connection, localOrganizationIdentity(), task(TaskStatus.REQUESTED))
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
