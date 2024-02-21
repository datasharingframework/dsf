package dev.dsf.bpe.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.ResourceType;
import org.postgresql.util.PGobject;

import ca.uhn.fhir.parser.DataFormatException;
import dev.dsf.bpe.plugin.ProcessIdAndVersion;
import dev.dsf.bpe.plugin.ProcessesResource;
import dev.dsf.bpe.plugin.ResourceInfo;

public class ProcessPluginResourcesDaoJdbc extends AbstractDaoJdbc implements ProcessPluginResourcesDao
{
	public ProcessPluginResourcesDaoJdbc(DataSource dataSource)
	{
		super(dataSource);
	}

	@Override
	public Map<ProcessIdAndVersion, List<ResourceInfo>> getResources() throws SQLException
	{
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(
						"SELECT process_key_and_version, resource_type, resource_id, url, version, name, identifier FROM process_plugin_resources ORDER BY process_key_and_version"))
		{
			try (ResultSet result = statement.executeQuery())
			{
				Map<ProcessIdAndVersion, List<ResourceInfo>> resources = new HashMap<>();

				ProcessIdAndVersion processKeyAndVersion = null;
				List<ResourceInfo> processKeyAndVersionResources = null;
				while (result.next())
				{
					ProcessIdAndVersion currentProcessKeyAndVersion = ProcessIdAndVersion
							.fromString(result.getString(1));

					if (!currentProcessKeyAndVersion.equals(processKeyAndVersion))
					{
						processKeyAndVersion = currentProcessKeyAndVersion;
						processKeyAndVersionResources = new ArrayList<>();
						resources.put(processKeyAndVersion, processKeyAndVersionResources);
					}

					String resourceTypeString = result.getString(2);
					UUID resourceId = result.getObject(3, UUID.class);
					String url = result.getString(4);
					String version = result.getString(5);
					String name = result.getString(6);
					String identifier = result.getString(7);

					ResourceInfo resourceInfo = new ResourceInfo(
							resourceTypeString == null ? null : ResourceType.valueOf(resourceTypeString), url, version,
							name, identifier).setResourceId(resourceId);
					processKeyAndVersionResources.add(resourceInfo);
				}

				return resources;
			}
		}
	}

	@Override
	public void addOrRemoveResources(Collection<? extends ProcessesResource> newResources,
			List<UUID> deletedResourcesIds, List<ProcessIdAndVersion> excludedProcesses) throws SQLException
	{
		Objects.requireNonNull(newResources, "newResources");
		Objects.requireNonNull(deletedResourcesIds, "deletedResourcesIds");
		Objects.requireNonNull(excludedProcesses, "excludedProcesses");

		if (newResources.isEmpty())
			return;

		try (Connection connection = dataSource.getConnection())
		{
			connection.setReadOnly(false);
			connection.setAutoCommit(false);

			for (ProcessesResource resource : newResources)
			{
				for (ProcessIdAndVersion process : resource.getProcesses())
				{
					final ResourceType resourceType = resource.getResourceInfo().getResourceType();

					// non NamingSystem and non Task resources
					if (!ResourceType.NamingSystem.equals(resourceType) && !ResourceType.Task.equals(resourceType))
					{
						try (PreparedStatement statement = connection.prepareStatement(
								"INSERT INTO process_plugin_resources (process_key_and_version, resource_type, resource_id, url, version) VALUES (?, ?, ?, ?, ?) "
										+ "ON CONFLICT (process_key_and_version, resource_type, url, version) "
										+ "WHERE resource_type <> 'NamingSystem'" + " DO UPDATE SET resource_id = ?"))
						{
							ResourceInfo resourceInfo = resource.getResourceInfo();

							statement.setString(1, process.toString());
							statement.setString(2, resourceType.name());
							statement.setObject(3, uuidToPgObject(resourceInfo.getResourceId()));
							statement.setString(4, resourceInfo.getUrl());
							statement.setString(5, resourceInfo.getVersion());

							statement.setObject(6, uuidToPgObject(resourceInfo.getResourceId()));

							statement.addBatch();

							statement.executeBatch();
						}
						catch (SQLException e)
						{
							connection.rollback();
							throw e;
						}
					}
					else if (ResourceType.NamingSystem.equals(resourceType))
					{
						// NamingSystem resources
						try (PreparedStatement statement = connection.prepareStatement(
								"INSERT INTO process_plugin_resources (process_key_and_version, resource_type, resource_id, name) VALUES (?, 'NamingSystem', ?, ?) "
										+ "ON CONFLICT (process_key_and_version, resource_type, name) "
										+ "WHERE resource_type = 'NamingSystem'" + " DO UPDATE SET resource_id = ?"))
						{

							ResourceInfo resourceInfo = resource.getResourceInfo();

							statement.setString(1, process.toString());
							statement.setObject(2, uuidToPgObject(resourceInfo.getResourceId()));
							statement.setString(3, resourceInfo.getName());

							statement.setObject(4, uuidToPgObject(resourceInfo.getResourceId()));

							statement.addBatch();

							statement.executeBatch();
						}
						catch (SQLException e)
						{
							connection.rollback();
							throw e;
						}
					}
					else if (ResourceType.Task.equals(resourceType))
					{
						// Task resources
						try (PreparedStatement statement = connection.prepareStatement(
								"INSERT INTO process_plugin_resources (process_key_and_version, resource_type, resource_id, identifier) VALUES (?, 'Task', ?, ?) "
										+ "ON CONFLICT (process_key_and_version, resource_type, identifier) "
										+ "WHERE resource_type = 'Task'" + " DO UPDATE SET resource_id = ?"))
						{

							ResourceInfo resourceInfo = resource.getResourceInfo();

							statement.setString(1, process.toString());
							statement.setObject(2, uuidToPgObject(resourceInfo.getResourceId()));
							statement.setString(3, resourceInfo.getIdentifier());

							statement.setObject(4, uuidToPgObject(resourceInfo.getResourceId()));

							statement.addBatch();

							statement.executeBatch();
						}
						catch (SQLException e)
						{
							connection.rollback();
							throw e;
						}
					}
				}
			}

			try (PreparedStatement statement = connection
					.prepareStatement("DELETE FROM process_plugin_resources WHERE resource_id = ?"))
			{
				for (UUID deletedId : deletedResourcesIds)
				{
					statement.setObject(1, uuidToPgObject(deletedId));

					statement.addBatch();
				}

				statement.executeBatch();
			}
			catch (SQLException e)
			{
				connection.rollback();
				throw e;
			}

			try (PreparedStatement statement = connection
					.prepareStatement("DELETE FROM process_plugin_resources WHERE process_key_and_version = ?"))
			{
				for (ProcessIdAndVersion process : excludedProcesses)
				{
					statement.setString(1, process.toString());

					statement.addBatch();
				}

				statement.executeBatch();
			}
			catch (SQLException e)
			{
				connection.rollback();
				throw e;
			}

			connection.commit();
		}
	}

	private PGobject uuidToPgObject(UUID uuid)
	{
		if (uuid == null)
			return null;

		try
		{
			PGobject o = new PGobject();
			o.setType("UUID");
			o.setValue(uuid.toString());
			return o;
		}
		catch (DataFormatException | SQLException e)
		{
			throw new RuntimeException(e);
		}
	}
}
