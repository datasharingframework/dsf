package dev.dsf.fhir.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
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

	@Test
	public void readVersion1ViaBatchBundle() throws Exception
	{
		PatientDao dao = getSpringWebApplicationContext().getBean(PatientDao.class);

		Patient created = dao.create(readAccessHelper.addAll(new Patient()));
		assertNotNull(created);
		assertNotNull(created.getIdElement().getIdPart());
		assertEquals("1", created.getIdElement().getVersionIdPart());

		Bundle bundle = new Bundle().setType(BundleType.BATCH);
		bundle.addEntry().getRequest().setMethod(HTTPVerb.GET).setUrl("Patient/" + created.getIdElement().getIdPart());
		bundle.addEntry().getRequest().setMethod(HTTPVerb.GET)
				.setUrl("Patient/" + created.getIdElement().getIdPart() + "/_history/1");
		bundle.addEntry().getRequest().setMethod(HTTPVerb.GET)
				.setUrl("Patient/" + created.getIdElement().getIdPart() + "/_history/2");
		bundle.addEntry().getRequest().setMethod(HTTPVerb.GET)
				.setUrl("Patient/" + created.getIdElement().getIdPart() + "/_history/3");

		Bundle responseBundle = getWebserviceClient().postBundle(bundle);
		assertNotNull(responseBundle);

		List<BundleEntryComponent> entries = responseBundle.getEntry();
		assertNotNull(entries);
		BundleEntryComponent e0 = entries.get(0);
		assertNotNull(e0);
		assertNotNull(e0.getResource());
		assertEquals(created.getIdElement().toString(), e0.getResource().getIdElement().toString());
		assertTrue(e0.getResponse().getStatus().startsWith("200"));

		BundleEntryComponent e1 = entries.get(1);
		assertNotNull(e1);
		assertNotNull(e1.getResource());
		assertEquals(created.getIdElement().toString(), e1.getResource().getIdElement().toString());
		assertTrue(e1.getResponse().getStatus().startsWith("200"));

		BundleEntryComponent e2 = entries.get(2);
		assertNotNull(e2);
		assertNull(e2.getResource());
		assertTrue(e2.getResponse().getStatus().startsWith("404"));

		BundleEntryComponent e3 = entries.get(3);
		assertNotNull(e3);
		assertNull(e3.getResource());
		assertTrue(e3.getResponse().getStatus().startsWith("404"));
	}

	@Test
	public void readVersion2ViaBatchBundle() throws Exception
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

		Bundle bundle = new Bundle().setType(BundleType.BATCH);
		bundle.addEntry().getRequest().setMethod(HTTPVerb.GET).setUrl("Patient/" + created.getIdElement().getIdPart());
		bundle.addEntry().getRequest().setMethod(HTTPVerb.GET)
				.setUrl("Patient/" + created.getIdElement().getIdPart() + "/_history/1");
		bundle.addEntry().getRequest().setMethod(HTTPVerb.GET)
				.setUrl("Patient/" + created.getIdElement().getIdPart() + "/_history/2");
		bundle.addEntry().getRequest().setMethod(HTTPVerb.GET)
				.setUrl("Patient/" + created.getIdElement().getIdPart() + "/_history/3");
		bundle.addEntry().getRequest().setMethod(HTTPVerb.GET)
				.setUrl("Patient/" + created.getIdElement().getIdPart() + "/_history/4");

		Bundle responseBundle = getWebserviceClient().postBundle(bundle);
		assertNotNull(responseBundle);

		List<BundleEntryComponent> entries = responseBundle.getEntry();
		assertNotNull(entries);
		BundleEntryComponent e0 = entries.get(0);
		assertNotNull(e0);
		assertNotNull(e0.getResource());
		assertEquals(updated.getIdElement().toString(), e0.getResource().getIdElement().toString());
		assertTrue(e0.getResponse().getStatus().startsWith("200"));

		BundleEntryComponent e1 = entries.get(1);
		assertNotNull(e1);
		assertNotNull(e1.getResource());
		assertEquals(created.getIdElement().toString(), e1.getResource().getIdElement().toString());
		assertTrue(e1.getResponse().getStatus().startsWith("200"));

		BundleEntryComponent e2 = entries.get(2);
		assertNotNull(e2);
		assertNotNull(e2.getResource());
		assertEquals(updated.getIdElement().toString(), e2.getResource().getIdElement().toString());
		assertTrue(e2.getResponse().getStatus().startsWith("200"));

		BundleEntryComponent e3 = entries.get(3);
		assertNotNull(e3);
		assertNull(e3.getResource());
		assertTrue(e3.getResponse().getStatus().startsWith("404"));

		BundleEntryComponent e4 = entries.get(4);
		assertNotNull(e4);
		assertNull(e4.getResource());
		assertTrue(e4.getResponse().getStatus().startsWith("404"));
	}

	@Test
	public void readVersion1DeletedViaBatchBundle() throws Exception
	{
		PatientDao dao = getSpringWebApplicationContext().getBean(PatientDao.class);

		Patient created = dao.create(readAccessHelper.addAll(new Patient()));
		assertNotNull(created);
		assertNotNull(created.getIdElement().getIdPart());
		assertEquals("1", created.getIdElement().getVersionIdPart());

		dao.delete(UUID.fromString(created.getIdElement().getIdPart()));

		Bundle bundle = new Bundle().setType(BundleType.BATCH);
		bundle.addEntry().getRequest().setMethod(HTTPVerb.GET).setUrl("Patient/" + created.getIdElement().getIdPart());
		bundle.addEntry().getRequest().setMethod(HTTPVerb.GET)
				.setUrl("Patient/" + created.getIdElement().getIdPart() + "/_history/1");
		bundle.addEntry().getRequest().setMethod(HTTPVerb.GET)
				.setUrl("Patient/" + created.getIdElement().getIdPart() + "/_history/2");
		bundle.addEntry().getRequest().setMethod(HTTPVerb.GET)
				.setUrl("Patient/" + created.getIdElement().getIdPart() + "/_history/3");

		Bundle responseBundle = getWebserviceClient().postBundle(bundle);
		assertNotNull(responseBundle);

		List<BundleEntryComponent> entries = responseBundle.getEntry();
		assertNotNull(entries);
		BundleEntryComponent e0 = entries.get(0);
		assertNotNull(e0);
		assertNull(e0.getResource());
		assertTrue(e0.getResponse().getStatus().startsWith("410"));

		BundleEntryComponent e1 = entries.get(1);
		assertNotNull(e1);
		assertNotNull(e1.getResource());
		assertEquals(created.getIdElement().toString(), e1.getResource().getIdElement().toString());
		assertTrue(e1.getResponse().getStatus().startsWith("200"));

		BundleEntryComponent e2 = entries.get(2);
		assertNotNull(e2);
		assertNull(e2.getResource());
		assertTrue(e2.getResponse().getStatus().startsWith("410"));

		BundleEntryComponent e3 = entries.get(3);
		assertNotNull(e3);
		assertNull(e3.getResource());
		assertTrue(e3.getResponse().getStatus().startsWith("404"));
	}

	@Test
	public void readVersion2DeletedViaBatchBundle() throws Exception
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

		Bundle bundle = new Bundle().setType(BundleType.BATCH);
		bundle.addEntry().getRequest().setMethod(HTTPVerb.GET).setUrl("Patient/" + created.getIdElement().getIdPart());
		bundle.addEntry().getRequest().setMethod(HTTPVerb.GET)
				.setUrl("Patient/" + created.getIdElement().getIdPart() + "/_history/1");
		bundle.addEntry().getRequest().setMethod(HTTPVerb.GET)
				.setUrl("Patient/" + created.getIdElement().getIdPart() + "/_history/2");
		bundle.addEntry().getRequest().setMethod(HTTPVerb.GET)
				.setUrl("Patient/" + created.getIdElement().getIdPart() + "/_history/3");
		bundle.addEntry().getRequest().setMethod(HTTPVerb.GET)
				.setUrl("Patient/" + created.getIdElement().getIdPart() + "/_history/4");

		Bundle responseBundle = getWebserviceClient().postBundle(bundle);
		assertNotNull(responseBundle);

		List<BundleEntryComponent> entries = responseBundle.getEntry();
		assertNotNull(entries);
		BundleEntryComponent e0 = entries.get(0);
		assertNotNull(e0);
		assertNull(e0.getResource());
		assertTrue(e0.getResponse().getStatus().startsWith("410"));

		BundleEntryComponent e1 = entries.get(1);
		assertNotNull(e1);
		assertNotNull(e1.getResource());
		assertEquals(created.getIdElement().toString(), e1.getResource().getIdElement().toString());
		assertTrue(e1.getResponse().getStatus().startsWith("200"));

		BundleEntryComponent e2 = entries.get(2);
		assertNotNull(e2);
		assertNotNull(e2.getResource());
		assertEquals(updated.getIdElement().toString(), e2.getResource().getIdElement().toString());
		assertTrue(e2.getResponse().getStatus().startsWith("200"));

		BundleEntryComponent e3 = entries.get(3);
		assertNotNull(e3);
		assertNull(e3.getResource());
		assertTrue(e3.getResponse().getStatus().startsWith("410"));

		BundleEntryComponent e4 = entries.get(4);
		assertNotNull(e4);
		assertNull(e4.getResource());
		assertTrue(e4.getResponse().getStatus().startsWith("404"));
	}
}
