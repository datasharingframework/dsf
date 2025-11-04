/*
 * Copyright 2018-2025 Heilbronn University of Applied Sciences
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.dsf.fhir.dao.jdbc;

import java.util.List;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.QuestionnaireResponse;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.fhir.dao.QuestionnaireResponseDao;
import dev.dsf.fhir.search.filter.QuestionnaireResponseIdentityFilter;
import dev.dsf.fhir.search.parameters.QuestionnaireResponseAuthor;
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
				List.of(factory(QuestionnaireResponseAuthor.PARAMETER_NAME, QuestionnaireResponseAuthor::new,
						QuestionnaireResponseAuthor.getNameModifiers(), QuestionnaireResponseAuthor::new,
						QuestionnaireResponseAuthor.getIncludeParameterValues()),
						factory(QuestionnaireResponseAuthored.PARAMETER_NAME, QuestionnaireResponseAuthored::new),
						factory(QuestionnaireResponseIdentifier.PARAMETER_NAME, QuestionnaireResponseIdentifier::new,
								QuestionnaireResponseIdentifier.getNameModifiers()),
						factory(QuestionnaireResponseQuestionnaire.PARAMETER_NAME,
								QuestionnaireResponseQuestionnaire::new,
								QuestionnaireResponseQuestionnaire.getNameModifiers(),
								QuestionnaireResponseQuestionnaire::new,
								QuestionnaireResponseQuestionnaire.getIncludeParameterValues()),
						factory(QuestionnaireResponseStatus.PARAMETER_NAME, QuestionnaireResponseStatus::new,
								QuestionnaireResponseStatus.getNameModifiers()),
						factory(QuestionnaireResponseSubject.PARAMETER_NAME, QuestionnaireResponseSubject::new,
								QuestionnaireResponseSubject.getNameModifiers(), QuestionnaireResponseSubject::new,
								QuestionnaireResponseSubject.getIncludeParameterValues())),
				List.of());
	}

	@Override
	protected QuestionnaireResponse copy(QuestionnaireResponse resource)
	{
		return resource.copy();
	}
}
