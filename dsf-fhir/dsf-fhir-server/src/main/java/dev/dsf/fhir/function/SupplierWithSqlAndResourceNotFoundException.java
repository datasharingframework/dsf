package dev.dsf.fhir.function;

import java.sql.SQLException;

import dev.dsf.fhir.dao.exception.ResourceNotFoundException;

@FunctionalInterface
public interface SupplierWithSqlAndResourceNotFoundException<R>
{
	R get() throws SQLException, ResourceNotFoundException;
}