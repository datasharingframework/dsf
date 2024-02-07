package dev.dsf.fhir.adapter;

import java.util.List;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Library;

public class ResourceLibrary extends AbstractMetdataResource<Library>
{
	private record Element(String subtitle, String description, List<String> type)
	{
	}

	public ResourceLibrary()
	{
		super(Library.class);
	}

	@Override
	protected Element toElement(Library resource)
	{
		String subtitle = getString(resource, Library::hasSubtitleElement, Library::getSubtitleElement);
		String description = getString(resource, Library::hasDescriptionElement, Library::getDescriptionElement);
		List<String> type = resource.hasType() && resource.getType().hasCoding()
				? resource.getType().getCoding().stream().filter(Coding::hasSystemElement)
						.filter(Coding::hasCodeElement).filter(c -> c.getSystemElement().hasValue())
						.filter(c -> c.getCodeElement().hasValue())
						.map(c -> c.getSystemElement().getValue() + " | " + c.getCodeElement().getValue()).toList()
				: null;

		return new Element(subtitle, description, type);
	}
}
