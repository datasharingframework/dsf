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
package dev.dsf.fhir.logging;

import java.util.List;
import java.util.function.Function;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.StringLayout;
import org.apache.logging.log4j.core.config.Configuration;

import dev.dsf.common.logging.Log4jConfiguration;

public class FhirLog4jConfiguration extends Log4jConfiguration
{
	public FhirLog4jConfiguration(LoggerContext loggerContext, String name, String fileNamePart,
			boolean consoleOutEnabled, Log4jLayout consoleOutLayout, Level consoleOutLevel, boolean consoleErrEnabled,
			Log4jLayout consoleErrLayout, Level consoleErrLevel, boolean fileEnabled, Log4jLayout fileLayout,
			Level fileLevel, List<String> minLevelLoggers, Function<Configuration, StringLayout> auditFile,
			Function<Configuration, StringLayout> auditOut, Function<Configuration, StringLayout> auditErr)
	{
		super(loggerContext, name, fileNamePart, consoleOutEnabled, consoleOutLayout, consoleOutLevel,
				consoleErrEnabled, consoleErrLayout, consoleErrLevel, fileEnabled, fileLayout, fileLevel,
				minLevelLoggers);

		addSpecialLogger("audit", fileNamePart, auditFile, auditOut, auditErr, Level.INFO);
	}
}
