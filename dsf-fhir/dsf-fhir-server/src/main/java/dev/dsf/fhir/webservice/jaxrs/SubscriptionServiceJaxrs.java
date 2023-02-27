package dev.dsf.fhir.webservice.jaxrs;

import javax.ws.rs.Path;

import org.hl7.fhir.r4.model.Subscription;

import dev.dsf.fhir.webservice.specification.SubscriptionService;

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
