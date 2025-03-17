package dev.dsf.bpe.dao;

import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.postgresql.Driver;
import org.slf4j.bridge.SLF4JBridgeHandler;

import dev.dsf.common.db.DataSourceWithLogger;

public abstract class AbstractDbTest
{
	static
	{
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
	}

	protected static final boolean LOG_DB_STATEMENTS = true;

	protected static final String BPE_CHANGE_LOG_FILE = "bpe/db/db.changelog.xml";

	protected static final String BPE_DATABASE_USERS_GROUP = "server_users_group";
	protected static final String BPE_DATABASE_USER = "server_user";
	protected static final String BPE_DATABASE_USER_PASSWORD = "server_user_password";

	protected static final String BPE_DATABASE_CAMUNDA_USERS_GROUP = "camunda_users_group";
	protected static final String BPE_DATABASE_CAMUNDA_USER = "camunda_user";
	protected static final String BPE_DATABASE_CAMUNDA_USER_PASSWORD = "camunda_user_password";

	protected static final String FHIR_CHANGE_LOG_FILE = "fhir/db/db.changelog.xml";

	protected static final String FHIR_DATABASE_USERS_GROUP = "server_users_group";
	protected static final String FHIR_DATABASE_USER = "server_user";
	protected static final String FHIR_DATABASE_USER_PASSWORD = "server_user_password";

	protected static final String FHIR_DATABASE_DELETE_USERS_GROUP = "server_permanent_delete_users_group";
	protected static final String FHIR_DATABASE_DELETE_USER = "server_permanent_delete_user";
	protected static final String FHIR_DATABASE_DELETE_USER_PASSWORD = "server_permanent_delete_user_password";

	protected static final String ROOT_USER = "postgres";

	protected static final Map<String, String> BPE_CHANGE_LOG_PARAMETERS = Map.of("db.liquibase_user", ROOT_USER,
			"db.server_users_group", BPE_DATABASE_USERS_GROUP, "db.server_user", BPE_DATABASE_USER,
			"db.server_user_password", BPE_DATABASE_USER_PASSWORD, "db.camunda_users_group",
			BPE_DATABASE_CAMUNDA_USERS_GROUP, "db.camunda_user", BPE_DATABASE_CAMUNDA_USER, "db.camunda_user_password",
			BPE_DATABASE_CAMUNDA_USER_PASSWORD);

	protected static final Map<String, String> FHIR_CHANGE_LOG_PARAMETERS = Map.of("db.liquibase_user", ROOT_USER,
			"db.server_users_group", FHIR_DATABASE_USERS_GROUP, "db.server_user", FHIR_DATABASE_USER,
			"db.server_user_password", FHIR_DATABASE_USER_PASSWORD, "db.server_permanent_delete_users_group",
			FHIR_DATABASE_DELETE_USERS_GROUP, "db.server_permanent_delete_user", FHIR_DATABASE_DELETE_USER,
			"db.server_permanent_delete_user_password", FHIR_DATABASE_DELETE_USER_PASSWORD);

	public static DataSource createBpeDefaultDataSource(String host, int port, String databaseName)
	{
		BasicDataSource dataSource = new BasicDataSource();
		dataSource.setDriverClassName(Driver.class.getName());
		dataSource.setUrl("jdbc:postgresql://" + host + ":" + port + "/" + databaseName);
		dataSource.setUsername(BPE_DATABASE_USER);
		dataSource.setPassword(BPE_DATABASE_USER_PASSWORD);
		dataSource.setDefaultReadOnly(true);

		dataSource.setTestOnBorrow(true);
		dataSource.setValidationQuery("SELECT 1");

		return new DataSourceWithLogger(LOG_DB_STATEMENTS, dataSource);
	}

	public static DataSource createBpeCamundaDataSource(String host, int port, String databaseName)
	{
		BasicDataSource dataSource = new BasicDataSource();
		dataSource.setDriverClassName(Driver.class.getName());
		dataSource.setUrl("jdbc:postgresql://" + host + ":" + port + "/" + databaseName);
		dataSource.setUsername(BPE_DATABASE_CAMUNDA_USER);
		dataSource.setPassword(BPE_DATABASE_CAMUNDA_USER_PASSWORD);
		dataSource.setDefaultReadOnly(true);

		dataSource.setTestOnBorrow(true);
		dataSource.setValidationQuery("SELECT 1");

		return new DataSourceWithLogger(LOG_DB_STATEMENTS, dataSource);
	}

	public static DataSource createFhirDefaultDataSource(String host, int port, String databaseName)
	{
		BasicDataSource dataSource = new BasicDataSource();
		dataSource.setDriverClassName(Driver.class.getName());
		dataSource.setUrl("jdbc:postgresql://" + host + ":" + port + "/" + databaseName);
		dataSource.setUsername(FHIR_DATABASE_USER);
		dataSource.setPassword(FHIR_DATABASE_USER_PASSWORD);
		dataSource.setDefaultReadOnly(true);

		dataSource.setTestOnBorrow(true);
		dataSource.setValidationQuery("SELECT 1");

		return new DataSourceWithLogger(LOG_DB_STATEMENTS, dataSource);
	}

	public static DataSource createFhirPermanentDeleteDataSource(String host, int port, String databaseName)
	{
		BasicDataSource dataSource = new BasicDataSource();
		dataSource.setDriverClassName(Driver.class.getName());
		dataSource.setUrl("jdbc:postgresql://" + host + ":" + port + "/" + databaseName);
		dataSource.setUsername(FHIR_DATABASE_DELETE_USER);
		dataSource.setPassword(FHIR_DATABASE_DELETE_USER_PASSWORD);
		dataSource.setDefaultReadOnly(true);

		dataSource.setTestOnBorrow(true);
		dataSource.setValidationQuery("SELECT 1");

		return new DataSourceWithLogger(LOG_DB_STATEMENTS, dataSource);
	}
}
