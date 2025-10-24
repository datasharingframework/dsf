package dev.dsf.fhir.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.hl7.fhir.r4.model.Resource;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.utility.DockerImageName;

import ca.uhn.fhir.context.FhirContext;
import de.hsheilbronn.mi.utils.test.PostgreSqlContainerLiquibaseTemplateClassRule;
import de.hsheilbronn.mi.utils.test.PostgresTemplateRule;
import dev.dsf.fhir.dao.exception.ResourceDeletedException;
import dev.dsf.fhir.dao.exception.ResourceNotFoundException;
import dev.dsf.fhir.dao.exception.ResourceNotMarkedDeletedException;
import dev.dsf.fhir.dao.exception.ResourceVersionNoMatchException;

public abstract class AbstractResourceDaoTest<D extends Resource, C extends ResourceDao<D>> extends AbstractDbTest
{
	private static final Logger logger = LoggerFactory.getLogger(AbstractResourceDaoTest.class);

	@FunctionalInterface
	public interface TriFunction<A, B, C, R>
	{
		R apply(A a, B b, C c);
	}

	protected static DataSource defaultDataSource;
	protected static DataSource permanentDeleteDataSource;

	@ClassRule
	public static final PostgreSqlContainerLiquibaseTemplateClassRule liquibaseRule = new PostgreSqlContainerLiquibaseTemplateClassRule(
			DockerImageName.parse("postgres:15"), ROOT_USER, "fhir", "fhir_template", CHANGE_LOG_FILE,
			CHANGE_LOG_PARAMETERS, true);

	@Rule
	public final PostgresTemplateRule templateRule = new PostgresTemplateRule(liquibaseRule);

	@BeforeClass
	public static void beforeClass() throws Exception
	{
		defaultDataSource = createDefaultDataSource(liquibaseRule.getHost(), liquibaseRule.getMappedPort(5432),
				liquibaseRule.getDatabaseName());
		defaultDataSource.unwrap(BasicDataSource.class).start();

		permanentDeleteDataSource = createPermanentDeleteDataSource(liquibaseRule.getHost(),
				liquibaseRule.getMappedPort(5432), liquibaseRule.getDatabaseName());
		permanentDeleteDataSource.unwrap(BasicDataSource.class).start();
	}

	@AfterClass
	public static void afterClass() throws Exception
	{
		if (defaultDataSource != null)
			defaultDataSource.unwrap(BasicDataSource.class).close();

		if (permanentDeleteDataSource != null)
			permanentDeleteDataSource.unwrap(BasicDataSource.class).close();
	}

	protected final Class<D> resouceClass;
	protected final TriFunction<DataSource, DataSource, FhirContext, C> daoCreator;

	protected final FhirContext fhirContext = FhirContext.forR4();
	protected C dao;

	protected AbstractResourceDaoTest(Class<D> resouceClass,
			TriFunction<DataSource, DataSource, FhirContext, C> daoCreator)
	{
		this.resouceClass = resouceClass;
		this.daoCreator = daoCreator;
	}

	protected boolean isSame(D d1, D d2)
	{
		return d1.equalsDeep(d2);
	}

	@Before
	public void before() throws Exception
	{
		dao = daoCreator.apply(defaultDataSource, permanentDeleteDataSource, fhirContext);
	}

	public C getDao()
	{
		return dao;
	}

	public Logger getLogger()
	{
		return logger;
	}

	@Test
	public void testEmpty() throws Exception
	{
		Optional<D> read = dao.read(UUID.randomUUID());
		assertTrue(read.isEmpty());
	}

	@Test
	public void testEmptyWithVersion() throws Exception
	{
		Optional<D> read = dao.readVersion(UUID.randomUUID(), 1L);
		assertTrue(read.isEmpty());
	}

	public abstract D createResource();

	@Test
	public void testCreate() throws Exception
	{
		D newResource = createResource();
		assertNull(newResource.getId());
		assertNull(newResource.getMeta().getVersionId());

		D createdResource = dao.create(newResource);
		assertNotNull(createdResource);
		assertNotNull(createdResource.getId());
		assertNotNull(createdResource.getMeta().getVersionId());
		assertEquals("1", createdResource.getIdElement().getVersionIdPart());
		assertEquals("1", createdResource.getMeta().getVersionId());

		newResource.setIdElement(createdResource.getIdElement().copy());
		newResource.setMeta(createdResource.getMeta().copy());

		assertTrue(isSame(newResource, createdResource));

		Optional<D> read = dao.read(UUID.fromString(createdResource.getIdElement().getIdPart()));
		assertTrue(read.isPresent());
		assertNotNull(read.get().getId());
		assertNotNull(read.get().getMeta().getVersionId());
		assertEquals("1", read.get().getIdElement().getVersionIdPart());
		assertEquals("1", read.get().getMeta().getVersionId());
	}

	protected abstract void checkCreated(D resource);

	protected abstract D updateResource(D resource);

	@Test
	public void testUpdate() throws Exception
	{
		D newResource = createResource();
		assertNull(newResource.getId());
		assertNull(newResource.getMeta().getVersionId());

		D createdResource = dao.create(newResource);
		assertNotNull(createdResource);
		assertNotNull(createdResource.getIdElement());
		assertNotNull(createdResource.getIdElement().getIdPart());
		assertNotNull(createdResource.getIdElement().getVersionIdPart());
		assertEquals(ResourceDao.FIRST_VERSION_STRING, createdResource.getIdElement().getVersionIdPart());
		assertNotNull(createdResource.getMeta());
		assertNotNull(createdResource.getMeta().getVersionId());
		assertEquals(ResourceDao.FIRST_VERSION_STRING, createdResource.getMeta().getVersionId());

		newResource.setIdElement(createdResource.getIdElement().copy());
		newResource.setMeta(createdResource.getMeta().copy());
		assertTrue(isSame(newResource, createdResource));

		D updatedResource = dao.update(updateResource(createdResource), (long) ResourceDao.FIRST_VERSION);
		assertNotNull(updatedResource);
		assertNotNull(updatedResource.getIdElement());
		assertNotNull(updatedResource.getIdElement().getIdPart());
		assertNotNull(updatedResource.getIdElement().getVersionIdPart());
		assertEquals(String.valueOf(ResourceDao.FIRST_VERSION + 1), updatedResource.getIdElement().getVersionIdPart());
		assertNotNull(updatedResource.getMeta());
		assertNotNull(updatedResource.getMeta().getVersionId());
		assertEquals(String.valueOf(ResourceDao.FIRST_VERSION + 1), updatedResource.getMeta().getVersionId());
		assertTrue(updatedResource.getMeta().getLastUpdated().after(createdResource.getMeta().getLastUpdated()));

		checkUpdates(updatedResource);
	}

	@Test(expected = ResourceNotFoundException.class)
	public void testUpdateNonExisting() throws Exception
	{
		D newResource = createResource();
		assertNull(newResource.getId());
		assertNull(newResource.getMeta().getVersionId());

		dao.update(newResource, null);
	}

	@Test(expected = ResourceVersionNoMatchException.class)
	public void testUpdateNotLatest() throws Exception
	{
		D newResource = createResource();
		assertNull(newResource.getId());
		assertNull(newResource.getMeta().getVersionId());

		D createdResource = dao.create(newResource);
		assertNotNull(createdResource);
		assertNotNull(createdResource.getId());
		assertNotNull(createdResource.getMeta().getVersionId());

		newResource.setIdElement(createdResource.getIdElement().copy());
		newResource.setMeta(createdResource.getMeta().copy());

		assertTrue(isSame(newResource, createdResource));

		dao.update(updateResource(createdResource), 0L);
	}

	@Test
	public void testUpdateLatest() throws Exception
	{
		D newResource = createResource();
		assertNull(newResource.getId());
		assertNull(newResource.getMeta().getVersionId());

		D createdResource = dao.create(newResource);
		assertNotNull(createdResource);
		assertNotNull(createdResource.getId());
		assertNotNull(createdResource.getMeta().getVersionId());

		newResource.setIdElement(createdResource.getIdElement().copy());
		newResource.setMeta(createdResource.getMeta().copy());

		assertTrue(isSame(newResource, createdResource));

		D updatedResource = dao.update(updateResource(createdResource), 1L);
		assertNotNull(updatedResource);
	}

	@Test
	public void testUpdateDeleted() throws Exception
	{
		D newResource = createResource();
		assertNull(newResource.getId());
		assertNull(newResource.getMeta().getVersionId());

		D createdResource = dao.create(newResource);
		assertNotNull(createdResource);
		assertNotNull(createdResource.getIdElement());
		assertNotNull(createdResource.getIdElement().getIdPart());
		assertNotNull(createdResource.getIdElement().getVersionIdPart());
		assertEquals(ResourceDao.FIRST_VERSION_STRING, createdResource.getIdElement().getVersionIdPart());
		assertNotNull(createdResource.getMeta());
		assertNotNull(createdResource.getMeta().getVersionId());
		assertEquals(ResourceDao.FIRST_VERSION_STRING, createdResource.getMeta().getVersionId());

		newResource.setIdElement(createdResource.getIdElement().copy());
		newResource.setMeta(createdResource.getMeta().copy());
		assertTrue(isSame(newResource, createdResource));

		boolean deleted = dao.delete(UUID.fromString(createdResource.getIdElement().getIdPart()));
		assertTrue(deleted);

		D updatedResource = dao.update(updateResource(createdResource), ResourceDao.FIRST_VERSION + 1L);
		assertNotNull(updatedResource);
		assertNotNull(updatedResource.getIdElement());
		assertNotNull(updatedResource.getIdElement().getIdPart());
		assertNotNull(updatedResource.getIdElement().getVersionIdPart());
		assertEquals(String.valueOf(ResourceDao.FIRST_VERSION + 2), updatedResource.getIdElement().getVersionIdPart());
		assertNotNull(updatedResource.getMeta());
		assertNotNull(updatedResource.getMeta().getVersionId());
		assertEquals(String.valueOf(ResourceDao.FIRST_VERSION + 2), updatedResource.getMeta().getVersionId());

		checkUpdates(updatedResource);
	}

	protected abstract void checkUpdates(D resource);

	@Test(expected = ResourceDeletedException.class)
	public void testDelete() throws Exception
	{
		D newResource = createResource();
		assertNull(newResource.getId());
		assertNull(newResource.getMeta().getVersionId());

		D createdResource = dao.create(newResource);
		assertNotNull(createdResource);
		assertNotNull(createdResource.getId());
		assertNotNull(createdResource.getMeta().getVersionId());

		newResource.setIdElement(createdResource.getIdElement().copy());
		newResource.setMeta(createdResource.getMeta().copy());

		assertTrue(isSame(newResource, createdResource));

		Optional<D> read = dao.read(UUID.fromString(createdResource.getIdElement().getIdPart()));
		assertTrue(read.isPresent());

		dao.delete(UUID.fromString(createdResource.getIdElement().getIdPart()));

		dao.read(UUID.fromString(createdResource.getIdElement().getIdPart()));
	}

	@Test(expected = ResourceNotMarkedDeletedException.class)
	public void testDeletePermanentlyNotMarkedAsDeleted() throws Exception
	{
		D newResource = createResource();
		assertNull(newResource.getId());
		assertNull(newResource.getMeta().getVersionId());

		D createdResource = dao.create(newResource);
		assertNotNull(createdResource);
		assertNotNull(createdResource.getId());
		assertNotNull(createdResource.getMeta().getVersionId());

		dao.deletePermanently(UUID.fromString(createdResource.getIdElement().getIdPart()));
	}

	@Test
	public void testDeletePermanently() throws Exception
	{
		D newResource = createResource();
		assertNull(newResource.getId());
		assertNull(newResource.getMeta().getVersionId());

		D createdResource = dao.create(newResource);
		assertNotNull(createdResource);
		assertNotNull(createdResource.getId());
		assertNotNull(createdResource.getMeta().getVersionId());

		dao.delete(UUID.fromString(createdResource.getIdElement().getIdPart()));

		dao.deletePermanently(UUID.fromString(createdResource.getIdElement().getIdPart()));

		assertFalse(dao.read(UUID.fromString(createdResource.getIdElement().getIdPart())).isPresent());
	}

	@Test(expected = ResourceNotFoundException.class)
	public void testDeletePermanentlyNotFound() throws Exception
	{
		dao.deletePermanently(UUID.randomUUID());
	}

	@Test
	public void testReadIncludingDeleted() throws Exception
	{
		D newResource = createResource();
		assertNull(newResource.getId());
		assertNull(newResource.getMeta().getVersionId());

		D createdResource = dao.create(newResource);
		assertNotNull(createdResource);
		assertNotNull(createdResource.getId());
		assertNotNull(createdResource.getMeta().getVersionId());

		newResource.setIdElement(createdResource.getIdElement().copy());
		newResource.setMeta(createdResource.getMeta().copy());

		assertTrue(isSame(newResource, createdResource));

		Optional<D> read = dao.read(UUID.fromString(createdResource.getIdElement().getIdPart()));
		assertTrue(read.isPresent());

		boolean d = dao.delete(UUID.fromString(createdResource.getIdElement().getIdPart()));
		assertTrue(d);

		Optional<D> deleted = dao.readIncludingDeleted(UUID.fromString(createdResource.getIdElement().getIdPart()));
		assertTrue(deleted.isPresent());
	}

	@Test
	public void testReadWithVersion() throws Exception
	{
		D newResource = createResource();
		assertNull(newResource.getId());
		assertNull(newResource.getMeta().getVersionId());

		D createdResource = dao.create(newResource);
		assertNotNull(createdResource);
		assertNotNull(createdResource.getId());
		assertNotNull(createdResource.getMeta().getVersionId());

		newResource.setIdElement(createdResource.getIdElement().copy());
		newResource.setMeta(createdResource.getMeta().copy());

		assertTrue(isSame(newResource, createdResource));

		Optional<D> read = dao.readVersion(UUID.fromString(createdResource.getIdElement().getIdPart()),
				createdResource.getIdElement().getVersionIdPartAsLong());
		assertTrue(read.isPresent());

		assertTrue(isSame(newResource, read.get()));
	}

	@Test
	public void testRead() throws Exception
	{
		D newResource = createResource();
		assertNull(newResource.getId());
		assertNull(newResource.getMeta().getVersionId());

		D createdResource = dao.create(newResource);
		assertNotNull(createdResource);
		assertNotNull(createdResource.getId());
		assertNotNull(createdResource.getMeta().getVersionId());

		newResource.setIdElement(createdResource.getIdElement().copy());
		newResource.setMeta(createdResource.getMeta().copy());

		assertTrue(isSame(newResource, createdResource));

		Optional<D> read = dao.read(UUID.fromString(createdResource.getIdElement().getIdPart()));
		assertTrue(read.isPresent());

		if (!isSame(newResource, read.get()))
		{
			String s1 = fhirContext.newXmlParser().setPrettyPrint(true).encodeResourceToString(newResource);
			String s2 = fhirContext.newXmlParser().setPrettyPrint(true).encodeResourceToString(read.get());
			fail(s1 + "\nvs\n" + s2);
		}
	}

	@Test
	public void testReadAll() throws Exception
	{
		D newResource = createResource();
		assertNull(newResource.getId());
		assertNull(newResource.getMeta().getVersionId());

		D createdResource = dao.create(newResource);
		assertNotNull(createdResource);
		assertNotNull(createdResource.getIdElement());
		assertNotNull(createdResource.getIdElement().getIdPart());
		assertNotNull(createdResource.getIdElement().getVersionIdPart());
		assertEquals(ResourceDao.FIRST_VERSION_STRING, createdResource.getIdElement().getVersionIdPart());
		assertNotNull(createdResource.getMeta());
		assertNotNull(createdResource.getMeta().getVersionId());
		assertEquals(ResourceDao.FIRST_VERSION_STRING, createdResource.getMeta().getVersionId());

		newResource.setIdElement(createdResource.getIdElement().copy());
		newResource.setMeta(createdResource.getMeta().copy());
		assertTrue(isSame(newResource, createdResource));

		D updatedResource = dao.update(updateResource(createdResource), (long) ResourceDao.FIRST_VERSION);
		assertNotNull(updatedResource);
		assertNotNull(updatedResource.getIdElement());
		assertNotNull(updatedResource.getIdElement().getIdPart());
		assertNotNull(updatedResource.getIdElement().getVersionIdPart());
		assertEquals(String.valueOf(ResourceDao.FIRST_VERSION + 1), updatedResource.getIdElement().getVersionIdPart());
		assertNotNull(updatedResource.getMeta());
		assertNotNull(updatedResource.getMeta().getVersionId());
		assertEquals(String.valueOf(ResourceDao.FIRST_VERSION + 1), updatedResource.getMeta().getVersionId());

		checkUpdates(updatedResource);

		List<D> all = dao.readAll();
		assertNotNull(all);
		assertEquals(1, all.size());
		assertNotNull(all.get(0));
		assertNotNull(all.get(0).getIdElement());
		assertNotNull(all.get(0).getIdElement().getIdPart());
		assertNotNull(all.get(0).getIdElement().getVersionIdPart());
		assertEquals(String.valueOf(ResourceDao.FIRST_VERSION + 1), all.get(0).getIdElement().getVersionIdPart());
		assertNotNull(all.get(0).getMeta());
		assertNotNull(all.get(0).getMeta().getVersionId());
		assertEquals(String.valueOf(ResourceDao.FIRST_VERSION + 1), all.get(0).getMeta().getVersionId());
	}

	@Test
	public void testReadLatest() throws Exception
	{
		D newResource = createResource();
		assertNull(newResource.getId());
		assertNull(newResource.getMeta().getVersionId());

		D createdResource = dao.create(newResource);
		assertNotNull(createdResource);
		assertNotNull(createdResource.getId());
		assertNotNull(createdResource.getMeta().getVersionId());
		assertEquals("1", createdResource.getIdElement().getVersionIdPart());

		D updatedResource = dao.update(createdResource, null);
		assertNotNull(updatedResource);
		assertNotNull(updatedResource.getId());
		assertNotNull(updatedResource.getMeta().getVersionId());
		assertEquals("2", updatedResource.getIdElement().getVersionIdPart());

		D updatedResource2 = dao.update(updatedResource, null);
		assertNotNull(updatedResource2);
		assertNotNull(updatedResource2.getId());
		assertNotNull(updatedResource2.getMeta().getVersionId());
		assertEquals("3", updatedResource2.getIdElement().getVersionIdPart());

		newResource.setIdElement(updatedResource2.getIdElement().copy());
		newResource.setMeta(updatedResource2.getMeta().copy());

		assertTrue(isSame(newResource, updatedResource2));

		Optional<D> read = dao.read(UUID.fromString(updatedResource2.getIdElement().getIdPart()));
		assertTrue(read.isPresent());

		if (!isSame(updatedResource2, read.get()))
		{
			String s1 = fhirContext.newXmlParser().setPrettyPrint(true).encodeResourceToString(updatedResource2);
			String s2 = fhirContext.newXmlParser().setPrettyPrint(true).encodeResourceToString(read.get());
			fail(s1 + "\nvs\n" + s2);
		}
		assertEquals("3", read.get().getIdElement().getVersionIdPart());
	}

	@Test
	public void testExistsNotDeletedNotExisting() throws Exception
	{
		boolean existsNotDeleted = dao.existsNotDeleted(UUID.randomUUID().toString(), "1");
		assertFalse(existsNotDeleted);
	}

	@Test
	public void testExistsNotDeletedExisting() throws Exception
	{
		D newResource = createResource();
		D createdResource = dao.create(newResource);

		boolean existsNotDeleted1 = dao.existsNotDeleted(createdResource.getIdElement().getIdPart(), null);
		assertTrue(existsNotDeleted1);

		boolean existsNotDeleted2 = dao.existsNotDeleted(createdResource.getIdElement().getIdPart(),
				createdResource.getIdElement().getVersionIdPart());
		assertTrue(existsNotDeleted2);
	}

	@Test
	public void testExistsNotDeletedDeleted() throws Exception
	{
		D newResource = createResource();
		D createdResource = dao.create(newResource);
		dao.delete(UUID.fromString(createdResource.getIdElement().getIdPart()));

		boolean existsNotDeleted1 = dao.existsNotDeleted(createdResource.getIdElement().getIdPart(), null);
		assertFalse(existsNotDeleted1);

		boolean existsNotDeleted2 = dao.existsNotDeleted(createdResource.getIdElement().getIdPart(),
				createdResource.getIdElement().getVersionIdPart());
		assertFalse(existsNotDeleted2);
	}
}
