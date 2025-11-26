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
package dev.dsf.fhir.adapter;

import org.hl7.fhir.r4.model.Endpoint;

public class SearchSetEndpoint extends AbstractSearchSet<Endpoint>
{
	private record Row(ElementId id, String status, String identifier, String name, String address,
			ElementId managingOrganization, String lastUpdated)
	{
	}

	public SearchSetEndpoint(int defaultPageCount)
	{
		super(defaultPageCount, Endpoint.class);
	}

	@Override
	protected Row toRow(ElementId id, Endpoint resource)
	{
		String status = resource.hasStatus() ? resource.getStatus().toCode() : "";

		String identifier = getIdentifierValues(resource, Endpoint::hasIdentifier, Endpoint::getIdentifier,
				NAMING_SYSTEM_ENDPOINT_IDENTIFIER);
		String name = resource.hasName() ? resource.getName() : "";
		String address = resource.hasAddress() ? resource.getAddress() : "";

		ElementId managingOrganization = ElementId.from(resource, Endpoint::hasManagingOrganization,
				Endpoint::getManagingOrganization);

		String lastUpdated = formatLastUpdated(resource);

		return new Row(id, status, identifier, name, address, managingOrganization, lastUpdated);
	}
}
