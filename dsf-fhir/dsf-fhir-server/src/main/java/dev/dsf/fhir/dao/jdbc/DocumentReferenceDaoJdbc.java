package dev.dsf.fhir.dao.jdbc;

import java.util.List;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.DocumentReference;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.fhir.dao.DocumentReferenceDao;
import dev.dsf.fhir.search.filter.DocumentReferenceIdentityFilter;
import dev.dsf.fhir.search.parameters.DocumentReferenceIdentifier;

public class DocumentReferenceDaoJdbc extends AbstractResourceDaoJdbc<DocumentReference> implements DocumentReferenceDao
{
	public DocumentReferenceDaoJdbc(DataSource dataSource, DataSource permanentDeleteDataSource,
			FhirContext fhirContext)
	{
		super(dataSource, permanentDeleteDataSource, fhirContext, DocumentReference.class, "document_references",
				"document_reference", "document_reference_id", DocumentReferenceIdentityFilter::new,
				List.of(factory(DocumentReferenceIdentifier.PARAMETER_NAME, DocumentReferenceIdentifier::new,
						DocumentReferenceIdentifier.getNameModifiers())),
				List.of());
	}

	@Override
	protected DocumentReference copy(DocumentReference resource)
	{
		return resource.copy();
	}
}
