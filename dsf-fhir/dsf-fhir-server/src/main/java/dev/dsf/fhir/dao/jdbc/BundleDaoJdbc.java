package dev.dsf.fhir.dao.jdbc;

import java.util.List;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.Bundle;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.fhir.dao.BundleDao;
import dev.dsf.fhir.search.filter.BundleIdentityFilter;
import dev.dsf.fhir.search.parameters.BundleIdentifier;

public class BundleDaoJdbc extends AbstractResourceDaoJdbc<Bundle> implements BundleDao
{
	public BundleDaoJdbc(DataSource dataSource, DataSource permanentDeleteDataSource, FhirContext fhirContext)
	{
		super(dataSource, permanentDeleteDataSource, fhirContext, Bundle.class, "bundles", "bundle", "bundle_id",
				BundleIdentityFilter::new, List.of(factory(BundleIdentifier.PARAMETER_NAME, BundleIdentifier::new,
						BundleIdentifier.getNameModifiers())),
				List.of());
	}

	@Override
	protected Bundle copy(Bundle resource)
	{
		return resource.copy();
	}
}
