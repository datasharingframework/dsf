package dev.dsf.fhir.adapter;

import java.util.List;

import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Measure;

public class ResourceMeasure extends AbstractMetdataResource<Measure>
{
	private record Element(String subtitle, String description, List<String> library, List<ElementSystemValue> scoring)
	{
	}

	public ResourceMeasure()
	{
		super(Measure.class);
	}

	@Override
	protected Element toElement(Measure resource)
	{
		String subtitle = getString(resource, Measure::hasSubtitleElement, Measure::getSubtitleElement);
		String description = getString(resource, Measure::hasDescriptionElement, Measure::getDescriptionElement);

		List<String> library = resource.hasLibrary()
				? resource.getLibrary().stream().filter(CanonicalType::hasValue).map(CanonicalType::getValue).toList()
				: null;

		List<ElementSystemValue> scoring = resource.hasScoring()
				? resource.getScoring().getCoding().stream().map(ElementSystemValue::from).toList()
				: null;

		return new Element(subtitle, description, library, scoring);
	}
}