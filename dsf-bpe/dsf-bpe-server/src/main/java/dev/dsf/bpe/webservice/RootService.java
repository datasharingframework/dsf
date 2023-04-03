package dev.dsf.bpe.webservice;

import java.security.Principal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.common.auth.conf.Identity;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;

@Path(RootService.PATH)
@Produces({ MediaType.TEXT_HTML })
public class RootService
{
	public static final String PATH = "";

	private static final Logger logger = LoggerFactory.getLogger(RootService.class);

	@GET
	public Response root(@Context UriInfo uri, @Context SecurityContext securityContext)
	{
		logger.trace("GET {}", uri.getRequestUri().toString());

		StringBuilder out = new StringBuilder();
		out.append("""
				<html>
				<head>
				<title>BPE Root</title>
				</head>
				<body>
				<span>Hello, ${user}</span>
				""".replace("${user}", getDisplayName(securityContext)));

		if ("OPENID".equals(securityContext.getAuthenticationScheme()))
		{
			final String basePath = uri.getBaseUri().getRawPath();

			out.append("""
						<a href="${basePath}logout">Logout</a>
					""".replace("${basePath}", basePath));
		}

		out.append("""
				</body>
				</html>
				""");

		return Response.ok(out.toString()).build();
	}

	private String getDisplayName(SecurityContext securityContext)
	{
		Principal userPrincipal = securityContext.getUserPrincipal();
		if (userPrincipal != null && userPrincipal instanceof Identity)
		{
			Identity identity = (Identity) userPrincipal;
			return identity.getDisplayName();
		}
		else
			return "?";

	}
}
