package dev.dsf.common.auth.logging;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jetty.security.UserPrincipal;
import org.slf4j.MDC;

import dev.dsf.common.auth.conf.DsfRole;
import dev.dsf.common.auth.conf.OrganizationIdentity;
import dev.dsf.common.auth.conf.PractitionerIdentity;

public class CurrentUserMdcLogger extends AbstractUserLogger
{
	public static final String DSF_IDENTITY_NAME = "dsf.identity.name";
	public static final String DSF_IDENTITY_TYPE = "dsf.identity.type";
	public static final String DSF_IDENTITY_ROLES = "dsf.identity.roles";

	@Override
	protected void before(OrganizationIdentity organization)
	{
		MDC.put(DSF_IDENTITY_NAME, organization.getName());
		MDC.put(DSF_IDENTITY_TYPE, "organization");
		MDC.put(DSF_IDENTITY_ROLES,
				organization.getDsfRoles().stream().map(DsfRole::name).collect(Collectors.joining(", ", "[", "]")));
	}

	@Override
	protected void before(PractitionerIdentity practitioner)
	{
		MDC.put(DSF_IDENTITY_NAME, practitioner.getName());
		MDC.put(DSF_IDENTITY_TYPE, "practitioner");
		MDC.put(DSF_IDENTITY_ROLES,
				Stream.concat(practitioner.getDsfRoles().stream().map(DsfRole::name),
						practitioner.getPractionerRoles().stream().map(c -> c.getSystem() + "|" + c.getCode()))
						.collect(Collectors.joining(", ", "[", "]")));
	}

	@Override
	protected void before(UserPrincipal userPrincipal)
	{
		MDC.put(DSF_IDENTITY_NAME, userPrincipal.getName());
		MDC.put(DSF_IDENTITY_TYPE, "userPrincipal");
	}

	@Override
	protected void after()
	{
		MDC.remove(DSF_IDENTITY_NAME);
		MDC.remove(DSF_IDENTITY_TYPE);
		MDC.remove(DSF_IDENTITY_ROLES);
	}
}
