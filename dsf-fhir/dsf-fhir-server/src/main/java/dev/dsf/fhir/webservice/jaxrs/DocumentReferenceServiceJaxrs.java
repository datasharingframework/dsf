package dev.dsf.fhir.webservice.jaxrs;

import org.hl7.fhir.r4.model.DocumentReference;

import dev.dsf.fhir.webservice.specification.DocumentReferenceService;
import jakarta.ws.rs.Path;

@Path(DocumentReferenceServiceJaxrs.PATH)
public class DocumentReferenceServiceJaxrs extends
		AbstractResourceServiceJaxrs<DocumentReference, DocumentReferenceService> implements DocumentReferenceService
{
	public static final String PATH = "DocumentReference";

	public DocumentReferenceServiceJaxrs(DocumentReferenceService delegate)
	{
		super(delegate);
	}
}
