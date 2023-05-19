package dev.dsf.fhir.search;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.function.BiFunctionWithSqlException;

public interface DbSearchQuery
{
	String getCountSql();

	String getSearchSql();

	void modifyStatement(PreparedStatement statement, BiFunctionWithSqlException<String, Object[], Array> arrayCreator)
			throws SQLException;

	PageAndCount getPageAndCount();

	void modifyIncludeResource(Resource resource, int columnIndex, Connection connection) throws SQLException;
}
