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

import org.hl7.fhir.r4.model.MetadataResource;

public class SearchSetMetadataResource<M extends MetadataResource> extends AbstractSearchSet<M>
{
	private record Row(ElementId id, String status, String urlVersion, String titleOrName, String lastUpdated)
	{
	}

	public SearchSetMetadataResource(int defaultPageCount, Class<M> matchResourceType)
	{
		super(defaultPageCount, matchResourceType, "searchsetMetadataResource");
	}

	@Override
	protected Row toRow(ElementId id, M resource)
	{
		String status = resource.hasStatus() ? resource.getStatus().toCode() : "";

		String urlVersion = (resource.hasUrlElement() && resource.getUrlElement().hasValue()
				? resource.getUrlElement().getValue()
				: "")
				+ " | "
				+ (resource.hasVersionElement() && resource.getVersionElement().hasValue()
						? resource.getVersionElement().getValue()
						: "");

		String titleOrName = resource.hasTitleElement() && resource.getTitleElement().hasValue()
				? resource.getTitleElement().getValue()
				: resource.hasNameElement() && resource.getNameElement().hasValue()
						? resource.getNameElement().getValue()
						: "";

		String lastUpdated = formatLastUpdated(resource);

		return new Row(id, status, urlVersion, titleOrName, lastUpdated);
	}
}
