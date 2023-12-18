package dev.dsf.fhir.dao.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.Questionnaire;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.fhir.dao.QuestionnaireDao;
import dev.dsf.fhir.search.filter.QuestionnaireIdentityFilter;
import dev.dsf.fhir.search.parameters.QuestionnaireDate;
import dev.dsf.fhir.search.parameters.QuestionnaireIdentifier;
import dev.dsf.fhir.search.parameters.QuestionnaireName;
import dev.dsf.fhir.search.parameters.QuestionnaireStatus;
import dev.dsf.fhir.search.parameters.QuestionnaireUrl;
import dev.dsf.fhir.search.parameters.QuestionnaireVersion;

public class QuestionnaireDaoJdbc extends AbstractResourceDaoJdbc<Questionnaire> implements QuestionnaireDao
{
	private final ReadByUrlDaoJdbc<Questionnaire> readByUrl;

	public QuestionnaireDaoJdbc(DataSource dataSource, DataSource permanentDeleteDataSource, FhirContext fhirContext)
	{
		super(dataSource, permanentDeleteDataSource, fhirContext, Questionnaire.class, "questionnaires",
				"questionnaire", "questionnaire_id", QuestionnaireIdentityFilter::new,
				List.of(factory(QuestionnaireDate.PARAMETER_NAME, QuestionnaireDate::new),
						factory(QuestionnaireIdentifier.PARAMETER_NAME, QuestionnaireIdentifier::new,
								QuestionnaireIdentifier.getNameModifiers()),
						factory(QuestionnaireName.PARAMETER_NAME, QuestionnaireName::new,
								QuestionnaireName.getNameModifiers()),
						factory(QuestionnaireStatus.PARAMETER_NAME, QuestionnaireStatus::new,
								QuestionnaireStatus.getNameModifiers()),
						factory(QuestionnaireUrl.PARAMETER_NAME, QuestionnaireUrl::new,
								QuestionnaireUrl.getNameModifiers()),
						factory(QuestionnaireVersion.PARAMETER_NAME, QuestionnaireVersion::new,
								QuestionnaireVersion.getNameModifiers())),
				List.of());

		readByUrl = new ReadByUrlDaoJdbc<>(this::getDataSource, this::getResource, getResourceTable(),
				getResourceColumn());
	}

	@Override
	protected Questionnaire copy(Questionnaire resource)
	{
		return resource.copy();
	}

	@Override
	public Optional<Questionnaire> readByUrlAndVersion(String urlAndVersion) throws SQLException
	{
		return readByUrl.readByUrlAndVersion(urlAndVersion);
	}

	@Override
	public Optional<Questionnaire> readByUrlAndVersionWithTransaction(Connection connection, String urlAndVersion)
			throws SQLException
	{
		return readByUrl.readByUrlAndVersionWithTransaction(connection, urlAndVersion);
	}

	@Override
	public Optional<Questionnaire> readByUrlAndVersion(String url, String version) throws SQLException
	{
		return readByUrl.readByUrlAndVersion(url, version);
	}

	@Override
	public Optional<Questionnaire> readByUrlAndVersionWithTransaction(Connection connection, String url, String version)
			throws SQLException
	{
		return readByUrl.readByUrlAndVersionWithTransaction(connection, url, version);
	}
}
