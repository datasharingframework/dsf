package dev.dsf.fhir.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.fhir.service.migration.MigrationJob;

public class InitialDataMigratorImpl implements InitialDataMigrator
{
	private static final Logger logger = LoggerFactory.getLogger(InitialDataMigratorImpl.class);

	private final List<MigrationJob> migrationJobs = new ArrayList<>();

	public InitialDataMigratorImpl(List<MigrationJob> migrationJobs)
	{
		if (migrationJobs != null)
			this.migrationJobs.addAll(migrationJobs);
	}

	@Override
	public void execute()
	{
		logger.info("Executing initial data migration jobs ...");

		for (MigrationJob job : migrationJobs)
		{
			try
			{
				logger.debug("Executing initial data migration job: {}", job.getClass().getName());
				job.execute();
			}
			catch (Exception e)
			{
				logger.debug("Initial data migration job '{}' failed with error", job.getClass().getName(), e);
				logger.warn("Initial data migration job '{}' failed with error: {} - {}", job.getClass().getName(),
						e.getClass().getName(), e.getMessage());
				throw new RuntimeException(e);
			}
		}

		logger.info("Executing initial data migration jobs [Done]");
	}
}
