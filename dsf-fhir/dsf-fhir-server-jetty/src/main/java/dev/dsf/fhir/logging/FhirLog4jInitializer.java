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
