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
package dev.dsf.bpe.v2.service;

import java.util.List;
import java.util.function.Predicate;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.OrganizationAffiliation;
import org.hl7.fhir.r4.model.Resource;

/**
 * Methods to configure read access for FHIR resources on a DSF FHIR server. Methods add and check resource {@link Meta}
 * tags.
 */
public interface ReadAccessHelper
{
	/**
	 * Adds LOCAL tag. Removes ALL tag if present.
	 *
	 * @param <R>
	 *            the resource type
	 * @param resource
	 *            may be <code>null</code>
	 * @return <code>null</code> if given <b>resource</b> is <code>null</code>
	 * @see #addAll(Resource)
	 */
	<R extends Resource> R addLocal(R resource);

	/**
	 * Adds ORGANIZATION tag for the given organization. Adds LOCAL tag if not present, removes ALL tag if present.
	 *
	 * @param <R>
	 *            the resource type
	 * @param resource
	 *            may be <code>null</code>
	 * @param organizationIdentifier
	 *            not <code>null</code>
	 * @return <code>null</code> if given <b>resource</b> is <code>null</code>
	 * @see #addLocal(Resource)
	 * @see #addOrganization(Resource, Organization)
	 */
	<R extends Resource> R addOrganization(R resource, String organizationIdentifier);

	/**
	 * Adds ORGANIZATION tag for the given organization. Adds LOCAL tag if not present, removes ALL tag if present.
	 *
	 * @param <R>
	 *            the resource type
	 * @param resource
	 *            may be <code>null</code>
	 * @param organization
	 *            not <code>null</code>
	 * @return <code>null</code> if given <b>resource</b> is <code>null</code>
	 * @throws NullPointerException
	 *             if given <b>organization</b> is <code>null</code>
	 * @throws IllegalArgumentException
	 *             if given <b>organization</b> does not have valid identifier
	 * @see #addLocal(Resource)
	 * @see #addOrganization(Resource, String)
	 */
	<R extends Resource> R addOrganization(R resource, Organization organization);

	/**
	 * Adds ROLE tag for the given affiliation. Adds LOCAL tag if not present, removes ALL tag if present.
	 *
	 * @param <R>
	 *            the resource type
	 * @param resource
	 *            may be <code>null</code>
	 * @param consortiumIdentifier
	 *            not <code>null</code>
	 * @param roleSystem
	 *            not <code>null</code>
	 * @param roleCode
	 *            not <code>null</code>
	 * @return <code>null</code> if given <b>resource</b> is <code>null</code>
	 * @see #addLocal(Resource)
	 * @see #addRole(Resource, OrganizationAffiliation)
	 */
	<R extends Resource> R addRole(R resource, String consortiumIdentifier, String roleSystem, String roleCode);

	/**
	 * Adds ROLE tag for the given affiliation. Adds LOCAL tag if not present, removes ALL tag if present.
	 *
	 * @param <R>
	 *            the resource type
	 * @param resource
	 *            may be <code>null</code>
	 * @param affiliation
	 *            not <code>null</code>
	 * @return <code>null</code> if given <b>resource</b> is <code>null</code>
	 * @throws NullPointerException
	 *             if given <b>affiliation</b> is <code>null</code>
	 * @throws IllegalArgumentException
	 *             if given <b>affiliation</b> does not have valid consortium identifier or organization role (only one
	 *             role supported)
	 * @see #addLocal(Resource)
	 * @see #addRole(Resource, String, String, String)
	 */
	<R extends Resource> R addRole(R resource, OrganizationAffiliation affiliation);

	/**
	 * Adds All tag. Removes LOCAL, ORGANIZATION and ROLE tags if present.
	 *
	 * @param <R>
	 *            the resource type
	 * @param resource
	 *            may be <code>null</code>
	 * @return <code>null</code> if given <b>resource</b> is <code>null</code>
	 * @see #addLocal(Resource)
	 * @see #addOrganization(Resource, String)
	 * @see #addRole(Resource, String, String, String)
	 */
	<R extends Resource> R addAll(R resource);

	boolean hasLocal(Resource resource);

	boolean hasOrganization(Resource resource, String organizationIdentifier);

	boolean hasOrganization(Resource resource, Organization organization);

	boolean hasAnyOrganization(Resource resource);

	boolean hasRole(Resource resource, String consortiumIdentifier, String roleSystem, String roleCode);

	boolean hasRole(Resource resource, OrganizationAffiliation affiliation);

	boolean hasRole(Resource resource, List<OrganizationAffiliation> affiliations);

	boolean hasAnyRole(Resource resource);

	boolean hasAll(Resource resource);

	/**
	 * <b>Resource with access tags valid if:</b><br>
	 *
	 * 1 LOCAL tag and n {ORGANIZATION, ROLE} tags {@code (n >= 0)}<br>
	 * or<br>
	 * 1 ALL tag<br>
	 * <br>
	 * All tags {LOCAL, ORGANIZATION, ROLE, ALL} valid<br>
	 * <br>
	 * Does not check if referenced organizations or roles exist
	 *
	 * @param resource
	 *            may be <code>null</code>
	 * @return <code>false</code> if given <b>resource</b> is <code>null</code> or resource not valid
	 */
	boolean isValid(Resource resource);

	/**
	 * <b>Resource with access tags valid if:</b><br>
	 *
	 * 1 LOCAL tag and n {ORGANIZATION, ROLE} tags {@code (n >= 0)}<br>
	 * or<br>
	 * 1 ALL tag<br>
	 * <br>
	 * All tags {LOCAL, ORGANIZATION, ROLE, ALL} valid
	 *
	 * @param resource
	 *            may be <code>null</code>
	 * @param organizationWithIdentifierExists
	 *            not <code>null</code>
	 * @param roleExists
	 *            not <code>null</code>
	 * @return <code>false</code> if given <b>resource</b> is <code>null</code> or resource not valid
	 */
	boolean isValid(Resource resource, Predicate<Identifier> organizationWithIdentifierExists,
			Predicate<Coding> roleExists);
}
