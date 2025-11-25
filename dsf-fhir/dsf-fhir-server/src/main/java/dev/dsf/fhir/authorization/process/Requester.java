/*
 * Copyright 2018-2025 Heilbronn University of Applied Sciences
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
		return all(true, null, null);
	}

	static Requester localAllPractitioner(String practitionerRoleSystem, String practitionerRoleCode)
	{
		return all(true, practitionerRoleSystem, practitionerRoleCode);
	}

	static Requester remoteAll()
	{
		return all(false, null, null);
	}

	static Requester all(boolean localIdentity, String userRoleSystem, String userRoleCode)
	{
		return new All(localIdentity, userRoleSystem, userRoleCode);
	}

	static Requester localOrganization(String organizationIdentifier)
	{
		return organization(true, organizationIdentifier, null, null);
	}

	static Requester localOrganizationPractitioner(String organizationIdentifier, String practitionerRoleSystem,
			String practitionerRoleCode)
	{
		return organization(true, organizationIdentifier, practitionerRoleSystem, practitionerRoleCode);
	}

	static Requester remoteOrganization(String organizationIdentifier)
	{
		return organization(false, organizationIdentifier, null, null);
	}

	static Requester organization(boolean localIdentity, String organizationIdentifier, String practitionerRoleSystem,
			String practitionerRoleCode)
	{
		return new Organization(localIdentity, organizationIdentifier, practitionerRoleSystem, practitionerRoleCode);
	}

	static Requester localRole(String parentOrganizationIdentifier, String organizatioRoleSystem,
			String organizatioRoleCode)
	{
		return role(true, parentOrganizationIdentifier, organizatioRoleSystem, organizatioRoleCode, null, null);
	}

	static Requester localRolePractitioner(String parentOrganizationIdentifier, String organizatioRoleSystem,
			String organizatioRoleCode, String practitionerRoleSystem, String practitionerRoleCode)
	{
		return role(true, parentOrganizationIdentifier, organizatioRoleSystem, organizatioRoleCode,
				practitionerRoleSystem, practitionerRoleCode);
	}

	static Requester remoteRole(String parentOrganizationIdentifier, String organizatioRoleSystem,
			String organizatioRoleCode)
	{
		return role(false, parentOrganizationIdentifier, organizatioRoleSystem, organizatioRoleCode, null, null);
	}

	static Requester role(boolean localIdentity, String parentOrganizationIdentifier, String organizatioRoleSystem,
			String organizatioRoleCode, String practitionerRoleSystem, String practitionerRoleCode)
	{
		return new Role(localIdentity, parentOrganizationIdentifier, organizatioRoleSystem, organizatioRoleCode,
				practitionerRoleSystem, practitionerRoleCode);
	}

	boolean requesterMatches(Extension requesterExtension);

	boolean isRequesterAuthorized(Identity requester, Stream<OrganizationAffiliation> requesterAffiliations);

	default boolean isRequesterAuthorized(Identity requester, Collection<OrganizationAffiliation> requesterAffiliations)
	{
		return isRequesterAuthorized(requester, requesterAffiliations == null ? null : requesterAffiliations.stream());
	}

	Extension toRequesterExtension();
}
