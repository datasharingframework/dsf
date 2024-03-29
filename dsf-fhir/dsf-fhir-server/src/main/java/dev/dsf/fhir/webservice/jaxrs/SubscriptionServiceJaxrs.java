package dev.dsf.fhir.webservice.jaxrs;

import org.hl7.fhir.r4.model.Subscription;

import dev.dsf.fhir.webservice.specification.SubscriptionService;
import jakarta.ws.rs.Path;

@Path(SubscriptionServiceJaxrs.PATH)
public class SubscriptionServiceJaxrs extends AbstractResourceServiceJaxrs<Subscription, SubscriptionService>
		implements SubscriptionService
{
	public static final String PATH = "Subscription";

	public SubscriptionServiceJaxrs(SubscriptionService delegate)
	{
		super(delegate);
	}
}
