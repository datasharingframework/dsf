package dev.dsf.fhir.webservice.secure;

import org.hl7.fhir.r4.model.Subscription;

import dev.dsf.fhir.authorization.AuthorizationRule;
import dev.dsf.fhir.dao.SubscriptionDao;
import dev.dsf.fhir.help.ExceptionHandler;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.service.DefaultProfileProvider;
import dev.dsf.fhir.service.ReferenceCleaner;
import dev.dsf.fhir.service.ReferenceExtractor;
import dev.dsf.fhir.service.ReferenceResolver;
import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.validation.ValidationRules;
import dev.dsf.fhir.webservice.specification.SubscriptionService;

public class SubscriptionServiceSecure extends
		AbstractResourceServiceSecure<SubscriptionDao, Subscription, SubscriptionService> implements SubscriptionService
{
	public SubscriptionServiceSecure(SubscriptionService delegate, String serverBase,
			ResponseGenerator responseGenerator, ReferenceResolver referenceResolver, ReferenceCleaner referenceCleaner,
			ReferenceExtractor referenceExtractor, SubscriptionDao subscriptionDao, ExceptionHandler exceptionHandler,
			ParameterConverter parameterConverter, AuthorizationRule<Subscription> authorizationRule,
			ResourceValidator resourceValidator, ValidationRules validationRules, DefaultProfileProvider defaultProfileProvider)
	{
		super(delegate, serverBase, responseGenerator, referenceResolver, referenceCleaner, referenceExtractor,
				Subscription.class, subscriptionDao, exceptionHandler, parameterConverter, authorizationRule,
				resourceValidator, validationRules, defaultProfileProvider);
	}
}
