package dev.dsf.fhir.dao.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.NamingSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.fhir.dao.NamingSystemDao;
import dev.dsf.fhir.search.filter.NamingSystemIdentityFilter;
import dev.dsf.fhir.search.parameters.NamingSystemDate;
import dev.dsf.fhir.search.parameters.NamingSystemName;
import dev.dsf.fhir.search.parameters.NamingSystemStatus;

public class NamingSystemDaoJdbc extends AbstractResourceDaoJdbc<NamingSystem> implements NamingSystemDao
{
	private static final Logger logger = LoggerFactory.getLogger(NamingSystemDaoJdbc.class);

	public NamingSystemDaoJdbc(DataSource dataSource, DataSource permanentDeleteDataSource, FhirContext fhirContext)
	{
		super(dataSource, permanentDeleteDataSource, fhirContext, NamingSystem.class, "naming_systems", "naming_system",
				"naming_system_id", NamingSystemIdentityFilter::new,
				List.of(factory(NamingSystemDate.PARAMETER_NAME, NamingSystemDate::new),
						factory(NamingSystemName.PARAMETER_NAME, NamingSystemName::new,
								NamingSystemName.getNameModifiers()),
						factory(NamingSystemStatus.PARAMETER_NAME, NamingSystemStatus::new,
								NamingSystemStatus.getNameModifiers())),
				List.of());
	}

	@Override
	protected NamingSystem copy(NamingSystem resource)
	{
		return resource.copy();
	}

	@Override
	public Optional<NamingSystem> readByName(String name) throws SQLException
	{
		try (Connection connection = getDataSource().getConnection())
		{
			return readByNameWithTransaction(connection, name);
		}
	}

	@Override
	public Optional<NamingSystem> readByNameWithTransaction(Connection connection, String name) throws SQLException
	{
		Objects.requireNonNull(connection, "connection");
		if (name == null || name.isBlank())
			return Optional.empty();

		try (PreparedStatement statement = connection
				.prepareStatement("SELECT naming_system FROM current_naming_systems WHERE naming_system->>'name' = ?"))
		{
			statement.setString(1, name);

			try (ResultSet result = statement.executeQuery())
			{
				if (result.next())
					return Optional.of(getResource(result, 1));
				else
					return Optional.empty();
			}
		}
	}

	@Override
	public boolean existsWithUniqueIdUriEntry(Connection connection, String uniqueIdValue) throws SQLException
	{
		Objects.requireNonNull(connection, "connection");
		if (uniqueIdValue == null || uniqueIdValue.isBlank())
			return false;

		final String namingSystem = "{\"uniqueId\":[{\"type\":\"uri\",\"value\":\"" + uniqueIdValue + "\"}]}";

		try (PreparedStatement statement = connection.prepareStatement(
				"SELECT count(*) FROM current_naming_systems WHERE naming_system->>'status' IN ('draft', 'active') AND naming_system @> ?::jsonb"))
		{
			statement.setString(1, namingSystem);

			try (ResultSet result = statement.executeQuery())
			{
				boolean found = result.next() && result.getInt(1) > 0;

				logger.debug("NamingSystem with uniqueId entry (uri/value({})) {}found", uniqueIdValue,
						found ? "" : "not ");

				return found;
			}
		}
	}

	@Override
	public boolean existsWithUniqueIdUriEntryResolvable(Connection connection, String uniqueIdValue) throws SQLException
	{
		Objects.requireNonNull(connection, "connection");
		if (uniqueIdValue == null || uniqueIdValue.isBlank())
			return false;

		final String namingSystem = "{\"uniqueId\":[{\"modifierExtension\":[{\"url\":\"http://dsf.dev/fhir/StructureDefinition/extension-check-logical-reference\",\"valueBoolean\":true}],"
				+ "\"value\":\"" + uniqueIdValue + "\"}]}";

		try (PreparedStatement statement = connection.prepareStatement(
				"SELECT count(*) FROM current_naming_systems WHERE naming_system->>'status' IN ('draft', 'active') AND naming_system @> ?::jsonb"))
		{
			statement.setString(1, namingSystem);

			try (ResultSet result = statement.executeQuery())
			{
				boolean found = result.next() && result.getInt(1) > 0;

				logger.debug(
						"NamingSystem with uniqueId entry (uri/value({})) and check-logical-reference true {}found",
						uniqueIdValue, found ? "" : "not ");

				return found;
			}
		}
	}
}
