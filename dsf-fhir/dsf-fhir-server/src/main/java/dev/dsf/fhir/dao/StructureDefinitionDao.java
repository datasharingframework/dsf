package dev.dsf.fhir.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.hl7.fhir.r4.model.StructureDefinition;

public interface StructureDefinitionDao extends ResourceDao<StructureDefinition>, ReadByUrlDao<StructureDefinition>
{
	List<StructureDefinition> readAllByBaseDefinitionWithTransaction(Connection connection, String baseDefinition)
			throws SQLException;
}
