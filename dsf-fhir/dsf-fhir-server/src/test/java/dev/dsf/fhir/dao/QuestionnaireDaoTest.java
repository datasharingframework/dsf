package dev.dsf.fhir.dao;

import static org.junit.Assert.assertEquals;

import org.hl7.fhir.r4.model.Questionnaire;
import org.junit.Test;

import dev.dsf.fhir.dao.jdbc.QuestionnaireDaoJdbc;

public class QuestionnaireDaoTest extends AbstractReadAccessDaoTest<Questionnaire, QuestionnaireDao>
		implements ReadByUrlDaoTest<Questionnaire>
{
	private static final String name = "Demo Questionnaire";
	private static final String description = "Demo Questionnaire Description";

	public QuestionnaireDaoTest()
	{
		super(Questionnaire.class, QuestionnaireDaoJdbc::new);
	}

	@Override
	public Questionnaire createResource()
	{
		Questionnaire questionnaire = new Questionnaire();
		questionnaire.setName(name);
		return questionnaire;
	}

	@Override
	protected void checkCreated(Questionnaire resource)
	{
		assertEquals(name, resource.getName());
	}

	@Override
	protected Questionnaire updateResource(Questionnaire resource)
	{
		resource.setDescription(description);
		return resource;
	}

	@Override
	protected void checkUpdates(Questionnaire resource)
	{
		assertEquals(description, resource.getDescription());
	}

	@Override
	public Questionnaire createResourceWithUrlAndVersion()
	{
		Questionnaire resource = createResource();
		resource.setUrl(getUrl());
		resource.setVersion(getVersion());
		return resource;
	}

	@Override
	public String getUrl()
	{
		return "http://test.com/fhir/Questionnaire/test-questionnaire";
	}

	@Override
	public String getVersion()
	{
		return "0.6.0";
	}

	@Override
	public ReadByUrlDao<Questionnaire> readByUrlDao()
	{
		return getDao();
	}

	@Override
	@Test
	public void testReadByUrlAndVersionWithUrl1() throws Exception
	{
		ReadByUrlDaoTest.super.testReadByUrlAndVersionWithUrl1();
	}

	@Override
	@Test
	public void testReadByUrlAndVersionWithUrlAndVersion1() throws Exception
	{
		ReadByUrlDaoTest.super.testReadByUrlAndVersionWithUrlAndVersion1();
	}

	@Override
	@Test
	public void testReadByUrlAndVersionWithUrl2() throws Exception
	{
		ReadByUrlDaoTest.super.testReadByUrlAndVersionWithUrl2();
	}

	@Override
	@Test
	public void testReadByUrlAndVersionWithUrlAndVersion2() throws Exception
	{
		ReadByUrlDaoTest.super.testReadByUrlAndVersionWithUrlAndVersion2();
	}
}
