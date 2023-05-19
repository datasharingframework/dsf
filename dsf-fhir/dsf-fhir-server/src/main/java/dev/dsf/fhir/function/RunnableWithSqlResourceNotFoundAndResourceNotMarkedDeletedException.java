package dev.dsf.fhir.function;

import java.sql.SQLException;

import dev.dsf.fhir.dao.exception.ResourceNotFoundException;
import dev.dsf.fhir.dao.exception.ResourceNotMarkedDeletedException;

@FunctionalInterface
public interface RunnableWithSqlResourceNotFoundAndResourceNotMarkedDeletedException
{
	void run() throws SQLException, ResourceNotFoundException, ResourceNotMarkedDeletedException;
}