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
package dev.dsf.common.db.migration;

import java.util.Map;
import java.util.regex.Pattern;

public interface DbMigratorConfig
{
	String POSTGRES_UNQUOTED_IDENTIFIER_STRING = "^[a-zA-Z_][a-zA-Z0-9_$]{0,62}$";
	Pattern POSTGRES_UNQUOTED_IDENTIFIER = Pattern.compile(POSTGRES_UNQUOTED_IDENTIFIER_STRING);

	String getDbUrl();

	String getDbLiquibaseUsername();

	char[] getDbLiquibasePassword();

	String getChangelogFile();

	Map<String, String> getChangeLogParameters();

	boolean forceLiquibaseUnlock();

	long getLiquibaseLockWaitTime();
}
