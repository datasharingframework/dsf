package dev.dsf.fhir.dao.jdbc;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.Binary;
import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.fhir.dao.BinaryDao;
import dev.dsf.fhir.dao.exception.ResourceDeletedException;
import dev.dsf.fhir.dao.exception.ResourceNotFoundException;
import dev.dsf.fhir.dao.exception.ResourceVersionNoMatchException;
import dev.dsf.fhir.model.DeferredBase64BinaryTypeImpl;
import dev.dsf.fhir.model.StreamableBase64BinaryType;
import dev.dsf.fhir.search.filter.BinaryIdentityFilter;
import dev.dsf.fhir.search.parameters.BinaryContentType;
import dev.dsf.fhir.webservice.RangeRequest;

public class BinaryDaoJdbc extends AbstractResourceDaoJdbc<Binary> implements BinaryDao
{
	private static final Logger logger = LoggerFactory.getLogger(BinaryDaoJdbc.class);

	private final String selectUpdateUser;

	private final ExecutorService loUnlinker;

	public BinaryDaoJdbc(DataSource dataSource, DataSource permanentDeleteDataSource, FhirContext fhirContext,
			String selectUpdateUser)
	{
		super(dataSource, permanentDeleteDataSource, Binary.class, "binaries", "binary_json", "binary_id",
				new PreparedStatementFactoryBinary(fhirContext), BinaryIdentityFilter::new,
				List.of(factory(BinaryContentType.PARAMETER_NAME, BinaryContentType::new,
						BinaryContentType.getNameModifiers())),
				List.of());

		this.selectUpdateUser = selectUpdateUser;

		loUnlinker = Executors.newFixedThreadPool(1, r -> new Thread(r, "binaries-large-object-unlinker"));
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		startLargeObjectUnlink();
	}

	@Override
	protected PreparedStatementFactoryBinary getPreparedStatementFactory()
	{
		return (PreparedStatementFactoryBinary) super.getPreparedStatementFactory();
	}

	@Override
	public LargeObjectManager createLargeObjectManager(Connection connection)
	{
		return new LargeObjectManagerJdbc(getPermanentDeleteDataSource(), selectUpdateUser, connection);
	}

	private void readData(Binary resource, OutputStream out) throws IOException
	{
		RangeRequest rangeRequest = (RangeRequest) resource.getUserData(RangeRequest.USER_DATA_VALUE_RANGE_REQUEST);

		try (Connection connection = getDataSource().getConnection())
		{
			connection.setAutoCommit(false);

			try (PreparedStatement statement = connection.prepareStatement(
					"SELECT binary_oid, binary_size FROM binaries WHERE binary_id = ? AND version = ?"))
			{
				PGobject uuidObject = getPreparedStatementFactory()
						.uuidToPgObject(toUuid(resource.getIdElement().getIdPart()));
				Long version = resource.getMeta().getVersionIdElement().getIdPartAsLong();

				statement.setObject(1, uuidObject);
				statement.setLong(2, version);

				try (ResultSet result = statement.executeQuery())
				{
					if (result.next())
					{
						long oid = result.getLong(1);
						long dataSize = result.getLong(2);

						if (dataSize <= 0)
							return;

						LargeObjectManager largeObjectManager = createLargeObjectManager(connection);
						largeObjectManager.read(oid, dataSize, rangeRequest, out);
					}
					else
						throw new SQLException(
								"Binary resource with id " + resource.getIdElement().getIdPart() + " not found");
				}
			}

			connection.commit();
		}
		catch (SQLException e)
		{
			logger.debug("Unable to read data for Binary resource", e);
			logger.warn("Unable to read data for Binary resource: {} - {}", e.getClass().getName(), e.getMessage());

			throw new IOException(e);
		}
	}

	@Override
	public Binary createWithTransactionAndId(LargeObjectManager largeObjectManager, Connection connection,
			Binary resource, UUID uuid) throws SQLException
	{
		Binary created = super.createWithTransactionAndId(largeObjectManager, connection, resource, uuid);

		if (created.getDataElement() instanceof StreamableBase64BinaryType)
			created.setDataElement(new DeferredBase64BinaryTypeImpl(out -> readData(created, out)));

		return created;
	}

	@Override
	public Binary updateWithTransaction(LargeObjectManager largeObjectManager, Connection connection, Binary resource,
			Long expectedVersion) throws SQLException, ResourceNotFoundException, ResourceVersionNoMatchException
	{
		Binary updated = super.updateWithTransaction(largeObjectManager, connection, resource, expectedVersion);

		if (updated.getDataElement() instanceof StreamableBase64BinaryType)
			updated.setDataElement(new DeferredBase64BinaryTypeImpl(out -> readData(updated, out)));

		return updated;
	}

	@Override
	public Optional<Binary> readWithTransaction(Connection connection, UUID uuid)
			throws SQLException, ResourceDeletedException
	{
		Optional<Binary> read = super.readWithTransaction(connection, uuid);
		return read.map(r -> r.setDataElement(new DeferredBase64BinaryTypeImpl(out -> readData(r, out))));
	}

	@Override
	public Optional<Binary> readVersionWithTransaction(Connection connection, UUID uuid, long version)
			throws SQLException, ResourceDeletedException
	{
		Optional<Binary> read = super.readVersionWithTransaction(connection, uuid, version);
		return read.map(r -> r.setDataElement(new DeferredBase64BinaryTypeImpl(out -> readData(r, out))));
	}

	@Override
	protected Binary copy(Binary resource)
	{
		return resource.copy();
	}

	@Override
	protected void modifySearchResultResource(Binary resource, Connection connection) throws SQLException
	{
		try (PreparedStatement statement = connection
				.prepareStatement("SELECT binary_size FROM binaries WHERE binary_id = ? AND version = ?"))
		{
			PGobject uuidObject = getPreparedStatementFactory()
					.uuidToPgObject(toUuid(resource.getIdElement().getIdPart()));
			Long version = resource.getMeta().getVersionIdElement().getIdPartAsLong();

			statement.setObject(1, uuidObject);
			statement.setLong(2, version);

			try (ResultSet result = statement.executeQuery())
			{
				if (result.next())
				{
					long dataSize = result.getLong(1);
					resource.setUserData(RangeRequest.USER_DATA_VALUE_DATA_SIZE, dataSize);
				}
				else
					throw new SQLException(
							"Binary resource with id " + resource.getIdElement().getIdPart() + " not found");
			}
		}

		resource.setDataElement(new DeferredBase64BinaryTypeImpl(out -> readData(resource, out)));
	}

	@Override
	public Optional<Binary> read(UUID uuid, RangeRequest rangeRequest) throws SQLException, ResourceDeletedException
	{
		Optional<Binary> binary = read(uuid);
		binary.ifPresent(b -> b.setUserData(RangeRequest.USER_DATA_VALUE_RANGE_REQUEST, rangeRequest));

		return binary;
	}

	@Override
	public Optional<Binary> readVersion(UUID uuid, long version, RangeRequest rangeRequest)
			throws SQLException, ResourceDeletedException
	{
		Optional<Binary> binary = readVersion(uuid, version);
		binary.ifPresent(b -> b.setUserData(RangeRequest.USER_DATA_VALUE_RANGE_REQUEST, rangeRequest));

		return binary;
	}

	@Override
	public void startLargeObjectUnlink()
	{
		loUnlinker.submit(this::doLargeObjectUnlink);
	}

	private void doLargeObjectUnlink()
	{
		logger.debug("Deleting entries from binaries_lo_unlink_queue");

		try (Connection connection = getPermanentDeleteDataSource().getConnection();
				PreparedStatement statement = connection.prepareStatement("DELETE FROM binaries_lo_unlink_queue"))
		{
			statement.execute();
		}
		catch (SQLException e)
		{
			logger.debug("Unable to delete entries from binaries_lo_unlink_queue table", e);
			logger.error("Unable to delete entries from binaries_lo_unlink_queue table: {} - {}", e.getClass().getName(),
					e.getMessage());
		}
	}

	@Override
	public void stopLargeObjectUnlinker()
	{
		startLargeObjectUnlink();

		logger.debug("Shutting down binaries-large-object-unlinker executor ...");

		loUnlinker.shutdown();

		try
		{
			if (!loUnlinker.awaitTermination(60, TimeUnit.SECONDS))
			{
				loUnlinker.shutdownNow();
			}
		}
		catch (InterruptedException ex)
		{
			loUnlinker.shutdownNow();
		}
	}
}
