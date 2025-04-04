package dev.dsf.fhir.dao.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.dao.ResourceDao;
import dev.dsf.fhir.dao.exception.ResourceDeletedException;
import dev.dsf.fhir.dao.exception.ResourceNotFoundException;
import dev.dsf.fhir.dao.exception.ResourceNotMarkedDeletedException;
import dev.dsf.fhir.dao.exception.ResourceVersionNoMatchException;
import dev.dsf.fhir.search.DbSearchQuery;
import dev.dsf.fhir.search.PageAndCount;
import dev.dsf.fhir.search.PartialResult;
import dev.dsf.fhir.search.SearchQuery;
import dev.dsf.fhir.search.SearchQuery.SearchQueryBuilder;
import dev.dsf.fhir.search.SearchQueryIdentityFilter;
import dev.dsf.fhir.search.SearchQueryIncludeParameter;
import dev.dsf.fhir.search.SearchQueryParameter;
import dev.dsf.fhir.search.SearchQueryParameterFactory;
import dev.dsf.fhir.search.SearchQueryRevIncludeParameter;
import dev.dsf.fhir.search.SearchQueryRevIncludeParameterFactory;
import dev.dsf.fhir.search.parameters.ResourceId;
import dev.dsf.fhir.search.parameters.ResourceLastUpdated;
import dev.dsf.fhir.search.parameters.ResourceProfile;

abstract class AbstractResourceDaoJdbc<R extends Resource> implements ResourceDao<R>, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(AbstractResourceDaoJdbc.class);

	protected static <R extends Resource> SearchQueryParameterFactory<R> factory(String parameterName,
			Supplier<SearchQueryParameter<R>> supplier)
	{
		Objects.requireNonNull(parameterName, "parameterName");
		Objects.requireNonNull(supplier, "supplier");

		return new SearchQueryParameterFactory<>(parameterName, supplier, null, null, null);
	}

	private static final class ResourceDistinctById
	{
		final IdType id;
		final Resource resource;

		ResourceDistinctById(IdType id, Resource resource)
		{
			this.id = id;
			this.resource = resource;
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(id);
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null || getClass() != obj.getClass())
				return false;
			ResourceDistinctById other = (ResourceDistinctById) obj;
			return Objects.equals(id, other.id);
		}

		public Resource getResource()
		{
			return resource;
		}
	}

	private final DataSource dataSource;
	private final DataSource permanentDeleteDataSource;
	private final Class<R> resourceType;
	private final String resourceTypeName;

	private final String resourceTable;
	private final String resourceColumn;
	private final String resourceIdColumn;

	private final PreparedStatementFactory<R> preparedStatementFactory;
	private final Function<Identity, SearchQueryIdentityFilter> identityFilter;
	private final List<SearchQueryParameterFactory<R>> searchParameterFactories = new ArrayList<>();
	private final List<SearchQueryRevIncludeParameterFactory> searchRevIncludeParameterFactories = new ArrayList<>();

	private final SearchQueryParameterFactory<R> resourceIdFactory;
	private final SearchQueryParameterFactory<R> resourceLastUpdatedFactory;
	private final SearchQueryParameterFactory<R> resourceProfileFactory;

	protected static SearchQueryRevIncludeParameterFactory factory(
			Supplier<SearchQueryRevIncludeParameter> revIncludeSupplier, List<String> revIncludeParameterValues)
	{
		Objects.requireNonNull(revIncludeSupplier, "revIncludeSupplier");
		Objects.requireNonNull(revIncludeParameterValues, "revIncludeParameterValues");

		return new SearchQueryRevIncludeParameterFactory(revIncludeSupplier, revIncludeParameterValues);
	}

	protected static <R extends Resource> SearchQueryParameterFactory<R> factory(String parameterName,
			Supplier<SearchQueryParameter<R>> supplier, List<String> nameModifiers,
			Supplier<SearchQueryIncludeParameter<R>> includeSupplier, List<String> includeParameterValues)
	{
		Objects.requireNonNull(parameterName, "parameterName");
		Objects.requireNonNull(supplier, "supplier");
		Objects.requireNonNull(nameModifiers, "nameModifiers");
		Objects.requireNonNull(includeSupplier, "includeSupplier");
		Objects.requireNonNull(includeParameterValues, "includeParameterValues");

		return new SearchQueryParameterFactory<>(parameterName, supplier, nameModifiers, includeSupplier,
				includeParameterValues);
	}

	protected static <R extends Resource> SearchQueryParameterFactory<R> factory(String parameterName,
			Supplier<SearchQueryParameter<R>> supplier, List<String> nameModifiers)
	{
		Objects.requireNonNull(parameterName, "parameterName");
		Objects.requireNonNull(supplier, "supplier");
		Objects.requireNonNull(nameModifiers, "nameModifiers");

		return new SearchQueryParameterFactory<>(parameterName, supplier, nameModifiers, null, null);
	}

	AbstractResourceDaoJdbc(DataSource dataSource, DataSource permanentDeleteDataSource, FhirContext fhirContext,
			Class<R> resourceType, String resourceTable, String resourceColumn, String resourceIdColumn,
			Function<Identity, SearchQueryIdentityFilter> userFilter,
			List<SearchQueryParameterFactory<R>> searchParameterFactories,
			List<SearchQueryRevIncludeParameterFactory> searchRevIncludeParameterFactories)
	{
		this(dataSource, permanentDeleteDataSource, resourceType, resourceTable, resourceColumn, resourceIdColumn,
				new PreparedStatementFactoryDefault<>(fhirContext, resourceType, resourceTable, resourceIdColumn,
						resourceColumn),
				userFilter, searchParameterFactories, searchRevIncludeParameterFactories);
	}

	AbstractResourceDaoJdbc(DataSource dataSource, DataSource permanentDeleteDataSource, Class<R> resourceType,
			String resourceTable, String resourceColumn, String resourceIdColumn,
			PreparedStatementFactory<R> preparedStatementFactory,
			Function<Identity, SearchQueryIdentityFilter> userFilter,
			List<SearchQueryParameterFactory<R>> searchParameterFactories,
			List<SearchQueryRevIncludeParameterFactory> searchRevIncludeParameterFactories)
	{
		this.dataSource = dataSource;
		this.permanentDeleteDataSource = permanentDeleteDataSource;
		this.resourceType = resourceType;
		resourceTypeName = Objects.requireNonNull(resourceType, "resourceType").getAnnotation(ResourceDef.class).name();

		this.resourceTable = resourceTable;
		this.resourceColumn = resourceColumn;
		this.resourceIdColumn = resourceIdColumn;

		this.preparedStatementFactory = preparedStatementFactory;

		this.identityFilter = userFilter;

		if (searchParameterFactories != null)
			this.searchParameterFactories.addAll(searchParameterFactories);
		if (searchRevIncludeParameterFactories != null)
			this.searchRevIncludeParameterFactories.addAll(searchRevIncludeParameterFactories);

		resourceIdFactory = new SearchQueryParameterFactory<>(ResourceId.PARAMETER_NAME,
				() -> new ResourceId<>(resourceType, resourceIdColumn));
		resourceLastUpdatedFactory = new SearchQueryParameterFactory<>(ResourceLastUpdated.PARAMETER_NAME,
				() -> new ResourceLastUpdated<>(resourceType, resourceColumn));
		resourceProfileFactory = new SearchQueryParameterFactory<>(ResourceProfile.PARAMETER_NAME,
				() -> new ResourceProfile<>(resourceType, resourceColumn), ResourceProfile.getNameModifiers());
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(dataSource, "dataSource");
		Objects.requireNonNull(permanentDeleteDataSource, "permanentDeleteDataSource");
		Objects.requireNonNull(resourceType, "resourceType");
		Objects.requireNonNull(resourceTable, "resourceTable");
		Objects.requireNonNull(resourceColumn, "resourceColumn");
		Objects.requireNonNull(resourceIdColumn, "resourceIdColumn");
		Objects.requireNonNull(preparedStatementFactory, "preparedStatementFactory");
		Objects.requireNonNull(identityFilter, "userFilter");
	}

	protected DataSource getDataSource()
	{
		return dataSource;
	}

	protected DataSource getPermanentDeleteDataSource()
	{
		return permanentDeleteDataSource;
	}

	protected String getResourceTable()
	{
		return resourceTable;
	}

	protected String getResourceIdColumn()
	{
		return resourceIdColumn;
	}

	protected String getResourceColumn()
	{
		return resourceColumn;
	}

	protected PreparedStatementFactory<R> getPreparedStatementFactory()
	{
		return preparedStatementFactory;
	}

	@Override
	public String getResourceTypeName()
	{
		return resourceTypeName;
	}

	@Override
	public Class<R> getResourceType()
	{
		return resourceType;
	}

	@Override
	public Connection newReadWriteTransaction() throws SQLException
	{
		Connection connection = dataSource.getConnection();
		connection.setReadOnly(false);
		connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
		connection.setAutoCommit(false);

		return connection;
	}

	@Override
	public final R create(R resource) throws SQLException
	{
		Objects.requireNonNull(resource, "resource");

		return createWithId(resource, UUID.randomUUID());
	}

	@Override
	public R createWithId(R resource, UUID uuid) throws SQLException
	{
		Objects.requireNonNull(resource, "resource");
		Objects.requireNonNull(uuid, "uuid");

		try (Connection connection = newReadWriteTransaction())
		{
			try
			{
				R createdResource = createWithTransactionAndId(connection, resource, uuid);
				connection.commit();

				return createdResource;
			}
			catch (Exception e)
			{
				connection.rollback();
				throw e;
			}
		}
	}

	@Override
	public R createWithTransactionAndId(Connection connection, R resource, UUID uuid) throws SQLException
	{
		Objects.requireNonNull(connection, "connection");
		Objects.requireNonNull(resource, "resource");
		Objects.requireNonNull(uuid, "uuid");
		if (connection.isReadOnly())
			throw new IllegalArgumentException("Connection is read-only");
		if (connection.getTransactionIsolation() != Connection.TRANSACTION_READ_COMMITTED)
			throw new IllegalArgumentException("Connection transaction isolation not READ_COMMITTED");
		if (connection.getAutoCommit())
			throw new IllegalArgumentException("Connection transaction is in auto commit mode");

		R inserted = create(connection, resource, uuid);

		logger.debug("{} with ID {} created", resourceTypeName, inserted.getId());
		return inserted;
	}

	private R create(Connection connection, R resource, UUID uuid) throws SQLException
	{
		resource = copy(resource); // XXX defensive copy, might want to remove this call
		resource.setIdElement(new IdType(resourceTypeName, uuid.toString(), FIRST_VERSION_STRING));
		resource.getMeta().setVersionId(FIRST_VERSION_STRING);
		resource.getMeta().setLastUpdated(new Date());

		try (PreparedStatement statement = connection.prepareStatement(preparedStatementFactory.getCreateSql()))
		{
			preparedStatementFactory.configureCreateStatement(statement, resource, uuid);

			statement.execute();
		}

		return resource;
	}

	protected abstract R copy(R resource);

	protected R getResource(ResultSet result, int index) throws SQLException
	{
		String json = result.getString(index);
		return preparedStatementFactory.getJsonParser().parseResource(resourceType, json);
	}

	@Override
	public final Optional<R> read(UUID uuid) throws SQLException, ResourceDeletedException
	{
		if (uuid == null)
			return Optional.empty();

		try (Connection connection = dataSource.getConnection())
		{
			return readWithTransaction(connection, uuid);
		}
	}

	@Override
	public Optional<R> readWithTransaction(Connection connection, UUID uuid)
			throws SQLException, ResourceDeletedException
	{
		Objects.requireNonNull(connection, "connection");
		if (uuid == null)
			return Optional.empty();

		try (PreparedStatement statement = connection.prepareStatement(preparedStatementFactory.getReadByIdSql()))
		{
			preparedStatementFactory.configureReadByIdStatement(statement, uuid);

			try (ResultSet result = statement.executeQuery())
			{
				if (result.next())
				{
					LocalDateTime deleted = preparedStatementFactory.getReadByIdDeleted(result);
					if (deleted != null)
					{
						long version = preparedStatementFactory.getReadByIdVersion(result);
						logger.debug("{} with IdPart {} found, but marked as deleted", resourceTypeName, uuid);
						throw newResourceDeletedException(uuid, deleted, version);
					}
					else
					{
						logger.debug("{} with IdPart {} found", resourceTypeName, uuid);
						return Optional.of(preparedStatementFactory.getReadByIdResource(result));
					}
				}
				else
				{
					logger.debug("{} with IdPart {} not found", resourceTypeName, uuid);
					return Optional.empty();
				}
			}
		}
	}

	private ResourceDeletedException newResourceDeletedException(UUID uuid, LocalDateTime deleted, long version)
	{
		return new ResourceDeletedException(new IdType(resourceTypeName, uuid.toString(), String.valueOf(version + 1)),
				deleted);
	}

	@Override
	public final Optional<R> readVersion(UUID uuid, long version) throws SQLException, ResourceDeletedException
	{
		if (uuid == null || version < FIRST_VERSION)
			return Optional.empty();

		try (Connection connection = dataSource.getConnection())
		{
			return readVersionWithTransaction(connection, uuid, version);
		}
	}

	@Override
	public Optional<R> readVersionWithTransaction(Connection connection, UUID uuid, long version)
			throws SQLException, ResourceDeletedException
	{
		Objects.requireNonNull(connection, "connection");
		if (uuid == null || version < FIRST_VERSION)
			return Optional.empty();

		try (PreparedStatement statement = connection
				.prepareStatement(preparedStatementFactory.getReadByIdAndVersionSql()))
		{
			preparedStatementFactory.configureReadByIdAndVersionStatement(statement, uuid, version);

			try (ResultSet result = statement.executeQuery())
			{
				if (result.next())
				{
					LocalDateTime deleted = preparedStatementFactory.getReadByIdVersionDeleted(result);
					long readVersion = preparedStatementFactory.getReadByIdVersionVersion(result);
					if (deleted == null && readVersion != version)
					{
						logger.debug("{} with IdPart {} and Version {} not found", resourceTypeName, uuid, version);
						return Optional.empty();
					}
					else if (deleted != null && readVersion + 1 == version)
					{
						logger.debug(
								"{} with IdPart {} and Version {} found, but marked as deleted (delete history entry)",
								resourceTypeName, uuid, version);
						throw newResourceDeletedException(uuid, deleted, readVersion);
					}
					else
					{
						logger.debug("{} with IdPart {} and Version {} found", resourceTypeName, uuid, version);
						return Optional.of(preparedStatementFactory.getReadByIdAndVersionResource(result));
					}
				}
				else
				{
					logger.debug("{} with IdPart {} and Version {} not found", resourceTypeName, uuid, version);
					return Optional.empty();
				}
			}
		}
	}

	@Override
	public Optional<R> readIncludingDeleted(UUID uuid) throws SQLException
	{
		if (uuid == null)
			return Optional.empty();

		try (Connection connection = dataSource.getConnection())
		{
			return readIncludingDeletedWithTransaction(connection, uuid);
		}
	}

	@Override
	public Optional<R> readIncludingDeletedWithTransaction(Connection connection, UUID uuid) throws SQLException
	{
		Objects.requireNonNull(connection, "connection");
		if (uuid == null)
			return Optional.empty();

		try (PreparedStatement statement = connection.prepareStatement(preparedStatementFactory.getReadByIdSql()))
		{
			preparedStatementFactory.configureReadByIdStatement(statement, uuid);

			try (ResultSet result = statement.executeQuery())
			{
				if (result.next())
				{
					if (preparedStatementFactory.getReadByIdDeleted(result) != null)
						logger.warn("{} with IdPart {} found, but marked as deleted", resourceTypeName, uuid);
					else
						logger.debug("{} with IdPart {} found", resourceTypeName, uuid);

					return Optional.of(preparedStatementFactory.getReadByIdResource(result));
				}
				else
				{
					logger.debug("{} with IdPart {} not found", resourceTypeName, uuid);
					return Optional.empty();
				}
			}
		}
	}

	@Override
	public List<R> readAll() throws SQLException
	{
		try (Connection connection = dataSource.getConnection())
		{
			return readAllWithTransaction(connection);
		}
	}

	@Override
	public List<R> readAllWithTransaction(Connection connection) throws SQLException
	{
		Objects.requireNonNull(connection, "connection");

		try (PreparedStatement statement = connection
				.prepareStatement("SELECT " + getResourceColumn() + " FROM current_" + getResourceTable()))
		{
			try (ResultSet result = statement.executeQuery())
			{
				List<R> all = new ArrayList<>();

				while (result.next())
					all.add(getResource(result, 1));

				return all;
			}
		}
	}

	@Override
	public boolean existsNotDeleted(String idString, String versionString) throws SQLException
	{
		try (Connection connection = dataSource.getConnection())
		{
			return existsNotDeletedWithTransaction(connection, idString, versionString);
		}
	}

	@Override
	public boolean existsNotDeletedWithTransaction(Connection connection, String idString, String versionString)
			throws SQLException
	{
		Objects.requireNonNull(connection, "connection");

		UUID uuid = toUuid(idString);

		if (uuid == null)
			return false;

		if (versionString == null || versionString.isBlank())
		{
			try (PreparedStatement statement = connection.prepareStatement("SELECT deleted IS NOT NULL FROM "
					+ resourceTable + " WHERE " + resourceIdColumn + " = ? ORDER BY version DESC LIMIT 1"))
			{
				statement.setObject(1, preparedStatementFactory.uuidToPgObject(uuid));

				try (ResultSet result = statement.executeQuery())
				{
					return result.next() && !result.getBoolean(1);
				}
			}
		}
		else
		{
			Long version = toLong(versionString);
			if (version == null || version < FIRST_VERSION)
				return false;

			try (PreparedStatement statement = connection.prepareStatement("SELECT deleted IS NOT NULL FROM "
					+ resourceTable + " WHERE " + resourceIdColumn + " = ? AND version = ?"))
			{
				statement.setObject(1, preparedStatementFactory.uuidToPgObject(uuid));
				statement.setLong(2, version);

				try (ResultSet result = statement.executeQuery())
				{
					return result.next() && !result.getBoolean(1);
				}
			}
		}
	}

	@Override
	public final R update(R resource, Long expectedVersion)
			throws SQLException, ResourceNotFoundException, ResourceVersionNoMatchException
	{
		Objects.requireNonNull(resource, "resource");
		// expectedVersion may be null

		try (Connection connection = newReadWriteTransaction())
		{
			try
			{
				R updatedResource = updateWithTransaction(connection, resource, expectedVersion);

				connection.commit();

				return updatedResource;
			}
			catch (Exception e)
			{
				connection.rollback();
				throw e;
			}
		}
	}

	@Override
	public R updateWithTransaction(Connection connection, R resource, Long expectedVersion)
			throws SQLException, ResourceNotFoundException, ResourceVersionNoMatchException
	{
		Objects.requireNonNull(connection, "connection");
		Objects.requireNonNull(resource, "resource");
		// expectedVersion may be null
		if (connection.isReadOnly())
			throw new IllegalArgumentException("Connection is read-only");
		if (connection.getTransactionIsolation() != Connection.TRANSACTION_READ_COMMITTED)
			throw new IllegalArgumentException("Connection transaction isolation not READ_COMMITTED");
		if (connection.getAutoCommit())
			throw new IllegalArgumentException("Connection transaction is in auto commit mode");

		resource = copy(resource); // XXX defensive copy, might want to remove this call

		LatestVersion latestVersion = getLatestVersion(resource, connection);

		if (expectedVersion != null && expectedVersion != latestVersion.version)
		{
			logger.info("Expected version {} does not match latest version {}", expectedVersion, latestVersion.version);
			throw new ResourceVersionNoMatchException(resource.getIdElement().getIdPart(), expectedVersion,
					latestVersion.version);
		}

		// latestVersion gives stored latest version +1 if resource is deleted
		long newVersion = latestVersion.version + 1;

		R updated = update(connection, resource, newVersion);

		logger.debug("{} with IdPart {} updated, new version {}", resourceTypeName, updated.getIdElement().getIdPart(),
				newVersion);
		return updated;
	}

	protected final UUID toUuid(String id)
	{
		if (id == null)
			return null;

		// TODO control flow by exception
		try
		{
			return UUID.fromString(id);
		}
		catch (IllegalArgumentException e)
		{
			return null;
		}
	}

	protected final Long toLong(String version)
	{
		if (version == null)
			return null;

		// TODO control flow by exception
		try
		{
			return Long.parseLong(version);
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}

	private R update(Connection connection, R resource, long version) throws SQLException
	{
		UUID uuid = toUuid(resource.getIdElement().getIdPart());
		if (uuid == null)
			throw new IllegalArgumentException("resource.id is not a UUID");

		resource = copy(resource);
		String versionAsString = String.valueOf(version);
		resource.setIdElement(new IdType(resourceTypeName, resource.getIdElement().getIdPart(), versionAsString));
		resource.getMeta().setVersionId(versionAsString);
		resource.getMeta().setLastUpdated(new Date());

		try (PreparedStatement statement = connection.prepareStatement(preparedStatementFactory.getUpdateNewRowSql()))
		{
			preparedStatementFactory.configureUpdateNewRowSqlStatement(statement, uuid, version, resource);

			statement.execute();
		}

		return resource;
	}

	protected static class LatestVersion
	{
		final long version;
		final boolean deleted;

		LatestVersion(long version, boolean deleted)
		{
			this.version = version + (deleted ? 1 : 0);
			this.deleted = deleted;
		}
	}

	protected final LatestVersion getLatestVersion(R resource, Connection connection)
			throws SQLException, ResourceNotFoundException
	{
		UUID uuid = toUuid(resource.getIdElement().getIdPart());
		if (uuid == null)
			throw new ResourceNotFoundException(resource.getId() != null ? resource.getId() : "'null'");

		return getLatestVersion(uuid, connection);
	}

	protected final LatestVersion getLatestVersion(UUID uuid, Connection connection)
			throws SQLException, ResourceNotFoundException
	{
		if (uuid == null)
			throw new ResourceNotFoundException("'null'");

		return getLatestVersionIfExists(uuid, connection)
				.orElseThrow(() -> new ResourceNotFoundException(uuid.toString()));
	}

	protected final Optional<LatestVersion> getLatestVersionIfExists(UUID uuid, Connection connection)
			throws SQLException, ResourceNotFoundException
	{
		if (uuid == null)
			return Optional.empty();

		try (PreparedStatement statement = connection.prepareStatement("SELECT version, deleted IS NOT NULL FROM "
				+ resourceTable + " WHERE " + resourceIdColumn + " = ? ORDER BY version DESC LIMIT 1"))
		{
			statement.setObject(1, preparedStatementFactory.uuidToPgObject(uuid));

			try (ResultSet result = statement.executeQuery())
			{
				if (result.next())
				{
					long version = result.getLong(1);
					boolean deleted = result.getBoolean(2);

					logger.debug("Latest version for {} with IdPart {} is {}{}", resourceTypeName, uuid.toString(),
							version, deleted ? ", resource marked as deleted" : "");
					return Optional.of(new LatestVersion(version, deleted));
				}
				else
				{
					logger.debug("{} with IdPart {} not found", resourceTypeName, uuid.toString());
					return Optional.empty();
				}
			}
		}
	}

	@Override
	public final boolean delete(UUID uuid) throws SQLException, ResourceNotFoundException
	{
		try (Connection connection = dataSource.getConnection())
		{
			connection.setReadOnly(false);

			return deleteWithTransaction(connection, uuid);
		}
	}

	@Override
	public boolean deleteWithTransaction(Connection connection, UUID uuid)
			throws SQLException, ResourceNotFoundException
	{
		return delete(connection, uuid);
	}

	protected final boolean delete(Connection connection, UUID uuid) throws SQLException, ResourceNotFoundException
	{
		Objects.requireNonNull(connection, "connection");
		if (connection.isReadOnly())
			throw new IllegalStateException("Connection is read-only");
		if (uuid == null)
			throw new ResourceNotFoundException("'null'");

		LatestVersion latestVersion = getLatestVersion(uuid, connection);

		if (latestVersion.deleted)
			return false;

		try (PreparedStatement statement = connection.prepareStatement("UPDATE " + resourceTable
				+ " SET deleted = ? WHERE " + resourceIdColumn + " = ? AND version = (SELECT MAX(version) FROM "
				+ resourceTable + " WHERE " + resourceIdColumn + " = ?)"))
		{
			statement.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
			statement.setObject(2, preparedStatementFactory.uuidToPgObject(uuid));
			statement.setObject(3, preparedStatementFactory.uuidToPgObject(uuid));

			statement.execute();

			logger.debug("{} with ID {} marked as deleted", resourceTypeName, uuid);
			return true;
		}
	}

	@Override
	public final PartialResult<R> search(DbSearchQuery query) throws SQLException
	{
		Objects.requireNonNull(query, "query");

		try (Connection connection = newReadWriteTransaction())
		{
			PartialResult<R> result = searchWithTransaction(connection, query);
			connection.commit();
			connection.commit();
			return result;
		}
	}

	@Override
	public PartialResult<R> searchWithTransaction(Connection connection, DbSearchQuery query) throws SQLException
	{
		Objects.requireNonNull(connection, "connection");
		Objects.requireNonNull(query, "query");

		int total = 0;
		try (PreparedStatement statement = connection.prepareStatement(query.getCountSql()))
		{
			query.modifyStatement(statement, connection::createArrayOf);

			try (ResultSet result = statement.executeQuery())
			{
				if (result.next())
					total = result.getInt(1);
			}
		}

		List<R> partialResult = new ArrayList<>();
		List<Resource> includes = new ArrayList<>();

		if (!query.getPageAndCount().isCountOnly(total))
		{
			try (PreparedStatement statement = connection.prepareStatement(query.getSearchSql()))
			{
				query.modifyStatement(statement, connection::createArrayOf);

				try (ResultSet result = statement.executeQuery())
				{
					ResultSetMetaData metaData = result.getMetaData();
					while (result.next())
					{
						R resource = getResource(result, 1);
						modifySearchResultResource(resource, connection);
						partialResult.add(resource);

						for (int columnIndex = 2; columnIndex <= metaData.getColumnCount(); columnIndex++)
							getResources(result, columnIndex, includes, connection, query);
					}
				}
			}
		}

		// needs to be filtered by read rules, before returning to user, see rest access layer
		includes = includes.stream().map(r -> new ResourceDistinctById(r.getIdElement(), r)).distinct()
				.map(ResourceDistinctById::getResource).collect(Collectors.toList());

		return new PartialResult<>(total, query.getPageAndCount(), partialResult, includes);
	}

	/**
	 * Override this method to modify resources retrieved by search queries before returning to the user. This method
	 * can be used, if the resource returned by the search is not complete and additional content needs to be retrieved.
	 * For example the content of a {@link Binary} resource might not be stored in the json column.
	 *
	 * @param resource
	 *            not <code>null</code>
	 * @param connection
	 *            not <code>null</code>
	 * @throws SQLException
	 *             if database access errors occur
	 */
	protected void modifySearchResultResource(R resource, Connection connection) throws SQLException
	{
	}

	private void getResources(ResultSet result, int columnIndex, List<? super Resource> includeResources,
			Connection connection, DbSearchQuery query) throws SQLException
	{
		String json = result.getString(columnIndex);

		if (json == null)
			return;

		JsonArray array = (JsonArray) JsonParser.parseString(json);
		for (JsonElement jsonElement : array)
		{
			IBaseResource resource = preparedStatementFactory.getJsonParser().parseResource(jsonElement.toString());
			if (resource instanceof Resource r)
			{
				query.modifyIncludeResource(r, columnIndex, connection);
				includeResources.add(r);
			}
			else
				logger.warn("parsed resouce of type {} not instance of {}, ignoring include resource",
						resource.getClass().getName(), Resource.class.getName());
		}
	}

	@Override
	public final SearchQuery<R> createSearchQuery(Identity identity, PageAndCount pageAndCount)
	{
		Objects.requireNonNull(identity, "identity");

		return doCreateSearchQuery(identity, pageAndCount);
	}

	@Override
	public SearchQuery<R> createSearchQueryWithoutUserFilter(PageAndCount pageAndCount)
	{
		return doCreateSearchQuery(null, pageAndCount);
	}

	private SearchQuery<R> doCreateSearchQuery(Identity identity, PageAndCount pageAndCount)
	{
		Objects.requireNonNull(pageAndCount, "pageAndCount");

		var builder = SearchQueryBuilder.create(resourceType, getResourceTable(), getResourceColumn(), pageAndCount);

		if (identity != null)
			builder = builder.with(identityFilter.apply(identity));

		return builder.with(resourceIdFactory).with(resourceLastUpdatedFactory).with(resourceProfileFactory)
				.with(searchParameterFactories).withRevInclude(searchRevIncludeParameterFactories).build();
	}

	@Override
	public void deletePermanently(UUID uuid)
			throws SQLException, ResourceNotFoundException, ResourceNotMarkedDeletedException
	{
		try (Connection connection = getPermanentDeleteDataSource().getConnection())
		{
			connection.setReadOnly(false);
			connection.setAutoCommit(false);

			deletePermanentlyWithTransaction(connection, uuid);
			connection.commit();
		}
	}

	@Override
	public void deletePermanentlyWithTransaction(Connection connection, UUID uuid)
			throws SQLException, ResourceNotFoundException, ResourceNotMarkedDeletedException
	{
		Objects.requireNonNull(connection, "connection");
		if (connection.isReadOnly())
			throw new IllegalStateException("Connection is read-only");
		if (uuid == null)
			throw new ResourceNotFoundException("'null'");

		LatestVersion latestVersion = getLatestVersion(uuid, connection);

		if (!latestVersion.deleted)
			throw new ResourceNotMarkedDeletedException(uuid.toString());

		try (PreparedStatement statement = connection
				.prepareStatement("DELETE FROM " + resourceTable + " WHERE " + resourceIdColumn + "= ?"))
		{
			statement.setObject(1, preparedStatementFactory.uuidToPgObject(uuid));

			statement.execute();

			logger.debug("{} with ID {} deleted permanently", resourceTypeName, uuid);
		}
	}
}
