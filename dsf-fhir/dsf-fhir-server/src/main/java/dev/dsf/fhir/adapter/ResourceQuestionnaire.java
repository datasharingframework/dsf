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
