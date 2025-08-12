package dev.dsf.fhir.logging;

import dev.dsf.common.logging.Log4jConfigurationFactory;
import dev.dsf.common.logging.Log4jInitializer;
import dev.dsf.fhir.logging.FhirLog4jConfiguration.Audit;

public class FhirLog4jInitializer extends Log4jInitializer
{
	public static final String AUDIT_OUT = "SYS_OUT";
	public static final String AUDIT_ERROR = "SYS_ERROR";
	public static final String AUDIT_FILE = "FILE";
	public static final String AUDIT_OFF = "OFF";

	public static final String LOG_AUDIT = "dev.dsf.log.audit";

	private final Audit audit;

	public FhirLog4jInitializer()
	{
		audit = getAudit(LOG_AUDIT, AUDIT_FILE);
	}

	@Override
	protected Log4jConfigurationFactory createLog4jConfigurationFactory()
	{
		return new Log4jConfigurationFactory((loggerContext, name) -> new FhirLog4jConfiguration(loggerContext, name,
				"fhir", consoleLayout, consoleLevel, fileLayout, fileLevel, audit));
	}

	private Audit getAudit(String parameter, String defaultValue)
	{
		String value = getValue(parameter, defaultValue);

		if (AUDIT_OUT.equalsIgnoreCase(value))
			return Audit.OUT;
		else if (AUDIT_ERROR.equalsIgnoreCase(value))
			return Audit.ERROR;
		else if (AUDIT_FILE.equalsIgnoreCase(value))
			return Audit.FILE;
		else if (AUDIT_OFF.equalsIgnoreCase(value))
			return Audit.OFF;
		else
			throw new IllegalArgumentException("Audit '" + value + "' for " + parameter + " not supported");
	}
}
