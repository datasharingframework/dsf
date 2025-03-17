package dev.dsf.fhir.dao.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.OrganizationAffiliation;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.fhir.dao.OrganizationAffiliationDao;
import dev.dsf.fhir.search.filter.OrganizationAffiliationIdentityFilter;
import dev.dsf.fhir.search.parameters.OrganizationAffiliationActive;
import dev.dsf.fhir.search.parameters.OrganizationAffiliationEndpoint;
import dev.dsf.fhir.search.parameters.OrganizationAffiliationIdentifier;
import dev.dsf.fhir.search.parameters.OrganizationAffiliationParticipatingOrganization;
import dev.dsf.fhir.search.parameters.OrganizationAffiliationPrimaryOrganization;
import dev.dsf.fhir.search.parameters.OrganizationAffiliationRole;

public class OrganizationAffiliationDaoJdbc extends AbstractResourceDaoJdbc<OrganizationAffiliation>
		implements OrganizationAffiliationDao
{
	public OrganizationAffiliationDaoJdbc(DataSource dataSource, DataSource permanentDeleteDataSource,
			FhirContext fhirContext)
	{
		super(dataSource, permanentDeleteDataSource, fhirContext, OrganizationAffiliation.class,
				"organization_affiliations", "organization_affiliation", "organization_affiliation_id",
				OrganizationAffiliationIdentityFilter::new,
				List.of(factory(OrganizationAffiliationActive.PARAMETER_NAME, OrganizationAffiliationActive::new),
						factory(OrganizationAffiliationEndpoint.PARAMETER_NAME, OrganizationAffiliationEndpoint::new,
								OrganizationAffiliationEndpoint.getNameModifiers(),
								OrganizationAffiliationEndpoint::new,
								OrganizationAffiliationEndpoint.getIncludeParameterValues()),
						factory(OrganizationAffiliationIdentifier.PARAMETER_NAME,
								OrganizationAffiliationIdentifier::new,
								OrganizationAffiliationIdentifier.getNameModifiers()),
						factory(OrganizationAffiliationParticipatingOrganization.PARAMETER_NAME,
								OrganizationAffiliationParticipatingOrganization::new,
								OrganizationAffiliationParticipatingOrganization.getNameModifiers(),
								OrganizationAffiliationParticipatingOrganization::new,
								OrganizationAffiliationParticipatingOrganization.getIncludeParameterValues()),
						factory(OrganizationAffiliationPrimaryOrganization.PARAMETER_NAME,
								OrganizationAffiliationPrimaryOrganization::new,
								OrganizationAffiliationPrimaryOrganization.getNameModifiers(),
								OrganizationAffiliationPrimaryOrganization::new,
								OrganizationAffiliationPrimaryOrganization.getIncludeParameterValues()),
						factory(OrganizationAffiliationRole.PARAMETER_NAME, OrganizationAffiliationRole::new,
								OrganizationAffiliationRole.getNameModifiers())),
				List.of());
	}

	@Override
	protected OrganizationAffiliation copy(OrganizationAffiliation resource)
	{
		return resource.copy();
	}

	@Override
	public List<OrganizationAffiliation> readActiveNotDeletedByMemberOrganizationIdentifierIncludingOrganizationIdentifiersWithTransaction(
			Connection connection, String identifierValue) throws SQLException
	{
		Objects.requireNonNull(connection, "connection");
		if (identifierValue == null || identifierValue.isBlank())
			return List.of();

		try (PreparedStatement statement = connection.prepareStatement("SELECT organization_affiliation"
				+ ",(SELECT identifiers->>'value' FROM current_organizations, jsonb_array_elements(organization->'identifier') AS identifiers "
				+ "WHERE identifiers->>'system' = 'http://dsf.dev/sid/organization-identifier' "
				+ "AND concat('Organization/', organization->>'id') = organization_affiliation->'organization'->>'reference' LIMIT 1) AS organization_identifier "
				+ "FROM current_organization_affiliations WHERE organization_affiliation->>'active' = 'true' AND "
				+ "(SELECT organization->'identifier' FROM current_organizations WHERE organization->>'active' = 'true' AND "
				+ "concat('Organization/', organization->>'id') = organization_affiliation->'participatingOrganization'->>'reference') @> ?::jsonb"))
		{
			statement.setString(1, "[{\"system\": \"http://dsf.dev/sid/organization-identifier\", \"value\": \""
					+ identifierValue + "\"}]");

			try (ResultSet result = statement.executeQuery())
			{
				List<OrganizationAffiliation> affiliations = new ArrayList<>();

				while (result.next())
				{
					OrganizationAffiliation oA = getResource(result, 1);
					String organizationIdentifier = result.getString(2);

					oA.getParticipatingOrganization().getIdentifier()
							.setSystem("http://dsf.dev/sid/organization-identifier").setValue(identifierValue);
					oA.getOrganization().getIdentifier().setSystem("http://dsf.dev/sid/organization-identifier")
							.setValue(organizationIdentifier);
					affiliations.add(oA);
				}

				return affiliations;
			}
		}
	}

	@Override
	public boolean existsNotDeletedByParentOrganizationMemberOrganizationRoleAndNotEndpointWithTransaction(
			Connection connection, UUID parentOrganization, UUID memberOrganization, String roleSystem, String roleCode,
			UUID endpoint) throws SQLException
	{
		Objects.requireNonNull(connection, "connection");
		Objects.requireNonNull(parentOrganization, "parentOrganization");
		Objects.requireNonNull(memberOrganization, "memberOrganization");
		Objects.requireNonNull(roleSystem, "roleSystem");
		Objects.requireNonNull(roleCode, "roleCode");
		Objects.requireNonNull(endpoint, "endpoint");

		try (PreparedStatement statement = connection
				.prepareStatement("SELECT count(*) FROM current_organization_affiliations "
						+ "WHERE organization_affiliation->'organization'->>'reference' = ? "
						+ "AND organization_affiliation->'participatingOrganization'->>'reference' = ? "
						+ "AND (SELECT jsonb_agg(coding) FROM jsonb_array_elements(organization_affiliation->'code') AS code, jsonb_array_elements(code->'coding') AS coding) @> ?::jsonb "
						+ "AND ? NOT IN (SELECT reference->>'reference' FROM jsonb_array_elements(organization_affiliation->'endpoint') AS reference)"))
		{
			statement.setString(1, "Organization/" + parentOrganization.toString());
			statement.setString(2, "Organization/" + memberOrganization.toString());
			statement.setString(3, "[{\"code\": \"" + roleCode + "\", \"system\": \"" + roleSystem + "\"}]");
			statement.setString(4, "Endpoint/" + endpoint.toString());

			try (ResultSet result = statement.executeQuery())
			{
				return result.next() && result.getInt(1) > 0;
			}
		}
	}
}
