package dev.dsf.fhir.dao.jdbc;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.Patient;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.fhir.dao.PatientDao;
import dev.dsf.fhir.search.parameters.PatientActive;
import dev.dsf.fhir.search.parameters.PatientIdentifier;
import dev.dsf.fhir.search.parameters.user.PatientUserFilter;

public class PatientDaoJdbc extends AbstractResourceDaoJdbc<Patient> implements PatientDao
{
	public PatientDaoJdbc(DataSource dataSource, DataSource permanentDeleteDataSource, FhirContext fhirContext)
	{
		super(dataSource, permanentDeleteDataSource, fhirContext, Patient.class, "patients", "patient", "patient_id",
				PatientUserFilter::new, with(PatientActive::new, PatientIdentifier::new), with());
	}

	@Override
	protected Patient copy(Patient resource)
	{
		return resource.copy();
	}
}
