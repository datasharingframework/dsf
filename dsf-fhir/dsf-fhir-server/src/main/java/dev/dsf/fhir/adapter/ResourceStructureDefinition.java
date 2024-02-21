package dev.dsf.fhir.adapter;

import org.hl7.fhir.r4.model.StructureDefinition;

public class ResourceStructureDefinition extends AbstractMetdataResource<StructureDefinition>
{
	private record Element(String description)
	{
	}

	public ResourceStructureDefinition()
	{
		super(StructureDefinition.class);
	}

	@Override
	protected Element toElement(StructureDefinition resource)
	{
		String description = getString(resource, StructureDefinition::hasDescriptionElement,
				StructureDefinition::getDescriptionElement);

		return new Element(description);
	}
}
