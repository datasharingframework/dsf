package dev.dsf.fhir.dao.jdbc;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.HealthcareService;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.fhir.dao.HealthcareServiceDao;
import dev.dsf.fhir.search.parameters.HealthcareServiceActive;
import dev.dsf.fhir.search.parameters.HealthcareServiceIdentifier;
import dev.dsf.fhir.search.parameters.user.HealthcareServiceUserFilter;

public class HealthcareServiceDaoJdbc extends AbstractResourceDaoJdbc<HealthcareService> implements HealthcareServiceDao
{
	public HealthcareServiceDaoJdbc(DataSource dataSource, DataSource permanentDeleteDataSource,
			FhirContext fhirContext)
	{
		super(dataSource, permanentDeleteDataSource, fhirContext, HealthcareService.class, "healthcare_services",
				"healthcare_service", "healthcare_service_id", HealthcareServiceUserFilter::new,
				with(HealthcareServiceActive::new, HealthcareServiceIdentifier::new), with());
	}

	@Override
	protected HealthcareService copy(HealthcareService resource)
	{
		return resource.copy();
	}
}
