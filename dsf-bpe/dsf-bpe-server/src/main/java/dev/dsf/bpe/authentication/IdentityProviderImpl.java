package dev.dsf.bpe.authentication;

import java.security.cert.X509Certificate;
import java.util.Objects;
import java.util.Optional;

import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Practitioner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import dev.dsf.bpe.service.LocalOrganizationProvider;
import dev.dsf.common.auth.conf.AbstractIdentityProvider;
import dev.dsf.common.auth.conf.Identity;
import dev.dsf.common.auth.conf.IdentityProvider;
import dev.dsf.common.auth.conf.PractitionerIdentityImpl;
import dev.dsf.common.auth.conf.RoleConfig;

public class IdentityProviderImpl extends AbstractIdentityProvider implements IdentityProvider, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(IdentityProviderImpl.class);

	private final LocalOrganizationProvider organizationProvider;

	public IdentityProviderImpl(RoleConfig roleConfig, LocalOrganizationProvider organizationProvider)
	{
		super(roleConfig);

		this.organizationProvider = organizationProvider;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(organizationProvider, "organizationProvider");
	}

	@Override
	protected Optional<Organization> getLocalOrganization()
	{
		return organizationProvider.getLocalOrganization();
	}

	@Override
	public Identity getIdentity(X509Certificate[] certificates)
	{
		if (certificates == null || certificates.length == 0)
			return null;

		String thumbprint = getThumbprint(certificates[0]);

		Optional<Practitioner> practitioner = toPractitioner(certificates[0]);
		Optional<Organization> localOrganization = organizationProvider.getLocalOrganization();
		if (practitioner.isPresent() && localOrganization.isPresent())
		{
			Practitioner p = practitioner.get();
			Organization o = localOrganization.get();

			return new PractitionerIdentityImpl(o, getDsfRolesFor(p, thumbprint, null, null), certificates[0], p,
					getPractitionerRolesFor(p, thumbprint, null, null), null);
		}
		else
		{
			logger.warn(
					"Certificate with thumbprint '{}' for '{}' unknown, not part of allowlist and not configured as local user or local organization",
					thumbprint, getDn(certificates[0]));
			return null;
		}
	}
}
