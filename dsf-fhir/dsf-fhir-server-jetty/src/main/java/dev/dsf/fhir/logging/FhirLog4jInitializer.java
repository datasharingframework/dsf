package dev.dsf.fhir.logging;

import java.util.function.Function;

import org.apache.logging.log4j.core.StringLayout;
import org.apache.logging.log4j.core.config.Configuration;

import dev.dsf.common.logging.Log4jConfigurationFactory;
import dev.dsf.common.logging.Log4jInitializer;

public class FhirLog4jInitializer extends Log4jInitializer
{
	public static final String LOG_AUDIT_FILE = "dev.dsf.log.audit.file";
	public static final String LOG_AUDIT_CONSOLE_OUT = "dev.dsf.log.audit.console.out";
	public static final String LOG_AUDIT_CONSOLE_ERR = "dev.dsf.log.audit.console.err";

	private final Function<Configuration, StringLayout> specialFile;
	private final Function<Configuration, StringLayout> specialConsoleOut;
	private final Function<Configuration, StringLayout> specialConsoleErr;

	public FhirLog4jInitializer()
	{
		specialFile = getSpecial(LOG_AUDIT_FILE, SPECIAL_TEXT_MDC);
		specialConsoleOut = getSpecial(LOG_AUDIT_CONSOLE_OUT, SPECIAL_OFF);
		specialConsoleErr = getSpecial(LOG_AUDIT_CONSOLE_ERR, SPECIAL_OFF);
	}

	@Override
	protected Log4jConfigurationFactory createLog4jConfigurationFactory()
	{
		return new Log4jConfigurationFactory((loggerContext, name) -> new FhirLog4jConfiguration(loggerContext, name,
				"fhir", consoleOutLayout, consoleOutLevel, consoleErrLayout, consoleErrLevel, fileLayout, fileLevel,
				specialFile, specialConsoleOut, specialConsoleErr));
	}
}
