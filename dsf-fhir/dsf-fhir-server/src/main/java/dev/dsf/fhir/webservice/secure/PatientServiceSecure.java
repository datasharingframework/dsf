package dev.dsf.fhir.webservice.secure;

import org.hl7.fhir.r4.model.Patient;

import dev.dsf.fhir.authorization.AuthorizationRule;
import dev.dsf.fhir.dao.PatientDao;
import dev.dsf.fhir.help.ExceptionHandler;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.service.DefaultProfileProvider;
import dev.dsf.fhir.service.ReferenceCleaner;
import dev.dsf.fhir.service.ReferenceExtractor;
import dev.dsf.fhir.service.ReferenceResolver;
import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.validation.ValidationRules;
import dev.dsf.fhir.webservice.specification.PatientService;

public class PatientServiceSecure extends AbstractResourceServiceSecure<PatientDao, Patient, PatientService>
		implements PatientService
{
	public PatientServiceSecure(PatientService delegate, String serverBase, ResponseGenerator responseGenerator,
			ReferenceResolver referenceResolver, ReferenceCleaner referenceCleaner,
			ReferenceExtractor referenceExtractor, PatientDao patientDao, ExceptionHandler exceptionHandler,
			ParameterConverter parameterConverter, AuthorizationRule<Patient> authorizationRule,
			ResourceValidator resourceValidator, ValidationRules validationRules, DefaultProfileProvider defaultProfileProvider)
	{
		super(delegate, serverBase, responseGenerator, referenceResolver, referenceCleaner, referenceExtractor,
				Patient.class, patientDao, exceptionHandler, parameterConverter, authorizationRule, resourceValidator,
				validationRules, defaultProfileProvider);
	}
}
