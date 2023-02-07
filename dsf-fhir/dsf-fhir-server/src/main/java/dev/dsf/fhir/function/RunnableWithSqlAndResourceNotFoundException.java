package dev.dsf.fhir.function;

import java.sql.SQLException;

import dev.dsf.fhir.dao.exception.ResourceNotFoundException;

@FunctionalInterface
public interface RunnableWithSqlAndResourceNotFoundException
{
	void run() throws SQLException, ResourceNotFoundException;
}