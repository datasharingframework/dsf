package dev.dsf.fhir.dao.jdbc;

import java.util.List;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.HealthcareService;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.fhir.dao.HealthcareServiceDao;
import dev.dsf.fhir.search.filter.HealthcareServiceIdentityFilter;
import dev.dsf.fhir.search.parameters.HealthcareServiceActive;
import dev.dsf.fhir.search.parameters.HealthcareServiceIdentifier;
import dev.dsf.fhir.search.parameters.HealthcareServiceName;

public class HealthcareServiceDaoJdbc extends AbstractResourceDaoJdbc<HealthcareService> implements HealthcareServiceDao
{
	public HealthcareServiceDaoJdbc(DataSource dataSource, DataSource permanentDeleteDataSource,
			FhirContext fhirContext)
	{
		super(dataSource, permanentDeleteDataSource, fhirContext, HealthcareService.class, "healthcare_services",
				"healthcare_service", "healthcare_service_id", HealthcareServiceIdentityFilter::new,
				List.of(factory(HealthcareServiceActive.PARAMETER_NAME, HealthcareServiceActive::new),
						factory(HealthcareServiceName.PARAMETER_NAME, HealthcareServiceName::new,
								HealthcareServiceName.getNameModifiers()),
						factory(HealthcareServiceIdentifier.PARAMETER_NAME, HealthcareServiceIdentifier::new,
								HealthcareServiceIdentifier.getNameModifiers())),
				List.of());
	}

	@Override
	protected HealthcareService copy(HealthcareService resource)
	{
		return resource.copy();
	}
}
