package dev.dsf.fhir.dao.jdbc;

import java.util.Collections;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.Provenance;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.fhir.dao.ProvenanceDao;
import dev.dsf.fhir.search.filter.ProvenanceIdentityFilter;

public class ProvenanceDaoJdbc extends AbstractResourceDaoJdbc<Provenance> implements ProvenanceDao
{
	public ProvenanceDaoJdbc(DataSource dataSource, DataSource permanentDeleteDataSource, FhirContext fhirContext)
	{
		super(dataSource, permanentDeleteDataSource, fhirContext, Provenance.class, "provenances", "provenance",
				"provenance_id", ProvenanceIdentityFilter::new, Collections.emptyList(), Collections.emptyList());
	}

	@Override
	protected Provenance copy(Provenance resource)
	{
		return resource.copy();
	}
}
