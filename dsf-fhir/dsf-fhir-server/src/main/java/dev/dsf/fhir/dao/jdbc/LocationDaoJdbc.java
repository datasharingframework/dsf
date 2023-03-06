package dev.dsf.fhir.dao.jdbc;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.Location;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.fhir.dao.LocationDao;
import dev.dsf.fhir.search.filter.LocationIdentityFilter;
import dev.dsf.fhir.search.parameters.LocationIdentifier;

public class LocationDaoJdbc extends AbstractResourceDaoJdbc<Location> implements LocationDao
{
	public LocationDaoJdbc(DataSource dataSource, DataSource permanentDeleteDataSource, FhirContext fhirContext)
	{
		super(dataSource, permanentDeleteDataSource, fhirContext, Location.class, "locations", "location",
				"location_id", LocationIdentityFilter::new, with(LocationIdentifier::new), with());
	}

	@Override
	protected Location copy(Location resource)
	{
		return resource.copy();
	}
}
