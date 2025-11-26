/*
 * Copyright 2018-2025 Heilbronn University of Applied Sciences
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.dsf.fhir.webservice.jaxrs;

import org.hl7.fhir.r4.model.Bundle;

import ca.uhn.fhir.rest.api.Constants;
import dev.dsf.fhir.webservice.specification.RootService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path(RootServiceJaxrs.PATH)
@Produces({ Constants.CT_FHIR_XML, Constants.CT_FHIR_XML_NEW, MediaType.APPLICATION_XML, Constants.CT_FHIR_JSON,
		Constants.CT_FHIR_JSON_NEW, MediaType.APPLICATION_JSON, MediaType.TEXT_HTML })
public class RootServiceJaxrs extends AbstractServiceJaxrs<RootService> implements RootService
{
	public static final String PATH = "";

	public RootServiceJaxrs(RootService delegate)
	{
		super(delegate);
	}

	@GET
	@Override
	public Response root(@Context UriInfo uri, @Context HttpHeaders headers)
	{
		return delegate.root(uri, headers);
	}

	@GET
	@Path("/_history")
	@Override
	public Response history(@Context UriInfo uri, @Context HttpHeaders headers)
	{
		return delegate.history(uri, headers);
	}

	@POST
	@Consumes({ Constants.CT_FHIR_XML, Constants.CT_FHIR_XML_NEW, MediaType.APPLICATION_XML, Constants.CT_FHIR_JSON,
			Constants.CT_FHIR_JSON_NEW, MediaType.APPLICATION_JSON })
	@Override
	public Response handleBundle(Bundle bundle, @Context UriInfo uri, @Context HttpHeaders headers)
	{
		return delegate.handleBundle(bundle, uri, headers);
	}
}
