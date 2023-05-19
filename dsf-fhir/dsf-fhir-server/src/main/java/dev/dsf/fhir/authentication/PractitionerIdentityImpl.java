package dev.dsf.fhir.authentication;

import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Practitioner;

import dev.dsf.common.auth.DsfOpenIdCredentials;
import dev.dsf.common.auth.conf.DsfRole;
import dev.dsf.common.auth.conf.PractitionerIdentity;

// TODO implement equals, hashCode, toString methods based on the DSF organization identifier to fully comply with the java.security.Principal specification
public class PractitionerIdentityImpl extends AbstractIdentity implements PractitionerIdentity
{
	private final Practitioner practitioner;
	private final DsfOpenIdCredentials credentials;

	private final Set<Coding> practitionerRoles = new HashSet<>();

	/**
	 * @param organization
	 *            not <code>null</code>
	 * @param dsfRoles
	 *            may be <code>null</code>
	 * @param certificate
	 *            may be <code>null</code>
	 * @param practitioner
	 *            not <code>null</code>
	 * @param practitionerRoles
	 *            may be <code>null</code>
	 * @param credentials
	 *            may be <code>null</code>
	 */
	public PractitionerIdentityImpl(Organization organization, Collection<? extends DsfRole> dsfRoles,
			X509Certificate certificate, Practitioner practitioner, Collection<? extends Coding> practitionerRoles,
			DsfOpenIdCredentials credentials)
	{
		super(true, organization, dsfRoles, certificate);

		this.practitioner = Objects.requireNonNull(practitioner, "practitioner");

		if (practitionerRoles != null)
			this.practitionerRoles.addAll(practitionerRoles);

		// null if login via client certificate
		this.credentials = credentials;
	}

	@Override
	public String getName()
	{
		return getOrganizationIdentifierValue().orElse("?") + "/"
				+ getIdentifierValue(practitioner::getIdentifier, PRACTITIONER_IDENTIFIER_SYSTEM).orElse("?");
	}

	@Override
	public String getDisplayName()
	{
		return practitioner.hasName() ? practitioner.getNameFirstRep().getNameAsSingleString() : "";
	}

	@Override
	public Practitioner getPractitioner()
	{
		return practitioner;
	}

	@Override
	public Set<Coding> getPractionerRoles()
	{
		return Collections.unmodifiableSet(practitionerRoles);
	}

	@Override
	public Optional<DsfOpenIdCredentials> getCredentials()
	{
		// null if login via client certificate
		return Optional.ofNullable(credentials);
	}
}
