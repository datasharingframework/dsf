package dev.dsf.fhir.dao;

import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.postgresql.Driver;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.fasterxml.jackson.core.StreamReadConstraints;

import dev.dsf.common.db.DataSourceWithLogger;

public abstract class AbstractDbTest
{
	static
	{
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();

		// TODO remove workaround after upgrading to HAPI 6.8+, see https://github.com/hapifhir/hapi-fhir/issues/5205
		StreamReadConstraints.overrideDefaultStreamReadConstraints(
				StreamReadConstraints.builder().maxStringLength(Integer.MAX_VALUE).build());
	}

	protected static final boolean LOG_DB_STATEMENTS = true;

	protected static final String CHANGE_LOG_FILE = "fhir/db/db.changelog.xml";

	protected static final String DATABASE_USERS_GROUP = "server_users_group";
	protected static final String DATABASE_USER = "server_user";
	protected static final String DATABASE_USER_PASSWORD = "server_user_password";

	protected static final String DATABASE_DELETE_USERS_GROUP = "server_permanent_delete_users_group";
	protected static final String DATABASE_DELETE_USER = "server_permanent_delete_user";
	protected static final String DATABASE_DELETE_USER_PASSWORD = "server_permanent_delete_user_password";

	protected static final String ROOT_USER = "postgres";

	protected static final Map<String, String> CHANGE_LOG_PARAMETERS = Map.of("db.liquibase_user", ROOT_USER,
			"db.server_users_group", DATABASE_USERS_GROUP, "db.server_user", DATABASE_USER, "db.server_user_password",
			DATABASE_USER_PASSWORD, "db.server_permanent_delete_users_group", DATABASE_DELETE_USERS_GROUP,
			"db.server_permanent_delete_user", DATABASE_DELETE_USER, "db.server_permanent_delete_user_password",
			DATABASE_DELETE_USER_PASSWORD);

	public static DataSource createDefaultDataSource(String host, int port, String databaseName)
	{
		BasicDataSource dataSource = new BasicDataSource();
		dataSource.setDriverClassName(Driver.class.getName());
		dataSource.setUrl("jdbc:postgresql://" + host + ":" + port + "/" + databaseName);
		dataSource.setUsername(DATABASE_USER);
		dataSource.setPassword(DATABASE_USER_PASSWORD);
		dataSource.setDefaultReadOnly(true);

		dataSource.setTestOnBorrow(true);
		dataSource.setValidationQuery("SELECT 1");

		return new DataSourceWithLogger(LOG_DB_STATEMENTS, dataSource);
	}

	public static DataSource createPermanentDeleteDataSource(String host, int port, String databaseName)
	{
		BasicDataSource dataSource = new BasicDataSource();
		dataSource.setDriverClassName(Driver.class.getName());
		dataSource.setUrl("jdbc:postgresql://" + host + ":" + port + "/" + databaseName);
		dataSource.setUsername(DATABASE_DELETE_USER);
		dataSource.setPassword(DATABASE_DELETE_USER_PASSWORD);
		dataSource.setDefaultReadOnly(true);

		dataSource.setTestOnBorrow(true);
		dataSource.setValidationQuery("SELECT 1");

		return new DataSourceWithLogger(LOG_DB_STATEMENTS, dataSource);
	}
}
