package dev.dsf.fhir.dao.jdbc;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.SQLException;

public interface LargeObjectManager
{
	LargeObjectManager NO_OP = new LargeObjectManager()
	{
		@Override
		public long create(InputStream inputStream) throws SQLException
		{
			return Long.MIN_VALUE;
		}

		@Override
		public void rollback() throws SQLException
		{
		}
	};

	long create(InputStream inputStream) throws SQLException;

	default long create(byte[] value) throws SQLException
	{
		return create(new ByteArrayInputStream(value));
	}

	void rollback() throws SQLException;
}
