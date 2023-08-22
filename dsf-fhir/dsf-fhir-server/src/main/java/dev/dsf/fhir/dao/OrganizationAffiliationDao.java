package dev.dsf.fhir.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.hl7.fhir.r4.model.OrganizationAffiliation;

public interface OrganizationAffiliationDao extends ResourceDao<OrganizationAffiliation>
{
	List<OrganizationAffiliation> readActiveNotDeletedByMemberOrganizationIdentifierIncludingOrganizationIdentifiersWithTransaction(
			Connection connection, String identifierValue) throws SQLException;

	boolean existsNotDeletedByParentOrganizationMemberOrganizationRoleAndNotEndpointWithTransaction(
			Connection connection, UUID parentOrganization, UUID memberOrganization, String roleSystem, String roleCode,
			UUID endpoint) throws SQLException;
}
