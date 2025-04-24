package dev.dsf.fhir.dao.jdbc;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.SQLException;

public interface LargeObjectManager
{
	final record OidAndSize(long oid, long size)
	{
	}

	LargeObjectManager NO_OP = new LargeObjectManager()
	{
		@Override
		public OidAndSize create(InputStream inputStream) throws SQLException
		{
			return null;
		}

		@Override
		public void rollback() throws SQLException
		{
		}
	};

	OidAndSize create(InputStream inputStream) throws SQLException;

	default OidAndSize create(byte[] value) throws SQLException
	{
		return create(new ByteArrayInputStream(value));
	}

	void rollback() throws SQLException;
}
