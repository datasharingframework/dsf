package dev.dsf.common.auth.logout;

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

	@GET
	public void logout(@Context UriInfo uri, @Context HttpServletRequest request) throws ServletException
	{
		request.logout();
	}
}
