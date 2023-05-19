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
		return new All(true);
	}

	static Recipient localOrganization(String organizationIdentifier)
	{
		return new Organization(true, organizationIdentifier);
	}

	static Recipient localRole(String consortiumIdentifier, String roleSystem, String roleCode)
	{
		return new Role(true, consortiumIdentifier, roleSystem, roleCode);
	}

	boolean recipientMatches(Extension recipientExtension);

	boolean isRecipientAuthorized(Identity recipientUser, Stream<OrganizationAffiliation> recipientAffiliations);

	default boolean isRecipientAuthorized(Identity recipientUser,
			Collection<OrganizationAffiliation> recipientAffiliations)
	{
		return isRecipientAuthorized(recipientUser,
				recipientAffiliations == null ? null : recipientAffiliations.stream());
	}

	default Extension toRecipientExtension()
	{
		return new Extension().setUrl(ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_RECIPIENT)
				.setValue(getProcessAuthorizationCode());
	}
}
