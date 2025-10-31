package dev.dsf.bpe.logging;

import java.util.function.Function;

import org.apache.logging.log4j.core.StringLayout;
import org.apache.logging.log4j.core.config.Configuration;

import dev.dsf.common.logging.Log4jConfigurationFactory;
import dev.dsf.common.logging.Log4jInitializer;

public class BpeLog4jInitializer extends Log4jInitializer
{
	public static final String DATA_FILE = "data.file";
	public static final String DATA_CONSOLE_OUT = "data.console.out";
	public static final String DATA_CONSOLE_ERR = "data.console.err";

	private final Function<Configuration, StringLayout> specialFile;
	private final Function<Configuration, StringLayout> specialConsoleOut;
	private final Function<Configuration, StringLayout> specialConsoleErr;

	public BpeLog4jInitializer()
	{
		specialFile = getSpecial(DATA_FILE, STYLE_TEXT, false);
		specialConsoleOut = getSpecial(DATA_CONSOLE_OUT, STYLE_TEXT, false);
		specialConsoleErr = getSpecial(DATA_CONSOLE_ERR, STYLE_TEXT, false);
	}

	@Override
	protected Log4jConfigurationFactory createLog4jConfigurationFactory()
	{
		return new Log4jConfigurationFactory(
				(loggerContext, name) -> new BpeLog4jConfiguration(loggerContext, name, "bpe", consoleOutEnabled,
						consoleOutLayout, consoleOutLevel, consoleErrEnabled, consoleErrLayout, consoleErrLevel,
						fileEnabled, fileLayout, fileLevel, specialFile, specialConsoleOut, specialConsoleErr));
	}
}
