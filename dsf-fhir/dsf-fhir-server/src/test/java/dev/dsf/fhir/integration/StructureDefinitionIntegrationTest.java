package dev.dsf.fhir.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.junit.Test;

import dev.dsf.fhir.dao.StructureDefinitionDao;
import dev.dsf.fhir.dao.exception.ResourceDeletedException;
import dev.dsf.fhir.event.Event;
import dev.dsf.fhir.event.EventManager;
import dev.dsf.fhir.event.ResourceCreatedEvent;
import dev.dsf.fhir.event.ResourceUpdatedEvent;

public class StructureDefinitionIntegrationTest extends AbstractIntegrationTest
{
	private static final Path PROFILE_FOLDER = Paths.get("src/test/resources/integration/structuredefinition");

	private void testCreateWithoutSnapshot(Function<StructureDefinition, StructureDefinition> createOp) throws Exception
	{
		EventManager eventManager = getSpringWebApplicationContext().getBean(EventManager.class);
		List<Event> events = new ArrayList<>();
		eventManager.addHandler(events::add);

		StructureDefinition profile = readProfile(PROFILE_FOLDER.resolve("dsf-task-test.xml"));
		StructureDefinition created = createOp.apply(profile);
		assertNotNull(created);
		assertTrue(created.hasIdElement());
		assertEquals("1", created.getIdElement().getVersionIdPart());
		assertFalse(created.hasSnapshot());

		assertEquals(1, events.size());

		Event event0 = events.get(0);
		assertEquals(ResourceCreatedEvent.class, event0.getClass());
		assertEquals(StructureDefinition.class, event0.getResourceType());

		StructureDefinition fromEvent = (StructureDefinition) event0.getResource();
		assertTrue(fromEvent.hasSnapshot());
		assertTrue(fromEvent.hasIdElement());
		assertEquals(created.getIdElement().getIdPart(), fromEvent.getIdElement().getIdPart());
		assertEquals(created.getIdElement().getVersionIdPart(), fromEvent.getIdElement().getVersionIdPart());

		testDbResources(UUID.fromString(created.getIdElement().getIdPart()),
				created.getIdElement().getVersionIdPartAsLong());
	}

	private void testCreateWithSnapshot(Function<StructureDefinition, StructureDefinition> createOp) throws Exception
	{
		EventManager eventManager = getSpringWebApplicationContext().getBean(EventManager.class);
		List<Event> events = new ArrayList<>();
		eventManager.addHandler(events::add);

		StructureDefinition profile = readProfile(PROFILE_FOLDER.resolve("dsf-task-test-snapshot.xml"));
		StructureDefinition created = createOp.apply(profile);
		assertNotNull(created);
		assertTrue(created.hasIdElement());
		assertEquals("1", created.getIdElement().getVersionIdPart());
		assertTrue(created.hasSnapshot());

		assertEquals(1, events.size());

		Event event0 = events.get(0);
		assertEquals(ResourceCreatedEvent.class, event0.getClass());
		assertEquals(StructureDefinition.class, event0.getResourceType());

		StructureDefinition fromEvent = (StructureDefinition) event0.getResource();
		assertTrue(fromEvent.hasSnapshot());
		assertTrue(fromEvent.hasIdElement());
		assertEquals(created.getIdElement().getIdPart(), fromEvent.getIdElement().getIdPart());
		assertEquals(created.getIdElement().getVersionIdPart(), fromEvent.getIdElement().getVersionIdPart());

		testDbResources(UUID.fromString(created.getIdElement().getIdPart()),
				created.getIdElement().getVersionIdPartAsLong());
	}

	private void testDbResources(UUID id, long version) throws SQLException, ResourceDeletedException
	{
		StructureDefinitionDao snapshotDao = getSpringWebApplicationContext().getBean("structureDefinitionSnapshotDao",
				StructureDefinitionDao.class);
		StructureDefinitionDao dao = getSpringWebApplicationContext().getBean("structureDefinitionDao",
				StructureDefinitionDao.class);

		Optional<StructureDefinition> readSnapshot = snapshotDao.readVersion(id, version);
		assertTrue(readSnapshot.isPresent());
		assertTrue(readSnapshot.get().hasSnapshot());

		Optional<StructureDefinition> read = dao.readVersion(id, version);
		assertTrue(read.isPresent());
		assertFalse(read.get().hasSnapshot());
	}

	private StructureDefinition readProfile(Path path) throws IOException
	{
		try (InputStream in = Files.newInputStream(path))
		{
			return fhirContext.newXmlParser().parseResource(StructureDefinition.class, in);
		}
	}

	@Test
	public void testCreateWithoutSnapshot() throws Exception
	{
		testCreateWithoutSnapshot(getWebserviceClient()::create);
	}

	@Test
	public void testCreateWithSnapshot() throws Exception
	{
		testCreateWithSnapshot(getWebserviceClient()::create);
	}

	@Test
	public void testCreateWithoutSnapshotViaTransactionBundle() throws Exception
	{
		testCreateWithoutSnapshot(profile ->
		{
			Bundle b = createPostBundle(profile, BundleType.TRANSACTION);
			Bundle returnBundle = getWebserviceClient().postBundle(b);
			testReturnBundle(returnBundle, BundleType.TRANSACTIONRESPONSE);

			return (StructureDefinition) returnBundle.getEntryFirstRep().getResource();
		});
	}

	@Test
	public void testCreateWithoutSnapshotViaBatchBundle() throws Exception
	{
		testCreateWithoutSnapshot(profile ->
		{
			Bundle createBundle = createPostBundle(profile, BundleType.BATCH);
			Bundle returnBundle = getWebserviceClient().postBundle(createBundle);
			testReturnBundle(returnBundle, BundleType.BATCHRESPONSE);

			return (StructureDefinition) returnBundle.getEntryFirstRep().getResource();
		});
	}

	@Test
	public void testCreateWithSnapshotViaTransactionBundle() throws Exception
	{
		testCreateWithSnapshot(profile ->
		{
			Bundle b = createPostBundle(profile, BundleType.TRANSACTION);
			Bundle returnBundle = getWebserviceClient().postBundle(b);
			testReturnBundle(returnBundle, BundleType.TRANSACTIONRESPONSE);

			return (StructureDefinition) returnBundle.getEntryFirstRep().getResource();
		});
	}

	@Test
	public void testCreateWithSnapshotViaBatchBundle() throws Exception
	{
		testCreateWithSnapshot(profile ->
		{
			Bundle createBundle = createPostBundle(profile, BundleType.BATCH);
			Bundle returnBundle = getWebserviceClient().postBundle(createBundle);
			testReturnBundle(returnBundle, BundleType.BATCHRESPONSE);

			return (StructureDefinition) returnBundle.getEntryFirstRep().getResource();
		});
	}

	private Bundle createPostBundle(StructureDefinition profile, BundleType type)
	{
		Bundle b = new Bundle();
		b.setType(type);
		b.addEntry().setFullUrl("urn:uuid:" + UUID.randomUUID().toString()).setResource(profile).getRequest()
				.setMethod(HTTPVerb.POST).setUrl("StructureDefinition");
		return b;
	}

	private Bundle createPutBundle(StructureDefinition profile, BundleType type)
	{
		Bundle b = new Bundle();
		b.setType(type);
		b.addEntry()
				.setFullUrl(profile.getIdElement().withServerBase(getBaseUrl(), "StructureDefinition").toVersionless()
						.toString())
				.setResource(profile).getRequest().setMethod(HTTPVerb.PUT)
				.setUrl("StructureDefinition/" + profile.getIdElement().getIdPart());
		return b;
	}

	private void testReturnBundle(Bundle bundle, BundleType type)
	{
		assertNotNull(bundle);
		assertEquals(type, bundle.getType());
		assertNotNull(bundle.getEntry());
		assertEquals(1, bundle.getEntry().size());
		assertNotNull(bundle.getEntryFirstRep().getResource());
		assertEquals(StructureDefinition.class, bundle.getEntryFirstRep().getResource().getClass());
	}

	private void testUpdateWithoutSnapshot(Function<StructureDefinition, StructureDefinition> updateOp) throws Exception
	{
		StructureDefinition profile = readProfile(PROFILE_FOLDER.resolve("dsf-task-test.xml"));

		StructureDefinition created = getWebserviceClient().create(profile);
		assertNotNull(created);

		EventManager eventManager = getSpringWebApplicationContext().getBean(EventManager.class);
		List<Event> events = new ArrayList<>();
		eventManager.addHandler(events::add);

		StructureDefinition updated = updateOp.apply(created);
		assertNotNull(updated);
		assertTrue(updated.hasIdElement());
		assertEquals("2", updated.getIdElement().getVersionIdPart());
		assertFalse(updated.hasSnapshot());

		assertEquals(1, events.size());
		Event event0 = events.get(0);
		assertEquals(StructureDefinition.class, event0.getResourceType());
		assertEquals(ResourceUpdatedEvent.class, event0.getClass());

		StructureDefinition fromEvent = (StructureDefinition) event0.getResource();
		assertTrue(fromEvent.hasSnapshot());
		assertTrue(fromEvent.hasIdElement());
		assertEquals(updated.getIdElement().getIdPart(), fromEvent.getIdElement().getIdPart());
		assertEquals(updated.getIdElement().getVersionIdPart(), fromEvent.getIdElement().getVersionIdPart());

		testDbResources(UUID.fromString(created.getIdElement().getIdPart()),
				created.getIdElement().getVersionIdPartAsLong());
		testDbResources(UUID.fromString(updated.getIdElement().getIdPart()),
				updated.getIdElement().getVersionIdPartAsLong());
	}


	private void testUpdateWithSnapshot(Function<StructureDefinition, StructureDefinition> updateOp) throws Exception
	{
		StructureDefinition profile = readProfile(PROFILE_FOLDER.resolve("dsf-task-test-snapshot.xml"));

		StructureDefinition created = getWebserviceClient().create(profile);
		assertNotNull(created);

		EventManager eventManager = getSpringWebApplicationContext().getBean(EventManager.class);
		List<Event> events = new ArrayList<>();
		eventManager.addHandler(events::add);

		StructureDefinition updated = updateOp.apply(created);
		assertNotNull(updated);
		assertTrue(updated.hasIdElement());
		assertEquals("2", updated.getIdElement().getVersionIdPart());
		assertTrue(updated.hasSnapshot());

		assertEquals(1, events.size());

		Event event0 = events.get(0);
		assertEquals(ResourceUpdatedEvent.class, event0.getClass());
		assertEquals(StructureDefinition.class, event0.getResourceType());

		StructureDefinition fromEvent = (StructureDefinition) event0.getResource();
		assertTrue(fromEvent.hasSnapshot());
		assertTrue(fromEvent.hasIdElement());
		assertEquals(updated.getIdElement().getIdPart(), fromEvent.getIdElement().getIdPart());
		assertEquals(updated.getIdElement().getVersionIdPart(), fromEvent.getIdElement().getVersionIdPart());

		testDbResources(UUID.fromString(created.getIdElement().getIdPart()),
				created.getIdElement().getVersionIdPartAsLong());
		testDbResources(UUID.fromString(updated.getIdElement().getIdPart()),
				updated.getIdElement().getVersionIdPartAsLong());
	}

	@Test
	public void testUpdataeWithoutSnapshot() throws Exception
	{
		testUpdateWithoutSnapshot(getWebserviceClient()::update);
	}

	@Test
	public void testUpdataeWithSnapshot() throws Exception
	{
		testUpdateWithSnapshot(getWebserviceClient()::update);
	}

	@Test
	public void testUpdateWithoutSnapshotViaTransactionBundle() throws Exception
	{
		testUpdateWithoutSnapshot(profile ->
		{
			Bundle b = createPutBundle(profile, BundleType.TRANSACTION);
			Bundle returnBundle = getWebserviceClient().postBundle(b);
			testReturnBundle(returnBundle, BundleType.TRANSACTIONRESPONSE);

			return (StructureDefinition) returnBundle.getEntryFirstRep().getResource();
		});
	}

	@Test
	public void testUpdateWithoutSnapshotViaBatchBundle() throws Exception
	{
		testUpdateWithoutSnapshot(profile ->
		{
			Bundle createBundle = createPutBundle(profile, BundleType.BATCH);
			Bundle returnBundle = getWebserviceClient().postBundle(createBundle);
			testReturnBundle(returnBundle, BundleType.BATCHRESPONSE);

			return (StructureDefinition) returnBundle.getEntryFirstRep().getResource();
		});
	}

	@Test
	public void testUpdateWithSnapshotViaTransactionBundle() throws Exception
	{
		testUpdateWithSnapshot(profile ->
		{
			Bundle b = createPutBundle(profile, BundleType.TRANSACTION);
			Bundle returnBundle = getWebserviceClient().postBundle(b);
			testReturnBundle(returnBundle, BundleType.TRANSACTIONRESPONSE);

			return (StructureDefinition) returnBundle.getEntryFirstRep().getResource();
		});
	}

	@Test
	public void testUpdateWithSnapshotViaBatchBundle() throws Exception
	{
		testUpdateWithSnapshot(profile ->
		{
			Bundle createBundle = createPutBundle(profile, BundleType.BATCH);
			Bundle returnBundle = getWebserviceClient().postBundle(createBundle);
			testReturnBundle(returnBundle, BundleType.BATCHRESPONSE);

			return (StructureDefinition) returnBundle.getEntryFirstRep().getResource();
		});
	}
}
