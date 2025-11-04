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

import org.hl7.fhir.r4.model.Resource;
import org.springframework.beans.factory.InitializingBean;

import ca.uhn.fhir.rest.api.Constants;
import dev.dsf.fhir.webservice.specification.BasicResourceService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

public abstract class AbstractResourceServiceJaxrs<R extends Resource, S extends BasicResourceService<R>>
		extends AbstractServiceJaxrs<S> implements BasicResourceService<R>, InitializingBean
{
	public AbstractResourceServiceJaxrs(S delegate)
	{
		super(delegate);
	}

	@POST
	@Consumes({ Constants.CT_FHIR_XML, Constants.CT_FHIR_XML_NEW, MediaType.APPLICATION_XML, Constants.CT_FHIR_JSON,
			Constants.CT_FHIR_JSON_NEW, MediaType.APPLICATION_JSON })
	@Produces({ Constants.CT_FHIR_XML, Constants.CT_FHIR_XML_NEW, MediaType.APPLICATION_XML, Constants.CT_FHIR_JSON,
			Constants.CT_FHIR_JSON_NEW, MediaType.APPLICATION_JSON, MediaType.TEXT_HTML })
	@Override
	public Response create(R resource, @Context UriInfo uri, @Context HttpHeaders headers)
	{
		return delegate.create(resource, uri, headers);
	}

	@GET
	@Path("/{id}")
	@Produces({ Constants.CT_FHIR_XML, Constants.CT_FHIR_XML_NEW, MediaType.APPLICATION_XML, Constants.CT_FHIR_JSON,
			Constants.CT_FHIR_JSON_NEW, MediaType.APPLICATION_JSON, MediaType.TEXT_HTML })
	@Override
	public Response read(@PathParam("id") String id, @Context UriInfo uri, @Context HttpHeaders headers)
	{
		return delegate.read(id, uri, headers);
	}

	@GET
	@Path("/{id}/_history/{version}")
	@Produces({ Constants.CT_FHIR_XML, Constants.CT_FHIR_XML_NEW, MediaType.APPLICATION_XML, Constants.CT_FHIR_JSON,
			Constants.CT_FHIR_JSON_NEW, MediaType.APPLICATION_JSON, MediaType.TEXT_HTML })
	@Override
	public Response vread(@PathParam("id") String id, @PathParam("version") long version, @Context UriInfo uri,
			@Context HttpHeaders headers)
	{
		return delegate.vread(id, version, uri, headers);
	}

	@GET
	@Path("/_history")
	@Produces({ Constants.CT_FHIR_XML, Constants.CT_FHIR_XML_NEW, MediaType.APPLICATION_XML, Constants.CT_FHIR_JSON,
			Constants.CT_FHIR_JSON_NEW, MediaType.APPLICATION_JSON, MediaType.TEXT_HTML })
	@Override
	public Response history(@Context UriInfo uri, @Context HttpHeaders headers)
	{
		return delegate.history(uri, headers);
	}

	@GET
	@Path("/{id}/_history")
	@Produces({ Constants.CT_FHIR_XML, Constants.CT_FHIR_XML_NEW, MediaType.APPLICATION_XML, Constants.CT_FHIR_JSON,
			Constants.CT_FHIR_JSON_NEW, MediaType.APPLICATION_JSON, MediaType.TEXT_HTML })
	@Override
	public Response history(@PathParam("id") String id, @Context UriInfo uri, @Context HttpHeaders headers)
	{
		return delegate.history(id, uri, headers);
	}

	@PUT
	@Path("/{id}")
	@Consumes({ Constants.CT_FHIR_XML, Constants.CT_FHIR_XML_NEW, MediaType.APPLICATION_XML, Constants.CT_FHIR_JSON,
			Constants.CT_FHIR_JSON_NEW, MediaType.APPLICATION_JSON })
	@Produces({ Constants.CT_FHIR_XML, Constants.CT_FHIR_XML_NEW, MediaType.APPLICATION_XML, Constants.CT_FHIR_JSON,
			Constants.CT_FHIR_JSON_NEW, MediaType.APPLICATION_JSON, MediaType.TEXT_HTML })
	@Override
	public Response update(@PathParam("id") String id, R resource, @Context UriInfo uri, @Context HttpHeaders headers)
	{
		return delegate.update(id, resource, uri, headers);
	}

	@PUT
	@Consumes({ Constants.CT_FHIR_XML, Constants.CT_FHIR_XML_NEW, MediaType.APPLICATION_XML, Constants.CT_FHIR_JSON,
			Constants.CT_FHIR_JSON_NEW, MediaType.APPLICATION_JSON })
	@Produces({ Constants.CT_FHIR_XML, Constants.CT_FHIR_XML_NEW, MediaType.APPLICATION_XML, Constants.CT_FHIR_JSON,
			Constants.CT_FHIR_JSON_NEW, MediaType.APPLICATION_JSON, MediaType.TEXT_HTML })
	@Override
	public Response update(R resource, @Context UriInfo uri, @Context HttpHeaders headers)
	{
		return delegate.update(resource, uri, headers);
	}

	@DELETE
	@Path("/{id}")
	@Consumes({ Constants.CT_FHIR_XML, Constants.CT_FHIR_XML_NEW, MediaType.APPLICATION_XML, Constants.CT_FHIR_JSON,
			Constants.CT_FHIR_JSON_NEW, MediaType.APPLICATION_JSON })
	@Produces({ Constants.CT_FHIR_XML, Constants.CT_FHIR_XML_NEW, MediaType.APPLICATION_XML, Constants.CT_FHIR_JSON,
			Constants.CT_FHIR_JSON_NEW, MediaType.APPLICATION_JSON, MediaType.TEXT_HTML })
	@Override
	public Response delete(@PathParam("id") String id, @Context UriInfo uri, @Context HttpHeaders headers)
	{
		return delegate.delete(id, uri, headers);
	}

	@DELETE
	@Consumes({ Constants.CT_FHIR_XML, Constants.CT_FHIR_XML_NEW, MediaType.APPLICATION_XML, Constants.CT_FHIR_JSON,
			Constants.CT_FHIR_JSON_NEW, MediaType.APPLICATION_JSON })
	@Produces({ Constants.CT_FHIR_XML, Constants.CT_FHIR_XML_NEW, MediaType.APPLICATION_XML, Constants.CT_FHIR_JSON,
			Constants.CT_FHIR_JSON_NEW, MediaType.APPLICATION_JSON, MediaType.TEXT_HTML })
	@Override
	public Response delete(@Context UriInfo uri, @Context HttpHeaders headers)
	{
		return delegate.delete(uri, headers);
	}

	@GET
	@Produces({ Constants.CT_FHIR_XML, Constants.CT_FHIR_XML_NEW, MediaType.APPLICATION_XML, Constants.CT_FHIR_JSON,
			Constants.CT_FHIR_JSON_NEW, MediaType.APPLICATION_JSON, MediaType.TEXT_HTML })
	@Override
	public Response search(@Context UriInfo uri, @Context HttpHeaders headers)
	{
		return delegate.search(uri, headers);
	}

	@POST
	@Path("/{id}/{delete : [$]permanent-delete(/)?}")
	@Consumes({ Constants.CT_FHIR_JSON, Constants.CT_FHIR_JSON_NEW, MediaType.APPLICATION_JSON, Constants.CT_FHIR_XML,
			Constants.CT_FHIR_XML_NEW, MediaType.APPLICATION_XML })
	@Produces({ Constants.CT_FHIR_XML, Constants.CT_FHIR_XML_NEW, MediaType.APPLICATION_XML, Constants.CT_FHIR_JSON,
			Constants.CT_FHIR_JSON_NEW, MediaType.APPLICATION_JSON, MediaType.TEXT_HTML })
	@Override
	public Response deletePermanently(@PathParam("delete") String deletePath, @PathParam("id") String id,
			@Context UriInfo uri, @Context HttpHeaders headers)
	{
		return delegate.deletePermanently(deletePath, id, uri, headers);
	}
}
