package dev.dsf.fhir.authentication;

import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Practitioner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import dev.dsf.common.auth.Identity;
import dev.dsf.common.auth.IdentityProvider;
import dev.dsf.common.auth.Role;
import dev.dsf.common.auth.RoleConfig;

public class IdentityProviderImpl extends AbstractProvider implements IdentityProvider, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(IdentityProviderImpl.class);

	private final OrganizationProvider organizationProvider;
	private final PractitionerProvider practitionerProvider;
	private final String localOrganizationIdentifierValue;
	private final RoleConfig roleConfig;

	public IdentityProviderImpl(OrganizationProvider organizationProvider, PractitionerProvider practitionerProvider,
			String localOrganizationIdentifierValue, RoleConfig roleConfig)
	{
		this.organizationProvider = organizationProvider;
		this.practitionerProvider = practitionerProvider;
		this.localOrganizationIdentifierValue = localOrganizationIdentifierValue;
		this.roleConfig = roleConfig;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(organizationProvider, "organizationProvider");
		Objects.requireNonNull(practitionerProvider, "practitionerProvider");
		Objects.requireNonNull(localOrganizationIdentifierValue, "localOrganizationIdentifierValue");
		Objects.requireNonNull(roleConfig, "roleConfig");
	}

	@Override
	public Identity getIdentity(String jwtToken)
	{
		logger.warn("JWT token based login not implemented");
		return null;
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
			boolean localOrganization = isLocalOrganization(organization.get());

			if (localOrganization)
				return new OrganizationIdentityImpl(true, organization.get(), FhirServerRole.LOCAL_ORGANIZATION);
			else
				return new OrganizationIdentityImpl(false, organization.get(), FhirServerRole.REMOTE_ORGANIZATION);
		}

		Optional<Practitioner> practitioner = practitionerProvider.getPractitioner(certificates[0]);
		Optional<Organization> localOrganization = organizationProvider.getLocalOrganization();
		if (practitioner.isPresent() && localOrganization.isPresent())
			return new PractitionerIdentityImpl(true, localOrganization.get(),
					getRolesFor(practitioner.get(), thumbprint, Collections.emptyList()), practitioner.get());

		logger.warn(
				"Certificate with thumbprint '{}' for '{}' not part of allowlist and not configured as local user or local organization unknown",
				thumbprint, getDn(certificates[0]));
		return null;
	}

	private boolean isLocalOrganization(Organization organization)
	{
		return organization != null && organization.getIdentifier().stream()
				.anyMatch(i -> i != null && OrganizationProvider.ORGANIZATION_IDENTIFIER_SYSTEM.equals(i.getSystem())
						&& localOrganizationIdentifierValue.equals(i.getValue()));
	}

	// thumbprint from certificate, claims from jwt
	private Set<Role> getRolesFor(Practitioner practitioner, String thumbprint, List<String> claims)
	{
		Stream<String> emailAddresses = practitioner.getIdentifier().stream()
				.filter(i -> PractitionerProvider.PRACTITIONER_IDENTIFIER_SYSTEM.equals(i.getSystem()) && i.hasValue())
				.map(Identifier::getValue);

		Stream<Role> r1 = emailAddresses.flatMap(e -> roleConfig.getRolesForEmail(e).stream());
		Stream<Role> r2 = roleConfig.getRolesForThumbprint(thumbprint).stream();
		Stream<Role> r3 = claims.stream().flatMap(c -> roleConfig.getRolesForClaim(c).stream());

		return Stream.concat(Stream.concat(r1, r2), r3).distinct().collect(Collectors.toSet());
	}
}
