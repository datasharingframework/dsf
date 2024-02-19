package dev.dsf.fhir.dao;

import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.GregorianCalendar;

import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.Patient;

import dev.dsf.fhir.dao.jdbc.PatientDaoJdbc;

public class PatientDaoTest extends AbstractReadAccessDaoTest<Patient, PatientDao>
{
	private final Date birthday = new GregorianCalendar(1980, 0, 2).getTime();
	private final AdministrativeGender gender = AdministrativeGender.FEMALE;

	public PatientDaoTest()
	{
		super(Patient.class, PatientDaoJdbc::new);
	}

	@Override
	public Patient createResource()
	{
		Patient patient = new Patient();
		patient.setBirthDate(birthday);
		return patient;
	}

	@Override
	protected void checkCreated(Patient resource)
	{
		assertEquals(birthday, resource.getBirthDate());
	}

	@Override
	protected Patient updateResource(Patient resource)
	{
		resource.setGender(gender);
		return resource;
	}

	@Override
	protected void checkUpdates(Patient resource)
	{
		assertEquals(gender, resource.getGender());
	}
}
