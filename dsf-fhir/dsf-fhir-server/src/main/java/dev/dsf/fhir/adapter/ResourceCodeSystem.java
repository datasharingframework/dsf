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

import org.hl7.fhir.r4.model.CodeSystem;

public class ResourceCodeSystem extends AbstractMetdataResource<CodeSystem>
{
	private record Element(String description, Boolean caseSensitive, String hierarchyMeaning, Boolean versionNeeded,
			String content, Integer count, List<Concept> concept)
	{
	}

	private record Concept(String code, String display, String definition)
	{
	}

	public ResourceCodeSystem()
	{
		super(CodeSystem.class);
	}

	@Override
	protected Element toElement(CodeSystem resource)
	{
		String description = getString(resource, CodeSystem::hasDescriptionElement, CodeSystem::getDescriptionElement);
		Boolean caseSensitive = getBoolean(resource, CodeSystem::hasCaseSensitiveElement,
				CodeSystem::getCaseSensitiveElement);
		String hierarchyMeaning = getEnumeration(resource, CodeSystem::hasHierarchyMeaningElement,
				CodeSystem::getHierarchyMeaningElement);
		Boolean versionNeeded = getBoolean(resource, CodeSystem::hasVersionNeededElement,
				CodeSystem::getVersionNeededElement);
		String content = getEnumeration(resource, CodeSystem::hasContentElement, CodeSystem::getContentElement);
		Integer count = getInteger(resource, CodeSystem::hasCountElement, CodeSystem::getCountElement);

		List<Concept> concept = resource.hasConcept() ? resource.getConcept().stream().map(i ->
		{
			String code = i.hasCodeElement() && i.getCodeElement().hasValue() ? i.getCodeElement().getValue() : null;
			String display = i.hasDisplayElement() && i.getDisplayElement().hasValue()
					? i.getDisplayElement().getValue()
					: null;
			String definition = i.hasDefinitionElement() && i.getDefinitionElement().hasValue()
					? i.getDefinitionElement().getValue()
					: null;

			return new Concept(code, display, definition);
		}).toList() : null;

		return new Element(description, caseSensitive, hierarchyMeaning, versionNeeded, content, count, concept);
	}
}
