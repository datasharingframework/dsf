package dev.dsf.common.auth.logging;

import java.io.IOException;
import java.security.Principal;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.common.auth.conf.OrganizationIdentity;
import dev.dsf.common.auth.conf.PractitionerIdentity;
import jakarta.annotation.Priority;
import jakarta.ws.rs.ConstrainedTo;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.SecurityContext;

@ConstrainedTo(RuntimeType.SERVER)
@PreMatching
@Priority(Integer.MIN_VALUE)
public class CurrentUserLogger implements ContainerRequestFilter
{
	private static final Logger logger = LoggerFactory.getLogger(CurrentUserLogger.class);

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException
	{
		Identity identity = getCurrentIdentity(requestContext.getSecurityContext());

		if (identity instanceof OrganizationIdentity)
		{
			logger.debug("Current organization identity '{}', dsf-roles {}", identity.getName(),
					identity.getDsfRoles());
		}
		else if (identity instanceof PractitionerIdentity practitioner)
		{
			logger.debug("Current practitioner identity '{}', dsf-roles {}, practitioner-roles {}", identity.getName(),
					identity.getDsfRoles(), practitioner.getPractionerRoles().stream()
							.map(c -> c.getSystem() + "|" + c.getCode()).collect(Collectors.joining(", ", "[", "]")));
		}
	}

	private Identity getCurrentIdentity(SecurityContext context)
	{
		Principal principal = context.getUserPrincipal();
		if (principal != null)
		{
			if (principal instanceof Identity identity)
				return identity;
			else
			{
				logger.warn("Unknown current user principal of type {}", principal.getClass().getName());
				return null;
			}
		}
		else
			return null;
	}
}
