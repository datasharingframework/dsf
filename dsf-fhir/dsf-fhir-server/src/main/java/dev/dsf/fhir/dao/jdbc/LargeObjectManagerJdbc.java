package dev.dsf.fhir.dao.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import javax.sql.DataSource;

import org.postgresql.PGConnection;
import org.postgresql.largeobject.LargeObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LargeObjectManagerJdbc implements LargeObjectManager
{
	private static final Logger logger = LoggerFactory.getLogger(LargeObjectManagerJdbc.class);

	private static final int BUFFER_SIZE = 8192; // postgres default page size

	private final DataSource permanentDeleteDataSource;
	private final String dbUsersGroup;
	private final Connection connection;

	private final List<Long> createdOids = new ArrayList<>();

	public LargeObjectManagerJdbc(DataSource permanentDeleteDataSource, String dbUsersGroup, Connection connection)
	{
		this.permanentDeleteDataSource = Objects.requireNonNull(permanentDeleteDataSource, "permanentDeleteDataSource");
		this.dbUsersGroup = Objects.requireNonNull(dbUsersGroup, "dbUsersGroup");
		this.connection = Objects.requireNonNull(connection, "connection");
	}

	private static org.postgresql.largeobject.LargeObjectManager getLargeObjectManager(Connection connection)
			throws SQLException
	{
		return connection.unwrap(PGConnection.class).getLargeObjectAPI();
	}

	@Override
	public OidAndSize create(InputStream inputStream) throws SQLException
	{
		long oid = createLargeObject();

		LargeObject largeObject = getLargeObjectManager(connection).open(oid);
		try (inputStream; OutputStream outputStream = largeObject.getOutputStream())
		{
			logger.info("Writing to large object '{}' ...", oid);
			long size = copy(inputStream, outputStream);
			logger.info("Writing to large object '{}' [Done, {} bytes]", oid, size);

			return new OidAndSize(oid, size);
		}
		catch (IOException e)
		{
			throw new SQLException("Unable to copy input stream with data to large object: " + e.getMessage(), e);
		}
	}

	private long createLargeObject() throws SQLException
	{
		try (Connection connection = permanentDeleteDataSource.getConnection())
		{
			connection.setReadOnly(false);
			connection.setAutoCommit(false);

			long oid = getLargeObjectManager(connection).createLO();

			try (PreparedStatement statement = connection
					.prepareStatement("GRANT SELECT, UPDATE ON LARGE OBJECT " + oid + " TO " + dbUsersGroup))
			{
				statement.execute();
			}

			connection.commit();

			createdOids.add(oid);
			return oid;
		}
	}

	private static long copy(InputStream inputStream, OutputStream outputStream) throws IOException
	{
		byte[] buffer = new byte[BUFFER_SIZE];

		long count = 0;
		int n;
		while (-1 != (n = inputStream.read(buffer)))
		{
			outputStream.write(buffer, 0, n);
			count += n;
		}

		return count;
	}

	@Override
	public void rollback() throws SQLException
	{
		if (createdOids.isEmpty())
			return;

		try (Connection connection = permanentDeleteDataSource.getConnection())
		{
			connection.setReadOnly(false);
			connection.setAutoCommit(false);

			createdOids.stream().forEach(delete(connection));

			connection.commit();
		}
	}

	private Consumer<Long> delete(Connection permanentDeleteConnection)
	{
		return oid ->
		{
			try
			{
				getLargeObjectManager(permanentDeleteConnection).delete(oid);
			}
			catch (SQLException e)
			{
				logger.debug("Unable to delete large object {}", oid, e);
				logger.warn("Unable to delete large object {}: {}", oid, e.getMessage());
			}
		};
	}
}
