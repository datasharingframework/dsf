package dev.dsf.bpe.v2.service.process;

import java.util.Collection;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.OrganizationAffiliation;

public interface Requester extends WithAuthorization
{
	boolean requesterMatches(Extension requesterExtension);

	boolean isRequesterAuthorized(Identity requesterUser, Stream<OrganizationAffiliation> requesterAffiliations);

	default boolean isRequesterAuthorized(Identity requesterUser,
			Collection<OrganizationAffiliation> requesterAffiliations)
	{
		return isRequesterAuthorized(requesterUser,
				requesterAffiliations == null ? null : requesterAffiliations.stream());
	}

	Extension toRequesterExtension();
}
