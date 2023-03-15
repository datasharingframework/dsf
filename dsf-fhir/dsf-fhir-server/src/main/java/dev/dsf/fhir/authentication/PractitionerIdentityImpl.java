package dev.dsf.fhir.authentication;

import java.security.cert.X509Certificate;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.eclipse.jetty.security.openid.OpenIdCredentials;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Practitioner;

import dev.dsf.common.auth.conf.PractitionerIdentity;
import dev.dsf.common.auth.conf.Role;

// TODO implement equals, hashCode, toString methods based on the DSF organization identifier to fully comply with the java.security.Principal specification
public class PractitionerIdentityImpl extends AbstractIdentity implements PractitionerIdentity
{
	private final Practitioner practitioner;
	private final OpenIdCredentials credentials;

	/**
	 * @param organization
	 *            not <code>null</code>
	 * @param roles
	 *            may be <code>null</code>
	 * @param practitioner
	 *            not <code>null</code>
	 * @param certificate
	 *            may be <code>null</code>
	 * @param credentials
	 *            may be <code>null</code>
	 */
	public PractitionerIdentityImpl(Organization organization, Set<? extends Role> roles, Practitioner practitioner,
			X509Certificate certificate, OpenIdCredentials credentials)
	{
		super(true, organization, roles, certificate);

		this.practitioner = Objects.requireNonNull(practitioner, "practitioner");
		this.credentials = credentials;
	}

	@Override
	public String getName()
	{
		return getOrganizationIdentifierValue() + "/"
				+ getIdentifierValue(practitioner::getIdentifier, PRACTITIONER_IDENTIFIER_SYSTEM);
	}

	@Override
	public String getDisplayName()
	{
		return practitioner != null && practitioner.hasName() ? practitioner.getNameFirstRep().getNameAsSingleString()
				: "";
	}

	@Override
	public Practitioner getPractitioner()
	{
		return practitioner;
	}

	@Override
	public Optional<OpenIdCredentials> getCredentials()
	{
		// null of login via client certificate
		return Optional.ofNullable(credentials);
	}
}
