package dev.dsf.fhir.dao.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hl7.fhir.r4.model.Resource;

import ca.uhn.fhir.context.FhirContext;

class PreparedStatementFactoryDefault<R extends Resource> extends AbstractPreparedStatementFactory<R>
{
	PreparedStatementFactoryDefault(FhirContext fhirContext, Class<R> resourceType, String resourceTable,
			String resourceIdColumn, String resourceColumn)
	{
		super(fhirContext, resourceType, createSql(resourceTable, resourceIdColumn, resourceColumn),
				readByIdSql(resourceTable, resourceIdColumn, resourceColumn),
				readByIdAndVersionSql(resourceTable, resourceIdColumn, resourceColumn),
				updateSql(resourceTable, resourceIdColumn, resourceColumn));
	}

	private static String createSql(String resourceTable, String resourceIdColumn, String resourceColumn)
	{
		return "INSERT INTO " + resourceTable + " (" + resourceIdColumn + ", " + resourceColumn + ") VALUES (?, ?)";
	}

	private static String readByIdSql(String resourceTable, String resourceIdColumn, String resourceColumn)
	{
		return "SELECT deleted, version, " + resourceColumn + " FROM " + resourceTable + " WHERE " + resourceIdColumn
				+ " = ? ORDER BY version DESC LIMIT 1";
	}

	private static String readByIdAndVersionSql(String resourceTable, String resourceIdColumn, String resourceColumn)
	{
		return "SELECT deleted, version," + resourceColumn + " FROM " + resourceTable + " WHERE " + resourceIdColumn
				+ " = ? AND (version = ? OR version = ?) ORDER BY version DESC LIMIT 1";
	}

	private static String updateSql(String resourceTable, String resourceIdColumn, String resourceColumn)
	{
		return "INSERT INTO " + resourceTable + " (" + resourceIdColumn + ", version, " + resourceColumn
				+ ") VALUES (?, ?, ?)";
	}

	@Override
	public void configureCreateStatement(LargeObjectManager largeObjectManager, PreparedStatement statement, R resource,
			UUID uuid) throws SQLException
	{
		statement.setObject(1, uuidToPgObject(uuid));
		statement.setObject(2, resourceToPgObject(resource));
	}

	@Override
	public void configureReadByIdStatement(PreparedStatement statement, UUID uuid) throws SQLException
	{
		statement.setObject(1, uuidToPgObject(uuid));
	}

	@Override
	public LocalDateTime getReadByIdDeleted(ResultSet result) throws SQLException
	{
		Timestamp deleted = result.getTimestamp(1);
		return deleted == null ? null : deleted.toLocalDateTime();
	}

	@Override
	public long getReadByIdVersion(ResultSet result) throws SQLException
	{
		return result.getLong(2);
	}

	@Override
	public R getReadByIdResource(ResultSet result) throws SQLException
	{
		String json = result.getString(3);

		return jsonToResource(json);
	}

	@Override
	public void configureReadByIdAndVersionStatement(PreparedStatement statement, UUID uuid, long version)
			throws SQLException
	{
		statement.setObject(1, uuidToPgObject(uuid));
		statement.setLong(2, version);
		statement.setLong(3, version - 1);
	}

	@Override
	public LocalDateTime getReadByIdVersionDeleted(ResultSet result) throws SQLException
	{
		Timestamp deleted = result.getTimestamp(1);
		return deleted == null ? null : deleted.toLocalDateTime();
	}

	@Override
	public long getReadByIdVersionVersion(ResultSet result) throws SQLException
	{
		return result.getLong(2);
	}

	@Override
	public R getReadByIdAndVersionResource(ResultSet result) throws SQLException
	{
		String json = result.getString(3);

		return jsonToResource(json);
	}

	@Override
	public void configureUpdateSqlStatement(LargeObjectManager largeObjectManager, PreparedStatement statement,
			UUID uuid, long version, R resource) throws SQLException
	{
		statement.setObject(1, uuidToPgObject(uuid));
		statement.setLong(2, version);
		statement.setObject(3, resourceToPgObject(resource));
	}
}
