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
package dev.dsf.fhir.history.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.common.auth.conf.OrganizationIdentity;

public class TaskHistoryIdentityFilterTest
{
	@Test
	public void identityWithoutRoleSeesNothing()
	{
		TaskHistoryIdentityFilter filter = new TaskHistoryIdentityFilter(mock(Identity.class));

		String query = filter.getFilterQuery();
		assertTrue(query.contains("type = 'Task'"));
		assertTrue(query.contains("FALSE"));
		assertEquals(0, filter.getSqlParameterCount());
	}

	@Test
	public void localOrganizationSeesTasksWhereItIsRecipient()
	{
		OrganizationIdentity identity = mock(OrganizationIdentity.class);
		when(identity.hasDsfRole(any())).thenReturn(true);
		when(identity.isLocalIdentity()).thenReturn(true);

		TaskHistoryIdentityFilter filter = new TaskHistoryIdentityFilter(identity);

		String query = filter.getFilterQuery();
		assertTrue(query.contains("type = 'Task'"));
		assertTrue(query.contains("recipient"));
		assertFalse(query.contains("FALSE"));
		assertEquals(1, filter.getSqlParameterCount());
	}

	@Test
	public void remoteOrganizationSeesTasksWhereItIsRequester()
	{
		OrganizationIdentity identity = mock(OrganizationIdentity.class);
		when(identity.hasDsfRole(any())).thenReturn(true);
		when(identity.isLocalIdentity()).thenReturn(false);

		TaskHistoryIdentityFilter filter = new TaskHistoryIdentityFilter(identity);

		String query = filter.getFilterQuery();
		assertTrue(query.contains("type = 'Task'"));
		assertTrue(query.contains("requester"));
		assertFalse(query.contains("FALSE"));
		assertEquals(1, filter.getSqlParameterCount());
	}
}
