package dev.dsf.fhir.service.migration;

public interface MigrationJob
{
	void execute() throws Exception;
}
