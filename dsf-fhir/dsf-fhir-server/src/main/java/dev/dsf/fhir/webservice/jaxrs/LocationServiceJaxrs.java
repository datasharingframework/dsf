package dev.dsf.fhir.webservice.jaxrs;

import org.hl7.fhir.r4.model.Location;

import dev.dsf.fhir.webservice.specification.LocationService;
import jakarta.ws.rs.Path;

@Path(LocationServiceJaxrs.PATH)
public class LocationServiceJaxrs extends AbstractResourceServiceJaxrs<Location, LocationService>
		implements LocationService
{
	public static final String PATH = "Location";

	public LocationServiceJaxrs(LocationService delegate)
	{
		super(delegate);
	}
}
