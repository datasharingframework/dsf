package dev.dsf.bpe.authentication;

import java.security.cert.X509Certificate;
import java.util.Objects;
import java.util.Optional;

import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Practitioner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import dev.dsf.bpe.service.LocalOrganizationAndEndpointProvider;
import dev.dsf.common.auth.conf.AbstractIdentityProvider;
import dev.dsf.common.auth.conf.Identity;
import dev.dsf.common.auth.conf.IdentityProvider;
import dev.dsf.common.auth.conf.PractitionerIdentityImpl;
import dev.dsf.common.auth.conf.RoleConfig;

public class IdentityProviderImpl extends AbstractIdentityProvider implements IdentityProvider, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(IdentityProviderImpl.class);

	private final LocalOrganizationAndEndpointProvider organizationAndEndpointProvider;

	public IdentityProviderImpl(RoleConfig roleConfig,
			LocalOrganizationAndEndpointProvider organizationAndEndpointProvider)
	{
		super(roleConfig);

		this.organizationAndEndpointProvider = organizationAndEndpointProvider;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(organizationAndEndpointProvider, "organizationAndEndpointProvider");
	}

	@Override
	protected Optional<Organization> getLocalOrganization()
	{
		return organizationAndEndpointProvider.getLocalOrganization();
	}

	@Override
	public Identity getIdentity(X509Certificate[] certificates)
	{
		if (certificates == null || certificates.length == 0)
			return null;

		String thumbprint = getThumbprint(certificates[0]);

		Optional<Practitioner> practitioner = toPractitioner(certificates[0]);
		Optional<Organization> localOrganization = organizationAndEndpointProvider.getLocalOrganization();
		Optional<Endpoint> localEndpoint = organizationAndEndpointProvider.getLocalEndpoint();
		if (practitioner.isPresent() && localOrganization.isPresent() && localEndpoint.isPresent())
		{
			Practitioner p = practitioner.get();
			Organization o = localOrganization.get();
			Endpoint e = localEndpoint.get();

			return new PractitionerIdentityImpl(o, e, getDsfRolesFor(p, thumbprint, null, null), certificates[0], p,
					getPractitionerRolesFor(p, thumbprint, null, null), null);
		}
		else
		{
			logger.warn(
					"Certificate with thumbprint '{}' for '{}' unknown, not configured as local user or local organization unknown",
					thumbprint, getDn(certificates[0]));
			return null;
		}
	}

	@Override
	protected Optional<Endpoint> getLocalEndpoint()
	{
		return Optional.empty();
	}
}
