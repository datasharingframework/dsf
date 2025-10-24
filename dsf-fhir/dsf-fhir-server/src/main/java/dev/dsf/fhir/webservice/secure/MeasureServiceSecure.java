package dev.dsf.fhir.webservice.secure;

import org.hl7.fhir.r4.model.Measure;

import dev.dsf.fhir.authorization.AuthorizationRule;
import dev.dsf.fhir.dao.MeasureDao;
import dev.dsf.fhir.help.ExceptionHandler;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.service.ReferenceCleaner;
import dev.dsf.fhir.service.ReferenceExtractor;
import dev.dsf.fhir.service.ReferenceResolver;
import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.validation.ValidationRules;
import dev.dsf.fhir.webservice.specification.MeasureService;

public class MeasureServiceSecure extends AbstractResourceServiceSecure<MeasureDao, Measure, MeasureService>
		implements MeasureService
{
	public MeasureServiceSecure(MeasureService delegate, String serverBase, ResponseGenerator responseGenerator,
			ReferenceResolver referenceResolver, ReferenceCleaner referenceCleaner,
			ReferenceExtractor referenceExtractor, MeasureDao measureDao, ExceptionHandler exceptionHandler,
			ParameterConverter parameterConverter, AuthorizationRule<Measure> authorizationRule,
			ResourceValidator resourceValidator, ValidationRules validationRules)
	{
		super(delegate, serverBase, responseGenerator, referenceResolver, referenceCleaner, referenceExtractor,
				Measure.class, measureDao, exceptionHandler, parameterConverter, authorizationRule, resourceValidator,
				validationRules);
	}
}
