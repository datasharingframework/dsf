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

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.testcontainers.utility.DockerImageName;

import de.hsheilbronn.mi.utils.test.PostgreSqlContainerLiquibaseTemplateClassRule;
import de.hsheilbronn.mi.utils.test.PostgresTemplateRule;

public class AbstractDaoTest extends AbstractDbTest
{
	public static final String DAO_DB_TEMPLATE_NAME = "dao_template";

	protected static DataSource defaultDataSource;
	protected static DataSource camundaDataSource;

	@ClassRule
	public static final PostgreSqlContainerLiquibaseTemplateClassRule liquibaseRule = new PostgreSqlContainerLiquibaseTemplateClassRule(
			DockerImageName.parse("postgres:18"), ROOT_USER, "bpe", "bpe_template", BPE_CHANGE_LOG_FILE,
			BPE_CHANGE_LOG_PARAMETERS, true);

	@Rule
	public final PostgresTemplateRule templateRule = new PostgresTemplateRule(liquibaseRule);

	@BeforeClass
	public static void beforeClass() throws Exception
	{
		defaultDataSource = createBpeDefaultDataSource(liquibaseRule.getHost(), liquibaseRule.getMappedPort(5432),
				liquibaseRule.getDatabaseName());
		defaultDataSource.unwrap(BasicDataSource.class).start();

		camundaDataSource = createBpeCamundaDataSource(liquibaseRule.getHost(), liquibaseRule.getMappedPort(5432),
				liquibaseRule.getDatabaseName());
		camundaDataSource.unwrap(BasicDataSource.class).start();
	}

	@AfterClass
	public static void afterClass() throws Exception
	{
		if (defaultDataSource != null)
			defaultDataSource.unwrap(BasicDataSource.class).close();

		if (camundaDataSource != null)
			camundaDataSource.unwrap(BasicDataSource.class).close();
	}
}
