package dev.dsf.bpe.dao;

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

	protected static BasicDataSource defaultDataSource;
	protected static BasicDataSource camundaDataSource;

	@ClassRule
	public static final PostgreSqlContainerLiquibaseTemplateClassRule liquibaseRule = new PostgreSqlContainerLiquibaseTemplateClassRule(
			DockerImageName.parse("postgres:15"), ROOT_USER, "bpe", "bpe_template", CHANGE_LOG_FILE,
			CHANGE_LOG_PARAMETERS, true);

	@Rule
	public final PostgresTemplateRule templateRule = new PostgresTemplateRule(liquibaseRule);

	@BeforeClass
	public static void beforeClass() throws Exception
	{
		defaultDataSource = createDefaultDataSource(liquibaseRule.getHost(), liquibaseRule.getMappedPort(5432),
				liquibaseRule.getDatabaseName());
		defaultDataSource.start();

		camundaDataSource = createCamundaDataSource(liquibaseRule.getHost(), liquibaseRule.getMappedPort(5432),
				liquibaseRule.getDatabaseName());
		camundaDataSource.start();
	}

	@AfterClass
	public static void afterClass() throws Exception
	{
		if (defaultDataSource != null)
			defaultDataSource.close();

		if (camundaDataSource != null)
			camundaDataSource.close();
	}
}
