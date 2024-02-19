package dev.dsf.fhir.adapter;

import org.hl7.fhir.r4.model.NamingSystem;

public class ResourceNamingSystem extends AbstractMetdataResource<NamingSystem>
{
	private record Element(String description)
	{
	}

	public ResourceNamingSystem()
	{
		super(NamingSystem.class);
	}

	@Override
	protected Element toElement(NamingSystem resource)
	{
		String description = getString(resource, NamingSystem::hasDescriptionElement,
				NamingSystem::getDescriptionElement);

		return new Element(description);
	}
}
