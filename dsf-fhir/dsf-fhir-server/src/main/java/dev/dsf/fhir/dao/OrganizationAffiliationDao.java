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
