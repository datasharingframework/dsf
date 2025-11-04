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
package dev.dsf.bpe.v2.service.process;

import dev.dsf.bpe.v2.service.process.ProcessAuthorizationHelper.RequesterFactory;

public class RequesterFactoryImpl implements RequesterFactory
{
	@Override
	public Requester localAll()
	{
		return all(true, null, null);
	}

	@Override
	public Requester localAllPractitioner(String practitionerRoleSystem, String practitionerRoleCode)
	{
		return all(true, practitionerRoleSystem, practitionerRoleCode);
	}

	@Override
	public Requester remoteAll()
	{
		return all(false, null, null);
	}

	private Requester all(boolean localIdentity, String userRoleSystem, String userRoleCode)
	{
		return new All(localIdentity, userRoleSystem, userRoleCode);
	}

	@Override
	public Requester localOrganization(String organizationIdentifier)
	{
		return organization(true, organizationIdentifier, null, null);
	}

	@Override
	public Requester localOrganizationPractitioner(String organizationIdentifier, String practitionerRoleSystem,
			String practitionerRoleCode)
	{
		return organization(true, organizationIdentifier, practitionerRoleSystem, practitionerRoleCode);
	}

	@Override
	public Requester remoteOrganization(String organizationIdentifier)
	{
		return organization(false, organizationIdentifier, null, null);
	}

	private Requester organization(boolean localIdentity, String organizationIdentifier, String practitionerRoleSystem,
			String practitionerRoleCode)
	{
		return new Organization(localIdentity, organizationIdentifier, practitionerRoleSystem, practitionerRoleCode);
	}

	@Override
	public Requester localRole(String parentOrganizationIdentifier, String organizatioRoleSystem,
			String organizatioRoleCode)
	{
		return role(true, parentOrganizationIdentifier, organizatioRoleSystem, organizatioRoleCode, null, null);
	}

	@Override
	public Requester localRolePractitioner(String parentOrganizationIdentifier, String organizatioRoleSystem,
			String organizatioRoleCode, String practitionerRoleSystem, String practitionerRoleCode)
	{
		return role(true, parentOrganizationIdentifier, organizatioRoleSystem, organizatioRoleCode,
				practitionerRoleSystem, practitionerRoleCode);
	}

	@Override
	public Requester remoteRole(String parentOrganizationIdentifier, String organizatioRoleSystem,
			String organizatioRoleCode)
	{
		return role(false, parentOrganizationIdentifier, organizatioRoleSystem, organizatioRoleCode, null, null);
	}

	private Requester role(boolean localIdentity, String parentOrganizationIdentifier, String organizatioRoleSystem,
			String organizatioRoleCode, String practitionerRoleSystem, String practitionerRoleCode)
	{
		return new Role(localIdentity, parentOrganizationIdentifier, organizatioRoleSystem, organizatioRoleCode,
				practitionerRoleSystem, practitionerRoleCode);
	}
}
