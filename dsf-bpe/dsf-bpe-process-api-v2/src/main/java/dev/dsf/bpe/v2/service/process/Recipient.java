package dev.dsf.bpe.v2.service.process;

import java.util.Collection;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.OrganizationAffiliation;

public interface Recipient extends WithAuthorization
{
	boolean recipientMatches(Extension recipientExtension);

	boolean isRecipientAuthorized(Identity recipientUser, Stream<OrganizationAffiliation> recipientAffiliations);

	default boolean isRecipientAuthorized(Identity recipientUser,
			Collection<OrganizationAffiliation> recipientAffiliations)
	{
		return isRecipientAuthorized(recipientUser,
				recipientAffiliations == null ? null : recipientAffiliations.stream());
	}

	Extension toRecipientExtension();
}
