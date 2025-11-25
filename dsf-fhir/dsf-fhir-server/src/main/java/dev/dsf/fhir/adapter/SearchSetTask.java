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

import java.util.regex.Matcher;

import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskStatus;

public class SearchSetTask extends AbstractSearchSet<Task>
{
	private record Row(ElementId id, String status, String processDomain, String processName, String processVersion,
			String messageName, String requester, String businessKeyOrIdentifier, String lastUpdated)
	{
	}

	public SearchSetTask(int defaultPageCount)
	{
		super(defaultPageCount, Task.class);
	}

	@Override
	protected Row toRow(ElementId id, Task resource)
	{
		String status = resource.hasStatus() ? resource.getStatus().toCode() : "";

		String processDomain = "", processName = "", processVersion = "";
		if (resource.getInstantiatesCanonical() != null && !resource.getInstantiatesCanonical().isBlank())
		{
			Matcher matcher = INSTANTIATES_CANONICAL_PATTERN.matcher(resource.getInstantiatesCanonical());
			if (matcher.matches())
			{
				processDomain = matcher.group("domain");
				processName = matcher.group("processName");
				processVersion = matcher.group("processVersion");
			}
		}

		String messageName = resource.getInput().stream()
				.filter(isStringParam(CODE_SYSTEM_BPMN_MESSAGE, CODE_SYSTEM_BPMN_MESSAGE_MESSAGE_NAME)).findFirst()
				.map(c -> ((StringType) c.getValue()).getValue()).orElse("");

		String requester = resource.hasRequester() && resource.getRequester().hasIdentifier()
				&& resource.getRequester().getIdentifier().hasValue()
						? resource.getRequester().getIdentifier().getValue()
						: "";

		String businessKeyOrIdentifier;
		if (TaskStatus.DRAFT.equals(resource.getStatus()))
		{
			businessKeyOrIdentifier = resource.getIdentifier().stream()
					.filter(i -> i.hasSystem() && NAMING_SYSTEM_TASK_IDENTIFIER.equals(i.getSystem()) && i.hasValue())
					.map(Identifier::getValue).findFirst().map(v ->
					{
						String[] parts = v.split("/");
						return parts.length > 0 ? parts[parts.length - 1] : "";
					}).orElse("");
		}
		else
		{
			businessKeyOrIdentifier = resource.getInput().stream()
					.filter(isStringParam(CODE_SYSTEM_BPMN_MESSAGE, CODE_SYSTEM_BPMN_MESSAGE_BUSINESS_KEY)).findFirst()
					.map(c -> ((StringType) c.getValue()).getValue()).orElse("");
		}

		String lastUpdated = formatLastUpdated(resource);

		return new Row(id, status, processDomain, processName, processVersion, messageName, requester,
				businessKeyOrIdentifier, lastUpdated);
	}
}
