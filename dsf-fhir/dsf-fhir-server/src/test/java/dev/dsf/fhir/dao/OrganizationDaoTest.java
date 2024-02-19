package dev.dsf.fhir.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
import org.postgresql.util.PGobject;

import dev.dsf.fhir.authorization.read.ReadAccessHelper;
import dev.dsf.fhir.authorization.read.ReadAccessHelperImpl;
import dev.dsf.fhir.dao.jdbc.BinaryDaoJdbc;
import dev.dsf.fhir.dao.jdbc.CodeSystemDaoJdbc;
import dev.dsf.fhir.dao.jdbc.OrganizationDaoJdbc;

public class OrganizationDaoTest extends AbstractReadAccessDaoTest<Organization, OrganizationDao>
{
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

		Organization cretedOrg = dao.create(org);
		assertNotNull(cretedOrg);

		Binary binary = new Binary();
		binary.setContentType("text/plain");
		binary.setData("1234567890".getBytes());
		new ReadAccessHelperImpl().addOrganization(binary, "organization.com");

		BinaryDaoJdbc binaryDao = new BinaryDaoJdbc(defaultDataSource, permanentDeleteDataSource, fhirContext);
		Binary createdBinary = binaryDao.create(binary);
		assertNotNull(createdBinary);

		dao.update(cretedOrg);
	}
}
