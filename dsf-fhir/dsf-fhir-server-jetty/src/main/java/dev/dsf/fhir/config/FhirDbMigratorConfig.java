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
package dev.dsf.fhir.config;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.ConfigurableEnvironment;

import dev.dsf.common.db.migration.DbMigrator;
import dev.dsf.common.db.migration.DbMigratorConfig;
import dev.dsf.common.docker.secrets.DockerSecretsPropertySourceFactory;
import dev.dsf.common.documentation.Documentation;

@Configuration
@PropertySource(value = "file:conf/config.properties", encoding = "UTF-8", ignoreResourceNotFound = true)
public class FhirDbMigratorConfig implements DbMigratorConfig
{
	private static final String DB_LIQUIBASE_USER = "db.liquibase_user";
	private static final String DB_SERVER_USERS_GROUP = "db.server_users_group";
	private static final String DB_SERVER_USER = "db.server_user";
	private static final String DB_SERVER_USER_PASSWORD = "db.server_user_password";
	private static final String DB_SERVER_PERMANENT_DELETE_USERS_GROUP = "db.server_permanent_delete_users_group";
	private static final String DB_SERVER_PERMANENT_DELETE_USER = "db.server_permanent_delete_user";
	private static final String DB_SERVER_PERMANENT_DELETE_USER_PASSWORD = "db.server_permanent_delete_user_password";

	@Documentation(required = true, description = "Address of the database used for the DSF FHIR server", recommendation = "Change only if you don't use the provided docker-compose from the installation guide or made changes to the database settings/networking in the docker-compose", example = "jdbc:postgresql://db/fhir")
	@Value("${dev.dsf.fhir.db.url}")
	private String dbUrl;

	@Documentation(description = "Username to access the database from the DSF FHIR server to execute database migrations")
	@Value("${dev.dsf.fhir.db.liquibase.username:liquibase_user}")
	private String dbLiquibaseUsername;

	@Documentation(required = true, description = "Password to access the database from the DSF FHIR server to execute database migrations", recommendation = "Use docker secret file to configure by using *${env_variable}_FILE*", example = "/run/secrets/db_liquibase.password")
	@Value("${dev.dsf.fhir.db.liquibase.password}")
	private char[] dbLiquibasePassword;

	@Documentation(description = "To force liquibase to unlock the migration lock set to `true`", recommendation = "Only use this option temporarily to unlock a stuck DB migration step")
	@Value("${dev.dsf.fhir.db.liquibase.forceUnlock:false}")
	private boolean dbLiquibaseUnlock;

	@Documentation(description = "Liquibase change lock wait time in minutes, default 2 minutes")
	@Value("${dev.dsf.fhir.db.liquibase.lockWaitTime:2}")
	private long dbLiquibaseLockWaitTime;

	@Documentation(description = "Name of the user group to access the database from the DSF FHIR server")
	@Value("${dev.dsf.fhir.db.user.group:fhir_users}")
	private String dbUsersGroup;

	@Documentation(description = "Username to access the database from the DSF FHIR server")
	@Value("${dev.dsf.fhir.db.user.username:fhir_server_user}")
	private String dbUsername;

	@Documentation(required = true, description = "Password to access the database from the DSF FHIR server", recommendation = "Use docker secret file to configure using *${env_variable}_FILE*", example = "/run/secrets/db_user.password")
	@Value("${dev.dsf.fhir.db.user.password}")
	private char[] dbPassword;

	@Documentation(description = "Name of the user group to access the database from the DSF FHIR server for permanent deletes")
	@Value("${dev.dsf.fhir.db.user.permanent.delete.group:fhir_permanent_delete_users}")
	private String dbPermanentDeleteUsersGroup;

	@Documentation(description = "Username to access the database from the DSF FHIR server for permanent deletes", recommendation = "Use a different user then *DEV_DSF_FHIR_DB_USER_USERNAME*")
	@Value("${dev.dsf.fhir.db.user.permanent.delete.username:fhir_server_permanent_delete_user}")
	private String dbPermanentDeleteUsername;

	@Documentation(required = true, description = "Password to access the database from the DSF FHIR server for permanent deletes", recommendation = "Use docker secret file to configure using *${env_variable}_FILE*", example = "/run/secrets/db_user_permanent_delete.password")
	@Value("${dev.dsf.fhir.db.user.permanent.delete.password}")
	private char[] dbPermanentDeletePassword;

	@Bean // static in order to initialize before @Configuration classes
	public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer(
			ConfigurableEnvironment environment)
	{
		new DockerSecretsPropertySourceFactory(environment).readDockerSecretsAndAddPropertiesToEnvironment();
		return new PropertySourcesPlaceholderConfigurer();
	}

	@Override
	public String getDbUrl()
	{
		return dbUrl;
	}

	@Override
	public String getDbLiquibaseUsername()
	{
		return dbLiquibaseUsername;
	}

	@Override
	public char[] getDbLiquibasePassword()
	{
		return dbLiquibasePassword;
	}

	@Override
	public String getChangelogFile()
	{
		return "fhir/db/db.changelog.xml";
	}

	@Override
	public Map<String, String> getChangeLogParameters()
	{
		return Map.of(DB_LIQUIBASE_USER, dbLiquibaseUsername, DB_SERVER_USERS_GROUP, dbUsersGroup, DB_SERVER_USER,
				dbUsername, DB_SERVER_USER_PASSWORD, toString(dbPassword), DB_SERVER_PERMANENT_DELETE_USERS_GROUP,
				dbPermanentDeleteUsersGroup, DB_SERVER_PERMANENT_DELETE_USER, dbPermanentDeleteUsername,
				DB_SERVER_PERMANENT_DELETE_USER_PASSWORD, toString(dbPermanentDeletePassword));
	}

	private String toString(char[] password)
	{
		return password == null ? null : String.valueOf(password);
	}

	@Override
	public boolean forceLiquibaseUnlock()
	{
		return dbLiquibaseUnlock;
	}

	@Override
	public long getLiquibaseLockWaitTime()
	{
		return dbLiquibaseLockWaitTime;
	}

	@Bean
	public DbMigrator dbMigrator()
	{
		return new DbMigrator(this);
	}
}
