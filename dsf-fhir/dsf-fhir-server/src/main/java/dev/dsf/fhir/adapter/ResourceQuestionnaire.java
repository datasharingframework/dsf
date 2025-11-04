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

import org.hl7.fhir.r4.model.Questionnaire;

public class ResourceQuestionnaire extends AbstractMetdataResource<Questionnaire>
{
	private record Element(String description, List<Item> item)
	{
	}

	private record Item(String linkId, String text, String type)
	{
	}

	public ResourceQuestionnaire()
	{
		super(Questionnaire.class);
	}

	@Override
	protected Element toElement(Questionnaire resource)
	{
		String description = getString(resource, Questionnaire::hasDescriptionElement,
				Questionnaire::getDescriptionElement);

		List<Item> item = resource.hasItem() ? resource.getItem().stream().map(i ->
		{
			String linkId = i.hasLinkIdElement() && i.getLinkIdElement().hasValue() ? i.getLinkIdElement().getValue()
					: null;
			String text = i.hasTextElement() && i.getTextElement().hasValue() ? i.getTextElement().getValue() : null;
			String type = i.hasTypeElement() && i.getTypeElement().hasCode() ? i.getTypeElement().getCode() : null;

			return new Item(linkId, text, type);
		}).toList() : null;

		return new Element(description, item);
	}
}
