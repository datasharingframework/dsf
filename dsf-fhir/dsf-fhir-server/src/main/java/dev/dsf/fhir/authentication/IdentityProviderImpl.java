package dev.dsf.fhir.authentication;

import java.security.cert.X509Certificate;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Practitioner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import dev.dsf.common.auth.conf.AbstractIdentityProvider;
import dev.dsf.common.auth.conf.Identity;
import dev.dsf.common.auth.conf.IdentityProvider;
import dev.dsf.common.auth.conf.OrganizationIdentityImpl;
import dev.dsf.common.auth.conf.PractitionerIdentityImpl;
import dev.dsf.common.auth.conf.RoleConfig;

public class IdentityProviderImpl extends AbstractIdentityProvider implements IdentityProvider, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(IdentityProviderImpl.class);

	private final OrganizationProvider organizationProvider;
	private final EndpointProvider endpointProvider;
	private final String localOrganizationIdentifierValue;

	public IdentityProviderImpl(RoleConfig roleConfig, OrganizationProvider organizationProvider,
			EndpointProvider endpointProvider, String localOrganizationIdentifierValue)
	{
		super(roleConfig);

		this.organizationProvider = organizationProvider;
		this.endpointProvider = endpointProvider;
		this.localOrganizationIdentifierValue = localOrganizationIdentifierValue;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(organizationProvider, "organizationProvider");
		Objects.requireNonNull(endpointProvider, "endpointProvider");
		Objects.requireNonNull(localOrganizationIdentifierValue, "localOrganizationIdentifierValue");
	}

	@Override
	protected Optional<Organization> getLocalOrganization()
	{
		return organizationProvider.getLocalOrganization();
	}

	@Override
	protected Optional<Endpoint> getLocalEndpoint()
	{
		return endpointProvider.getLocalEndpoint();
	}

	@Override
	public Identity getIdentity(X509Certificate[] certificates)
	{
		if (certificates == null || certificates.length == 0)
			return null;

		String thumbprint = getThumbprint(certificates[0]);

		Optional<Organization> organization = organizationProvider.getOrganization(certificates[0]);
		if (organization.isPresent())
		{
			Organization o = organization.get();

			boolean local = isLocalOrganization(o);

			Optional<Endpoint> e = local ? getLocalEndpoint() : endpointProvider.getEndpoint(o, certificates[0]);
			Set<FhirServerRole> r = local ? FhirServerRole.LOCAL_ORGANIZATION : FhirServerRole.REMOTE_ORGANIZATION;

			return new OrganizationIdentityImpl(local, o, e.orElse(null), r, certificates[0]);
		}

		Optional<Practitioner> practitioner = toPractitioner(certificates[0]);
		Optional<Organization> localOrganization = getLocalOrganization();
		if (practitioner.isPresent() && localOrganization.isPresent())
		{
			Practitioner p = practitioner.get();
			Organization o = localOrganization.get();
			Endpoint e = getLocalEndpoint().orElse(null);

			return new PractitionerIdentityImpl(o, e, getDsfRolesFor(p, thumbprint, null, null), certificates[0], p,
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

	private boolean isLocalOrganization(Organization organization)
	{
		return organization != null && organization.getIdentifier().stream().filter(i -> i != null)
				.filter(i -> OrganizationProvider.ORGANIZATION_IDENTIFIER_SYSTEM.equals(i.getSystem()))
				.anyMatch(i -> localOrganizationIdentifierValue.equals(i.getValue()));
	}
}
