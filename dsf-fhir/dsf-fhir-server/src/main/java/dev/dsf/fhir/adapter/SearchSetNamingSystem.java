package dev.dsf.fhir.adapter;

import org.hl7.fhir.r4.model.NamingSystem;

public class SearchSetNamingSystem extends AbstractSearchSet<NamingSystem>
{
	private record Row(ElementId id, String status, String uniqueId, String name, String lastUpdated)
	{
	}

	public SearchSetNamingSystem(int defaultPageCount)
	{
		super(defaultPageCount, NamingSystem.class);
	}

	@Override
	protected Row toRow(ElementId id, NamingSystem resource)
	{
		String status = resource.hasStatus() ? resource.getStatus().toCode() : "";

		String uniqueId = resource.hasUniqueId() && resource.getUniqueIdFirstRep().hasValueElement()
				&& resource.getUniqueIdFirstRep().getValueElement().hasValue()
						? resource.getUniqueIdFirstRep().getValueElement().getValue()
						: "";

		if (resource.hasUniqueId() && resource.getUniqueId().size() > 1 && !uniqueId.isBlank())
			uniqueId += ", ...";

		String name = resource.hasNameElement() && resource.getNameElement().hasValue()
				? resource.getNameElement().getValue()
				: "";

		String lastUpdated = formatLastUpdated(resource);

		return new Row(id, status, uniqueId, name, lastUpdated);
	}
}
