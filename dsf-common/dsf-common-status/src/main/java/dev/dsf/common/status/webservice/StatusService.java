package dev.dsf.common.status.webservice;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

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
public class StatusService implements InitializingBean
{
	public static final String PATH = "status";

	private static final Logger logger = LoggerFactory.getLogger(StatusService.class);

	private final BasicDataSource dataSource;
	private final int statusConnectorPort;

	public StatusService(BasicDataSource dataSource, int statusConnectorPort)
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
		logger.trace("GET {}, Local port {}", uri.getRequestUri().toString(), request.getLocalPort());

		if (request.getLocalPort() != statusConnectorPort)
		{
			logger.warn("Sending '401 Unauthorized' request not on status port {}", statusConnectorPort);
			return Response.status(Status.UNAUTHORIZED).build();
		}

		try (Connection connection = dataSource.getConnection())
		{
			return Response.ok().build();
		}
		catch (SQLException e)
		{
			logger.error("Error while accessing DB", e);
			return Response.serverError().build();
		}
	}
}
