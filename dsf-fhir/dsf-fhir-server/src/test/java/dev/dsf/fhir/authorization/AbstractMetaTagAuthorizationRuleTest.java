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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.sql.Connection;

import org.hl7.fhir.r4.model.Resource;
import org.junit.Test;

public abstract class AbstractMetaTagAuthorizationRuleTest<R extends Resource> extends AbstractAuthorizationRuleTest<R>
{
	@Test
	public void createDeniedWhenIdentityNotLocal()
	{
		Connection connection = mock(Connection.class);

		assertTrue(rule.reasonCreateAllowed(connection, identity(false, true), newResource()).isEmpty());
		verifyNoInteractions(connection);
	}

	@Test
	public void createDeniedWhenIdentityHasNoCreateRole()
	{
		Connection connection = mock(Connection.class);

		assertTrue(rule.reasonCreateAllowed(connection, identity(true, false), newResource()).isEmpty());
		verifyNoInteractions(connection);
	}

	@Test
	public void readDeniedWhenIdentityHasNoReadRole()
	{
		Connection connection = mock(Connection.class);

		assertTrue(rule.reasonReadAllowed(connection, identity(true, false), newResource()).isEmpty());
		verifyNoInteractions(connection);
	}

	@Test
	public void updateDeniedWhenIdentityNotLocal()
	{
		Connection connection = mock(Connection.class);

		assertTrue(rule.reasonUpdateAllowed(connection, identity(false, true), newResource(), newResource()).isEmpty());
		verifyNoInteractions(connection);
	}

	@Test
	public void updateDeniedWhenIdentityHasNoUpdateRole()
	{
		Connection connection = mock(Connection.class);

		assertTrue(rule.reasonUpdateAllowed(connection, identity(true, false), newResource(), newResource()).isEmpty());
		verifyNoInteractions(connection);
	}

	@Test
	public void deleteDeniedWhenIdentityNotLocal()
	{
		Connection connection = mock(Connection.class);

		assertTrue(rule.reasonDeleteAllowed(connection, identity(false, true), newResource()).isEmpty());
		verifyNoInteractions(connection);
	}

	@Test
	public void deleteDeniedWhenIdentityHasNoDeleteRole()
	{
		Connection connection = mock(Connection.class);

		assertTrue(rule.reasonDeleteAllowed(connection, identity(true, false), newResource()).isEmpty());
		verifyNoInteractions(connection);
	}
}
