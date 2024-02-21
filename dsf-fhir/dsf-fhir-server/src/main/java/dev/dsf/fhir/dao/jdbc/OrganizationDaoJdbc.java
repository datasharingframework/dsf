package dev.dsf.fhir.dao.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.Organization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.fhir.dao.OrganizationDao;
import dev.dsf.fhir.search.filter.OrganizationIdentityFilter;
import dev.dsf.fhir.search.parameters.OrganizationActive;
import dev.dsf.fhir.search.parameters.OrganizationEndpoint;
import dev.dsf.fhir.search.parameters.OrganizationIdentifier;
import dev.dsf.fhir.search.parameters.OrganizationName;
import dev.dsf.fhir.search.parameters.OrganizationType;
import dev.dsf.fhir.search.parameters.rev.include.EndpointOrganizationRevInclude;
import dev.dsf.fhir.search.parameters.rev.include.OrganizationAffiliationParticipatingOrganizationRevInclude;
import dev.dsf.fhir.search.parameters.rev.include.OrganizationAffiliationPrimaryOrganizationRevInclude;

public class OrganizationDaoJdbc extends AbstractResourceDaoJdbc<Organization> implements OrganizationDao
{
	private static final Logger logger = LoggerFactory.getLogger(OrganizationDaoJdbc.class);

	public OrganizationDaoJdbc(DataSource dataSource, DataSource permanentDeleteDataSource, FhirContext fhirContext)
	{
		super(dataSource, permanentDeleteDataSource, fhirContext, Organization.class, "organizations", "organization",
				"organization_id", OrganizationIdentityFilter::new,
				List.of(factory(OrganizationActive.PARAMETER_NAME, OrganizationActive::new),
						factory(OrganizationEndpoint.PARAMETER_NAME, OrganizationEndpoint::new,
								OrganizationEndpoint.getNameModifiers(), OrganizationEndpoint::new,
								OrganizationEndpoint.getIncludeParameterValues()),
						factory(OrganizationIdentifier.PARAMETER_NAME, OrganizationIdentifier::new,
								OrganizationIdentifier.getNameModifiers()),
						factory(OrganizationName.PARAMETER_NAME, OrganizationName::new,
								OrganizationName.getNameModifiers()),
						factory(OrganizationType.PARAMETER_NAME, OrganizationType::new,
								OrganizationType.getNameModifiers())),
				List.of(factory(EndpointOrganizationRevInclude::new,
						EndpointOrganizationRevInclude.getRevIncludeParameterValues()),
						factory(OrganizationAffiliationPrimaryOrganizationRevInclude::new,
								OrganizationAffiliationPrimaryOrganizationRevInclude.getRevIncludeParameterValues()),
						factory(OrganizationAffiliationParticipatingOrganizationRevInclude::new,
								OrganizationAffiliationParticipatingOrganizationRevInclude
										.getRevIncludeParameterValues())));
	}

	@Override
	protected Organization copy(Organization resource)
	{
		return resource.copy();
	}

	@Override
	public Optional<Organization> readActiveNotDeletedByThumbprint(String thumbprintHex) throws SQLException
	{
		if (thumbprintHex == null || thumbprintHex.isBlank())
			return Optional.empty();

		try (Connection connection = getDataSource().getConnection();
				PreparedStatement statement = connection.prepareStatement(
						"SELECT organization FROM current_organizations WHERE organization->'extension' @> ?::jsonb AND organization->>'active' = 'true'"))
		{

			String search = "[{\"url\": \"http://dsf.dev/fhir/StructureDefinition/extension-certificate-thumbprint\", \"valueString\": \""
					+ thumbprintHex + "\"}]";
			statement.setString(1, search);

			try (ResultSet result = statement.executeQuery())
			{
				if (result.next())
				{
					Organization organization = getResource(result, 1);
					if (result.next())
					{
						logger.warn("Found multiple Organizations with thumbprint {}", thumbprintHex);
						throw new SQLException("Found multiple Organizations with thumbprint " + thumbprintHex
								+ ", single result expected");
					}
					else
					{
						logger.debug("Organization with thumbprint {}, IdPart {} found", thumbprintHex,
								organization.getIdElement().getIdPart());
						return Optional.of(organization);
					}
				}
				else
				{
					logger.debug("Organization with thumbprint {} not found", thumbprintHex);
					return Optional.empty();
				}
			}
		}
	}

	@Override
	public Optional<Organization> readActiveNotDeletedByIdentifier(String identifierValue) throws SQLException
	{
		if (identifierValue == null || identifierValue.isBlank())
			return Optional.empty();

		try (Connection connection = getDataSource().getConnection();
				PreparedStatement statement = connection.prepareStatement(
						"SELECT organization FROM current_organizations WHERE organization->'identifier' @> ?::jsonb AND organization->>'active' = 'true'"))
		{

			String search = "[{\"system\": \"http://dsf.dev/sid/organization-identifier\", \"value\": \""
					+ identifierValue + "\"}]";
			statement.setString(1, search);

			try (ResultSet result = statement.executeQuery())
			{
				if (result.next())
				{
					Organization organization = getResource(result, 1);
					if (result.next())
					{
						logger.warn("Found multiple Organizations with identifier {}", identifierValue);
						throw new SQLException("Found multiple Organizations with identifier " + identifierValue
								+ ", single result expected");
					}
					else
					{
						logger.debug("Organization with identifier {}, IdPart {} found", identifierValue,
								organization.getIdElement().getIdPart());
						return Optional.of(organization);
					}
				}
				else
				{
					logger.warn("Organization with identifier {} not found", identifierValue);
					return Optional.empty();
				}
			}
		}
	}

	@Override
	public boolean existsNotDeletedByThumbprintWithTransaction(Connection connection, String thumbprintHex)
			throws SQLException
	{
		if (thumbprintHex == null || thumbprintHex.isBlank())
			return false;

		try (PreparedStatement statement = connection.prepareStatement(
				"SELECT organization FROM current_organizations WHERE organization->'extension' @> ?::jsonb"))
		{
			String search = "[{\"url\": \"http://dsf.dev/fhir/StructureDefinition/extension-certificate-thumbprint\", \"valueString\": \""
					+ thumbprintHex + "\"}]";
			statement.setString(1, search);

			try (ResultSet result = statement.executeQuery())
			{
				if (result.next())
				{
					Organization organization = getResource(result, 1);
					logger.debug("Organization with thumbprint {}, IdPart {} found", thumbprintHex,
							organization.getIdElement().getIdPart());
					return true;
				}
				else
				{
					logger.debug("Organization with thumbprint {} not found", thumbprintHex);
					return false;
				}
			}
		}
	}
}
