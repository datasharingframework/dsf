package dev.dsf.fhir.webservice.secure;

import org.hl7.fhir.r4.model.Provenance;

import dev.dsf.fhir.authorization.AuthorizationRule;
import dev.dsf.fhir.dao.ProvenanceDao;
import dev.dsf.fhir.help.ExceptionHandler;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.service.DefaultProfileProvider;
import dev.dsf.fhir.service.ReferenceCleaner;
import dev.dsf.fhir.service.ReferenceExtractor;
import dev.dsf.fhir.service.ReferenceResolver;
import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.validation.ValidationRules;
import dev.dsf.fhir.webservice.specification.ProvenanceService;

public class ProvenanceServiceSecure extends AbstractResourceServiceSecure<ProvenanceDao, Provenance, ProvenanceService>
		implements ProvenanceService
{
	public ProvenanceServiceSecure(ProvenanceService delegate, String serverBase, ResponseGenerator responseGenerator,
			ReferenceResolver referenceResolver, ReferenceCleaner referenceCleaner,
			ReferenceExtractor referenceExtractor, ProvenanceDao provenanceDao, ExceptionHandler exceptionHandler,
			ParameterConverter parameterConverter, AuthorizationRule<Provenance> authorizationRule,
			ResourceValidator resourceValidator, ValidationRules validationRules,
			DefaultProfileProvider defaultProfileProvider)
	{
		super(delegate, serverBase, responseGenerator, referenceResolver, referenceCleaner, referenceExtractor,
				Provenance.class, provenanceDao, exceptionHandler, parameterConverter, authorizationRule,
				resourceValidator, validationRules, defaultProfileProvider);
	}
}
