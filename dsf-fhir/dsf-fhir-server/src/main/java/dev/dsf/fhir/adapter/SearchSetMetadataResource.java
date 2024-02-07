package dev.dsf.fhir.adapter;

import org.hl7.fhir.r4.model.MetadataResource;

public class SearchSetMetadataResource<M extends MetadataResource> extends AbstractSearchSet<M>
{
	private record Row(ElementId id, String status, String urlVersion, String titleOrName, String lastUpdated)
	{
	}

	public SearchSetMetadataResource(int defaultPageCount, Class<M> matchResourceType)
	{
		super(defaultPageCount, matchResourceType, "searchsetMetadataResource");
	}

	@Override
	protected Row toRow(ElementId id, M resource)
	{
		String status = resource.hasStatus() ? resource.getStatus().toCode() : "";

		String urlVersion = (resource.hasUrlElement() && resource.getUrlElement().hasValue()
				? resource.getUrlElement().getValue()
				: "")
				+ " | "
				+ (resource.hasVersionElement() && resource.getVersionElement().hasValue()
						? resource.getVersionElement().getValue()
						: "");

		String titleOrName = resource.hasTitleElement() && resource.getTitleElement().hasValue()
				? resource.getTitleElement().getValue()
				: resource.hasNameElement() && resource.getNameElement().hasValue()
						? resource.getNameElement().getValue()
						: "";

		String lastUpdated = formatLastUpdated(resource);

		return new Row(id, status, urlVersion, titleOrName, lastUpdated);
	}
}
