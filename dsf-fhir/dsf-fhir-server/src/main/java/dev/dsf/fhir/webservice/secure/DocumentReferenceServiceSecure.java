package dev.dsf.fhir.webservice.secure;

import org.hl7.fhir.r4.model.DocumentReference;

import dev.dsf.fhir.authorization.AuthorizationRule;
import dev.dsf.fhir.dao.DocumentReferenceDao;
import dev.dsf.fhir.help.ExceptionHandler;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.service.DefaultProfileProvider;
import dev.dsf.fhir.service.ReferenceCleaner;
import dev.dsf.fhir.service.ReferenceExtractor;
import dev.dsf.fhir.service.ReferenceResolver;
import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.validation.ValidationRules;
import dev.dsf.fhir.webservice.specification.DocumentReferenceService;

public class DocumentReferenceServiceSecure
		extends AbstractResourceServiceSecure<DocumentReferenceDao, DocumentReference, DocumentReferenceService>
		implements DocumentReferenceService
{
	public DocumentReferenceServiceSecure(DocumentReferenceService delegate, String serverBase,
			ResponseGenerator responseGenerator, ReferenceResolver referenceResolver, ReferenceCleaner referenceCleaner,
			ReferenceExtractor referenceExtractor, DocumentReferenceDao documentReferenceDao,
			ExceptionHandler exceptionHandler, ParameterConverter parameterConverter,
			AuthorizationRule<DocumentReference> authorizationRule, ResourceValidator resourceValidator,
			ValidationRules validationRules, DefaultProfileProvider defaultProfileProvider)
	{
		super(delegate, serverBase, responseGenerator, referenceResolver, referenceCleaner, referenceExtractor,
				DocumentReference.class, documentReferenceDao, exceptionHandler, parameterConverter, authorizationRule,
				resourceValidator, validationRules, defaultProfileProvider);
	}
}
