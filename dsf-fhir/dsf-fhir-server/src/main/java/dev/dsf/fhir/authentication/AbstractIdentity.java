package dev.dsf.fhir.authentication;

import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.common.auth.conf.Role;

public abstract class AbstractIdentity implements Identity
{
	private final boolean localIdentity;
	private final Organization organization;
	private final Set<Role> roles = new HashSet<>();
	private final X509Certificate certificate;

	/**
	 * @param localIdentity
	 *            <code>true</code> if this is a local identity
	 * @param organization
	 *            not <code>null</code>
	 * @param roles
	 *            may be <code>null</code>
	 * @param certificate
	 *            may be <code>null</code>
	 */
	public AbstractIdentity(boolean localIdentity, Organization organization, Set<? extends Role> roles,
			X509Certificate certificate)
	{
		this.localIdentity = localIdentity;
		this.organization = Objects.requireNonNull(organization, "organization");

		if (roles != null)
			this.roles.addAll(roles);

		this.certificate = certificate;
	}

	@Override
	public boolean isLocalIdentity()
	{
		return localIdentity;
	}

	@Override
	public Organization getOrganization()
	{
		return organization;
	}

	@Override
	public String getOrganizationIdentifierValue()
	{
		return getIdentifierValue(organization::getIdentifier, ORGANIZATION_IDENTIFIER_SYSTEM);
	}

	protected String getIdentifierValue(Supplier<List<Identifier>> identifiers, String identifierSystem)
	{
		Objects.requireNonNull(identifiers, "identifiers");
		Objects.requireNonNull(identifierSystem, "identifierSystem");

		List<Identifier> ids = identifiers.get();
		if (ids == null)
			return "";

		return ids.stream().filter(i -> i != null).filter(i -> identifierSystem.equals(i.getSystem()))
				.filter(Identifier::hasValue).findFirst().map(Identifier::getValue).orElse("");
	}

	@Override
	public Set<Role> getRoles()
	{
		return Collections.unmodifiableSet(roles);
	}

	@Override
	public boolean hasRole(Role role)
	{
		return roles.contains(role);
	}

	@Override
	public boolean hasRole(String role)
	{
		return FhirServerRole.isValid(role) && hasRole(FhirServerRole.valueOf(role));
	}

	@Override
	public Optional<X509Certificate> getCertificate()
	{
		// null if login via OIDC
		return Optional.ofNullable(certificate);
	}
}
