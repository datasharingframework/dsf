/*
 * Copyright 2018-2025 Heilbronn University of Applied Sciences
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
