package dev.dsf.fhir.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent;
import org.hl7.fhir.r4.model.Bundle.BundleLinkComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestResourceComponent;
import org.hl7.fhir.r4.model.ElementDefinition;
import org.hl7.fhir.r4.model.Enumerations.FHIRVersion;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.StructureDefinition.StructureDefinitionKind;
import org.hl7.fhir.r4.model.StructureDefinition.TypeDerivationRule;
import org.hl7.fhir.r4.model.UriType;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.fhir.dao.PatientDao;
import dev.dsf.fhir.dao.StructureDefinitionDao;

public class BundleIntegrationTest extends AbstractIntegrationTest
{
	private static final Logger logger = LoggerFactory.getLogger(BundleIntegrationTest.class);

	@Test
	public void testCreateBundle() throws Exception
	{
		Bundle allowList = readBundle(Paths.get("src/test/resources/integration/allow-list.json"),
				fhirContext.newJsonParser());

		logger.debug(fhirContext.newJsonParser().encodeResourceToString(allowList));

		Bundle updatedBundle = getWebserviceClient().updateConditionaly(allowList,
				Map.of("identifier", List.of("http://dsf.dev/fhir/CodeSystem/update-allow-list|allow_list")));

		assertNotNull(updatedBundle);
	}

	@Test
	public void testCreateBundleReturnMinimal() throws Exception
	{
		Bundle allowList = readBundle(Paths.get("src/test/resources/integration/allow-list.json"),
				fhirContext.newJsonParser());

		logger.debug(fhirContext.newJsonParser().encodeResourceToString(allowList));

		IdType id = getWebserviceClient().withMinimalReturn().updateConditionaly(allowList,
				Map.of("identifier", List.of("http://dsf.dev/fhir/CodeSystem/update-allow-list|allow_list")));

		assertNotNull(id);
	}

	@Test
	public void testCreateBundleReturnOperationOutcome() throws Exception
	{
		Bundle allowList = readBundle(Paths.get("src/test/resources/integration/allow-list.json"),
				fhirContext.newJsonParser());

		logger.debug(fhirContext.newJsonParser().encodeResourceToString(allowList));

		OperationOutcome outcome = getWebserviceClient().withOperationOutcomeReturn().updateConditionaly(allowList,
				Map.of("identifier", List.of("http://dsf.dev/fhir/CodeSystem/update-allow-list|allow_list")));

		assertNotNull(outcome);
	}

	@Test
	public void testDeleteTaskProfileViaBundleTestSupportedProfilesInConformanceStatement() throws Exception
	{
		final String taskProfileUrl = "http://dsf.dev/fhir/StructureDefinition/task-test";
		final String taskProfileVersion = "1.2.3";

		StructureDefinition newS = new StructureDefinition();
		newS.getMeta().addTag().setSystem("http://dsf.dev/fhir/CodeSystem/read-access-tag").setCode("ALL");
		newS.setUrl(taskProfileUrl);
		newS.setVersion(taskProfileVersion);
		newS.setName("TaskTest");
		newS.setStatus(PublicationStatus.ACTIVE);
		newS.setFhirVersion(FHIRVersion._4_0_1);
		newS.setKind(StructureDefinitionKind.RESOURCE);
		newS.setAbstract(false);
		newS.setType("Task");
		newS.setBaseDefinition("http://dsf.dev/fhir/StructureDefinition/task-base");
		newS.setDerivation(TypeDerivationRule.CONSTRAINT);
		ElementDefinition diff = newS.getDifferential().addElement();
		diff.setId("Task.instantiatesUri");
		diff.setPath("Task.instantiatesUri");
		diff.setFixed(new UriType("http://dsf.dev/bpe/Process/taskTest/1.2.3"));

		assertFalse(testProfileSupported(taskProfileUrl));

		IdType newSid = getWebserviceClient().withMinimalReturn().create(newS);
		assertNotNull(newSid);

		assertTrue(testProfileSupported(taskProfileUrl));

		Bundle deleteBundle = new Bundle();
		deleteBundle.setType(BundleType.TRANSACTION);
		BundleEntryComponent deleteEntry = deleteBundle.addEntry();
		deleteEntry.setFullUrl(newSid.withServerBase(getWebserviceClient().getBaseUrl(), "Task").toString());

		BundleEntryRequestComponent request = deleteEntry.getRequest();
		request.setMethod(HTTPVerb.DELETE);
		request.setUrl("StructureDefinition/" + newSid.getIdPart());

		getWebserviceClient().withMinimalReturn().postBundle(deleteBundle);

		assertFalse(testProfileSupported(taskProfileUrl));
		StructureDefinitionDao sDdao = getSpringWebApplicationContext().getBean("structureDefinitionDao",
				StructureDefinitionDao.class);
		StructureDefinitionDao sDsDao = getSpringWebApplicationContext().getBean("structureDefinitionSnapshotDao",
				StructureDefinitionDao.class);

		assertTrue(sDdao.readByUrlAndVersion(taskProfileUrl, taskProfileVersion).isEmpty());
		assertTrue(sDsDao.readByUrlAndVersion(taskProfileUrl, taskProfileVersion).isEmpty());
	}

	private boolean testProfileSupported(String taskProfileUrl)
	{
		CapabilityStatement conformance = getWebserviceClient().getConformance();
		assertNotNull(conformance);
		assertTrue(conformance.hasRest());
		assertEquals(1, conformance.getRest().size());
		CapabilityStatementRestComponent rest = conformance.getRest().get(0);
		assertNotNull(rest);
		assertTrue(rest.hasResource());
		assertTrue(rest.getResource().size() > 0);

		Optional<CapabilityStatementRestResourceComponent> taskResourceOpt = rest.getResource().stream()
				.filter(r -> "Task".equals(r.getType())).findFirst();
		assertTrue(taskResourceOpt.isPresent());

		CapabilityStatementRestResourceComponent taskResource = taskResourceOpt.get();
		if (taskResource.hasSupportedProfile())
		{
			assertTrue(taskResource.getSupportedProfile().size() > 0);
			List<CanonicalType> profiles = taskResource.getSupportedProfile();

			return profiles.stream().filter(t -> Objects.equals(t.getValue(), taskProfileUrl)).count() == 1;
		}
		else
			return false;
	}

	@Test
	public void testPostTransactionBundle() throws Exception
	{
		Patient p0 = new Patient();
		PatientDao dao = getSpringWebApplicationContext().getBean(PatientDao.class);
		Patient p1 = dao.create(p0);

		Bundle bundle = createTestBundle(BundleType.TRANSACTION, p1.getIdElement());

		Bundle rBundle = getWebserviceClient().postBundle(bundle);

		checkReturnBundle(BundleType.TRANSACTIONRESPONSE, rBundle, bundle.getEntry().size(),
				List.of("200 OK", "201 Created", "200 OK", "200 OK", "200 OK", "200 OK", "200 OK", "404 Not Found"));

		DataSource dataSource = getSpringWebApplicationContext().getBean("dataSource", DataSource.class);
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement("SELECT count(*) FROM current_patients");
				ResultSet result = statement.executeQuery())
		{
			assertTrue(result.next());
			assertEquals(1, result.getInt(1));
		}
	}

	@Test
	public void testPostBatchBundle() throws Exception
	{
		Patient p0 = new Patient();
		PatientDao dao = getSpringWebApplicationContext().getBean(PatientDao.class);
		Patient p1 = dao.create(p0);

		Bundle bundle = createTestBundle(BundleType.BATCH, p1.getIdElement());

		Bundle rBundle = getWebserviceClient().postBundle(bundle);

		checkReturnBundle(BundleType.BATCHRESPONSE, rBundle, bundle.getEntry().size(),
				List.of("200 OK", "201 Created", "200 OK", "200 OK", "200 OK", "200 OK", "200 OK", "404 Not Found"));

		DataSource dataSource = getSpringWebApplicationContext().getBean("dataSource", DataSource.class);
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement("SELECT count(*) FROM current_patients");
				ResultSet result = statement.executeQuery())
		{
			assertTrue(result.next());
			assertEquals(1, result.getInt(1));
		}
	}

	private Bundle createTestBundle(BundleType type, IdType resourceToDelete)
	{
		Bundle bundle = new Bundle();
		bundle.setType(type);

		BundleEntryComponent delete = bundle.addEntry();
		delete.getRequest().setMethod(HTTPVerb.DELETE)
				.setUrl(resourceToDelete.getResourceType() + "/" + resourceToDelete.getIdPart());

		Identifier patientId = new Identifier().setSystem("http://test.org/sid/patient-id")
				.setValue(UUID.randomUUID().toString());

		Patient createPatient = new Patient();
		createPatient.addIdentifier(patientId);
		createPatient.setActive(true);
		getReadAccessHelper().addAll(createPatient);

		BundleEntryComponent create = bundle.addEntry();
		create.setFullUrl("urn:uuid:" + UUID.randomUUID().toString());
		create.getRequest().setMethod(HTTPVerb.POST).setUrl("Patient");
		create.setResource(createPatient);

		BundleEntryComponent createIfNotExists = bundle.addEntry();
		createIfNotExists.setFullUrl("urn:uuid:" + UUID.randomUUID().toString());
		createIfNotExists.getRequest().setMethod(HTTPVerb.POST).setUrl("Patient")
				.setIfNoneExist("identifier=" + patientId.getSystem() + "|" + patientId.getValue());
		createIfNotExists.setResource(createPatient);

		Patient updatePatient = new Patient();
		updatePatient.addIdentifier(patientId);
		updatePatient.setActive(false);
		getReadAccessHelper().addAll(updatePatient);

		BundleEntryComponent update = bundle.addEntry();
		update.setFullUrl("urn:uuid:" + UUID.randomUUID().toString());
		update.getRequest().setMethod(HTTPVerb.PUT)
				.setUrl("Patient?identifier=" + patientId.getSystem() + "|" + patientId.getValue());
		update.setResource(updatePatient);

		BundleEntryComponent get = bundle.addEntry();
		get.getRequest().setMethod(HTTPVerb.GET)
				.setUrl("Patient?identifier=" + patientId.getSystem() + "|" + patientId.getValue());

		BundleEntryComponent searchNotFound1 = bundle.addEntry();
		searchNotFound1.getRequest().setMethod(HTTPVerb.GET)
				.setUrl("Patient?identifier=" + patientId.getSystem() + "|not-existing");

		BundleEntryComponent searchNotFound2 = bundle.addEntry();
		searchNotFound2.getRequest().setMethod(HTTPVerb.GET).setUrl("Patient?id=" + UUID.randomUUID().toString());

		BundleEntryComponent getNotFound = bundle.addEntry();
		getNotFound.getRequest().setMethod(HTTPVerb.GET).setUrl("Patient/" + UUID.randomUUID().toString());

		return bundle;
	}

	private void checkReturnBundle(BundleType type, Bundle rBundle, int expectedEntrySize, List<String> expectedStatus)
	{
		logger.debug("Return Bundle:\n{}", newJsonParser().setPrettyPrint(true).encodeResourceToString(rBundle));

		assertNotNull(rBundle);
		assertEquals(type, rBundle.getType());
		assertEquals(expectedEntrySize, rBundle.getEntry().size());

		for (int i = 0; i < expectedEntrySize; i++)
		{
			assertTrue(rBundle.getEntry().get(i).hasResponse());
			assertTrue(rBundle.getEntry().get(i).getResponse().hasStatus());
			assertEquals(rBundle.getEntry().get(i).getResponse().getStatus(), expectedStatus.get(i));
		}
	}

	@Test
	public void testPostFailingTransactionBundle() throws Exception
	{
		Patient p0 = new Patient();
		PatientDao dao = getSpringWebApplicationContext().getBean(PatientDao.class);
		Patient p1 = dao.create(p0);

		Bundle bundle = createFailingTestBundle(BundleType.TRANSACTION, p1.getIdElement());

		expectBadRequest(() -> getWebserviceClient().postBundle(bundle));
	}

	@Test
	public void testPostPartialyFailingBatchBundle() throws Exception
	{
		Patient p0 = new Patient();
		PatientDao dao = getSpringWebApplicationContext().getBean(PatientDao.class);
		Patient p1 = dao.create(p0);

		Bundle bundle = createFailingTestBundle(BundleType.BATCH, p1.getIdElement());

		Bundle rBundle = getWebserviceClient().postBundle(bundle);

		checkReturnBundle(BundleType.BATCHRESPONSE, rBundle, bundle.getEntry().size(),
				List.of("200 OK", "405 Method Not Allowed"));

		DataSource dataSource = getSpringWebApplicationContext().getBean("dataSource", DataSource.class);
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection
						.prepareStatement("SELECT count(*) FROM current_patients WHERE patient_id::text = ?"))
		{
			statement.setString(1, p1.getIdElement().getIdPart());

			try (ResultSet result = statement.executeQuery())
			{
				assertTrue(result.next());
				assertEquals(0, result.getInt(1));
			}
		}
	}

	private Bundle createFailingTestBundle(BundleType type, IdType resourceToDelete)
	{
		Bundle bundle = new Bundle();
		bundle.setType(type);

		BundleEntryComponent delete = bundle.addEntry();
		delete.getRequest().setMethod(HTTPVerb.DELETE)
				.setUrl(resourceToDelete.getResourceType() + "/" + resourceToDelete.getIdPart());

		String updateId = UUID.randomUUID().toString();

		Patient updatePatient = new Patient();
		updatePatient.getIdElement().setValue(updateId);
		updatePatient.setActive(false);
		getReadAccessHelper().addAll(updatePatient);

		BundleEntryComponent update = bundle.addEntry();
		update.setFullUrl(getWebserviceClient().getBaseUrl() + "Patient/" + updateId);
		update.getRequest().setMethod(HTTPVerb.PUT).setUrl("Patient/" + updateId);
		update.setResource(updatePatient);

		return bundle;
	}

	@Test
	public void createBundleInBundle1() throws Exception
	{
		StructureDefinition sd = readTestBundleProfile("test-bundle-profile1.xml");
		getWebserviceClient().create(sd);

		Bundle b = new Bundle().setType(BundleType.BATCH);
		b.getMeta().addProfile(sd.getUrl() + "|" + sd.getVersion());

		b.addEntry().setRequest(new BundleEntryRequestComponent().setMethod(HTTPVerb.GET).setUrl("Conset"));
		b.addEntry().setRequest(new BundleEntryRequestComponent().setMethod(HTTPVerb.GET).setUrl("Condition"));

		getReadAccessHelper().addAll(b);
		Bundle created = getWebserviceClient().create(b);

		assertNotNull(created);
		assertNotNull(created.getIdElement());
		assertNotNull(created.getIdElement().getIdPart());
	}

	@Test
	public void createBundleInBundle2() throws Exception
	{
		StructureDefinition sd = readTestBundleProfile("test-bundle-profile2.xml");
		getWebserviceClient().create(sd);

		Bundle b = new Bundle().setType(BundleType.BATCHRESPONSE);
		b.getMeta().addProfile(sd.getUrl() + "|" + sd.getVersion());

		BundleEntryComponent e = b.addEntry();
		e.setResource(new Bundle().setType(BundleType.SEARCHSET).setTotal(0)
				.addLink(new BundleLinkComponent().setRelation("self").setUrl("Medication")));
		e.getResponse().setStatus("200");

		getReadAccessHelper().addAll(b);
		Bundle created = getWebserviceClient().create(b);

		assertNotNull(created);
		assertNotNull(created.getIdElement());
		assertNotNull(created.getIdElement().getIdPart());
	}

	private StructureDefinition readTestBundleProfile(String bundleFile) throws IOException
	{
		try (InputStream in = Files.newInputStream(Paths.get("src/test/resources/integration/bundle", bundleFile)))
		{
			return fhirContext.newXmlParser().parseResource(StructureDefinition.class, in);
		}
	}
}