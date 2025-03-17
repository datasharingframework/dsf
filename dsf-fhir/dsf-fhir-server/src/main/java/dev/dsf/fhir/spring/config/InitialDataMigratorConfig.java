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

@Configuration
public class InitialDataMigratorConfig
{
	@Autowired
	public DaoConfig daoConfig;

	@Bean
	public List<MigrationJob> migrationJobs()
	{
		// currently no migration jobs
		// add future migration jobs here
		return List.of();
	}

	@Bean
	public InitialDataMigrator initialDataMigrator()
	{
		return new InitialDataMigratorImpl(migrationJobs());
	}

	@Order(HIGHEST_PRECEDENCE + 1)
	@EventListener({ ContextRefreshedEvent.class })
	public void onContextRefreshedEvent(ContextRefreshedEvent event) throws Exception
	{
		initialDataMigrator().execute();
	}
}
