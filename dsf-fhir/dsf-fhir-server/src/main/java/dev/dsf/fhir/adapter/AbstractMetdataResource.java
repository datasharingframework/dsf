package dev.dsf.fhir.adapter;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.MetadataResource;

public abstract class AbstractMetdataResource<R extends MetadataResource> extends AbstractResource<R>
{
	private record Element(String urlAndVersion, List<ElementSystemValue> identifier, String name, String title,
			Boolean experimental, String date)
	{
	}

	private final Predicate<R> hasIdentifier;
	private final Function<R, List<Identifier>> getIdentifier;

	public AbstractMetdataResource(Class<R> resourceType)
	{
		this(resourceType, null, null);
	}

	public AbstractMetdataResource(Class<R> resourceType, Predicate<R> hasIdentifier,
			Function<R, List<Identifier>> getIdentifier)
	{
		super(resourceType,
				ActiveOrStatus.status(MetadataResource::hasStatusElement, MetadataResource::getStatusElement));

		this.hasIdentifier = hasIdentifier;
		this.getIdentifier = getIdentifier;
	}

	@Override
	protected void doSetAdditionalVariables(BiConsumer<String, Object> variables, R resource)
	{
		String url = getUri(resource, MetadataResource::hasUrlElement, MetadataResource::getUrlElement);
		String version = getString(resource, MetadataResource::hasVersionElement, MetadataResource::getVersionElement);
		String urlAndVersion = (url != null ? url : "") + " | " + (version != null ? version : "");
		List<ElementSystemValue> identifier = hasIdentifier != null && getIdentifier != null
				? getIdentifiers(resource, hasIdentifier, getIdentifier)
				: null;
		String name = getString(resource, MetadataResource::hasNameElement, MetadataResource::getNameElement);
		String title = getString(resource, MetadataResource::hasTitleElement, MetadataResource::getTitleElement);
		Boolean experimental = getBoolean(resource, MetadataResource::hasExperimentalElement,
				MetadataResource::getExperimentalElement);
		String date = getDateTime(resource, MetadataResource::hasDateElement, MetadataResource::getDateElement);

		variables.accept("metadataResource", new Element(urlAndVersion, identifier, name, title, experimental, date));
	}
}
