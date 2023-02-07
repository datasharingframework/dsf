package dev.dsf.fhir.function;

import java.sql.SQLException;

@FunctionalInterface
public interface SupplierWithSqlException<R>
{
	R get() throws SQLException;
}