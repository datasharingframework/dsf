package dev.dsf.common.auth.logging;

import java.util.stream.Collectors;

import org.eclipse.jetty.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.common.auth.conf.OrganizationIdentity;
import dev.dsf.common.auth.conf.PractitionerIdentity;

public class CurrentUserLogger extends AbstractUserLogger
{
	private static final Logger logger = LoggerFactory.getLogger(CurrentUserLogger.class);

	@Override
	protected void before(OrganizationIdentity organization)
	{
		logger.debug("Current organization identity '{}', dsf-roles {}", organization.getName(),
				organization.getDsfRoles());
	}

	@Override
	protected void before(PractitionerIdentity practitioner)
	{
		logger.debug("Current practitioner identity '{}', dsf-roles {}, practitioner-roles {}", practitioner.getName(),
				practitioner.getDsfRoles(), practitioner.getPractionerRoles().stream()
						.map(c -> c.getSystem() + "|" + c.getCode()).collect(Collectors.joining(", ", "[", "]")));
	}

	@Override
	protected void before(UserPrincipal userPrincipal)
	{
		logger.debug("Current identity '{}'", userPrincipal.getName());
	}
}
