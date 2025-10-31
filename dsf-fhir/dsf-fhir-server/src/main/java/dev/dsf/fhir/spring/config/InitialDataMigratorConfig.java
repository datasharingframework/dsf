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
