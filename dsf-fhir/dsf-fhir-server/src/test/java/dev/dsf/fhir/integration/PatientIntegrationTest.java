package dev.dsf.fhir.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.UUID;

import org.hl7.fhir.r4.model.Patient;
import org.junit.Test;

import dev.dsf.fhir.authorization.read.ReadAccessHelper;
import dev.dsf.fhir.authorization.read.ReadAccessHelperImpl;
import dev.dsf.fhir.dao.PatientDao;

public class PatientIntegrationTest extends AbstractIntegrationTest
{
	private static final ReadAccessHelper readAccessHelper = new ReadAccessHelperImpl();

	@Test
	public void readVersion1() throws Exception
	{
		PatientDao dao = getSpringWebApplicationContext().getBean(PatientDao.class);

		Patient created = dao.create(readAccessHelper.addAll(new Patient()));
		assertNotNull(created);
		assertNotNull(created.getIdElement().getIdPart());
		assertEquals("1", created.getIdElement().getVersionIdPart());

		Patient latest = getWebserviceClient().read(Patient.class, created.getIdElement().getIdPart());
		assertNotNull(latest);
		Patient v1 = getWebserviceClient().read(Patient.class, created.getIdElement().getIdPart(), "1");
		assertNotNull(v1);

		expectNotFound(() -> getWebserviceClient().read(Patient.class, created.getIdElement().getIdPart(), "2"));
		expectNotFound(() -> getWebserviceClient().read(Patient.class, created.getIdElement().getIdPart(), "3"));
	}

	@Test
	public void readVersion2() throws Exception
	{
		PatientDao dao = getSpringWebApplicationContext().getBean(PatientDao.class);

		Patient created = dao.create(readAccessHelper.addAll(new Patient()));
		assertNotNull(created);
		assertNotNull(created.getIdElement().getIdPart());
		assertEquals("1", created.getIdElement().getVersionIdPart());
		Patient updated = dao.update(created);
		assertNotNull(updated);
		assertNotNull(updated.getIdElement().getIdPart());
		assertEquals("2", updated.getIdElement().getVersionIdPart());

		Patient latest = getWebserviceClient().read(Patient.class, created.getIdElement().getIdPart());
		assertNotNull(latest);
		Patient v1 = getWebserviceClient().read(Patient.class, created.getIdElement().getIdPart(), "1");
		assertNotNull(v1);
		Patient v2 = getWebserviceClient().read(Patient.class, created.getIdElement().getIdPart(), "2");
		assertNotNull(v2);

		expectNotFound(() -> getWebserviceClient().read(Patient.class, created.getIdElement().getIdPart(), "3"));
		expectNotFound(() -> getWebserviceClient().read(Patient.class, created.getIdElement().getIdPart(), "4"));
	}

	@Test
	public void readVersion1Deleted() throws Exception
	{
		PatientDao dao = getSpringWebApplicationContext().getBean(PatientDao.class);

		Patient created = dao.create(readAccessHelper.addAll(new Patient()));
		assertNotNull(created);
		assertNotNull(created.getIdElement().getIdPart());
		assertEquals("1", created.getIdElement().getVersionIdPart());

		dao.delete(UUID.fromString(created.getIdElement().getIdPart()));

		expectGone(() -> getWebserviceClient().read(Patient.class, created.getIdElement().getIdPart()));

		Patient v1 = getWebserviceClient().read(Patient.class, created.getIdElement().getIdPart(), "1");
		assertNotNull(v1);

		expectGone(() -> getWebserviceClient().read(Patient.class, created.getIdElement().getIdPart(), "2"));

		expectNotFound(() -> getWebserviceClient().read(Patient.class, created.getIdElement().getIdPart(), "3"));
	}

	@Test
	public void readVersion2Deleted() throws Exception
	{
		PatientDao dao = getSpringWebApplicationContext().getBean(PatientDao.class);

		Patient created = dao.create(readAccessHelper.addAll(new Patient()));
		assertNotNull(created);
		assertNotNull(created.getIdElement().getIdPart());
		assertEquals("1", created.getIdElement().getVersionIdPart());
		Patient updated = dao.update(created);
		assertNotNull(updated);
		assertNotNull(updated.getIdElement().getIdPart());
		assertEquals("2", updated.getIdElement().getVersionIdPart());

		dao.delete(UUID.fromString(created.getIdElement().getIdPart()));

		expectGone(() -> getWebserviceClient().read(Patient.class, created.getIdElement().getIdPart()));

		Patient v1 = getWebserviceClient().read(Patient.class, created.getIdElement().getIdPart(), "1");
		assertNotNull(v1);
		Patient v2 = getWebserviceClient().read(Patient.class, created.getIdElement().getIdPart(), "2");
		assertNotNull(v2);

		expectGone(() -> getWebserviceClient().read(Patient.class, created.getIdElement().getIdPart(), "3"));

		expectNotFound(() -> getWebserviceClient().read(Patient.class, created.getIdElement().getIdPart(), "4"));
	}
}
