package dev.dsf.fhir.dao.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.fhir.dao.BundleDao;
import dev.dsf.fhir.search.parameters.BundleIdentifier;
import dev.dsf.fhir.search.parameters.user.BundleUserFilter;

public class BundleDaoJdbc extends AbstractResourceDaoJdbc<Bundle> implements BundleDao
{
	public BundleDaoJdbc(DataSource dataSource, DataSource permanentDeleteDataSource, FhirContext fhirContext)
	{
		super(dataSource, permanentDeleteDataSource, fhirContext, Bundle.class, "bundles", "bundle", "bundle_id",
				BundleUserFilter::new, with(BundleIdentifier::new), with());
	}

	@Override
	protected Bundle copy(Bundle resource)
	{
		return resource.copy();
	}

	@Override
	protected Bundle getResource(ResultSet result, int index) throws SQLException
	{
		// TODO Bugfix HAPI is removing version information from bundle.id
		Bundle bundle = super.getResource(result, index);
		IdType fixedId = new IdType(bundle.getResourceType().name(), bundle.getIdElement().getIdPart(),
				bundle.getMeta().getVersionId());
		bundle.setIdElement(fixedId);
		return bundle;
	}
}
