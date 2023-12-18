package dev.dsf.fhir.dao.jdbc;

import java.util.List;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.Patient;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.fhir.dao.PatientDao;
import dev.dsf.fhir.search.filter.PatientIdentityFilter;
import dev.dsf.fhir.search.parameters.PatientActive;
import dev.dsf.fhir.search.parameters.PatientIdentifier;

public class PatientDaoJdbc extends AbstractResourceDaoJdbc<Patient> implements PatientDao
{
	public PatientDaoJdbc(DataSource dataSource, DataSource permanentDeleteDataSource, FhirContext fhirContext)
	{
		super(dataSource, permanentDeleteDataSource, fhirContext, Patient.class, "patients", "patient", "patient_id",
				PatientIdentityFilter::new,
				List.of(factory(PatientActive.PARAMETER_NAME, PatientActive::new),
						factory(PatientIdentifier.PARAMETER_NAME, PatientIdentifier::new,
								PatientIdentifier.getNameModifiers())),
				List.of());
	}

	@Override
	protected Patient copy(Patient resource)
	{
		return resource.copy();
	}
}
