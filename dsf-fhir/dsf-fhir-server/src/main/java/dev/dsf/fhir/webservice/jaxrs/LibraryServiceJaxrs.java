package dev.dsf.fhir.webservice.jaxrs;

import org.hl7.fhir.r4.model.Library;

import dev.dsf.fhir.webservice.specification.LibraryService;
import jakarta.ws.rs.Path;

@Path(LibraryServiceJaxrs.PATH)
public class LibraryServiceJaxrs extends AbstractResourceServiceJaxrs<Library, LibraryService> implements LibraryService
{
	public static final String PATH = "Library";

	public LibraryServiceJaxrs(LibraryService delegate)
	{
		super(delegate);
	}
}
