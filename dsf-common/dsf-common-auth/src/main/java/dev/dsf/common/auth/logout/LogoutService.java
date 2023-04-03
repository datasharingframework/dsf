package dev.dsf.common.auth.logout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;

@Path(LogoutService.PATH)
public class LogoutService
{
	public static final String PATH = "logout";

	private static final Logger logger = LoggerFactory.getLogger(LogoutService.class);

	@GET
	public void logout(@Context UriInfo uri, @Context HttpServletRequest request) throws ServletException
	{
		logger.trace("GET {}", uri.getRequestUri().toString());

		request.logout();
	}
}
