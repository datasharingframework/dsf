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

import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StructureDefinition;

import dev.dsf.fhir.webservice.specification.StructureDefinitionService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path(StructureDefinitionServiceJaxrs.PATH)
public class StructureDefinitionServiceJaxrs
		extends AbstractResourceServiceJaxrs<StructureDefinition, StructureDefinitionService>
		implements StructureDefinitionService
{
	public static final String PATH = "StructureDefinition";

	public StructureDefinitionServiceJaxrs(StructureDefinitionService delegate)
	{
		super(delegate);
	}

	@POST
	@Path("/{snapshot : [$]snapshot(/)?}")
	@Override
	public Response postSnapshotNew(@PathParam("snapshot") String snapshotPath, Parameters parameters,
			@Context UriInfo uri, @Context HttpHeaders headers)
	{
		return delegate.postSnapshotNew(snapshotPath, parameters, uri, headers);
	}

	@GET
	@Path("/{snapshot : [$]snapshot(/)?}")
	@Override
	public Response getSnapshotNew(@PathParam("snapshot") String snapshotPath, @Context UriInfo uri,
			@Context HttpHeaders headers)
	{
		return delegate.getSnapshotNew(snapshotPath, uri, headers);
	}

	@POST
	@Path("/{id}/{snapshot : [$]snapshot(/)?}")
	@Override
	public Response postSnapshotExisting(@PathParam("snapshot") String snapshotPath, @PathParam("id") String id,
			@Context UriInfo uri, @Context HttpHeaders headers)
	{
		return delegate.postSnapshotExisting(snapshotPath, id, uri, headers);
	}

	@GET
	@Path("/{id}/{snapshot : [$]snapshot(/)?}")
	@Override
	public Response getSnapshotExisting(@PathParam("snapshot") String snapshotPath, @PathParam("id") String id,
			@Context UriInfo uri, @Context HttpHeaders headers)
	{
		return delegate.getSnapshotExisting(snapshotPath, id, uri, headers);
	}
}
