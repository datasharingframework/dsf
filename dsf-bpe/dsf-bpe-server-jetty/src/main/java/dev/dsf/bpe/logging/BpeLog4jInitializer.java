package dev.dsf.bpe.logging;

import dev.dsf.bpe.logging.BpeLog4jConfiguration.Data;
import dev.dsf.common.logging.Log4jConfigurationFactory;
import dev.dsf.common.logging.Log4jInitializer;

public class BpeLog4jInitializer extends Log4jInitializer
{
	public static final String DATA_OUT = "SYS_OUT";
	public static final String DATA_ERROR = "SYS_ERROR";
	public static final String DATA_FILE = "FILE";
	public static final String DATA_OFF = "OFF";

	public static final String LOG_DATA = "dev.dsf.log.data";

	private final Data data;

	public BpeLog4jInitializer()
	{
		data = getData(LOG_DATA, DATA_FILE);
	}

	@Override
	protected Log4jConfigurationFactory createLog4jConfigurationFactory()
	{
		return new Log4jConfigurationFactory((loggerContext, name) -> new BpeLog4jConfiguration(loggerContext, name,
				"bpe", consoleLayout, consoleLevel, fileLayout, fileLevel, data));
	}

	private Data getData(String parameter, String defaultValue)
	{
		String value = getValue(parameter, defaultValue);

		if (DATA_OUT.equalsIgnoreCase(value))
			return Data.OUT;
		else if (DATA_ERROR.equalsIgnoreCase(value))
			return Data.ERROR;
		else if (DATA_FILE.equalsIgnoreCase(value))
			return Data.FILE;
		else if (DATA_OFF.equalsIgnoreCase(value))
			return Data.OFF;
		else
			throw new IllegalArgumentException("Data '" + value + "' for " + parameter + " not supported");
	}
}
