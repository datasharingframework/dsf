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

import org.hl7.fhir.r4.model.ResearchStudy;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.fhir.dao.ResearchStudyDao;
import dev.dsf.fhir.search.filter.ResearchStudyIdentityFilter;
import dev.dsf.fhir.search.parameters.ResearchStudyEnrollment;
import dev.dsf.fhir.search.parameters.ResearchStudyIdentifier;
import dev.dsf.fhir.search.parameters.ResearchStudyPrincipalInvestigator;

public class ResearchStudyDaoJdbc extends AbstractResourceDaoJdbc<ResearchStudy> implements ResearchStudyDao
{
	public ResearchStudyDaoJdbc(DataSource dataSource, DataSource permanentDeleteDataSource, FhirContext fhirContext)
	{
		super(dataSource, permanentDeleteDataSource, fhirContext, ResearchStudy.class, "research_studies",
				"research_study", "research_study_id", ResearchStudyIdentityFilter::new,
				List.of(factory(ResearchStudyEnrollment.PARAMETER_NAME, ResearchStudyEnrollment::new,
						ResearchStudyEnrollment.getNameModifiers(), ResearchStudyEnrollment::new,
						ResearchStudyEnrollment.getIncludeParameterValues()),
						factory(ResearchStudyIdentifier.PARAMETER_NAME, ResearchStudyIdentifier::new,
								ResearchStudyIdentifier.getNameModifiers()),
						factory(ResearchStudyPrincipalInvestigator.PARAMETER_NAME,
								ResearchStudyPrincipalInvestigator::new,
								ResearchStudyPrincipalInvestigator.getNameModifiers(),
								ResearchStudyPrincipalInvestigator::new,
								ResearchStudyPrincipalInvestigator.getIncludeParameterValues())),
				List.of());
	}

	@Override
	protected ResearchStudy copy(ResearchStudy resource)
	{
		return resource.copy();
	}
}
