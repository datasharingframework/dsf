package dev.dsf.fhir.dao.jdbc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;

import dev.dsf.fhir.webservice.RangeRequest;

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

		@Override
		public void read(long oid, long length, RangeRequest rangeRequest, OutputStream out)
				throws SQLException, IOException
		{
		}
	};

	OidAndSize create(InputStream inputStream) throws SQLException;

	default OidAndSize create(byte[] value) throws SQLException
	{
		return create(new ByteArrayInputStream(value));
	}

	void rollback() throws SQLException;

	void read(long oid, long length, RangeRequest rangeRequest, OutputStream out) throws SQLException, IOException;
}
