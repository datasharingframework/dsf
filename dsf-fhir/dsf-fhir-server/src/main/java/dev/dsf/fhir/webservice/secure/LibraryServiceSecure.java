package dev.dsf.fhir.webservice.secure;

import org.hl7.fhir.r4.model.Library;

import dev.dsf.fhir.authorization.AuthorizationRule;
import dev.dsf.fhir.dao.LibraryDao;
import dev.dsf.fhir.help.ExceptionHandler;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.service.ReferenceCleaner;
import dev.dsf.fhir.service.ReferenceExtractor;
import dev.dsf.fhir.service.ReferenceResolver;
import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.webservice.specification.LibraryService;

public class LibraryServiceSecure extends AbstractResourceServiceSecure<LibraryDao, Library, LibraryService>
		implements LibraryService
{
	public LibraryServiceSecure(LibraryService delegate, String serverBase, ResponseGenerator responseGenerator,
			ReferenceResolver referenceResolver, ReferenceCleaner referenceCleaner,
			ReferenceExtractor referenceExtractor, LibraryDao libraryDao, ExceptionHandler exceptionHandler,
			ParameterConverter parameterConverter, AuthorizationRule<Library> authorizationRule,
			ResourceValidator resourceValidator)
	{
		super(delegate, serverBase, responseGenerator, referenceResolver, referenceCleaner, referenceExtractor,
				Library.class, libraryDao, exceptionHandler, parameterConverter, authorizationRule, resourceValidator);
	}
}
