package dev.dsf.fhir.function;

import java.sql.SQLException;

import dev.dsf.fhir.dao.exception.ResourceNotFoundException;
import dev.dsf.fhir.dao.exception.ResourceVersionNoMatchException;

@FunctionalInterface
public interface SupplierWithSqlAndResourceNotFoundAndResouceVersionNoMatchException<R>
{
	R get() throws SQLException, ResourceNotFoundException, ResourceVersionNoMatchException;
}