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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

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
public class StatusService implements InitializingBean
{
	public static final String PATH = "status";

	private static final Logger logger = LoggerFactory.getLogger(StatusService.class);

	private final DataSource dataSource;
	private final int statusConnectorPort;

	public StatusService(DataSource dataSource, int statusConnectorPort)
	{
		this.dataSource = dataSource;
		this.statusConnectorPort = statusConnectorPort;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(dataSource, "dataSource");
	}

	@GET
	public Response status(@Context UriInfo uri, @Context HttpHeaders headers, @Context HttpServletRequest request)
	{
		if (request.getLocalPort() != statusConnectorPort)
		{
			logger.warn("Sending '401 Unauthorized' request not on status port {}", statusConnectorPort);
			return Response.status(Status.UNAUTHORIZED).build();
		}

		try (Connection _ = dataSource.getConnection())
		{
			return Response.ok().build();
		}
		catch (SQLException e)
		{
			String errorMessage = getErrorMessage(e);

			logger.debug("Error while accessing DB", e);
			logger.error("Error while accessing DB: {}", errorMessage);

			return Response.serverError().entity(errorMessage).build();
		}
	}

	private String getErrorMessage(Throwable e)
	{
		StringBuilder b = new StringBuilder();

		while (true)
		{
			b.append(e.getClass().getSimpleName());
			b.append(": ");
			b.append(e.getMessage());

			if (e.getCause() != null)
			{
				b.append(" -> ");
				e = e.getCause();
			}
			else
				break;
		}

		return b.toString();
	}
}
