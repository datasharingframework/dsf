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
