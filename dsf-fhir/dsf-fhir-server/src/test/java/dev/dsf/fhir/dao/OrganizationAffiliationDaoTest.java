package dev.dsf.fhir.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.OrganizationAffiliation;
import org.junit.Test;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.fhir.authorization.read.ReadAccessHelper;
import dev.dsf.fhir.authorization.read.ReadAccessHelperImpl;
import dev.dsf.fhir.dao.jdbc.BinaryDaoJdbc;
import dev.dsf.fhir.dao.jdbc.OrganizationAffiliationDaoJdbc;
import dev.dsf.fhir.dao.jdbc.OrganizationDaoJdbc;

public class OrganizationAffiliationDaoTest
		extends AbstractReadAccessDaoTest<OrganizationAffiliation, OrganizationAffiliationDao>
{
	private static final Logger logger = LoggerFactory.getLogger(OrganizationAffiliationDaoTest.class);

	private static final String identifierSystem = "http://dsf.dev/sid/organization-identifier";
	private static final String identifierValue = "identifier.test";
	private static final boolean active = true;

	private final OrganizationDao organizationDao = new OrganizationDaoJdbc(defaultDataSource,
			permanentDeleteDataSource, fhirContext);

	public OrganizationAffiliationDaoTest()
	{
		super(OrganizationAffiliation.class, OrganizationAffiliationDaoJdbc::new);
	}

	@Override
	public OrganizationAffiliation createResource()
	{
		OrganizationAffiliation organizationAffiliation = new OrganizationAffiliation();
		organizationAffiliation.addIdentifier().setSystem(identifierSystem).setValue(identifierValue);
		return organizationAffiliation;
	}

	@Override
	protected void checkCreated(OrganizationAffiliation resource)
	{
		assertTrue(resource.hasIdentifier());
		assertEquals(identifierSystem, resource.getIdentifierFirstRep().getSystem());
		assertEquals(identifierValue, resource.getIdentifierFirstRep().getValue());
	}

	@Override
	protected OrganizationAffiliation updateResource(OrganizationAffiliation resource)
	{
		resource.setActive(active);
		return resource;
	}

	@Override
	protected void checkUpdates(OrganizationAffiliation resource)
	{
		assertEquals(active, resource.getActive());
	}

	@Test
	public void testReadActiveNotDeletedByMemberOrganizationIdentifier() throws Exception
	{
		final String parentIdentifier = "parent.org";

		try (Connection connection = getDao().newReadWriteTransaction())
		{
			Organization memberOrg = createAndStoreOrganizationInDb(identifierValue, connection);
			Organization parentOrg = createAndStoreOrganizationInDb(parentIdentifier, connection);

			OrganizationAffiliation affiliation = createAndStoreOrganizationAffiliationInDb(parentOrg, memberOrg,
					connection);

			List<OrganizationAffiliation> affiliations = getDao()
					.readActiveNotDeletedByMemberOrganizationIdentifierIncludingOrganizationIdentifiersWithTransaction(
							connection, identifierValue);
			assertNotNull(affiliations);
			assertEquals(1, affiliations.size());
			assertEquals(affiliation.getIdElement().getIdPart(), affiliations.get(0).getIdElement().getIdPart());
			assertTrue(affiliations.get(0).hasParticipatingOrganization());
			assertTrue(affiliations.get(0).getParticipatingOrganization().hasReference());
			assertEquals("Organization/" + memberOrg.getIdElement().getIdPart(),
					affiliations.get(0).getParticipatingOrganization().getReference());
			assertTrue(affiliations.get(0).getParticipatingOrganization().hasIdentifier());
			assertTrue(affiliations.get(0).getParticipatingOrganization().getIdentifier().hasSystem());
			assertEquals(identifierSystem,
					affiliations.get(0).getParticipatingOrganization().getIdentifier().getSystem());
			assertTrue(affiliations.get(0).getParticipatingOrganization().getIdentifier().hasValue());
			assertEquals(identifierValue,
					affiliations.get(0).getParticipatingOrganization().getIdentifier().getValue());
			assertTrue(affiliations.get(0).hasOrganization());
			assertTrue(affiliations.get(0).getOrganization().hasReference());
			assertEquals("Organization/" + parentOrg.getIdElement().getIdPart(),
					affiliations.get(0).getOrganization().getReference());
			assertTrue(affiliations.get(0).getOrganization().hasIdentifier());
			assertTrue(affiliations.get(0).getOrganization().getIdentifier().hasSystem());
			assertEquals(identifierSystem, affiliations.get(0).getOrganization().getIdentifier().getSystem());
			assertTrue(affiliations.get(0).getOrganization().getIdentifier().hasValue());
			assertEquals(parentIdentifier, affiliations.get(0).getOrganization().getIdentifier().getValue());
		}
	}

	@Test
	public void testSizeOfReadActiveNotDeletedByMemberOrganizationIdentifier() throws Exception
	{
		final String parentFooIdentifier = "parentFoo.org";
		final String parentBarIdentifier = "parentBar.org";

		try (Connection connection = getDao().newReadWriteTransaction())
		{
			Organization memberOrg = createAndStoreOrganizationInDb(identifierValue, connection);
			Organization parentFooOrg = createAndStoreOrganizationInDb(parentFooIdentifier, connection);
			Organization parentBarOrg = createAndStoreOrganizationInDb(parentBarIdentifier, connection);

			createAndStoreOrganizationAffiliationInDb(parentFooOrg, memberOrg, connection);
			createAndStoreOrganizationAffiliationInDb(parentBarOrg, memberOrg, connection);

			List<OrganizationAffiliation> affiliations = getDao()
					.readActiveNotDeletedByMemberOrganizationIdentifierIncludingOrganizationIdentifiersWithTransaction(
							connection, identifierValue);
			assertNotNull(affiliations);

			assertEquals(2, affiliations.size());
			assertEquals(identifierValue,
					affiliations.get(0).getParticipatingOrganization().getIdentifier().getValue());
			assertEquals(identifierValue,
					affiliations.get(1).getParticipatingOrganization().getIdentifier().getValue());
			assertNotEquals(affiliations.get(0).getOrganization().getIdentifier().getValue(),
					affiliations.get(1).getOrganization().getIdentifier().getValue());
			assertTrue(List.of(parentFooIdentifier, parentBarIdentifier)
					.contains(affiliations.get(0).getOrganization().getIdentifier().getValue()));
			assertTrue(List.of(parentFooIdentifier, parentBarIdentifier)
					.contains(affiliations.get(1).getOrganization().getIdentifier().getValue()));
		}
	}

	private Organization createAndStoreOrganizationInDb(String identifierValue, Connection connection)
			throws SQLException
	{
		Organization memberOrg = new Organization();
		memberOrg.setActive(true);
		memberOrg.addIdentifier().setSystem(identifierSystem).setValue(identifierValue);

		return organizationDao.createWithTransactionAndId(connection, memberOrg, UUID.randomUUID());
	}

	private OrganizationAffiliation createAndStoreOrganizationAffiliationInDb(Organization parent, Organization member,
			Connection connection) throws SQLException
	{
		OrganizationAffiliation organizationAffiliation = new OrganizationAffiliation();
		organizationAffiliation.setActive(true);
		organizationAffiliation.getParticipatingOrganization()
				.setReference("Organization/" + member.getIdElement().getIdPart());
		organizationAffiliation.getOrganization().setReference("Organization/" + parent.getIdElement().getIdPart());

		return getDao().createWithTransactionAndId(connection, organizationAffiliation, UUID.randomUUID());
	}

	@Test
	public void testUpdateWithExistingBinary() throws Exception
	{
		BinaryDaoJdbc binaryDao = new BinaryDaoJdbc(defaultDataSource, permanentDeleteDataSource, fhirContext);
		OrganizationDaoJdbc orgDao = new OrganizationDaoJdbc(defaultDataSource, permanentDeleteDataSource, fhirContext);

		Organization memberOrg = new Organization();
		memberOrg.setActive(true);
		memberOrg.getIdentifierFirstRep().setSystem(ReadAccessHelper.ORGANIZATION_IDENTIFIER_SYSTEM)
				.setValue("member.com");

		Organization parentOrg = new Organization();
		parentOrg.setActive(true);
		parentOrg.getIdentifierFirstRep().setSystem(ReadAccessHelper.ORGANIZATION_IDENTIFIER_SYSTEM)
				.setValue("parent.com");

		Organization createdParentOrg = orgDao.create(parentOrg);
		Organization createdMemberOrg = orgDao.create(memberOrg);

		OrganizationAffiliation affiliation = new OrganizationAffiliation();
		affiliation.setActive(true);
		affiliation.getParticipatingOrganization()
				.setReference("Organization/" + createdMemberOrg.getIdElement().getIdPart());
		affiliation.getOrganization().setReference("Organization/" + createdParentOrg.getIdElement().getIdPart());
		affiliation.addCode().getCodingFirstRep().setSystem("role-system").setCode("role-code");

		OrganizationAffiliation createdAffiliation = dao.create(affiliation);

		Binary binary = new Binary();
		binary.setContentType("text/plain");
		binary.setData("1234567890".getBytes());
		new ReadAccessHelperImpl().addRole(binary, "parent.com", "role-system", "role-code");

		Binary createdBinary = binaryDao.create(binary);
		assertNotNull(createdBinary);

		dao.update(createdAffiliation);
	}

	@Test
	public void testUpdateWithExistingBinaryUpdateMemberOrg() throws Exception
	{
		BinaryDaoJdbc binaryDao = new BinaryDaoJdbc(defaultDataSource, permanentDeleteDataSource, fhirContext);
		OrganizationDaoJdbc orgDao = new OrganizationDaoJdbc(defaultDataSource, permanentDeleteDataSource, fhirContext);

		Organization memberOrg = new Organization();
		memberOrg.setActive(true);
		memberOrg.getIdentifierFirstRep().setSystem(ReadAccessHelper.ORGANIZATION_IDENTIFIER_SYSTEM)
				.setValue("member.com");

		Organization parentOrg = new Organization();
		parentOrg.setActive(true);
		parentOrg.getIdentifierFirstRep().setSystem(ReadAccessHelper.ORGANIZATION_IDENTIFIER_SYSTEM)
				.setValue("parent.com");

		Organization createdParentOrg = orgDao.create(parentOrg);
		Organization createdMemberOrg = orgDao.create(memberOrg);

		OrganizationAffiliation affiliation = new OrganizationAffiliation();
		affiliation.setActive(true);
		affiliation.getParticipatingOrganization()
				.setReference("Organization/" + createdMemberOrg.getIdElement().getIdPart());
		affiliation.getOrganization().setReference("Organization/" + createdParentOrg.getIdElement().getIdPart());
		affiliation.addCode().getCodingFirstRep().setSystem("role-system").setCode("role-code");

		dao.create(affiliation);

		Binary binary = new Binary();
		binary.setContentType("text/plain");
		binary.setData("1234567890".getBytes());
		new ReadAccessHelperImpl().addRole(binary, "parent.com", "role-system", "role-code");

		Binary createdBinary = binaryDao.create(binary);
		assertNotNull(createdBinary);

		orgDao.update(createdMemberOrg);
	}

	@Test
	public void testUpdateWithExistingBinaryUpdateParentOrg() throws Exception
	{
		BinaryDaoJdbc binaryDao = new BinaryDaoJdbc(defaultDataSource, permanentDeleteDataSource, fhirContext);
		OrganizationDaoJdbc orgDao = new OrganizationDaoJdbc(defaultDataSource, permanentDeleteDataSource, fhirContext);

		Organization memberOrg = new Organization();
		memberOrg.setActive(true);
		memberOrg.getIdentifierFirstRep().setSystem(ReadAccessHelper.ORGANIZATION_IDENTIFIER_SYSTEM)
				.setValue("member.com");

		Organization parentOrg = new Organization();
		parentOrg.setActive(true);
		parentOrg.getIdentifierFirstRep().setSystem(ReadAccessHelper.ORGANIZATION_IDENTIFIER_SYSTEM)
				.setValue("parent.com");

		Organization createdParentOrg = orgDao.create(parentOrg);
		Organization createdMemberOrg = orgDao.create(memberOrg);

		OrganizationAffiliation affiliation = new OrganizationAffiliation();
		affiliation.setActive(true);
		affiliation.getParticipatingOrganization()
				.setReference("Organization/" + createdMemberOrg.getIdElement().getIdPart());
		affiliation.getOrganization().setReference("Organization/" + createdParentOrg.getIdElement().getIdPart());
		affiliation.addCode().getCodingFirstRep().setSystem("role-system").setCode("role-code");

		dao.create(affiliation);

		Binary binary = new Binary();
		binary.setContentType("text/plain");
		binary.setData("1234567890".getBytes());
		new ReadAccessHelperImpl().addRole(binary, "parent.com", "role-system", "role-code");

		Binary createdBinary = binaryDao.create(binary);
		assertNotNull(createdBinary);

		orgDao.update(createdParentOrg);
	}

	@Test
	public void testExistsNotDeletedByParentOrganizationMemberOrganizationRoleAndNotEndpointWithTransaction()
			throws Exception
	{
		final UUID parentOrganization = UUID.randomUUID();
		final UUID memberOrganization = UUID.randomUUID();
		final UUID endpoint = UUID.randomUUID();
		final String roleSystem = "system";
		final String roleCode = "code";

		OrganizationAffiliation a = new OrganizationAffiliation();
		a.setActive(true);
		a.getOrganization().setReference("Organization/" + parentOrganization.toString()).setType("Organization");
		a.getParticipatingOrganization().setReference("Organization/" + memberOrganization.toString())
				.setType("Organization");
		a.addEndpoint().setReference("Endpoint/" + endpoint.toString()).setType("Endpoint");
		a.getCodeFirstRep().getCodingFirstRep().setSystem(roleSystem).setCode(roleCode);

		OrganizationAffiliation created = dao.create(a);
		assertNotNull(created);

		try (Connection connection = dao.newReadWriteTransaction())
		{
			boolean exists1 = dao
					.existsNotDeletedByParentOrganizationMemberOrganizationRoleAndNotEndpointWithTransaction(connection,
							parentOrganization, memberOrganization, roleSystem, roleCode, endpoint);
			assertFalse(exists1);

			boolean exists2 = dao
					.existsNotDeletedByParentOrganizationMemberOrganizationRoleAndNotEndpointWithTransaction(connection,
							parentOrganization, memberOrganization, roleSystem, roleCode, UUID.randomUUID());
			assertTrue(exists2);
		}
	}

	private class TaskAsCsvGeneratorReader extends Reader
	{
		public static final int TASK_ROW_LINE_LENGTH = 1615;

		private int currentTask;
		private int maxTasks;

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
		OrganizationDaoJdbc orgDao = new OrganizationDaoJdbc(defaultDataSource, permanentDeleteDataSource, fhirContext);

		Organization memberOrg = new Organization();
		memberOrg.setActive(true);
		memberOrg.getIdentifierFirstRep().setSystem(ReadAccessHelper.ORGANIZATION_IDENTIFIER_SYSTEM)
				.setValue("organization.com");

		Organization createdMemberOrg = orgDao.create(memberOrg);
		assertNotNull(createdMemberOrg);

		Organization parentOrg = new Organization();
		parentOrg.setActive(true);
		parentOrg.getIdentifierFirstRep().setSystem(ReadAccessHelper.ORGANIZATION_IDENTIFIER_SYSTEM)
				.setValue("organization.com");

		Organization createdParentOrg = orgDao.create(parentOrg);
		assertNotNull(createdParentOrg);

		OrganizationAffiliation affiliation = new OrganizationAffiliation();
		affiliation.setActive(true);
		affiliation.getParticipatingOrganization()
				.setReference("Organization/" + createdMemberOrg.getIdElement().getIdPart());
		affiliation.getOrganization().setReference("Organization/" + createdParentOrg.getIdElement().getIdPart());
		affiliation.addCode().getCodingFirstRep().setSystem("role-system").setCode("role-code");

		OrganizationAffiliation createdAffiliation = dao.create(affiliation);
		assertNotNull(createdAffiliation);

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

		OrganizationAffiliation updatedAffiliation1 = dao.update(createdAffiliation);
		assertNotNull(updatedAffiliation1);

		OrganizationAffiliation updatedAffiliation2 = dao.update(updatedAffiliation1);
		assertNotNull(updatedAffiliation2);

		long t1 = System.currentTimeMillis();

		logger.info("OrganizationAffiliation updates executed in {} ms", t1 - t0);
		assertTrue("OrganizationAffiliation updates took longer then 200 ms", t1 - t0 <= 200);
	}
}
