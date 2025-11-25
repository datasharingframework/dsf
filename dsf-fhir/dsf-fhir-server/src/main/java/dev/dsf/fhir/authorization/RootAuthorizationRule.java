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

import java.sql.Connection;
import java.util.Optional;

import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.common.auth.conf.DsfRole;
import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.authentication.FhirServerRoleImpl;

public class RootAuthorizationRule implements AuthorizationRule<Resource>
{
	private static final Logger logger = LoggerFactory.getLogger(RootAuthorizationRule.class);

	@Override
	public Class<Resource> getResourceType()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<String> reasonCreateAllowed(Identity identity, Resource newResource)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<String> reasonCreateAllowed(Connection connection, Identity identity, Resource newResource)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<String> reasonReadAllowed(Identity identity, Resource existingResource)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<String> reasonReadAllowed(Connection connection, Identity identity, Resource existingResource)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<String> reasonUpdateAllowed(Identity identity, Resource oldResource, Resource newResource)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<String> reasonUpdateAllowed(Connection connection, Identity identity, Resource oldResource,
			Resource newResource)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<String> reasonDeleteAllowed(Identity identity, Resource oldResource)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<String> reasonDeleteAllowed(Connection connection, Identity identity, Resource oldResource)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<String> reasonSearchAllowed(Identity identity)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<String> reasonHistoryAllowed(Identity identity)
	{
		if (identity.getDsfRoles().stream().map(DsfRole::name)
				.anyMatch(FhirServerRoleImpl.Operation.HISTORY.name()::equals))
		{
			logger.info("History of root authorized for identity '{}'", identity.getName());
			return Optional.of("Identity has role " + FhirServerRoleImpl.Operation.HISTORY);
		}
		else
		{
			logger.warn("History of root unauthorized for identity '{}', no role {}", identity.getName(),
					FhirServerRoleImpl.Operation.HISTORY);
			return Optional.empty();
		}
	}

	@Override
	public Optional<String> reasonPermanentDeleteAllowed(Identity identity, Resource oldResource)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<String> reasonPermanentDeleteAllowed(Connection connection, Identity identity, Resource oldResource)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<String> reasonWebsocketAllowed(Identity identity, Resource existingResource)
	{
		throw new UnsupportedOperationException();
	}
}
