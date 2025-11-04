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
package dev.dsf.common.db.logging;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ConnectionBuilder;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.ShardingKeyBuilder;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;

public class DataSourceWithLogger implements DataSource
{
	private final boolean loggerEnabled;

	private final BasicDataSource delegate;

	public DataSourceWithLogger(boolean loggerEnabled, BasicDataSource delegate)
	{
		this.loggerEnabled = loggerEnabled;
		this.delegate = delegate;
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException
	{
		if (BasicDataSource.class.equals(iface))
			return iface.cast(delegate);

		return delegate.unwrap(iface);
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException
	{
		if (BasicDataSource.class.equals(iface))
			return true;

		return delegate.isWrapperFor(iface);
	}

	@Override
	public Connection getConnection() throws SQLException
	{
		return new ConnectionWithLogger(loggerEnabled, delegate.getConnection());
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException
	{
		return new ConnectionWithLogger(loggerEnabled, delegate.getConnection(username, password));
	}

	@Override
	public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException
	{
		return delegate.getParentLogger();
	}

	@Override
	public PrintWriter getLogWriter() throws SQLException
	{
		return delegate.getLogWriter();
	}

	@Override
	public void setLogWriter(PrintWriter out) throws SQLException
	{
		delegate.setLogWriter(out);
	}

	@Override
	public void setLoginTimeout(int seconds) throws SQLException
	{
		delegate.setLoginTimeout(seconds);
	}

	@Override
	public int getLoginTimeout() throws SQLException
	{
		return delegate.getLoginTimeout();
	}

	@Override
	public ConnectionBuilder createConnectionBuilder() throws SQLException
	{
		return delegate.createConnectionBuilder();
	}

	@Override
	public ShardingKeyBuilder createShardingKeyBuilder() throws SQLException
	{
		return delegate.createShardingKeyBuilder();
	}

	@Override
	public String toString()
	{
		return delegate.toString();
	}
}
