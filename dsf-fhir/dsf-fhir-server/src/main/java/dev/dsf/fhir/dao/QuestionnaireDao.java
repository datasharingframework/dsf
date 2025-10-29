package dev.dsf.fhir.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.hl7.fhir.r4.model.Questionnaire;

public interface QuestionnaireDao extends ResourceDao<Questionnaire>, ReadByUrlDao<Questionnaire>
{
	List<Questionnaire> readAllByProfileWithTransaction(Connection connection, String profile) throws SQLException;
}
