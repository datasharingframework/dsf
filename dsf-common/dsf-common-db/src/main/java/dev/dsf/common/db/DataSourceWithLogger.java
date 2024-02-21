package dev.dsf.common.db;

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

	public <T> T unwrap(Class<T> iface) throws SQLException
	{
		if (BasicDataSource.class.equals(iface))
			return iface.cast(delegate);

		return delegate.unwrap(iface);
	}

	public boolean isWrapperFor(Class<?> iface) throws SQLException
	{
		if (BasicDataSource.class.equals(iface))
			return true;

		return delegate.isWrapperFor(iface);
	}

	public Connection getConnection() throws SQLException
	{
		return new ConnectionWithLogger(loggerEnabled, delegate.getConnection());
	}

	public Connection getConnection(String username, String password) throws SQLException
	{
		return new ConnectionWithLogger(loggerEnabled, delegate.getConnection(username, password));
	}

	public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException
	{
		return delegate.getParentLogger();
	}

	public PrintWriter getLogWriter() throws SQLException
	{
		return delegate.getLogWriter();
	}

	public void setLogWriter(PrintWriter out) throws SQLException
	{
		delegate.setLogWriter(out);
	}

	public void setLoginTimeout(int seconds) throws SQLException
	{
		delegate.setLoginTimeout(seconds);
	}

	public int getLoginTimeout() throws SQLException
	{
		return delegate.getLoginTimeout();
	}

	public ConnectionBuilder createConnectionBuilder() throws SQLException
	{
		return delegate.createConnectionBuilder();
	}

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
