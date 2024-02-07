package dev.dsf.fhir.adapter;

import org.hl7.fhir.r4.model.ValueSet;

public class ResourceValueSet extends AbstractMetdataResource<ValueSet>
{
	private record Element(String description)
	{
	}

	public ResourceValueSet()
	{
		super(ValueSet.class);
	}

	@Override
	protected Element toElement(ValueSet resource)
	{
		String description = getString(resource, ValueSet::hasDescriptionElement, ValueSet::getDescriptionElement);

		return new Element(description);
	}
}
