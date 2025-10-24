package dev.dsf.fhir.adapter;

import org.hl7.fhir.r4.model.DocumentReference;

public class SearchSetDocumentReference extends AbstractSearchSet<DocumentReference>
{
	private record Row(ElementId id, String masterIdentifier, String author, String status, String docStatus,
			String lastUpdated)
	{
	}

	public SearchSetDocumentReference(int defaultPageCount)
	{
		super(defaultPageCount, DocumentReference.class);
	}

	@Override
	protected Row toRow(ElementId id, DocumentReference resource)
	{
		String masterIdentifier = getIdentifierValue(resource, DocumentReference::hasMasterIdentifier,
				DocumentReference::getMasterIdentifier);
		String author = getReferenceIdentifierValues(resource, DocumentReference::hasAuthor,
				DocumentReference::getAuthor);
		String status = resource.hasStatus() ? resource.getStatus().toCode() : "";
		String docStatus = resource.hasDocStatus() ? resource.getDocStatus().toCode() : "";
		String lastUpdated = formatLastUpdated(resource);

		return new Row(id, masterIdentifier, author, status, docStatus, lastUpdated);
	}
}
