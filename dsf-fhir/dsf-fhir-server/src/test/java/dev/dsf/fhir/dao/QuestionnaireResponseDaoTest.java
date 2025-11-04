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
package dev.dsf.fhir.dao;

import static org.junit.Assert.assertEquals;

import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseStatus;

import dev.dsf.fhir.dao.jdbc.QuestionnaireResponseDaoJdbc;

public class QuestionnaireResponseDaoTest
		extends AbstractResourceDaoTest<QuestionnaireResponse, QuestionnaireResponseDao>
{
	public QuestionnaireResponseDaoTest()
	{
		super(QuestionnaireResponse.class, QuestionnaireResponseDaoJdbc::new);
	}

	@Override
	public QuestionnaireResponse createResource()
	{
		QuestionnaireResponse questionnaireResponse = new QuestionnaireResponse();
		questionnaireResponse.setStatus(QuestionnaireResponseStatus.INPROGRESS);
		return questionnaireResponse;
	}

	@Override
	protected void checkCreated(QuestionnaireResponse resource)
	{
		assertEquals(QuestionnaireResponseStatus.INPROGRESS, resource.getStatus());
	}

	@Override
	protected QuestionnaireResponse updateResource(QuestionnaireResponse resource)
	{
		resource.setStatus(QuestionnaireResponseStatus.COMPLETED);
		return resource;
	}

	@Override
	protected void checkUpdates(QuestionnaireResponse resource)
	{
		assertEquals(QuestionnaireResponseStatus.COMPLETED, resource.getStatus());
	}
}
