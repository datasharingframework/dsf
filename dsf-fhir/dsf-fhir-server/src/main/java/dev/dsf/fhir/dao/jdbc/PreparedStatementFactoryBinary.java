package dev.dsf.fhir.dao.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hl7.fhir.r4.model.Base64BinaryType;
import org.hl7.fhir.r4.model.Binary;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.fhir.dao.jdbc.LargeObjectManager.OidAndSize;
import dev.dsf.fhir.model.StreamableBase64BinaryType;

class PreparedStatementFactoryBinary extends AbstractPreparedStatementFactory<Binary>
{
	private static final String createSql = "INSERT INTO binaries (binary_id, binary_json, binary_oid, binary_size) VALUES (?, ?, ?, ?)";
	private static final String readByIdSql = "SELECT deleted, version, binary_json FROM binaries WHERE binary_id = ? ORDER BY version DESC LIMIT 1";
	private static final String readByIdAndVersionSql = "SELECT deleted, version, binary_json FROM binaries WHERE binary_id = ? AND (version = ? OR version = ?) ORDER BY version DESC LIMIT 1";
	private static final String updateSql = "INSERT INTO binaries (binary_id, version, binary_json, binary_oid, binary_size) VALUES (?, ?, ?, ?, ?)";

	PreparedStatementFactoryBinary(FhirContext fhirContext)
	{
		super(fhirContext, Binary.class, createSql, readByIdSql, readByIdAndVersionSql, updateSql);
	}

	@Override
	public void configureCreateStatement(LargeObjectManager largeObjectManager, PreparedStatement statement,
			Binary resource, UUID uuid) throws SQLException
	{
		Base64BinaryType data = resource.getDataElement();
		resource.setData(null);

		statement.setObject(1, uuidToPgObject(uuid));
		statement.setObject(2, resourceToPgObject(resource));

		if (data instanceof StreamableBase64BinaryType s)
		{
			OidAndSize oidAndSize = largeObjectManager.create(s.getValueAsStream());
			statement.setLong(3, oidAndSize.oid());
			statement.setLong(4, oidAndSize.size());
		}
		else if (data != null && data.getValue() != null)
		{
			OidAndSize oidAndSize = largeObjectManager.create(data.getValue());
			statement.setLong(3, oidAndSize.oid());
			statement.setLong(4, oidAndSize.size());
		}
		else
		{
			statement.setNull(3, Types.BLOB);
			statement.setLong(4, 0);
		}

		resource.setDataElement(data);
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
	public Binary getReadByIdResource(ResultSet result) throws SQLException
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
	public Binary getReadByIdAndVersionResource(ResultSet result) throws SQLException
	{
		String json = result.getString(3);

		return jsonToResource(json);
	}

	@Override
	public void configureUpdateSqlStatement(LargeObjectManager largeObjectManager, PreparedStatement statement,
			UUID uuid, long version, Binary resource) throws SQLException
	{
		Base64BinaryType data = resource.getDataElement();
		resource.setData(null);

		statement.setObject(1, uuidToPgObject(uuid));
		statement.setLong(2, version);
		statement.setObject(3, resourceToPgObject(resource));

		if (data instanceof StreamableBase64BinaryType s)
		{
			OidAndSize oidAndSize = largeObjectManager.create(s.getValueAsStream());
			statement.setLong(4, oidAndSize.oid());
			statement.setLong(5, oidAndSize.size());
		}
		else if (data != null && data.getValue() != null)
		{
			OidAndSize oidAndSize = largeObjectManager.create(data.getValue());
			statement.setLong(4, oidAndSize.oid());
			statement.setLong(5, oidAndSize.size());
		}
		else
		{
			statement.setNull(4, Types.BLOB);
			statement.setLong(4, 0);
		}

		resource.setDataElement(data);
	}
}
