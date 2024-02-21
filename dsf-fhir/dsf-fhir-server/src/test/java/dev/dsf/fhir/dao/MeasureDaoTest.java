package dev.dsf.fhir.dao;

import static org.junit.Assert.assertEquals;

import org.hl7.fhir.r4.model.Measure;
import org.junit.Test;

import dev.dsf.fhir.dao.jdbc.MeasureDaoJdbc;

public class MeasureDaoTest extends AbstractReadAccessDaoTest<Measure, MeasureDao> implements ReadByUrlDaoTest<Measure>
{
	private static final String name = "Demo Measure";
	private static final String description = "Demo Measure Description";

	public MeasureDaoTest()
	{
		super(Measure.class, MeasureDaoJdbc::new);
	}

	@Override
	public Measure createResource()
	{
		Measure measure = new Measure();
		measure.setName(name);
		return measure;
	}

	@Override
	protected void checkCreated(Measure resource)
	{
		assertEquals(name, resource.getName());
	}

	@Override
	protected Measure updateResource(Measure resource)
	{
		resource.setDescription(description);
		return resource;
	}

	@Override
	protected void checkUpdates(Measure resource)
	{
		assertEquals(description, resource.getDescription());
	}

	@Override
	public Measure createResourceWithUrlAndVersion()
	{
		Measure resource = createResource();
		resource.setUrl(getUrl());
		resource.setVersion(getVersion());
		return resource;
	}

	@Override
	public String getUrl()
	{
		return "http://test.com/fhir/Measure/test-measure";
	}

	@Override
	public String getVersion()
	{
		return "1.0.0";
	}

	@Override
	public ReadByUrlDao<Measure> readByUrlDao()
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
