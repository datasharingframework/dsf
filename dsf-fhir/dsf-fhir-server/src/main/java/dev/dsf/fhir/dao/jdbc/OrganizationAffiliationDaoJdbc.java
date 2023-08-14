package dev.dsf.fhir.dao.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.OrganizationAffiliation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private static final Logger logger = LoggerFactory.getLogger(OrganizationAffiliationDaoJdbc.class);

	public OrganizationAffiliationDaoJdbc(DataSource dataSource, DataSource permanentDeleteDataSource,
			FhirContext fhirContext)
	{
		super(dataSource, permanentDeleteDataSource, fhirContext, OrganizationAffiliation.class,
				"organization_affiliations", "organization_affiliation", "organization_affiliation_id",
				OrganizationAffiliationIdentityFilter::new,
				Arrays.asList(factory(OrganizationAffiliationActive.PARAMETER_NAME, OrganizationAffiliationActive::new),
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
				Collections.emptyList());
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
			return Collections.emptyList();

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

			logger.trace("Executing query '{}'", statement);
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
}
