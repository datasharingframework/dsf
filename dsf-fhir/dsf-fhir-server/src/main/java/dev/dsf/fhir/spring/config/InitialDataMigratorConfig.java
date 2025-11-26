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
package dev.dsf.fhir.spring.config;

import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;

import dev.dsf.fhir.service.InitialDataMigrator;
import dev.dsf.fhir.service.InitialDataMigratorImpl;
import dev.dsf.fhir.service.migration.MigrationJob;
import dev.dsf.fhir.service.migration.QuestionnairesMigrationJob;
import dev.dsf.fhir.service.migration.StructureDefinitionTaskProfileMigrationJob;

@Configuration
public class InitialDataMigratorConfig
{
	@Autowired
	private DaoConfig daoConfig;

	@Autowired
	private SnapshotConfig snapshotConfig;

	@Autowired
	private HelperConfig helperConfig;

	@Autowired
	private EventConfig eventConfig;

	@Bean
	public List<MigrationJob> migrationJobs()
	{
		return List.of(
				new StructureDefinitionTaskProfileMigrationJob(daoConfig.structureDefinitionDao(),
						daoConfig.structureDefinitionSnapshotDao(), snapshotConfig.snapshotGenerator(),
						helperConfig.exceptionHandler(), eventConfig.eventManager(), eventConfig.eventGenerator()),
				new QuestionnairesMigrationJob(daoConfig.questionnaireDao(), eventConfig.eventManager(),
						eventConfig.eventGenerator()));
	}

	@Bean
	public InitialDataMigrator initialDataMigrator()
	{
		return new InitialDataMigratorImpl(migrationJobs());
	}

	@Order(HIGHEST_PRECEDENCE + 1)
	@EventListener({ ContextRefreshedEvent.class })
	public void onContextRefreshedEvent()
	{
		initialDataMigrator().execute();
	}
}
