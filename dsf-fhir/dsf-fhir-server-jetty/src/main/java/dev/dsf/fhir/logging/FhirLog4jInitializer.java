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

import java.util.function.Function;

import org.apache.logging.log4j.core.StringLayout;
import org.apache.logging.log4j.core.config.Configuration;

import dev.dsf.common.logging.Log4jConfigurationFactory;
import dev.dsf.common.logging.Log4jInitializer;

public class FhirLog4jInitializer extends Log4jInitializer
{
	public static final String AUDIT_FILE = "audit.file";
	public static final String AUDIT_CONSOLE_OUT = "audit.console.out";
	public static final String AUDIT_CONSOLE_ERR = "audit.console.err";

	private final Function<Configuration, StringLayout> specialFile;
	private final Function<Configuration, StringLayout> specialConsoleOut;
	private final Function<Configuration, StringLayout> specialConsoleErr;

	public FhirLog4jInitializer()
	{
		specialFile = getSpecial(AUDIT_FILE, STYLE_TEXT_MDC, true);
		specialConsoleOut = getSpecial(AUDIT_CONSOLE_OUT, STYLE_TEXT, false);
		specialConsoleErr = getSpecial(AUDIT_CONSOLE_ERR, STYLE_TEXT, false);
	}

	@Override
	protected Log4jConfigurationFactory createLog4jConfigurationFactory()
	{
		return new Log4jConfigurationFactory(
				(loggerContext, name) -> new FhirLog4jConfiguration(loggerContext, name, "fhir", consoleOutEnabled,
						consoleOutLayout, consoleOutLevel, consoleErrEnabled, consoleErrLayout, consoleErrLevel,
						fileEnabled, fileLayout, fileLevel, specialFile, specialConsoleOut, specialConsoleErr));
	}
}
