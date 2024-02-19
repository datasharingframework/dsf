package dev.dsf.fhir.dao;

import static org.junit.Assert.assertEquals;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleType;

import dev.dsf.fhir.dao.jdbc.BundleDaoJdbc;

public class BundleDaoTest extends AbstractReadAccessDaoTest<Bundle, BundleDao>
{
	private static final BundleType type = BundleType.SEARCHSET;
	private static final String language = "Demo Bundle language";

	public BundleDaoTest()
	{
		super(Bundle.class, BundleDaoJdbc::new);
	}

	@Override
	public Bundle createResource()
	{
		Bundle bundle = new Bundle();
		bundle.setType(type);
		return bundle;
	}

	@Override
	protected void checkCreated(Bundle resource)
	{
		assertEquals(type, resource.getType());
	}

	@Override
	protected Bundle updateResource(Bundle resource)
	{
		resource.setLanguage(language);
		return resource;
	}

	@Override
	protected void checkUpdates(Bundle resource)
	{
		assertEquals(language, resource.getLanguage());
	}
}
