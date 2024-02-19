package dev.dsf.fhir.dao;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.hl7.fhir.r4.model.DocumentReference;

import dev.dsf.fhir.dao.jdbc.DocumentReferenceDaoJdbc;

public class DocumentReferenceDaoTest extends AbstractReadAccessDaoTest<DocumentReference, DocumentReferenceDao>
{
	private static final String description = "Demo DocumentReference Description";
	private static final Date date = new Date();

	public DocumentReferenceDaoTest()
	{
		super(DocumentReference.class, DocumentReferenceDaoJdbc::new);
	}

	@Override
	public DocumentReference createResource()
	{
		DocumentReference documentReference = new DocumentReference();
		documentReference.setDescription(description);
		return documentReference;
	}

	@Override
	protected void checkCreated(DocumentReference resource)
	{
		assertEquals(description, resource.getDescription());
	}

	@Override
	protected DocumentReference updateResource(DocumentReference resource)
	{
		resource.setDate(date);
		return resource;
	}

	@Override
	protected void checkUpdates(DocumentReference resource)
	{
		assertEquals(date, resource.getDate());
	}
}
