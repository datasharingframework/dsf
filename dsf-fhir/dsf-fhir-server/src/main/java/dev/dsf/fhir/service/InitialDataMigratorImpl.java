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
