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
package dev.dsf.common.auth.conf;

import java.util.Collection;

import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Organization;

// TODO implement equals, hashCode, toString methods based on the DSF organization identifier to fully comply with the java.security.Principal specification
public class OrganizationIdentityImpl extends AbstractIdentity implements OrganizationIdentity
{
	/**
	 * @param localIdentity
	 *            <code>true</code> if this is a local identity
	 * @param organization
	 *            not <code>null</code>
	 * @param endpoint
	 *            may be <code>null</code>
	 * @param dsfRoles
	 *            may be <code>null</code>
	 * @param certificate
	 *            may be <code>null</code>
	 */
	public OrganizationIdentityImpl(boolean localIdentity, Organization organization, Endpoint endpoint,
			Collection<? extends DsfRole> dsfRoles, X509CertificateWrapper certificate)
	{
		super(localIdentity, organization, endpoint, dsfRoles, certificate);
	}

	@Override
	public String getName()
	{
		return getOrganizationIdentifierValue().orElse("?");
	}

	@Override
	public String getDisplayName()
	{
		return getOrganizationIdentifierValue().orElse("?");
	}
}
