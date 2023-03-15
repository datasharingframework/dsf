package dev.dsf.fhir.authorization.process;

import java.util.Collection;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.OrganizationAffiliation;

import dev.dsf.common.auth.conf.Identity;

public interface Requester extends WithAuthorization
{
	static Requester localAll()
	{
		return all(true);
	}

	static Requester remoteAll()
	{
		return all(false);
	}

	static Requester all(boolean localIdentity)
	{
		return new All(localIdentity);
	}

	static Requester localOrganization(String organizationIdentifier)
	{
		return organization(true, organizationIdentifier);
	}

	static Requester remoteOrganization(String organizationIdentifier)
	{
		return organization(false, organizationIdentifier);
	}

	static Requester organization(boolean localIdentity, String organizationIdentifier)
	{
		return new Organization(localIdentity, organizationIdentifier);
	}

	static Requester localRole(String consortiumIdentifier, String roleSystem, String roleCode)
	{
		return role(true, consortiumIdentifier, roleSystem, roleCode);
	}

	static Requester remoteRole(String consortiumIdentifier, String roleSystem, String roleCode)
	{
		return role(false, consortiumIdentifier, roleSystem, roleCode);
	}

	static Requester role(boolean localIdentity, String consortiumIdentifier, String roleSystem, String roleCode)
	{
		return new Role(localIdentity, consortiumIdentifier, roleSystem, roleCode);
	}

	boolean requesterMatches(Extension requesterExtension);

	boolean isRequesterAuthorized(Identity requesterUser, Stream<OrganizationAffiliation> requesterAffiliations);

	default boolean isRequesterAuthorized(Identity requesterUser,
			Collection<OrganizationAffiliation> requesterAffiliations)
	{
		return isRequesterAuthorized(requesterUser,
				requesterAffiliations == null ? null : requesterAffiliations.stream());
	}

	default Extension toRequesterExtension()
	{
		return new Extension().setUrl(ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_REQUESTER)
				.setValue(getProcessAuthorizationCode());
	}
}
