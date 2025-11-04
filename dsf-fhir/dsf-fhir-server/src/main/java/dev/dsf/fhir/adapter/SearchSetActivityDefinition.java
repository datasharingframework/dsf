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

import java.util.List;
import java.util.regex.Matcher;

import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.StringType;

public class SearchSetActivityDefinition extends AbstractSearchSet<ActivityDefinition>
{
	private record Row(ElementId id, String status, String title, String processDomain, String processName,
			String processVersion, String messageNames, String lastUpdated)
	{
	}

	public SearchSetActivityDefinition(int defaultPageCount)
	{
		super(defaultPageCount, ActivityDefinition.class);
	}

	@Override
	protected Row toRow(ElementId id, ActivityDefinition resource)
	{
		String status = resource.hasStatus() ? resource.getStatus().toCode() : "";

		String title = resource.hasTitleElement() && resource.getTitleElement().hasValue()
				? resource.getTitleElement().getValue()
				: "";

		String processDomain = "", processName = "", processVersion = "";
		if (resource.getUrl() != null && !resource.getUrl().isBlank())
		{
			Matcher matcher = INSTANTIATES_CANONICAL_PATTERN
					.matcher(resource.getUrl() + (resource.hasVersion() ? "|" + resource.getVersion() : ""));
			if (matcher.matches())
			{
				processDomain = matcher.group("domain");
				processName = matcher.group("processName");
				processVersion = matcher.group("processVersion");
			}
		}

		List<String> messageNames = resource.getExtensionsByUrl(EXTENSION_PROCESS_AUTHORIZATION).stream()
				.flatMap(e -> e.getExtensionsByUrl(EXTENSION_PROCESS_AUTHORIZATION_MESSAGE_NAME).stream())
				.filter(e -> e.getValue() instanceof StringType).map(e -> ((StringType) e.getValue()).getValue())
				.toList();

		String combinedMessageNames = (messageNames.size() > 2)
				? String.join(", ", messageNames.subList(0, 2)) + ", ..."
				: String.join(", ", messageNames);

		String lastUpdated = formatLastUpdated(resource);

		return new Row(id, status, title, processDomain, processName, processVersion, combinedMessageNames,
				lastUpdated);
	}
}
