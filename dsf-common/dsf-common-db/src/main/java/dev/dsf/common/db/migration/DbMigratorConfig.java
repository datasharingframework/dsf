package dev.dsf.common.db.migration;

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
