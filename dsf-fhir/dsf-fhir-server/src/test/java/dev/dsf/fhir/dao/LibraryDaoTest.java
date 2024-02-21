package dev.dsf.fhir.dao;

import static org.junit.Assert.assertEquals;

import org.hl7.fhir.r4.model.Library;
import org.junit.Test;

import dev.dsf.fhir.dao.jdbc.LibraryDaoJdbc;

public class LibraryDaoTest extends AbstractReadAccessDaoTest<Library, LibraryDao> implements ReadByUrlDaoTest<Library>
{
	private static final String name = "Demo Library";
	private static final String description = "Demo Library Description";

	public LibraryDaoTest()
	{
		super(Library.class, LibraryDaoJdbc::new);
	}

	@Override
	public Library createResource()
	{
		Library library = new Library();
		library.setName(name);
		return library;
	}

	@Override
	protected void checkCreated(Library resource)
	{
		assertEquals(name, resource.getName());
	}

	@Override
	protected Library updateResource(Library resource)
	{
		resource.setDescription(description);
		return resource;
	}

	@Override
	protected void checkUpdates(Library resource)
	{
		assertEquals(description, resource.getDescription());
	}

	@Override
	public Library createResourceWithUrlAndVersion()
	{
		Library resource = createResource();
		resource.setUrl(getUrl());
		resource.setVersion(getVersion());
		return resource;
	}

	@Override
	public String getUrl()
	{
		return "http://test.com/fhir/Library/test-library";
	}

	@Override
	public String getVersion()
	{
		return "1.0.0";
	}

	@Override
	public ReadByUrlDao<Library> readByUrlDao()
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
