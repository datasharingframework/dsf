package dev.dsf.fhir.logging;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;

import dev.dsf.common.logging.Log4jConfiguration;
import dev.dsf.common.logging.Log4jConfiguration.Log4jLayout;
import dev.dsf.common.logging.Log4jConfigurationFactory;
import dev.dsf.fhir.logging.FhirLog4jConfiguration.Audit;

public class FhirLog4jConfigurationFactory extends Log4jConfigurationFactory
{
	private final Audit audit;

	public FhirLog4jConfigurationFactory(String fileNamePart, Log4jLayout consoleLayout, Level consoleLevel,
			Log4jLayout fileLayout, Level fileLevel, Audit audit)
	{
		super(fileNamePart, consoleLayout, consoleLevel, fileLayout, fileLevel);

		this.audit = audit;
	}

	@Override
	protected Log4jConfiguration doGetConfiguration(LoggerContext loggerContext, String name)
	{
		return new FhirLog4jConfiguration(loggerContext, name, fileNamePart, consoleLayout, consoleLevel, fileLayout,
				fileLevel, audit);
	}
}
