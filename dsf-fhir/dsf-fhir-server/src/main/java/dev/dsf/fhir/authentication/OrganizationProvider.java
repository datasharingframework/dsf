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
package dev.dsf.fhir.authentication;

import java.util.Optional;

import org.hl7.fhir.r4.model.Organization;

import dev.dsf.common.auth.conf.Identity;

public interface OrganizationProvider
{
	String ORGANIZATION_IDENTIFIER_SYSTEM = "http://dsf.dev/sid/organization-identifier";

	/**
	 * @param thumbprint
	 *            may be <code>null</code>
	 * @return {@link Optional#empty()} if no {@link Organization} is found, or the given <b>thumbprint</b> is
	 *         <code>null</code>
	 */
	Optional<Organization> getOrganization(String thumbprint);

	Optional<Organization> getLocalOrganization();

	Optional<Identity> getLocalOrganizationAsIdentity();

	String getLocalOrganizationIdentifierValue();
}
