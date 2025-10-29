package dev.dsf.fhir.webservice.secure;

import org.hl7.fhir.r4.model.Location;

import dev.dsf.fhir.authorization.AuthorizationRule;
import dev.dsf.fhir.dao.LocationDao;
import dev.dsf.fhir.help.ExceptionHandler;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.service.DefaultProfileProvider;
import dev.dsf.fhir.service.ReferenceCleaner;
import dev.dsf.fhir.service.ReferenceExtractor;
import dev.dsf.fhir.service.ReferenceResolver;
import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.validation.ValidationRules;
import dev.dsf.fhir.webservice.specification.LocationService;

public class LocationServiceSecure extends AbstractResourceServiceSecure<LocationDao, Location, LocationService>
		implements LocationService
{
	public LocationServiceSecure(LocationService delegate, String serverBase, ResponseGenerator responseGenerator,
			ReferenceResolver referenceResolver, ReferenceCleaner referenceCleaner,
			ReferenceExtractor referenceExtractor, LocationDao locationDao, ExceptionHandler exceptionHandler,
			ParameterConverter parameterConverter, AuthorizationRule<Location> authorizationRule,
			ResourceValidator resourceValidator, ValidationRules validationRules,
			DefaultProfileProvider defaultProfileProvider)
	{
		super(delegate, serverBase, responseGenerator, referenceResolver, referenceCleaner, referenceExtractor,
				Location.class, locationDao, exceptionHandler, parameterConverter, authorizationRule, resourceValidator,
				validationRules, defaultProfileProvider);
	}
}
