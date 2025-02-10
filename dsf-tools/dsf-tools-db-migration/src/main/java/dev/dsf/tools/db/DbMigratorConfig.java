package dev.dsf.tools.db;

import java.util.Map;

public interface DbMigratorConfig
{
	String getDbUrl();

	String getDbLiquibaseUsername();

	char[] getDbLiquibasePassword();

	String getChangelogFile();

	Map<String, String> getChangeLogParameters();

	boolean forceLiquibaseUnlock();

	long getLiquibaseLockWaitTime();
}
