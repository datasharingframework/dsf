package dev.dsf.fhir.authorization.process;

import java.util.Collection;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.OrganizationAffiliation;

import dev.dsf.common.auth.conf.Identity;

public interface Recipient extends WithAuthorization
{
	static Recipient localAll()
	{
		return new All(true, null, null);
	}

	static Recipient localOrganization(String organizationIdentifier)
	{
		return new Organization(true, organizationIdentifier, null, null);
	}

	static Recipient localRole(String parentOrganizationIdentifier, String roleSystem, String roleCode)
	{
		return new Role(true, parentOrganizationIdentifier, roleSystem, roleCode, null, null);
	}

	boolean recipientMatches(Extension recipientExtension);

	boolean isRecipientAuthorized(Identity recipient, Stream<OrganizationAffiliation> recipientAffiliations);

	default boolean isRecipientAuthorized(Identity recipient, Collection<OrganizationAffiliation> recipientAffiliations)
	{
		return isRecipientAuthorized(recipient, recipientAffiliations == null ? null : recipientAffiliations.stream());
	}

	Extension toRecipientExtension();
}
