package dev.dsf.fhir.dao;

import static org.junit.Assert.assertEquals;

import org.hl7.fhir.r4.model.Location;

import dev.dsf.fhir.dao.jdbc.LocationDaoJdbc;

public class LocationDaoTest extends AbstractReadAccessDaoTest<Location, LocationDao>
{
	private static final String name = "Demo Location";
	private static final String description = "Demo Location Description";

	public LocationDaoTest()
	{
		super(Location.class, LocationDaoJdbc::new);
	}

	@Override
	public Location createResource()
	{
		Location location = new Location();
		location.setName(name);
		return location;
	}

	@Override
	protected void checkCreated(Location resource)
	{
		assertEquals(name, resource.getName());
	}

	@Override
	protected Location updateResource(Location resource)
	{
		resource.setDescription(description);
		return resource;
	}

	@Override
	protected void checkUpdates(Location resource)
	{
		assertEquals(description, resource.getDescription());
	}
}
