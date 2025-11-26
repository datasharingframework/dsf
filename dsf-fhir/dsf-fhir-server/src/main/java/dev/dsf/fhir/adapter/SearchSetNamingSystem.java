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

import org.hl7.fhir.r4.model.NamingSystem;

public class SearchSetNamingSystem extends AbstractSearchSet<NamingSystem>
{
	private record Row(ElementId id, String status, String uniqueId, String name, String lastUpdated)
	{
	}

	public SearchSetNamingSystem(int defaultPageCount)
	{
		super(defaultPageCount, NamingSystem.class);
	}

	@Override
	protected Row toRow(ElementId id, NamingSystem resource)
	{
		String status = resource.hasStatus() ? resource.getStatus().toCode() : "";

		String uniqueId = resource.hasUniqueId() && resource.getUniqueIdFirstRep().hasValueElement()
				&& resource.getUniqueIdFirstRep().getValueElement().hasValue()
						? resource.getUniqueIdFirstRep().getValueElement().getValue()
						: "";

		if (resource.hasUniqueId() && resource.getUniqueId().size() > 1 && !uniqueId.isBlank())
			uniqueId += ", ...";

		String name = resource.hasNameElement() && resource.getNameElement().hasValue()
				? resource.getNameElement().getValue()
				: "";

		String lastUpdated = formatLastUpdated(resource);

		return new Row(id, status, uniqueId, name, lastUpdated);
	}
}
