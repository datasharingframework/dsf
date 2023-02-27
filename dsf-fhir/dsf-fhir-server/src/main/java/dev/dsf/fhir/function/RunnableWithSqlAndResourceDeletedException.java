package dev.dsf.fhir.function;

import java.sql.SQLException;

import dev.dsf.fhir.dao.exception.ResourceDeletedException;

@FunctionalInterface
public interface RunnableWithSqlAndResourceDeletedException
{
	void run() throws SQLException, ResourceDeletedException;
}