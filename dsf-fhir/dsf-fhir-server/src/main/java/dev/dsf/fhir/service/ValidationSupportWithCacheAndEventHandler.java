package dev.dsf.fhir.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.IValidationSupport;
import dev.dsf.fhir.event.Event;
import dev.dsf.fhir.event.EventHandler;
import dev.dsf.fhir.event.ResourceCreatedEvent;
import dev.dsf.fhir.event.ResourceDeletedEvent;
import dev.dsf.fhir.event.ResourceUpdatedEvent;
import dev.dsf.fhir.validation.ValidationSupportWithCache;

public class ValidationSupportWithCacheAndEventHandler extends ValidationSupportWithCache
		implements IValidationSupport, EventHandler
{
	public ValidationSupportWithCacheAndEventHandler(FhirContext context, IValidationSupport delegate)
	{
		super(context, delegate);
	}

	@Override
	public void handleEvent(Event event)
	{
		if (event == null)
			return;

		if (event instanceof ResourceCreatedEvent && resourceSupported(event.getResource()))
			add(event.getResource());
		else if (event instanceof ResourceDeletedEvent && resourceSupported(event.getResourceType(), event.getId()))
			remove(event.getResourceType(), event.getId());
		else if (event instanceof ResourceUpdatedEvent && resourceSupported(event.getResource()))
			update(event.getResource());
	}
}
