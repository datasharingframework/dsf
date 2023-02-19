package dev.dsf.fhir.authentication;

import java.util.Objects;
import java.util.Set;

import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Practitioner;

import dev.dsf.common.auth.PractitionerIdentity;
import dev.dsf.common.auth.Role;

// TODO implement equals, hashCode, toString methods based on the DSF organization identifier to fully comply with the java.security.Principal specification
public class PractitionerIdentityImpl extends AbstractIdentity implements PractitionerIdentity
{
	private final Practitioner practitioner;

	/**
	 * @param localIdentity
	 *            <code>true</code> if this is a local identity
	 * @param organization
	 *            not <code>null</code>
	 * @param roles
	 *            may be <code>null</code>
	 * @param practitioner
	 *            not <code>null</code>
	 */
	public PractitionerIdentityImpl(boolean localIdentity, Organization organization, Set<? extends Role> roles,
			Practitioner practitioner)
	{
		super(localIdentity, organization, roles);

		this.practitioner = Objects.requireNonNull(practitioner, "practitioner");
	}

	@Override
	public String getName()
	{
		return getOrganizationIdentifierValue() + "/"
				+ getIdentifierValue(practitioner::getIdentifier, PRACTITIONER_IDENTIFIER_SYSTEM);
	}

	@Override
	public Practitioner getPractitioner()
	{
		return practitioner;
	}
}
