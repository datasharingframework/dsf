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
package dev.dsf.bpe.dao;

import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.postgresql.Driver;
import org.slf4j.bridge.SLF4JBridgeHandler;

import dev.dsf.common.db.logging.DataSourceWithLogger;

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

	protected static final String BPE_DATABASE_ENGINE_USERS_GROUP = "engine_users_group";
	protected static final String BPE_DATABASE_ENGINE_USER = "engine_user";
	protected static final String BPE_DATABASE_ENGINE_USER_PASSWORD = "engine_user_password";

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
			"db.server_user_password", BPE_DATABASE_USER_PASSWORD, "db.engine_users_group",
			BPE_DATABASE_ENGINE_USERS_GROUP, "db.engine_user", BPE_DATABASE_ENGINE_USER, "db.engine_user_password",
			BPE_DATABASE_ENGINE_USER_PASSWORD);

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
		dataSource.setUsername(BPE_DATABASE_ENGINE_USER);
		dataSource.setPassword(BPE_DATABASE_ENGINE_USER_PASSWORD);
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
