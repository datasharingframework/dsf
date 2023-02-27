package dev.dsf.fhir.function;

import java.sql.SQLException;

import dev.dsf.fhir.dao.exception.ResourceNotFoundException;

@FunctionalInterface
public interface FunctionWithSqlAndResourceNotFoundException<T, R>
{
	R apply(T t) throws SQLException, ResourceNotFoundException;
}