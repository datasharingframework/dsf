package dev.dsf.fhir.dao.jdbc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.Binary;
import org.postgresql.util.PGobject;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.fhir.dao.BinaryDao;
import dev.dsf.fhir.dao.exception.ResourceDeletedException;
import dev.dsf.fhir.dao.exception.ResourceNotFoundException;
import dev.dsf.fhir.dao.exception.ResourceVersionNoMatchException;
import dev.dsf.fhir.model.DeferredBase64BinaryType;
import dev.dsf.fhir.model.StreamableBase64BinaryType;
import dev.dsf.fhir.search.filter.BinaryIdentityFilter;
import dev.dsf.fhir.search.parameters.BinaryContentType;

public class BinaryDaoJdbc extends AbstractResourceDaoJdbc<Binary> implements BinaryDao
{
	public static final class BlobInputStream extends InputStream
	{
		private final Blob blob;
		private final InputStream stream;

		public BlobInputStream(Blob blob) throws SQLException
		{
			this.blob = blob;
			this.stream = blob.getBinaryStream();
		}

		public static InputStream nullInputStream()
		{
			return InputStream.nullInputStream();
		}

		@Override
		public int read() throws IOException
		{
			return stream.read();
		}

		@Override
		public int read(byte[] b) throws IOException
		{
			return stream.read(b);
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException
		{
			return stream.read(b, off, len);
		}

		@Override
		public byte[] readAllBytes() throws IOException
		{
			return stream.readAllBytes();
		}

		@Override
		public byte[] readNBytes(int len) throws IOException
		{
			return stream.readNBytes(len);
		}

		@Override
		public int readNBytes(byte[] b, int off, int len) throws IOException
		{
			return stream.readNBytes(b, off, len);
		}

		@Override
		public long skip(long n) throws IOException
		{
			return stream.skip(n);
		}

		@Override
		public void skipNBytes(long n) throws IOException
		{
			stream.skipNBytes(n);
		}

		@Override
		public int available() throws IOException
		{
			return stream.available();
		}

		@Override
		public void close() throws IOException
		{
			stream.close();

			try
			{
				blob.free();
			}
			catch (SQLException e)
			{
				throw new IOException(e);
			}
		}

		@Override
		public void mark(int readlimit)
		{
			stream.mark(readlimit);
		}

		@Override
		public void reset() throws IOException
		{
			stream.reset();
		}

		@Override
		public boolean markSupported()
		{
			return stream.markSupported();
		}

		@Override
		public long transferTo(OutputStream out) throws IOException
		{
			return stream.transferTo(out);
		}
	}

	public static final class DataInputStream extends InputStream
	{
		private final Connection connection;
		private final PreparedStatement statement;
		private final ResultSet resultSet;
		private final InputStream data;

		public DataInputStream(Connection connection, PreparedStatement statement, ResultSet resultSet,
				InputStream data)
		{
			this.connection = Objects.requireNonNull(connection, "connection");
			this.statement = Objects.requireNonNull(statement, "statement");
			this.resultSet = Objects.requireNonNull(resultSet, "resultSet");
			this.data = Objects.requireNonNull(data, "data");
		}

		@Override
		public int read() throws IOException
		{
			return data.read();
		}

		@Override
		public int read(byte[] b) throws IOException
		{
			return data.read(b);
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException
		{
			return data.read(b, off, len);
		}

		@Override
		public byte[] readAllBytes() throws IOException
		{
			return data.readAllBytes();
		}

		@Override
		public byte[] readNBytes(int len) throws IOException
		{
			return data.readNBytes(len);
		}

		@Override
		public int readNBytes(byte[] b, int off, int len) throws IOException
		{
			return data.readNBytes(b, off, len);
		}

		@Override
		public long skip(long n) throws IOException
		{
			return data.skip(n);
		}

		@Override
		public void skipNBytes(long n) throws IOException
		{
			data.skipNBytes(n);
		}

		@Override
		public int available() throws IOException
		{
			return data.available();
		}

		@Override
		public void close() throws IOException
		{
			try
			{
				data.close();
			}
			finally
			{
				try
				{
					resultSet.close();
				}
				catch (SQLException e)
				{
					throw new IOException(e);
				}
				finally
				{
					try
					{
						statement.close();
					}
					catch (SQLException e)
					{
						throw new IOException(e);
					}
					finally
					{
						try
						{
							connection.close();
						}
						catch (SQLException e)
						{
							throw new IOException(e);
						}
					}
				}
			}
		}

		@Override
		public void mark(int readlimit)
		{
			data.mark(readlimit);
		}

		@Override
		public void reset() throws IOException
		{
			data.reset();
		}

		@Override
		public boolean markSupported()
		{
			return data.markSupported();
		}

		@Override
		public long transferTo(OutputStream out) throws IOException
		{
			return data.transferTo(out);
		}
	}

	private final String selectUpdateUser;

	public BinaryDaoJdbc(DataSource dataSource, DataSource permanentDeleteDataSource, FhirContext fhirContext,
			String selectUpdateUser)
	{
		super(dataSource, permanentDeleteDataSource, Binary.class, "binaries", "binary_json", "binary_id",
				new PreparedStatementFactoryBinary(fhirContext), BinaryIdentityFilter::new,
				List.of(factory(BinaryContentType.PARAMETER_NAME, BinaryContentType::new,
						BinaryContentType.getNameModifiers())),
				List.of());

		this.selectUpdateUser = selectUpdateUser;
	}

	@Override
	public LargeObjectManager createLargeObjectManager(Connection connection)
	{
		return new LargeObjectManagerJdbc(getPermanentDeleteDataSource(), selectUpdateUser, connection);
	}

	private InputStream readData(Binary resource)
	{
		try
		{
			Connection connection = getDataSource().getConnection();
			connection.setAutoCommit(false);

			PreparedStatement statement = connection
					.prepareStatement("SELECT binary_oid FROM binaries WHERE binary_id = ? AND version = ?");
			PGobject uuidObject = getPreparedStatementFactory()
					.uuidToPgObject(toUuid(resource.getIdElement().getIdPart()));
			Long version = resource.getMeta().getVersionIdElement().getIdPartAsLong();

			statement.setObject(1, uuidObject);
			statement.setLong(2, version);

			ResultSet result = statement.executeQuery();
			if (result.next())
			{
				Blob blob = result.getBlob(1);
				InputStream data = blob == null ? new ByteArrayInputStream(new byte[0]) : new BlobInputStream(blob);

				return new DataInputStream(connection, statement, result, data);
			}
			else
				throw new SQLException("Binary resource with id " + resource.getIdElement().getIdPart() + " not found");
		}
		catch (SQLException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public Binary createWithTransactionAndId(LargeObjectManager largeObjectManager, Connection connection,
			Binary resource, UUID uuid) throws SQLException
	{
		Binary created = super.createWithTransactionAndId(largeObjectManager, connection, resource, uuid);

		if (created.getDataElement() instanceof StreamableBase64BinaryType)
			created.setDataElement(new DeferredBase64BinaryType(() -> readData(created)));

		return created;
	}

	@Override
	public Binary updateWithTransaction(LargeObjectManager largeObjectManager, Connection connection, Binary resource,
			Long expectedVersion) throws SQLException, ResourceNotFoundException, ResourceVersionNoMatchException
	{
		Binary updated = super.updateWithTransaction(largeObjectManager, connection, resource, expectedVersion);

		if (updated.getDataElement() instanceof StreamableBase64BinaryType)
			updated.setDataElement(new DeferredBase64BinaryType(() -> readData(updated)));

		return updated;
	}

	@Override
	public Optional<Binary> readWithTransaction(Connection connection, UUID uuid)
			throws SQLException, ResourceDeletedException
	{
		Optional<Binary> read = super.readWithTransaction(connection, uuid);
		return read.map(r -> r.setDataElement(new DeferredBase64BinaryType(() -> readData(r))));
	}

	@Override
	public Optional<Binary> readVersionWithTransaction(Connection connection, UUID uuid, long version)
			throws SQLException, ResourceDeletedException
	{
		Optional<Binary> read = super.readVersionWithTransaction(connection, uuid, version);
		return read.map(r -> r.setDataElement(new DeferredBase64BinaryType(() -> readData(r))));
	}

	@Override
	protected Binary copy(Binary resource)
	{
		return resource.copy();
	}

	@Override
	protected void modifySearchResultResource(Binary resource, Connection connection) throws SQLException
	{
		resource.setDataElement(new DeferredBase64BinaryType(() -> readData(resource)));
	}
}
