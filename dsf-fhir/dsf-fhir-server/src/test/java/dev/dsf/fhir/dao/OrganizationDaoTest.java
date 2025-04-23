package dev.dsf.fhir.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.codec.binary.Hex;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.StringType;
import org.junit.Test;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.fhir.authorization.read.ReadAccessHelper;
import dev.dsf.fhir.authorization.read.ReadAccessHelperImpl;
import dev.dsf.fhir.dao.jdbc.BinaryDaoJdbc;
import dev.dsf.fhir.dao.jdbc.CodeSystemDaoJdbc;
import dev.dsf.fhir.dao.jdbc.OrganizationDaoJdbc;

public class OrganizationDaoTest extends AbstractReadAccessDaoTest<Organization, OrganizationDao>
{
	private static final Logger logger = LoggerFactory.getLogger(OrganizationDaoTest.class);

	private static final String name = "Demo Organization";
	private static final boolean active = true;

	public OrganizationDaoTest()
	{
		super(Organization.class, OrganizationDaoJdbc::new);
	}

	@Override
	public Organization createResource()
	{
		Organization organization = new Organization();
		organization.setName(name);
		return organization;
	}

	@Override
	protected void checkCreated(Organization resource)
	{
		assertEquals(name, resource.getName());
	}

	@Override
	protected Organization updateResource(Organization resource)
	{
		resource.setActive(active);
		return resource;
	}

	@Override
	protected void checkUpdates(Organization resource)
	{
		assertEquals(active, resource.getActive());
	}

	@Test
	public void testReadActiveNotDeletedByThumbprint() throws Exception
	{
		final String certHex = Hex.encodeHexString("FooBarBaz".getBytes(StandardCharsets.UTF_8));

		Organization org = new Organization();
		org.setActive(true);
		org.setName("Test");
		org.addExtension().setUrl("http://dsf.dev/fhir/StructureDefinition/extension-certificate-thumbprint")
				.setValue(new StringType(certHex));

		Organization created = dao.create(org);
		assertNotNull(created);

		Optional<Organization> read = dao.readActiveNotDeletedByThumbprint(certHex);
		assertNotNull(read);
		assertTrue(read.isPresent());
		assertNotNull(read.get()
				.getExtensionByUrl("http://dsf.dev/fhir/StructureDefinition/extension-certificate-thumbprint"));
		assertEquals(StringType.class,
				read.get().getExtensionByUrl("http://dsf.dev/fhir/StructureDefinition/extension-certificate-thumbprint")
						.getValue().getClass());
		assertEquals(certHex,
				((StringType) read.get()
						.getExtensionByUrl("http://dsf.dev/fhir/StructureDefinition/extension-certificate-thumbprint")
						.getValue()).asStringValue());
	}

	@Test
	public void testReadActiveNotDeletedByThumbprintNotActive() throws Exception
	{
		final String certHex = Hex.encodeHexString("FooBarBaz".getBytes(StandardCharsets.UTF_8));

		Organization org = new Organization();
		org.setActive(false);
		org.setName("Test");
		org.addExtension().setUrl("http://dsf.dev/fhir/StructureDefinition/extension-certificate-thumbprint")
				.setValue(new StringType(certHex));

		Organization created = dao.create(org);
		assertNotNull(created);

		Optional<Organization> read = dao.readActiveNotDeletedByThumbprint(certHex);
		assertNotNull(read);
		assertTrue(read.isEmpty());

		Optional<Organization> read2 = dao.read(UUID.fromString(created.getIdElement().getIdPart()));
		assertNotNull(read2);
		assertTrue(read2.isPresent());
	}

	@Test
	public void testReadActiveNotDeletedByThumbprintDeleted() throws Exception
	{
		final String certHex = Hex.encodeHexString("FooBarBaz".getBytes(StandardCharsets.UTF_8));

		Organization org = new Organization();
		org.setActive(false);
		org.setName("Test");
		org.addExtension().setUrl("http://dsf.dev/fhir/StructureDefinition/extension-certificate-thumbprint")
				.setValue(new StringType(certHex));

		Organization created = dao.create(org);
		assertNotNull(created);
		dao.delete(UUID.fromString(created.getIdElement().getIdPart()));

		Optional<Organization> read = dao.readActiveNotDeletedByThumbprint(certHex);
		assertNotNull(read);
		assertTrue(read.isEmpty());
	}

	@Test
	public void testReadActiveNotDeletedByThumbprintNotExisting() throws Exception
	{
		final String certHex = Hex.encodeHexString("FooBarBaz".getBytes(StandardCharsets.UTF_8));

		Optional<Organization> read = dao.readActiveNotDeletedByThumbprint(certHex);
		assertNotNull(read);
		assertTrue(read.isEmpty());
	}

	@Test
	public void testReadActiveNotDeletedByThumbprintNull() throws Exception
	{
		Optional<Organization> read = dao.readActiveNotDeletedByThumbprint(null);
		assertNotNull(read);
		assertTrue(read.isEmpty());
	}

	@Test
	public void testReadActiveNotDeletedByThumbprintBlank() throws Exception
	{
		Optional<Organization> read = dao.readActiveNotDeletedByThumbprint("  ");
		assertNotNull(read);
		assertTrue(read.isEmpty());
	}

	@Test
	public void testReadActiveNotDeletedByIdentifier() throws Exception
	{
		final String identifierValue = "foo";

		Organization createResource = createResource();
		createResource.getIdentifierFirstRep().setSystem("http://dsf.dev/sid/organization-identifier")
				.setValue(identifierValue);
		dao.create(createResource);

		dao.readActiveNotDeletedByIdentifier(identifierValue);
	}

	@Test
	public void testOrganizationInsertTrigger() throws Exception
	{
		CodeSystem c = new CodeSystem();
		new ReadAccessHelperImpl().addOrganization(c, "organization.com");
		CodeSystem createdC = new CodeSystemDaoJdbc(defaultDataSource, permanentDeleteDataSource, fhirContext)
				.create(c);

		try (Connection connection = defaultDataSource.getConnection();
				PreparedStatement statement = connection
						.prepareStatement("SELECT count(*) FROM read_access WHERE resource_id = ? AND access_type = ?"))
		{
			PGobject resourceId = new PGobject();
			resourceId.setType("UUID");
			resourceId.setValue(createdC.getIdElement().getIdPart());
			statement.setObject(1, resourceId);
			statement.setString(2, ReadAccessHelper.READ_ACCESS_TAG_VALUE_ORGANIZATION);

			try (ResultSet result = statement.executeQuery())
			{
				assertTrue(result.next());
				assertEquals(0, result.getInt(1));
			}
		}

		Organization o = createResource();
		o.setActive(true);
		o.getIdentifierFirstRep().setSystem(ReadAccessHelper.ORGANIZATION_IDENTIFIER_SYSTEM)
				.setValue("organization.com");

		Organization createdO = dao.create(o);

		try (Connection connection = defaultDataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(
						"SELECT count(*) FROM read_access WHERE resource_id = ? AND access_type = ? AND organization_id = ?"))
		{
			PGobject resourceId = new PGobject();
			resourceId.setType("UUID");
			resourceId.setValue(createdC.getIdElement().getIdPart());
			statement.setObject(1, resourceId);
			statement.setString(2, ReadAccessHelper.READ_ACCESS_TAG_VALUE_ORGANIZATION);
			PGobject organizationId = new PGobject();
			organizationId.setType("UUID");
			organizationId.setValue(createdO.getIdElement().getIdPart());
			statement.setObject(3, organizationId);

			try (ResultSet result = statement.executeQuery())
			{
				assertTrue(result.next());
				assertEquals(1, result.getInt(1));
			}
		}
	}

	@Test
	public void testUpdateWithExistingBinary() throws Exception
	{
		Organization org = new Organization();
		org.setActive(true);
		org.getIdentifierFirstRep().setSystem(ReadAccessHelper.ORGANIZATION_IDENTIFIER_SYSTEM)
				.setValue("organization.com");

		Organization createdOrg = dao.create(org);
		assertNotNull(createdOrg);

		Binary binary = new Binary();
		binary.setContentType("text/plain");
		binary.setData("1234567890".getBytes());
		new ReadAccessHelperImpl().addOrganization(binary, "organization.com");

		BinaryDaoJdbc binaryDao = new BinaryDaoJdbc(defaultDataSource, permanentDeleteDataSource, fhirContext,
				DATABASE_USERS_GROUP);
		Binary createdBinary = binaryDao.create(binary);
		assertNotNull(createdBinary);

		dao.update(createdOrg);
	}

	private static class TaskAsCsvGeneratorReader extends Reader
	{
		public static final int TASK_ROW_LINE_LENGTH = 1615;

		private final int maxTasks;
		private int currentTask;

		public TaskAsCsvGeneratorReader(int maxTasks)
		{
			this.maxTasks = maxTasks;
		}

		@Override
		public int read(char[] cbuf, int off, int len) throws IOException
		{
			if (len != TASK_ROW_LINE_LENGTH)
				throw new IllegalArgumentException("Buffer length " + TASK_ROW_LINE_LENGTH + " expected, not " + len);
			if (off % TASK_ROW_LINE_LENGTH != 0)
				throw new IllegalArgumentException("Buffer offset mod " + TASK_ROW_LINE_LENGTH + " == 0 expected, not "
						+ (off & TASK_ROW_LINE_LENGTH) + " (off = " + off + ")");

			if (currentTask < maxTasks)
			{
				String line = generateLine();

				if (line.length() != TASK_ROW_LINE_LENGTH)
					throw new IllegalArgumentException("Line length " + TASK_ROW_LINE_LENGTH + " expected");

				System.arraycopy(line.toCharArray(), 0, cbuf, 0, TASK_ROW_LINE_LENGTH);
				currentTask++;

				return TASK_ROW_LINE_LENGTH;
			}
			else
				return -1;
		}

		@Override
		public void close() throws IOException
		{
		}

		private String generateLine()
		{
			String id = UUID.randomUUID().toString();
			return "${id},3,,\"{\"\"resourceType\"\":\"\"Task\"\",\"\"id\"\":\"\"${id}\"\",\"\"meta\"\":{\"\"versionId\"\":\"\"3\"\",\"\"lastUpdated\"\":\"\"2024-10-01T18:22:06.765+02:00\"\",\"\"profile\"\":[\"\"http://medizininformatik-initiative.de/fhir/StructureDefinition/feasibility-task-execute|1.0\"\"]},\"\"instantiatesCanonical\"\":\"\"http://medizininformatik-initiative.de/bpe/Process/feasibilityExecute|1.0\"\",\"\"status\"\":\"\"completed\"\",\"\"intent\"\":\"\"order\"\",\"\"authoredOn\"\":\"\"2024-10-01T18:22:07+02:00\"\",\"\"requester\"\":{\"\"type\"\":\"\"Organization\"\",\"\"identifier\"\":{\"\"system\"\":\"\"http://dsf.dev/sid/organization-identifier\"\",\"\"value\"\":\"\"organization.com\"\"}},\"\"restriction\"\":{\"\"recipient\"\":[{\"\"type\"\":\"\"Organization\"\",\"\"identifier\"\":{\"\"system\"\":\"\"http://dsf.dev/sid/organization-identifier\"\",\"\"value\"\":\"\"organization.com\"\"}}]},\"\"input\"\":[{\"\"type\"\":{\"\"coding\"\":[{\"\"system\"\":\"\"http://dsf.dev/fhir/CodeSystem/bpmn-message\"\",\"\"code\"\":\"\"message-name\"\"}]},\"\"valueString\"\":\"\"feasibilityExecuteMessage\"\"},{\"\"type\"\":{\"\"coding\"\":[{\"\"system\"\":\"\"http://dsf.dev/fhir/CodeSystem/bpmn-message\"\",\"\"code\"\":\"\"business-key\"\"}]},\"\"valueString\"\":\"\"${business-key}\"\"},{\"\"type\"\":{\"\"coding\"\":[{\"\"system\"\":\"\"http://dsf.dev/fhir/CodeSystem/bpmn-message\"\",\"\"code\"\":\"\"correlation-key\"\"}]},\"\"valueString\"\":\"\"${correlation-key}\"\"},{\"\"type\"\":{\"\"coding\"\":[{\"\"system\"\":\"\"http://medizininformatik-initiative.de/fhir/CodeSystem/feasibility\"\",\"\"code\"\":\"\"measure-reference\"\"}]},\"\"valueReference\"\":{\"\"reference\"\":\"\"https://dsf.fdpg.test.forschen-fuer-gesundheit.de/fhir/Measure/02bb7540-0d99-4c0e-8764-981e545cf646\"\"}}]}\"\n"
					.replace("${id}", id).replace("${business-key}", UUID.randomUUID().toString())
					.replace("${correlation-key}", UUID.randomUUID().toString());
		}
	}

	@Test
	public void testBigUpdate() throws Exception
	{
		Organization org = new Organization();
		org.setActive(true);
		org.getIdentifierFirstRep().setSystem(ReadAccessHelper.ORGANIZATION_IDENTIFIER_SYSTEM)
				.setValue("organization.com");

		Organization createdOrg = dao.create(org);
		assertNotNull(createdOrg);

		final int taskCount = 500_000;

		logger.info("Inserting {} Task resources ...", taskCount);
		try (Connection connection = defaultDataSource.getConnection())
		{
			CopyManager copyManager = new CopyManager(connection.unwrap(BaseConnection.class));
			TaskAsCsvGeneratorReader taskGenerator = new TaskAsCsvGeneratorReader(taskCount);
			long insertedRows = copyManager.copyIn("COPY tasks FROM STDIN (FORMAT csv)", taskGenerator,
					TaskAsCsvGeneratorReader.TASK_ROW_LINE_LENGTH);

			assertEquals(taskCount, insertedRows);
		}
		logger.info("Inserting {} Task resources [Done]", taskCount);

		long t0 = System.currentTimeMillis();

		Organization updatedOrg1 = dao.update(createdOrg);
		assertNotNull(updatedOrg1);

		Organization updatedOrg2 = dao.update(createdOrg);
		assertNotNull(updatedOrg2);

		long t1 = System.currentTimeMillis();

		logger.info("Organization updates executed in {} ms", t1 - t0);
		assertTrue("Organization updates took longer then 200 ms", t1 - t0 <= 200);
	}
}
