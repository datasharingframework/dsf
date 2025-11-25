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
package dev.dsf.fhir.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.hl7.fhir.r4.model.OrganizationAffiliation;

public interface OrganizationAffiliationDao extends ResourceDao<OrganizationAffiliation>
{
	/**
	 * @param connection
	 *            not <code>null</code>
	 * @param organizationIdentifierValue
	 *            may be <code>null</code>
	 * @param endpointIdentifierValue
	 *            may be <code>null</code>
	 * @return empty list if <b>organizationIdentifierValue</b> is null or blank
	 * @throws SQLException
	 */
	List<OrganizationAffiliation> readActiveNotDeletedByMemberOrganizationIdentifierIncludingOrganizationIdentifiersWithTransaction(
			Connection connection, String organizationIdentifierValue, String endpointIdentifierValue)
			throws SQLException;

	boolean existsNotDeletedByParentOrganizationMemberOrganizationRoleAndNotEndpointWithTransaction(
			Connection connection, UUID parentOrganization, UUID memberOrganization, String roleSystem, String roleCode,
			UUID endpoint) throws SQLException;
}
