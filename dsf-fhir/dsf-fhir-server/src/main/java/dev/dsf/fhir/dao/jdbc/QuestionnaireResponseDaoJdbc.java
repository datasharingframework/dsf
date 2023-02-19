package dev.dsf.fhir.dao.jdbc;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.QuestionnaireResponse;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.fhir.dao.QuestionnaireResponseDao;
import dev.dsf.fhir.search.filter.QuestionnaireResponseIdentityFilter;
import dev.dsf.fhir.search.parameters.QuestionnaireResponseAuthored;
import dev.dsf.fhir.search.parameters.QuestionnaireResponseIdentifier;
import dev.dsf.fhir.search.parameters.QuestionnaireResponseQuestionnaire;
import dev.dsf.fhir.search.parameters.QuestionnaireResponseStatus;
import dev.dsf.fhir.search.parameters.QuestionnaireResponseSubject;

public class QuestionnaireResponseDaoJdbc extends AbstractResourceDaoJdbc<QuestionnaireResponse>
		implements QuestionnaireResponseDao
{
	public QuestionnaireResponseDaoJdbc(DataSource dataSource, DataSource permanentDeleteDataSource,
			FhirContext fhirContext)
	{
		super(dataSource, permanentDeleteDataSource, fhirContext, QuestionnaireResponse.class,
				"questionnaire_responses", "questionnaire_response", "questionnaire_response_id",
				QuestionnaireResponseIdentityFilter::new,
				with(QuestionnaireResponseAuthored::new, QuestionnaireResponseIdentifier::new,
						QuestionnaireResponseQuestionnaire::new, QuestionnaireResponseStatus::new,
						QuestionnaireResponseSubject::new),
				with());
	}

	@Override
	protected QuestionnaireResponse copy(QuestionnaireResponse resource)
	{
		return resource.copy();
	}
}
