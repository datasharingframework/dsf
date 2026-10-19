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
package dev.dsf.common.status.webservice;

import java.util.List;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.common.status.health.HealthCheck;
import dev.dsf.common.status.health.HealthStatus;
import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriInfo;

@Path(StatusService.PATH)
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@RolesAllowed("STATUS_PORT_ROLE")
public class StatusService
{
	public static final String PATH = "status";

	private static final Logger logger = LoggerFactory.getLogger(StatusService.class);

	private final int statusConnectorPort;
	private final List<HealthCheck> healthChecks;

	public StatusService(int statusConnectorPort, List<HealthCheck> healthChecks)
	{
		this.statusConnectorPort = statusConnectorPort;
		this.healthChecks = healthChecks == null ? List.of() : List.copyOf(healthChecks);
	}

	@GET
	public Response status(@Context UriInfo uri, @Context HttpHeaders headers, @Context HttpServletRequest request)
	{
		if (request.getLocalPort() != statusConnectorPort)
		{
			logger.warn("Sending '401 Unauthorized' request not on status port {}", statusConnectorPort);
			return Response.status(Status.UNAUTHORIZED).build();
		}

		if (healthChecks.stream().map(HealthCheck::check).allMatch(HealthStatus::ok))
			return Response.ok().build();
		else
		{
			healthChecks.stream().map(HealthCheck::check).filter(Predicate.not(HealthStatus::ok))
					.forEach(s -> logger.debug("{}: {}", s.component(), s.message()));

			return Response.status(Status.SERVICE_UNAVAILABLE).build();
		}
	}
}
