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
package dev.dsf.fhir.authentication;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Reference;
import org.springframework.beans.factory.InitializingBean;

import dev.dsf.fhir.dao.EndpointDao;
import dev.dsf.fhir.help.ExceptionHandler;

public class EndpointProviderImpl implements EndpointProvider, InitializingBean
{
	private final ExceptionHandler exceptionHandler;
	private final EndpointDao dao;
	private final String serverBaseUrl;

	public EndpointProviderImpl(ExceptionHandler exceptionHandler, EndpointDao dao, String serverBaseUrl)
	{
		this.exceptionHandler = exceptionHandler;
		this.dao = dao;
		this.serverBaseUrl = serverBaseUrl;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(exceptionHandler, "exceptionHandler");
		Objects.requireNonNull(dao, "dao");
		Objects.requireNonNull(serverBaseUrl, "serverBaseUrl");
	}

	@Override
	public Optional<Endpoint> getLocalEndpoint()
	{
		return exceptionHandler.catchAndLogSqlExceptionAndIfReturn(
				() -> dao.readActiveNotDeletedByAddress(serverBaseUrl), Optional::empty);
	}

	@Override
	public Optional<String> getLocalEndpointIdentifierValue()
	{
		return getLocalEndpoint().filter(Endpoint::hasIdentifier).map(Endpoint::getIdentifier)
				.flatMap(ids -> getIdentifierValue(ids, ENDPOINT_IDENTIFIER_SYSTEM));
	}

	private Optional<String> getIdentifierValue(List<Identifier> identifiers, String system)
	{
		return identifiers.stream().filter(Identifier::hasSystem).filter(Identifier::hasValue)
				.filter(i -> system.equals(i.getSystem())).map(Identifier::getValue).findFirst();
	}

	@Override
	public Optional<Endpoint> getEndpoint(Organization organization, String thumbprint)
	{
		Optional<Endpoint> endpoint = exceptionHandler.catchAndLogSqlExceptionAndIfReturn(
				() -> dao.readActiveNotDeletedByThumbprint(thumbprint), Optional::empty);

		if (endpoint.isEmpty())
		{
			List<Reference> endpoints = organization.getEndpoint();
			if (endpoints.size() == 1 && endpoints.get(0).hasReference())
			{
				String id = endpoints.get(0).getReferenceElement().getIdPart();

				endpoint = exceptionHandler.catchAndLogSqlAndResourceDeletedExceptionAndIfReturn(
						() -> dao.read(UUID.fromString(id)), Optional::empty, Optional::empty);
			}
		}

		return endpoint;
	}
}
