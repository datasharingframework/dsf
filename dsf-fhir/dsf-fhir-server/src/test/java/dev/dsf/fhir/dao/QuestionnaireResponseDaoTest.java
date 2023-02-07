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
