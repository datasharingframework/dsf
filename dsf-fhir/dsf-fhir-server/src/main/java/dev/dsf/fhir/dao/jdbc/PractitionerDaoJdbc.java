package dev.dsf.fhir.dao.jdbc;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.Practitioner;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.fhir.dao.PractitionerDao;
import dev.dsf.fhir.search.filter.PractitionerIdentityFilter;
import dev.dsf.fhir.search.parameters.PractitionerActive;
import dev.dsf.fhir.search.parameters.PractitionerIdentifier;

public class PractitionerDaoJdbc extends AbstractResourceDaoJdbc<Practitioner> implements PractitionerDao
{
	public PractitionerDaoJdbc(DataSource dataSource, DataSource permanentDeleteDataSource, FhirContext fhirContext)
	{
		super(dataSource, permanentDeleteDataSource, fhirContext, Practitioner.class, "practitioners", "practitioner",
				"practitioner_id", PractitionerIdentityFilter::new,
				with(PractitionerActive::new, PractitionerIdentifier::new), with());
	}

	@Override
	protected Practitioner copy(Practitioner resource)
	{
		return resource.copy();
	}
}
