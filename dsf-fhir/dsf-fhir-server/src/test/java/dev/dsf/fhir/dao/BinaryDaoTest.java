package dev.dsf.fhir.dao;

import static dev.dsf.fhir.authorization.read.ReadAccessHelper.ORGANIZATION_IDENTIFIER_SYSTEM;
import static dev.dsf.fhir.authorization.read.ReadAccessHelper.READ_ACCESS_TAG_VALUE_ALL;
import static dev.dsf.fhir.authorization.read.ReadAccessHelper.READ_ACCESS_TAG_VALUE_LOCAL;
import static dev.dsf.fhir.authorization.read.ReadAccessHelper.READ_ACCESS_TAG_VALUE_ORGANIZATION;
import static dev.dsf.fhir.authorization.read.ReadAccessHelper.READ_ACCESS_TAG_VALUE_ROLE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.io.output.CountingOutputStream;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.hl7.fhir.r4.model.Base64BinaryType;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.OrganizationAffiliation;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResearchStudy;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.common.db.logging.PreparedStatementWithLogger;
import dev.dsf.fhir.adapter.DeferredBase64BinaryType;
import dev.dsf.fhir.authorization.read.ReadAccessHelperImpl;
import dev.dsf.fhir.dao.jdbc.BinaryDaoJdbc;
import dev.dsf.fhir.dao.jdbc.OrganizationAffiliationDaoJdbc;
import dev.dsf.fhir.dao.jdbc.OrganizationDaoJdbc;
import dev.dsf.fhir.dao.jdbc.ResearchStudyDaoJdbc;
import dev.dsf.fhir.integration.random.RandomInputStream;
import dev.dsf.fhir.model.StreamableBase64BinaryType;
import dev.dsf.fhir.search.PageAndCount;
import dev.dsf.fhir.search.PartialResult;
import dev.dsf.fhir.search.SearchQuery;
import dev.dsf.fhir.webservice.RangeRequestImpl;
import jakarta.ws.rs.core.MediaType;

public class BinaryDaoTest extends AbstractReadAccessDaoTest<Binary, BinaryDao>
{
	private static final Logger logger = LoggerFactory.getLogger(BinaryDaoTest.class);

	private static final String CONTENT_TYPE = "text/plain";
	private static final byte[] DATA1 = "1234567890".getBytes();
	private static final byte[] DATA2 = "VBERi0xLjUNJeLjz9MNCjEwIDAgb2JqDTw8L0xpbmVhcml6ZWQgMS9MIDEzMDA2OC9PIDEyL0UgMTI1NzM1L04gMS9UIDEyOTc2NC9IIFsgNTQ2IDIwNF"
			.getBytes();

	private final OrganizationDao organizationDao = new OrganizationDaoJdbc(defaultDataSource,
			permanentDeleteDataSource, fhirContext);
	private final ResearchStudyDao researchStudyDao = new ResearchStudyDaoJdbc(defaultDataSource,
			permanentDeleteDataSource, fhirContext);
	private final OrganizationAffiliationDao organizationAffiliationDao = new OrganizationAffiliationDaoJdbc(
			defaultDataSource, permanentDeleteDataSource, fhirContext);

	public BinaryDaoTest()
	{
		super(Binary.class,
				(defaultDataSource, permanentDeleteDataSource, fhirContext) -> new BinaryDaoJdbc(defaultDataSource,
						permanentDeleteDataSource, fhirContext, DATABASE_USERS_GROUP));
	}

	@Override
	protected boolean isSame(Binary d1, Binary d2)
	{
		setDefaultDetaElement(d1);
		setDefaultDetaElement(d2);

		return super.isSame(d1, d2);
	}

	private void setDefaultDetaElement(Binary binary)
	{
		try
		{
			Base64BinaryType data = binary.getDataElement();
			if (data instanceof DeferredBase64BinaryType d)
			{
				ByteArrayOutputStream out = new ByteArrayOutputStream();

				try
				{
					d.writeExternal(out);
				}
				catch (IOException e)
				{
					throw new RuntimeException(e);
				}

				byte[] bytes = out.toByteArray();
				binary.setDataElement(new Base64BinaryType(bytes));
			}
			else if (data instanceof StreamableBase64BinaryType s)
			{
				byte[] bytes = s.getValueAsStream().readAllBytes();
				binary.setDataElement(new Base64BinaryType(bytes));
			}
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public Binary createResource()
	{
		Binary binary = new Binary();
		binary.setContentType(CONTENT_TYPE);
		binary.setData(DATA1);
		return binary;
	}

	@Override
	protected void checkCreated(Binary resource)
	{
		assertNotNull(resource.getContentType());
		assertEquals(CONTENT_TYPE, resource.getContentType());
		assertNotNull(resource.getData());
		assertTrue(Arrays.equals(DATA1, resource.getData()));
	}

	@Override
	protected Binary updateResource(Binary resource)
	{
		resource.setData(DATA2);
		return resource;
	}

	@Override
	protected void checkUpdates(Binary resource)
	{
		assertNotNull(resource.getData());
		assertTrue(Arrays.equals(DATA2, resource.getData()));
	}

	@Test
	public void testCreateCheckDataNullInJsonColumn() throws Exception
	{
		Binary newResource = createResource();
		assertNull(newResource.getId());
		assertNull(newResource.getMeta().getVersionId());

		Binary createdResource = dao.create(newResource);
		assertNotNull(createdResource);
		assertNotNull(createdResource.getId());
		assertNotNull(createdResource.getMeta().getVersionId());
		assertEquals("1", createdResource.getIdElement().getVersionIdPart());
		assertEquals("1", createdResource.getMeta().getVersionId());

		try (Connection connection = defaultDataSource.getConnection())
		{
			connection.setAutoCommit(false);
			try (PreparedStatement statement = connection
					.prepareStatement("SELECT binary_json, binary_oid FROM binaries");
					ResultSet result = statement.executeQuery())
			{
				connection.commit();

				assertTrue(result.next());

				String json = result.getString(1);
				Binary readResource = fhirContext.newJsonParser().parseResource(Binary.class, json);
				assertNotNull(readResource);
				assertNull(readResource.getData());

				Blob blob = result.getBlob(2);
				byte[] data = blob.getBinaryStream().readAllBytes();
				blob.free();

				assertNotNull(data);
				assertTrue(Arrays.equals(DATA1, data));

				assertFalse(result.next());
			}
		}
	}

	@Test
	public void testSearch() throws Exception
	{
		Organization org = new Organization();
		org.getMeta().addTag().setSystem("http://dsf.dev/fhir/CodeSystem/read-access-tag").setCode("ALL");
		org.setActive(true);
		org.addIdentifier().setSystem("http://dsf.dev/sid/organization-identifier").setValue("Test_Organization");

		Organization createdOrg = organizationDao.create(org);
		assertNotNull(createdOrg);

		Binary b = createResource();
		b.getSecurityContext().setReference("Organization/" + createdOrg.getIdElement().getIdPart());
		Binary createdB = dao.create(b);
		assertNotNull(createdB);

		SearchQuery<Binary> query = dao.createSearchQuery(TestOrganizationIdentity.local(org), PageAndCount.single());
		query.configureParameters(Map.of());
		assertNotNull(query);

		PartialResult<Binary> result = dao.search(query);
		assertNotNull(result);
	}

	@Test
	public void testSearchBinaryWithSecurityContext() throws Exception
	{
		Organization org = new Organization();
		org.getMeta().addTag().setSystem("http://dsf.dev/fhir/CodeSystem/read-access-tag").setCode("ALL");
		org.setActive(true);
		org.addIdentifier().setSystem("http://dsf.dev/sid/organization-identifier").setValue("Test_Organization");
		Organization createdOrg = organizationDao.create(org);
		assertNotNull(createdOrg);

		ResearchStudy rs = new ResearchStudy();
		rs.getMeta().addTag().setSystem("http://dsf.dev/fhir/CodeSystem/read-access-tag").setCode("LOCAL");
		ResearchStudy createdRs = researchStudyDao.create(rs);

		Binary b = createResource();
		b.setSecurityContext(new Reference(createdRs.getIdElement().toVersionless()));
		Binary createdB = dao.create(b);
		assertNotNull(createdB);

		SearchQuery<Binary> query = dao.createSearchQuery(TestOrganizationIdentity.local(org), PageAndCount.single());
		query.configureParameters(Map.of());
		assertNotNull(query);

		PartialResult<Binary> result = dao.search(query);
		assertNotNull(result);
		assertEquals(1, result.getTotal());
		assertEquals(1, result.getPartialResult().size());
		assertNotNull(result.getPartialResult().get(0));

		Binary foundBinary = result.getPartialResult().get(0);
		assertNotNull(foundBinary);
		assertEquals(createdB.getContentAsBase64(), foundBinary.getContentAsBase64());
	}

	@Test
	public void testSearchBinaryWithSecurityContextOrganization() throws Exception
	{
		Organization org = new Organization();
		org.getMeta().addTag().setSystem("http://dsf.dev/fhir/CodeSystem/read-access-tag").setCode("ALL");
		org.setActive(true);
		org.addIdentifier().setSystem("http://dsf.dev/sid/organization-identifier").setValue("Test_Organization");
		Organization createdOrg = organizationDao.create(org);
		assertNotNull(createdOrg);

		ResearchStudy rs = new ResearchStudy();
		rs.getMeta().addTag().setSystem("http://dsf.dev/fhir/CodeSystem/read-access-tag").setCode("LOCAL");
		rs.getMeta().addTag().setSystem("http://dsf.dev/fhir/CodeSystem/read-access-tag").setCode("ORGANIZATION")
				.addExtension().setUrl("http://dsf.dev/fhir/StructureDefinition/extension-read-access-organization")
				.setValue(new Identifier().setSystem("http://dsf.dev/sid/organization-identifier")
						.setValue("Test_Organization"));
		ResearchStudy createdRs = researchStudyDao.create(rs);

		Binary b = createResource();
		b.setSecurityContext(new Reference(createdRs.getIdElement().toVersionless()));
		Binary createdB = dao.create(b);
		assertNotNull(createdB);

		SearchQuery<Binary> query = dao.createSearchQuery(TestOrganizationIdentity.local(createdOrg),
				PageAndCount.single());
		query.configureParameters(Map.of());
		assertNotNull(query);

		PartialResult<Binary> result = dao.search(query);
		assertNotNull(result);
		assertEquals(1, result.getTotal());
		assertEquals(1, result.getPartialResult().size());
		assertNotNull(result.getPartialResult().get(0));

		Binary foundBinary = result.getPartialResult().get(0);
		assertNotNull(foundBinary);
		assertEquals(createdB.getContentAsBase64(), foundBinary.getContentAsBase64());
	}

	@Test
	public void testSearchBinaryWithSecurityContextRole() throws Exception
	{
		Organization parentOrg = new Organization();
		parentOrg.getMeta().addTag().setSystem("http://dsf.dev/fhir/CodeSystem/read-access-tag").setCode("ALL");
		parentOrg.setActive(true);
		parentOrg.addIdentifier().setSystem("http://dsf.dev/sid/organization-identifier").setValue("Test_Consortium");

		Organization memberOrg = new Organization();
		memberOrg.getMeta().addTag().setSystem("http://dsf.dev/fhir/CodeSystem/read-access-tag").setCode("ALL");
		memberOrg.setActive(true);
		memberOrg.addIdentifier().setSystem("http://dsf.dev/sid/organization-identifier").setValue("Test_Organization");

		Organization createdParentOrg = organizationDao.create(parentOrg);
		assertNotNull(createdParentOrg);
		Organization createdMemberOrg = organizationDao.create(memberOrg);
		assertNotNull(createdMemberOrg);

		OrganizationAffiliation affiliation = new OrganizationAffiliation();
		affiliation.getMeta().addTag().setSystem("http://dsf.dev/fhir/CodeSystem/read-access-tag").setCode("ALL");
		affiliation.setActive(true);
		affiliation.setOrganization(new Reference(createdParentOrg.getIdElement().toVersionless()));
		affiliation.setParticipatingOrganization(new Reference(createdMemberOrg.getIdElement().toVersionless()));
		affiliation.getCodeFirstRep().getCodingFirstRep().setSystem("http://dsf.dev/fhir/CodeSystem/organization-role")
				.setCode("DIC");

		organizationAffiliationDao.create(affiliation);

		ResearchStudy rs = new ResearchStudy();
		rs.getMeta().addTag().setSystem("http://dsf.dev/fhir/CodeSystem/read-access-tag").setCode("LOCAL");
		Extension ex = rs.getMeta().addTag().setSystem("http://dsf.dev/fhir/CodeSystem/read-access-tag").setCode("ROLE")
				.addExtension().setUrl("http://dsf.dev/fhir/StructureDefinition/extension-read-access-consortium-role");
		ex.addExtension().setUrl("consortium").setValue(
				new Identifier().setSystem("http://dsf.dev/sid/organization-identifier").setValue("Test_Consortium"));
		ex.addExtension().setUrl("organization-role")
				.setValue(new Coding().setSystem("http://dsf.dev/fhir/CodeSystem/organization-role").setCode("DIC"));
		ResearchStudy createdRs = researchStudyDao.create(rs);

		Binary b = createResource();
		b.setSecurityContext(new Reference(createdRs.getIdElement().toVersionless()));
		Binary createdB = dao.create(b);
		assertNotNull(createdB);

		SearchQuery<Binary> query = dao.createSearchQuery(TestOrganizationIdentity.local(createdMemberOrg),
				PageAndCount.single());
		query.configureParameters(Map.of());
		assertNotNull(query);

		PartialResult<Binary> result = dao.search(query);
		assertNotNull(result);
		assertEquals(1, result.getTotal());
		assertEquals(1, result.getPartialResult().size());
		assertNotNull(result.getPartialResult().get(0));

		Binary foundBinary = result.getPartialResult().get(0);
		assertNotNull(foundBinary);
		assertEquals(createdB.getContentAsBase64(), foundBinary.getContentAsBase64());
	}

	private void testReadAccessTriggerSecurityContext(String accessType, Consumer<ResearchStudy> readAccessModifier,
			Function<ResearchStudy, IdType> securityContext) throws Exception
	{
		ResearchStudy rS = new ResearchStudy();
		readAccessModifier.accept(rS);
		ResearchStudy createdRs = new ResearchStudyDaoJdbc(defaultDataSource, permanentDeleteDataSource, fhirContext)
				.create(rS);

		assertReadAccessEntryCount(1, 1, createdRs, accessType);

		Binary b = createResource();
		b.setSecurityContext(new Reference(securityContext.apply(createdRs)));
		Binary createdB = dao.create(b);

		assertReadAccessEntryCount(2, 1, createdRs, accessType);
		assertReadAccessEntryCount(2, 1, createdB, accessType);
	}

	@Test
	public void testReadAccessTriggerSecurityContextAll() throws Exception
	{
		testReadAccessTriggerSecurityContext(READ_ACCESS_TAG_VALUE_ALL, new ReadAccessHelperImpl()::addAll,
				rs -> rs.getIdElement().toUnqualifiedVersionless());
	}

	@Test
	public void testReadAccessTriggerSecurityContextLocal() throws Exception
	{
		testReadAccessTriggerSecurityContext(READ_ACCESS_TAG_VALUE_LOCAL, new ReadAccessHelperImpl()::addLocal,
				rs -> rs.getIdElement().toUnqualifiedVersionless());
	}

	@Test
	public void testReadAccessTriggerSecurityContextVersionSpecificAll() throws Exception
	{
		testReadAccessTriggerSecurityContext(READ_ACCESS_TAG_VALUE_ALL, new ReadAccessHelperImpl()::addAll,
				ResearchStudy::getIdElement);
	}

	@Test
	public void testReadAccessTriggerSecurityContextVersionSpecificLocal() throws Exception
	{
		testReadAccessTriggerSecurityContext(READ_ACCESS_TAG_VALUE_LOCAL, new ReadAccessHelperImpl()::addLocal,
				ResearchStudy::getIdElement);
	}

	private void testReadAccessTriggerSecurityContextOrganization(Function<ResearchStudy, IdType> securityContext)
			throws SQLException, Exception
	{
		Organization org = new Organization();
		org.setActive(true);
		org.addIdentifier().setSystem(ORGANIZATION_IDENTIFIER_SYSTEM).setValue("org.com");
		Organization createdOrg = new OrganizationDaoJdbc(defaultDataSource, permanentDeleteDataSource, fhirContext)
				.create(org);

		ResearchStudy rS = new ResearchStudy();
		new ReadAccessHelperImpl().addOrganization(rS, createdOrg);
		ResearchStudy createdRs = new ResearchStudyDaoJdbc(defaultDataSource, permanentDeleteDataSource, fhirContext)
				.create(rS);

		Binary b = createResource();
		b.setSecurityContext(new Reference(securityContext.apply(createdRs)));
		Binary createdB = dao.create(b);

		assertReadAccessEntryCount(4, 1, createdRs, READ_ACCESS_TAG_VALUE_LOCAL);
		assertReadAccessEntryCount(4, 1, createdB, READ_ACCESS_TAG_VALUE_LOCAL);
		assertReadAccessEntryCount(4, 1, createdRs, READ_ACCESS_TAG_VALUE_ORGANIZATION, createdOrg);
		assertReadAccessEntryCount(4, 1, createdB, READ_ACCESS_TAG_VALUE_ORGANIZATION, createdOrg);
	}

	@Test
	public void testReadAccessTriggerSecurityContextOrganization() throws Exception
	{
		testReadAccessTriggerSecurityContextOrganization(rs -> rs.getIdElement().toUnqualifiedVersionless());
	}

	@Test
	public void testReadAccessTriggerSecurityContextVersionSpecificOrganization() throws Exception
	{
		testReadAccessTriggerSecurityContextOrganization(ResearchStudy::getIdElement);
	}

	@Test
	public void testReadAccessTriggerSecurityContextOrganization2Organizations1Matching() throws Exception
	{
		OrganizationDaoJdbc organizationDao = new OrganizationDaoJdbc(defaultDataSource, permanentDeleteDataSource,
				fhirContext);

		Organization org1 = new Organization();
		org1.setActive(true);
		org1.addIdentifier().setSystem(ORGANIZATION_IDENTIFIER_SYSTEM).setValue("org1.com");
		Organization createdOrg1 = organizationDao.create(org1);

		Organization org2 = new Organization();
		org2.setActive(true);
		org2.addIdentifier().setSystem(ORGANIZATION_IDENTIFIER_SYSTEM).setValue("org2.com");
		Organization createdOrg2 = organizationDao.create(org2);

		ResearchStudy rS = new ResearchStudy();
		new ReadAccessHelperImpl().addOrganization(rS, createdOrg1);
		ResearchStudy createdRs = new ResearchStudyDaoJdbc(defaultDataSource, permanentDeleteDataSource, fhirContext)
				.create(rS);

		Binary b = createResource();
		b.setSecurityContext(new Reference(createdRs.getIdElement().toUnqualifiedVersionless()));
		Binary createdB = dao.create(b);

		assertReadAccessEntryCount(4, 1, createdRs, READ_ACCESS_TAG_VALUE_LOCAL);
		assertReadAccessEntryCount(4, 1, createdB, READ_ACCESS_TAG_VALUE_LOCAL);
		assertReadAccessEntryCount(4, 1, createdRs, READ_ACCESS_TAG_VALUE_ORGANIZATION, createdOrg1);
		assertReadAccessEntryCount(4, 1, createdB, READ_ACCESS_TAG_VALUE_ORGANIZATION, createdOrg1);
		assertReadAccessEntryCount(4, 0, createdRs, READ_ACCESS_TAG_VALUE_ORGANIZATION, createdOrg2);
		assertReadAccessEntryCount(4, 0, createdB, READ_ACCESS_TAG_VALUE_ORGANIZATION, createdOrg2);
	}

	@Test
	public void testReadAccessTriggerSecurityContextOrganization2Organizations2Matching() throws Exception
	{
		OrganizationDaoJdbc organizationDao = new OrganizationDaoJdbc(defaultDataSource, permanentDeleteDataSource,
				fhirContext);

		Organization org1 = new Organization();
		org1.setActive(true);
		org1.addIdentifier().setSystem(ORGANIZATION_IDENTIFIER_SYSTEM).setValue("org1.com");
		Organization createdOrg1 = organizationDao.create(org1);

		Organization org2 = new Organization();
		org2.setActive(true);
		org2.addIdentifier().setSystem(ORGANIZATION_IDENTIFIER_SYSTEM).setValue("org2.com");
		Organization createdOrg2 = organizationDao.create(org2);

		ResearchStudy rS = new ResearchStudy();
		new ReadAccessHelperImpl().addOrganization(rS, createdOrg1);
		new ReadAccessHelperImpl().addOrganization(rS, createdOrg2);
		ResearchStudy createdRs = new ResearchStudyDaoJdbc(defaultDataSource, permanentDeleteDataSource, fhirContext)
				.create(rS);

		Binary b = createResource();
		b.setSecurityContext(new Reference(createdRs.getIdElement().toUnqualifiedVersionless()));
		Binary createdB = dao.create(b);

		assertReadAccessEntryCount(6, 1, createdRs, READ_ACCESS_TAG_VALUE_LOCAL);
		assertReadAccessEntryCount(6, 1, createdB, READ_ACCESS_TAG_VALUE_LOCAL);
		assertReadAccessEntryCount(6, 1, createdRs, READ_ACCESS_TAG_VALUE_ORGANIZATION, createdOrg1);
		assertReadAccessEntryCount(6, 1, createdB, READ_ACCESS_TAG_VALUE_ORGANIZATION, createdOrg1);
		assertReadAccessEntryCount(6, 1, createdRs, READ_ACCESS_TAG_VALUE_ORGANIZATION, createdOrg2);
		assertReadAccessEntryCount(6, 1, createdB, READ_ACCESS_TAG_VALUE_ORGANIZATION, createdOrg2);
	}

	private void testReadAccessTriggerSecurityContextRole(Function<ResearchStudy, IdType> securityContext)
			throws Exception
	{
		Organization parentOrg = new Organization();
		parentOrg.setActive(true);
		parentOrg.addIdentifier().setSystem(ORGANIZATION_IDENTIFIER_SYSTEM).setValue("parent.com");

		Organization memberOrg = new Organization();
		memberOrg.setActive(true);
		memberOrg.addIdentifier().setSystem(ORGANIZATION_IDENTIFIER_SYSTEM).setValue("member.com");

		OrganizationDao orgDao = new OrganizationDaoJdbc(defaultDataSource, permanentDeleteDataSource, fhirContext);
		Organization createdParentOrg = orgDao.create(parentOrg);
		Organization createdMemberOrg = orgDao.create(memberOrg);

		OrganizationAffiliation aff = new OrganizationAffiliation();
		aff.setActive(true);
		aff.getCodeFirstRep().getCodingFirstRep().setSystem("http://dsf.dev/fhir/CodeSystem/organization-role")
				.setCode("DIC");
		aff.getOrganization().setReference("Organization/" + createdParentOrg.getIdElement().getIdPart());
		aff.getParticipatingOrganization().setReference("Organization/" + createdMemberOrg.getIdElement().getIdPart());

		OrganizationAffiliation createdAff = new OrganizationAffiliationDaoJdbc(defaultDataSource,
				permanentDeleteDataSource, fhirContext).create(aff);

		ResearchStudy rS = new ResearchStudy();
		new ReadAccessHelperImpl().addRole(rS, "parent.com", "http://dsf.dev/fhir/CodeSystem/organization-role", "DIC");
		ResearchStudy createdRs = new ResearchStudyDaoJdbc(defaultDataSource, permanentDeleteDataSource, fhirContext)
				.create(rS);

		Binary b = createResource();
		b.setSecurityContext(new Reference(securityContext.apply(createdRs)));
		Binary createdB = dao.create(b);

		assertReadAccessEntryCount(4, 1, createdRs, READ_ACCESS_TAG_VALUE_LOCAL);
		assertReadAccessEntryCount(4, 1, createdB, READ_ACCESS_TAG_VALUE_LOCAL);

		assertReadAccessEntryCount(4, 1, createdRs, READ_ACCESS_TAG_VALUE_ROLE, createdMemberOrg, createdAff);
		assertReadAccessEntryCount(4, 1, createdB, READ_ACCESS_TAG_VALUE_ROLE, createdMemberOrg, createdAff);
	}

	@Test
	public void testReadAccessTriggerSecurityContextRole() throws Exception
	{
		testReadAccessTriggerSecurityContextRole(r -> r.getIdElement().toUnqualifiedVersionless());
	}

	@Test
	public void testReadAccessTriggerSecurityContextVersionSpecificRole() throws Exception
	{
		testReadAccessTriggerSecurityContextRole(ResearchStudy::getIdElement);
	}

	@Test
	public void testReadAccessTriggerSecurityContextRole2Organizations1Matching() throws Exception
	{
		Organization parentOrg = new Organization();
		parentOrg.setActive(true);
		parentOrg.addIdentifier().setSystem(ORGANIZATION_IDENTIFIER_SYSTEM).setValue("parent.com");

		Organization memberOrg1 = new Organization();
		memberOrg1.setActive(true);
		memberOrg1.addIdentifier().setSystem(ORGANIZATION_IDENTIFIER_SYSTEM).setValue("member1.com");

		Organization memberOrg2 = new Organization();
		memberOrg2.setActive(true);
		memberOrg2.addIdentifier().setSystem(ORGANIZATION_IDENTIFIER_SYSTEM).setValue("member2.com");

		OrganizationDao orgDao = new OrganizationDaoJdbc(defaultDataSource, permanentDeleteDataSource, fhirContext);
		Organization createdParentOrg = orgDao.create(parentOrg);
		Organization createdMemberOrg1 = orgDao.create(memberOrg1);
		Organization createdMemberOrg2 = orgDao.create(memberOrg2);

		OrganizationAffiliation aff1 = new OrganizationAffiliation();
		aff1.setActive(true);
		aff1.getCodeFirstRep().getCodingFirstRep().setSystem("http://dsf.dev/fhir/CodeSystem/organization-role")
				.setCode("DIC");
		aff1.getOrganization().setReference("Organization/" + createdParentOrg.getIdElement().getIdPart());
		aff1.getParticipatingOrganization()
				.setReference("Organization/" + createdMemberOrg1.getIdElement().getIdPart());

		OrganizationAffiliation createdAff1 = new OrganizationAffiliationDaoJdbc(defaultDataSource,
				permanentDeleteDataSource, fhirContext).create(aff1);

		OrganizationAffiliation aff2 = new OrganizationAffiliation();
		aff2.setActive(true);
		aff2.getCodeFirstRep().getCodingFirstRep().setSystem("http://dsf.dev/fhir/CodeSystem/organization-role")
				.setCode("TTP");
		aff2.getOrganization().setReference("Organization/" + createdParentOrg.getIdElement().getIdPart());
		aff2.getParticipatingOrganization()
				.setReference("Organization/" + createdMemberOrg2.getIdElement().getIdPart());

		OrganizationAffiliation createdAff2 = new OrganizationAffiliationDaoJdbc(defaultDataSource,
				permanentDeleteDataSource, fhirContext).create(aff2);

		ResearchStudy rS = new ResearchStudy();
		new ReadAccessHelperImpl().addRole(rS, "parent.com", "http://dsf.dev/fhir/CodeSystem/organization-role", "DIC");
		ResearchStudy createdRs = new ResearchStudyDaoJdbc(defaultDataSource, permanentDeleteDataSource, fhirContext)
				.create(rS);

		Binary b = createResource();
		b.setSecurityContext(new Reference(createdRs.getIdElement().toUnqualifiedVersionless()));
		Binary createdB = dao.create(b);

		assertReadAccessEntryCount(4, 1, createdRs, READ_ACCESS_TAG_VALUE_LOCAL);
		assertReadAccessEntryCount(4, 1, createdB, READ_ACCESS_TAG_VALUE_LOCAL);

		assertReadAccessEntryCount(4, 1, createdRs, READ_ACCESS_TAG_VALUE_ROLE, createdMemberOrg1, createdAff1);
		assertReadAccessEntryCount(4, 1, createdB, READ_ACCESS_TAG_VALUE_ROLE, createdMemberOrg1, createdAff1);

		assertReadAccessEntryCount(4, 0, createdRs, READ_ACCESS_TAG_VALUE_ROLE, createdMemberOrg2, createdAff2);
		assertReadAccessEntryCount(4, 0, createdB, READ_ACCESS_TAG_VALUE_ROLE, createdMemberOrg2, createdAff2);
	}

	@Test
	public void testReadAccessTriggerSecurityContextRole2Organizations2Matching() throws Exception
	{
		Organization parentOrg = new Organization();
		parentOrg.setActive(true);
		parentOrg.addIdentifier().setSystem(ORGANIZATION_IDENTIFIER_SYSTEM).setValue("parent.com");

		Organization memberOrg1 = new Organization();
		memberOrg1.setActive(true);
		memberOrg1.addIdentifier().setSystem(ORGANIZATION_IDENTIFIER_SYSTEM).setValue("member1.com");

		Organization memberOrg2 = new Organization();
		memberOrg2.setActive(true);
		memberOrg2.addIdentifier().setSystem(ORGANIZATION_IDENTIFIER_SYSTEM).setValue("member2.com");

		OrganizationDao orgDao = new OrganizationDaoJdbc(defaultDataSource, permanentDeleteDataSource, fhirContext);
		Organization createdParentOrg = orgDao.create(parentOrg);
		Organization createdMemberOrg1 = orgDao.create(memberOrg1);
		Organization createdMemberOrg2 = orgDao.create(memberOrg2);

		OrganizationAffiliation aff1 = new OrganizationAffiliation();
		aff1.setActive(true);
		aff1.getCodeFirstRep().getCodingFirstRep().setSystem("http://dsf.dev/fhir/CodeSystem/organization-role")
				.setCode("DIC");
		aff1.getOrganization().setReference("Organization/" + createdParentOrg.getIdElement().getIdPart());
		aff1.getParticipatingOrganization()
				.setReference("Organization/" + createdMemberOrg1.getIdElement().getIdPart());

		OrganizationAffiliation createdAff1 = new OrganizationAffiliationDaoJdbc(defaultDataSource,
				permanentDeleteDataSource, fhirContext).create(aff1);

		OrganizationAffiliation aff2 = new OrganizationAffiliation();
		aff2.setActive(true);
		aff2.getCodeFirstRep().getCodingFirstRep().setSystem("http://dsf.dev/fhir/CodeSystem/organization-role")
				.setCode("DIC");
		aff2.getOrganization().setReference("Organization/" + createdParentOrg.getIdElement().getIdPart());
		aff2.getParticipatingOrganization()
				.setReference("Organization/" + createdMemberOrg2.getIdElement().getIdPart());

		OrganizationAffiliation createdAff2 = new OrganizationAffiliationDaoJdbc(defaultDataSource,
				permanentDeleteDataSource, fhirContext).create(aff2);

		ResearchStudy rS = new ResearchStudy();
		new ReadAccessHelperImpl().addRole(rS, "parent.com", "http://dsf.dev/fhir/CodeSystem/organization-role", "DIC");
		ResearchStudy createdRs = new ResearchStudyDaoJdbc(defaultDataSource, permanentDeleteDataSource, fhirContext)
				.create(rS);

		Binary b = createResource();
		b.setSecurityContext(new Reference(createdRs.getIdElement().toUnqualifiedVersionless()));
		Binary createdB = dao.create(b);

		assertReadAccessEntryCount(6, 1, createdRs, READ_ACCESS_TAG_VALUE_LOCAL);
		assertReadAccessEntryCount(6, 1, createdB, READ_ACCESS_TAG_VALUE_LOCAL);

		assertReadAccessEntryCount(6, 1, createdRs, READ_ACCESS_TAG_VALUE_ROLE, createdMemberOrg1, createdAff1);
		assertReadAccessEntryCount(6, 1, createdB, READ_ACCESS_TAG_VALUE_ROLE, createdMemberOrg1, createdAff1);

		assertReadAccessEntryCount(6, 1, createdRs, READ_ACCESS_TAG_VALUE_ROLE, createdMemberOrg2, createdAff2);
		assertReadAccessEntryCount(6, 1, createdB, READ_ACCESS_TAG_VALUE_ROLE, createdMemberOrg2, createdAff2);
	}

	private void testReadAccessTriggerSecurityContextUpdate(String accessType,
			Consumer<ResearchStudy> readAccessModifier) throws Exception
	{
		final ResearchStudyDaoJdbc researchStudyDao = new ResearchStudyDaoJdbc(defaultDataSource,
				permanentDeleteDataSource, fhirContext);

		ResearchStudy rS = new ResearchStudy();
		readAccessModifier.accept(rS);
		ResearchStudy v1 = researchStudyDao.create(rS);
		assertEquals(1L, (long) v1.getIdElement().getVersionIdPartAsLong());

		assertReadAccessEntryCount(1, 1, v1, accessType);

		Binary b = createResource();
		b.setSecurityContext(new Reference(v1.getIdElement().toUnqualifiedVersionless()));
		Binary createdB = dao.create(b);

		assertReadAccessEntryCount(2, 1, v1, accessType);
		assertReadAccessEntryCount(2, 1, createdB, accessType);

		v1.getMeta().setTag(List.of());
		ResearchStudy v2 = researchStudyDao.update(v1);
		assertEquals(2L, (long) v2.getIdElement().getVersionIdPartAsLong());

		assertReadAccessEntryCount(1, 1, v1, accessType);
		assertReadAccessEntryCount(1, 0, v2, accessType);
		assertReadAccessEntryCount(1, 0, createdB, accessType);

		readAccessModifier.accept(v2);
		ResearchStudy v3 = researchStudyDao.update(v2);
		assertEquals(3L, (long) v3.getIdElement().getVersionIdPartAsLong());

		assertReadAccessEntryCount(3, 1, v1, accessType);
		assertReadAccessEntryCount(3, 0, v2, accessType);
		assertReadAccessEntryCount(3, 1, v3, accessType);
		assertReadAccessEntryCount(3, 1, createdB, accessType);
	}

	@Test
	public void testReadAccessTriggerSecurityContextAllUpdate() throws Exception
	{
		testReadAccessTriggerSecurityContextUpdate(READ_ACCESS_TAG_VALUE_ALL, new ReadAccessHelperImpl()::addAll);
	}

	@Test
	public void testReadAccessTriggerSecurityContextLocalUpdate() throws Exception
	{
		testReadAccessTriggerSecurityContextUpdate(READ_ACCESS_TAG_VALUE_LOCAL, new ReadAccessHelperImpl()::addLocal);
	}

	private void testReadAccessTriggerSecurityContextVersionSpecificUpdate(String accessType,
			Consumer<ResearchStudy> readAccessModifier) throws Exception
	{
		final ResearchStudyDaoJdbc researchStudyDao = new ResearchStudyDaoJdbc(defaultDataSource,
				permanentDeleteDataSource, fhirContext);

		ResearchStudy rS = new ResearchStudy();
		readAccessModifier.accept(rS);
		ResearchStudy v1 = researchStudyDao.create(rS);
		assertEquals(1L, (long) v1.getIdElement().getVersionIdPartAsLong());

		assertReadAccessEntryCount(1, 1, v1, accessType);

		Binary b = createResource();
		b.setSecurityContext(new Reference(v1.getIdElement()));
		Binary createdB = dao.create(b);

		assertReadAccessEntryCount(2, 1, v1, accessType);
		assertReadAccessEntryCount(2, 1, createdB, accessType);

		v1.getMeta().setTag(List.of());
		ResearchStudy v2 = researchStudyDao.update(v1);
		assertEquals(2L, (long) v2.getIdElement().getVersionIdPartAsLong());

		assertReadAccessEntryCount(2, 1, v1, accessType);
		assertReadAccessEntryCount(2, 0, v2, accessType);
		assertReadAccessEntryCount(2, 1, createdB, accessType);

		readAccessModifier.accept(v2);
		ResearchStudy v3 = researchStudyDao.update(v2);
		assertEquals(3L, (long) v3.getIdElement().getVersionIdPartAsLong());

		assertReadAccessEntryCount(3, 1, v1, accessType);
		assertReadAccessEntryCount(3, 0, v2, accessType);
		assertReadAccessEntryCount(3, 1, v3, accessType);
		assertReadAccessEntryCount(3, 1, createdB, accessType);
	}

	@Test
	public void testReadAccessTriggerSecurityContextVersionSpecificAllUpdate() throws Exception
	{
		testReadAccessTriggerSecurityContextVersionSpecificUpdate(READ_ACCESS_TAG_VALUE_ALL,
				new ReadAccessHelperImpl()::addAll);
	}

	@Test
	public void testReadAccessTriggerSecurityContextVersionSpecificLocalUpdate() throws Exception
	{
		testReadAccessTriggerSecurityContextVersionSpecificUpdate(READ_ACCESS_TAG_VALUE_LOCAL,
				new ReadAccessHelperImpl()::addLocal);
	}

	@Test
	public void testReadAccessTriggerSecurityContextOrganizationUpdate() throws Exception
	{
		final OrganizationDaoJdbc organizationDao = new OrganizationDaoJdbc(defaultDataSource,
				permanentDeleteDataSource, fhirContext);

		Organization org = new Organization();
		org.setActive(true);
		org.addIdentifier().setSystem(ORGANIZATION_IDENTIFIER_SYSTEM).setValue("org.com");
		Organization createdOrg = organizationDao.create(org);

		ResearchStudy rS = new ResearchStudy();
		new ReadAccessHelperImpl().addOrganization(rS, createdOrg);
		ResearchStudy createdRs = new ResearchStudyDaoJdbc(defaultDataSource, permanentDeleteDataSource, fhirContext)
				.create(rS);

		assertReadAccessEntryCount(2, 1, createdRs, READ_ACCESS_TAG_VALUE_LOCAL);
		assertReadAccessEntryCount(2, 1, createdRs, READ_ACCESS_TAG_VALUE_ORGANIZATION, createdOrg);

		Binary b = createResource();
		b.setSecurityContext(new Reference(createdRs.getIdElement().toUnqualifiedVersionless()));
		Binary createdB = dao.create(b);

		assertReadAccessEntryCount(4, 1, createdRs, READ_ACCESS_TAG_VALUE_LOCAL);
		assertReadAccessEntryCount(4, 1, createdB, READ_ACCESS_TAG_VALUE_LOCAL);
		assertReadAccessEntryCount(4, 1, createdRs, READ_ACCESS_TAG_VALUE_ORGANIZATION, createdOrg);
		assertReadAccessEntryCount(4, 1, createdB, READ_ACCESS_TAG_VALUE_ORGANIZATION, createdOrg);

		createdOrg.setActive(false);
		Organization updatedOrg = organizationDao.update(createdOrg);

		assertReadAccessEntryCount(2, 1, createdRs, READ_ACCESS_TAG_VALUE_LOCAL);
		assertReadAccessEntryCount(2, 1, createdB, READ_ACCESS_TAG_VALUE_LOCAL);
		assertReadAccessEntryCount(2, 0, createdRs, READ_ACCESS_TAG_VALUE_ORGANIZATION, createdOrg);
		assertReadAccessEntryCount(2, 0, createdB, READ_ACCESS_TAG_VALUE_ORGANIZATION, createdOrg);

		updatedOrg.setActive(true);
		organizationDao.update(updatedOrg);

		assertReadAccessEntryCount(4, 1, createdRs, READ_ACCESS_TAG_VALUE_LOCAL);
		assertReadAccessEntryCount(4, 1, createdB, READ_ACCESS_TAG_VALUE_LOCAL);
		assertReadAccessEntryCount(4, 1, createdRs, READ_ACCESS_TAG_VALUE_ORGANIZATION, createdOrg);
		assertReadAccessEntryCount(4, 1, createdB, READ_ACCESS_TAG_VALUE_ORGANIZATION, createdOrg);
	}

	@Test
	public void testReadAccessTriggerSecurityContextRoleUpdate() throws Exception
	{
		final OrganizationAffiliationDaoJdbc organizationAffiliationDao = new OrganizationAffiliationDaoJdbc(
				defaultDataSource, permanentDeleteDataSource, fhirContext);

		Organization parentOrg = new Organization();
		parentOrg.setActive(true);
		parentOrg.addIdentifier().setSystem(ORGANIZATION_IDENTIFIER_SYSTEM).setValue("parent.com");

		Organization memberOrg = new Organization();
		memberOrg.setActive(true);
		memberOrg.addIdentifier().setSystem(ORGANIZATION_IDENTIFIER_SYSTEM).setValue("member.com");

		OrganizationDao orgDao = new OrganizationDaoJdbc(defaultDataSource, permanentDeleteDataSource, fhirContext);
		Organization createdParentOrg = orgDao.create(parentOrg);
		Organization createdMemberOrg = orgDao.create(memberOrg);

		OrganizationAffiliation aff = new OrganizationAffiliation();
		aff.setActive(true);
		aff.getCodeFirstRep().getCodingFirstRep().setSystem("http://dsf.dev/fhir/CodeSystem/organization-role")
				.setCode("DIC");
		aff.getOrganization().setReference("Organization/" + createdParentOrg.getIdElement().getIdPart());
		aff.getParticipatingOrganization().setReference("Organization/" + createdMemberOrg.getIdElement().getIdPart());

		OrganizationAffiliation createdAff = organizationAffiliationDao.create(aff);

		ResearchStudy rS = new ResearchStudy();
		new ReadAccessHelperImpl().addRole(rS, "parent.com", "http://dsf.dev/fhir/CodeSystem/organization-role", "DIC");
		ResearchStudy createdRs = new ResearchStudyDaoJdbc(defaultDataSource, permanentDeleteDataSource, fhirContext)
				.create(rS);

		Binary b = createResource();
		b.setSecurityContext(new Reference(createdRs.getIdElement().toUnqualifiedVersionless()));
		Binary createdB = dao.create(b);

		assertReadAccessEntryCount(4, 1, createdRs, READ_ACCESS_TAG_VALUE_LOCAL);
		assertReadAccessEntryCount(4, 1, createdB, READ_ACCESS_TAG_VALUE_LOCAL);

		assertReadAccessEntryCount(4, 1, createdRs, READ_ACCESS_TAG_VALUE_ROLE, createdMemberOrg, createdAff);
		assertReadAccessEntryCount(4, 1, createdB, READ_ACCESS_TAG_VALUE_ROLE, createdMemberOrg, createdAff);

		createdAff.setActive(false);
		OrganizationAffiliation updatedAff = organizationAffiliationDao.update(createdAff);

		assertReadAccessEntryCount(2, 1, createdRs, READ_ACCESS_TAG_VALUE_LOCAL);
		assertReadAccessEntryCount(2, 1, createdB, READ_ACCESS_TAG_VALUE_LOCAL);

		assertReadAccessEntryCount(2, 0, createdRs, READ_ACCESS_TAG_VALUE_ROLE, createdMemberOrg, updatedAff);
		assertReadAccessEntryCount(2, 0, createdB, READ_ACCESS_TAG_VALUE_ROLE, createdMemberOrg, updatedAff);

		updatedAff.setActive(true);
		organizationAffiliationDao.update(updatedAff);

		assertReadAccessEntryCount(4, 1, createdRs, READ_ACCESS_TAG_VALUE_LOCAL);
		assertReadAccessEntryCount(4, 1, createdB, READ_ACCESS_TAG_VALUE_LOCAL);

		assertReadAccessEntryCount(4, 1, createdRs, READ_ACCESS_TAG_VALUE_ROLE, createdMemberOrg, updatedAff);
		assertReadAccessEntryCount(4, 1, createdB, READ_ACCESS_TAG_VALUE_ROLE, createdMemberOrg, updatedAff);
	}

	@Test
	public void testReadAccessTriggerSecurityContextRoleUpdateMemberOrganizationNonActive() throws Exception
	{
		final OrganizationAffiliationDaoJdbc organizationAffiliationDao = new OrganizationAffiliationDaoJdbc(
				defaultDataSource, permanentDeleteDataSource, fhirContext);

		Organization parentOrg = new Organization();
		parentOrg.setActive(true);
		parentOrg.addIdentifier().setSystem(ORGANIZATION_IDENTIFIER_SYSTEM).setValue("parent.com");

		Organization memberOrg = new Organization();
		memberOrg.setActive(true);
		memberOrg.addIdentifier().setSystem(ORGANIZATION_IDENTIFIER_SYSTEM).setValue("member.com");

		OrganizationDao orgDao = new OrganizationDaoJdbc(defaultDataSource, permanentDeleteDataSource, fhirContext);
		Organization createdParentOrg = orgDao.create(parentOrg);
		Organization createdMemberOrg = orgDao.create(memberOrg);

		OrganizationAffiliation aff = new OrganizationAffiliation();
		aff.setActive(true);
		aff.getCodeFirstRep().getCodingFirstRep().setSystem("http://dsf.dev/fhir/CodeSystem/organization-role")
				.setCode("DIC");
		aff.getOrganization().setReference("Organization/" + createdParentOrg.getIdElement().getIdPart());
		aff.getParticipatingOrganization().setReference("Organization/" + createdMemberOrg.getIdElement().getIdPart());

		OrganizationAffiliation createdAff = organizationAffiliationDao.create(aff);

		ResearchStudy rS = new ResearchStudy();
		new ReadAccessHelperImpl().addRole(rS, "parent.com", "http://dsf.dev/fhir/CodeSystem/organization-role", "DIC");
		ResearchStudy createdRs = new ResearchStudyDaoJdbc(defaultDataSource, permanentDeleteDataSource, fhirContext)
				.create(rS);

		Binary b = createResource();
		b.setSecurityContext(new Reference(createdRs.getIdElement().toUnqualifiedVersionless()));
		Binary createdB = dao.create(b);

		assertReadAccessEntryCount(4, 1, createdRs, READ_ACCESS_TAG_VALUE_LOCAL);
		assertReadAccessEntryCount(4, 1, createdB, READ_ACCESS_TAG_VALUE_LOCAL);
		assertReadAccessEntryCount(4, 1, createdRs, READ_ACCESS_TAG_VALUE_ROLE, createdMemberOrg, createdAff);
		assertReadAccessEntryCount(4, 1, createdB, READ_ACCESS_TAG_VALUE_ROLE, createdMemberOrg, createdAff);

		createdMemberOrg.setActive(false);
		Organization updatedMemberOrg = orgDao.update(createdMemberOrg);

		assertReadAccessEntryCount(2, 1, createdRs, READ_ACCESS_TAG_VALUE_LOCAL);
		assertReadAccessEntryCount(2, 1, createdB, READ_ACCESS_TAG_VALUE_LOCAL);
		assertReadAccessEntryCount(2, 0, createdRs, READ_ACCESS_TAG_VALUE_ROLE, updatedMemberOrg, createdAff);
		assertReadAccessEntryCount(2, 0, createdB, READ_ACCESS_TAG_VALUE_ROLE, updatedMemberOrg, createdAff);

		updatedMemberOrg.setActive(true);
		orgDao.update(updatedMemberOrg);

		assertReadAccessEntryCount(4, 1, createdRs, READ_ACCESS_TAG_VALUE_LOCAL);
		assertReadAccessEntryCount(4, 1, createdB, READ_ACCESS_TAG_VALUE_LOCAL);
		assertReadAccessEntryCount(4, 1, createdRs, READ_ACCESS_TAG_VALUE_ROLE, updatedMemberOrg, createdAff);
		assertReadAccessEntryCount(4, 1, createdB, READ_ACCESS_TAG_VALUE_ROLE, updatedMemberOrg, createdAff);
	}

	@Test
	public void testReadAccessTriggerSecurityContextRoleUpdateParentOrganizationNonActive() throws Exception
	{
		final OrganizationAffiliationDaoJdbc organizationAffiliationDao = new OrganizationAffiliationDaoJdbc(
				defaultDataSource, permanentDeleteDataSource, fhirContext);

		Organization parentOrg = new Organization();
		parentOrg.setActive(true);
		parentOrg.addIdentifier().setSystem(ORGANIZATION_IDENTIFIER_SYSTEM).setValue("parent.com");

		Organization memberOrg = new Organization();
		memberOrg.setActive(true);
		memberOrg.addIdentifier().setSystem(ORGANIZATION_IDENTIFIER_SYSTEM).setValue("member.com");

		OrganizationDao orgDao = new OrganizationDaoJdbc(defaultDataSource, permanentDeleteDataSource, fhirContext);
		Organization createdParentOrg = orgDao.create(parentOrg);
		Organization createdMemberOrg = orgDao.create(memberOrg);

		OrganizationAffiliation aff = new OrganizationAffiliation();
		aff.setActive(true);
		aff.getCodeFirstRep().getCodingFirstRep().setSystem("http://dsf.dev/fhir/CodeSystem/organization-role")
				.setCode("DIC");
		aff.getOrganization().setReference("Organization/" + createdParentOrg.getIdElement().getIdPart());
		aff.getParticipatingOrganization().setReference("Organization/" + createdMemberOrg.getIdElement().getIdPart());

		OrganizationAffiliation createdAff = organizationAffiliationDao.create(aff);

		ResearchStudy rS = new ResearchStudy();
		new ReadAccessHelperImpl().addRole(rS, "parent.com", "http://dsf.dev/fhir/CodeSystem/organization-role", "DIC");
		ResearchStudy createdRs = new ResearchStudyDaoJdbc(defaultDataSource, permanentDeleteDataSource, fhirContext)
				.create(rS);

		Binary b = createResource();
		b.setSecurityContext(new Reference(createdRs.getIdElement().toUnqualifiedVersionless()));
		Binary createdB = dao.create(b);

		assertReadAccessEntryCount(4, 1, createdRs, READ_ACCESS_TAG_VALUE_LOCAL);
		assertReadAccessEntryCount(4, 1, createdB, READ_ACCESS_TAG_VALUE_LOCAL);
		assertReadAccessEntryCount(4, 1, createdRs, READ_ACCESS_TAG_VALUE_ROLE, createdMemberOrg, createdAff);
		assertReadAccessEntryCount(4, 1, createdB, READ_ACCESS_TAG_VALUE_ROLE, createdMemberOrg, createdAff);

		createdParentOrg.setActive(false);
		Organization updatedParentOrg = orgDao.update(createdParentOrg);

		assertReadAccessEntryCount(2, 1, createdRs, READ_ACCESS_TAG_VALUE_LOCAL);
		assertReadAccessEntryCount(2, 1, createdB, READ_ACCESS_TAG_VALUE_LOCAL);
		assertReadAccessEntryCount(2, 0, createdRs, READ_ACCESS_TAG_VALUE_ROLE, createdMemberOrg, createdAff);
		assertReadAccessEntryCount(2, 0, createdB, READ_ACCESS_TAG_VALUE_ROLE, createdMemberOrg, createdAff);

		updatedParentOrg.setActive(true);
		orgDao.update(updatedParentOrg);

		assertReadAccessEntryCount(4, 1, createdRs, READ_ACCESS_TAG_VALUE_LOCAL);
		assertReadAccessEntryCount(4, 1, createdB, READ_ACCESS_TAG_VALUE_LOCAL);
		assertReadAccessEntryCount(4, 1, createdRs, READ_ACCESS_TAG_VALUE_ROLE, createdMemberOrg, createdAff);
		assertReadAccessEntryCount(4, 1, createdB, READ_ACCESS_TAG_VALUE_ROLE, createdMemberOrg, createdAff);
	}

	@Test
	public void testReadAccessTriggerSecurityContextRoleUpdateMemberAndParentOrganizationNonActive() throws Exception
	{
		final OrganizationAffiliationDaoJdbc organizationAffiliationDao = new OrganizationAffiliationDaoJdbc(
				defaultDataSource, permanentDeleteDataSource, fhirContext);

		Organization parentOrg = new Organization();
		parentOrg.setActive(true);
		parentOrg.addIdentifier().setSystem(ORGANIZATION_IDENTIFIER_SYSTEM).setValue("parent.com");

		Organization memberOrg = new Organization();
		memberOrg.setActive(true);
		memberOrg.addIdentifier().setSystem(ORGANIZATION_IDENTIFIER_SYSTEM).setValue("member.com");

		OrganizationDao orgDao = new OrganizationDaoJdbc(defaultDataSource, permanentDeleteDataSource, fhirContext);
		Organization createdParentOrg = orgDao.create(parentOrg);
		Organization createdMemberOrg = orgDao.create(memberOrg);

		OrganizationAffiliation aff = new OrganizationAffiliation();
		aff.setActive(true);
		aff.getCodeFirstRep().getCodingFirstRep().setSystem("http://dsf.dev/fhir/CodeSystem/organization-role")
				.setCode("DIC");
		aff.getOrganization().setReference("Organization/" + createdParentOrg.getIdElement().getIdPart());
		aff.getParticipatingOrganization().setReference("Organization/" + createdMemberOrg.getIdElement().getIdPart());

		OrganizationAffiliation createdAff = organizationAffiliationDao.create(aff);

		ResearchStudy rS = new ResearchStudy();
		new ReadAccessHelperImpl().addRole(rS, "parent.com", "http://dsf.dev/fhir/CodeSystem/organization-role", "DIC");
		ResearchStudy createdRs = new ResearchStudyDaoJdbc(defaultDataSource, permanentDeleteDataSource, fhirContext)
				.create(rS);

		Binary b = createResource();
		b.setSecurityContext(new Reference(createdRs.getIdElement().toUnqualifiedVersionless()));
		Binary createdB = dao.create(b);
		logger.debug("Created Binary {}", fhirContext.newJsonParser().encodeResourceToString(createdB));

		assertReadAccessEntryCount(4, 1, createdRs, READ_ACCESS_TAG_VALUE_LOCAL);
		assertReadAccessEntryCount(4, 1, createdB, READ_ACCESS_TAG_VALUE_LOCAL);
		assertReadAccessEntryCount(4, 1, createdRs, READ_ACCESS_TAG_VALUE_ROLE, createdMemberOrg, createdAff);
		assertReadAccessEntryCount(4, 1, createdB, READ_ACCESS_TAG_VALUE_ROLE, createdMemberOrg, createdAff);

		createdParentOrg.setActive(false);
		createdMemberOrg.setActive(false);
		Organization updatedParentOrg = orgDao.update(createdParentOrg);
		Organization updatedMemberOrg = orgDao.update(createdMemberOrg);

		assertReadAccessEntryCount(2, 1, createdRs, READ_ACCESS_TAG_VALUE_LOCAL);
		assertReadAccessEntryCount(2, 1, createdB, READ_ACCESS_TAG_VALUE_LOCAL);
		assertReadAccessEntryCount(2, 0, createdRs, READ_ACCESS_TAG_VALUE_ROLE, updatedMemberOrg, createdAff);
		assertReadAccessEntryCount(2, 0, createdB, READ_ACCESS_TAG_VALUE_ROLE, updatedMemberOrg, createdAff);

		updatedParentOrg.setActive(true);
		orgDao.update(updatedParentOrg);

		assertReadAccessEntryCount(2, 1, createdRs, READ_ACCESS_TAG_VALUE_LOCAL);
		assertReadAccessEntryCount(2, 1, createdB, READ_ACCESS_TAG_VALUE_LOCAL);
		assertReadAccessEntryCount(2, 0, createdRs, READ_ACCESS_TAG_VALUE_ROLE, updatedMemberOrg, createdAff);
		assertReadAccessEntryCount(2, 0, createdB, READ_ACCESS_TAG_VALUE_ROLE, updatedMemberOrg, createdAff);

		updatedMemberOrg.setActive(true);
		Organization updatedMemberOrg2 = orgDao.update(updatedMemberOrg);

		assertReadAccessEntryCount(4, 1, createdRs, READ_ACCESS_TAG_VALUE_LOCAL);
		assertReadAccessEntryCount(4, 1, createdB, READ_ACCESS_TAG_VALUE_LOCAL);
		assertReadAccessEntryCount(4, 1, createdRs, READ_ACCESS_TAG_VALUE_ROLE, updatedMemberOrg2, createdAff);
		assertReadAccessEntryCount(4, 1, createdB, READ_ACCESS_TAG_VALUE_ROLE, updatedMemberOrg2, createdAff);
	}

	private void testReadAccessTriggerSecurityContextDelete(String accessType,
			Consumer<ResearchStudy> readAccessModifier) throws Exception
	{
		final ResearchStudyDaoJdbc researchStudyDao = new ResearchStudyDaoJdbc(defaultDataSource,
				permanentDeleteDataSource, fhirContext);

		ResearchStudy rS = new ResearchStudy();
		readAccessModifier.accept(rS);
		ResearchStudy createdRs = researchStudyDao.create(rS);

		assertReadAccessEntryCount(1, 1, createdRs, accessType);

		Binary b = createResource();
		b.setSecurityContext(new Reference(createdRs.getIdElement().toUnqualifiedVersionless()));
		Binary createdB = dao.create(b);

		assertReadAccessEntryCount(2, 1, createdRs, accessType);
		assertReadAccessEntryCount(2, 1, createdB, accessType);

		researchStudyDao.delete(UUID.fromString(createdRs.getIdElement().getIdPart()));

		assertReadAccessEntryCount(1, 1, createdRs, accessType);
		assertReadAccessEntryCount(1, 0, createdB, accessType);
	}

	@Test
	public void testReadAccessTriggerSecurityContextAllDelete() throws Exception
	{
		testReadAccessTriggerSecurityContextDelete(READ_ACCESS_TAG_VALUE_ALL, new ReadAccessHelperImpl()::addAll);
	}

	@Test
	public void testReadAccessTriggerSecurityContextLocalDelete() throws Exception
	{
		testReadAccessTriggerSecurityContextUpdate(READ_ACCESS_TAG_VALUE_LOCAL, new ReadAccessHelperImpl()::addLocal);
	}

	@Test
	public void testReadAccessTriggerSecurityContextOrganizationDelete() throws Exception
	{
		final OrganizationDaoJdbc organizationDao = new OrganizationDaoJdbc(defaultDataSource,
				permanentDeleteDataSource, fhirContext);

		Organization org = new Organization();
		org.setActive(true);
		org.addIdentifier().setSystem(ORGANIZATION_IDENTIFIER_SYSTEM).setValue("org.com");
		Organization createdOrg = organizationDao.create(org);

		ResearchStudy rS = new ResearchStudy();
		new ReadAccessHelperImpl().addOrganization(rS, createdOrg);
		ResearchStudy createdRs = new ResearchStudyDaoJdbc(defaultDataSource, permanentDeleteDataSource, fhirContext)
				.create(rS);

		Binary b = createResource();
		b.setSecurityContext(new Reference(createdRs.getIdElement().toUnqualifiedVersionless()));
		Binary createdB = dao.create(b);

		assertReadAccessEntryCount(4, 1, createdRs, READ_ACCESS_TAG_VALUE_LOCAL);
		assertReadAccessEntryCount(4, 1, createdB, READ_ACCESS_TAG_VALUE_LOCAL);
		assertReadAccessEntryCount(4, 1, createdRs, READ_ACCESS_TAG_VALUE_ORGANIZATION, createdOrg);
		assertReadAccessEntryCount(4, 1, createdB, READ_ACCESS_TAG_VALUE_ORGANIZATION, createdOrg);

		organizationDao.delete(UUID.fromString(createdOrg.getIdElement().getIdPart()));

		assertReadAccessEntryCount(2, 1, createdRs, READ_ACCESS_TAG_VALUE_LOCAL);
		assertReadAccessEntryCount(2, 1, createdB, READ_ACCESS_TAG_VALUE_LOCAL);
		assertReadAccessEntryCount(2, 0, createdRs, READ_ACCESS_TAG_VALUE_ORGANIZATION, createdOrg);
		assertReadAccessEntryCount(2, 0, createdB, READ_ACCESS_TAG_VALUE_ORGANIZATION, createdOrg);
	}

	@Test
	public void testReadAccessTriggerSecurityContextRoleDelete() throws Exception
	{
		Organization parentOrg = new Organization();
		parentOrg.setActive(true);
		parentOrg.addIdentifier().setSystem(ORGANIZATION_IDENTIFIER_SYSTEM).setValue("parent.com");

		Organization memberOrg = new Organization();
		memberOrg.setActive(true);
		memberOrg.addIdentifier().setSystem(ORGANIZATION_IDENTIFIER_SYSTEM).setValue("member.com");

		OrganizationDao orgDao = new OrganizationDaoJdbc(defaultDataSource, permanentDeleteDataSource, fhirContext);
		Organization createdParentOrg = orgDao.create(parentOrg);
		Organization createdMemberOrg = orgDao.create(memberOrg);

		OrganizationAffiliation aff = new OrganizationAffiliation();
		aff.setActive(true);
		aff.getCodeFirstRep().getCodingFirstRep().setSystem("http://dsf.dev/fhir/CodeSystem/organization-role")
				.setCode("DIC");
		aff.getOrganization().setReference("Organization/" + createdParentOrg.getIdElement().getIdPart());
		aff.getParticipatingOrganization().setReference("Organization/" + createdMemberOrg.getIdElement().getIdPart());

		final OrganizationAffiliationDaoJdbc orgAffDao = new OrganizationAffiliationDaoJdbc(defaultDataSource,
				permanentDeleteDataSource, fhirContext);

		OrganizationAffiliation createdAff = orgAffDao.create(aff);

		ResearchStudy rS = new ResearchStudy();
		new ReadAccessHelperImpl().addRole(rS, "parent.com", "http://dsf.dev/fhir/CodeSystem/organization-role", "DIC");
		ResearchStudy createdRs = new ResearchStudyDaoJdbc(defaultDataSource, permanentDeleteDataSource, fhirContext)
				.create(rS);

		Binary b = createResource();
		b.setSecurityContext(new Reference(createdRs.getIdElement().toUnqualifiedVersionless()));
		Binary createdB = dao.create(b);

		assertReadAccessEntryCount(4, 1, createdRs, READ_ACCESS_TAG_VALUE_LOCAL);
		assertReadAccessEntryCount(4, 1, createdB, READ_ACCESS_TAG_VALUE_LOCAL);

		assertReadAccessEntryCount(4, 1, createdRs, READ_ACCESS_TAG_VALUE_ROLE, createdMemberOrg, createdAff);
		assertReadAccessEntryCount(4, 1, createdB, READ_ACCESS_TAG_VALUE_ROLE, createdMemberOrg, createdAff);

		orgAffDao.delete(UUID.fromString(createdAff.getIdElement().getIdPart()));

		assertReadAccessEntryCount(2, 1, createdRs, READ_ACCESS_TAG_VALUE_LOCAL);
		assertReadAccessEntryCount(2, 1, createdB, READ_ACCESS_TAG_VALUE_LOCAL);

		assertReadAccessEntryCount(2, 0, createdRs, READ_ACCESS_TAG_VALUE_ROLE, createdMemberOrg, createdAff);
		assertReadAccessEntryCount(2, 0, createdB, READ_ACCESS_TAG_VALUE_ROLE, createdMemberOrg, createdAff);
	}

	@Test
	public void testReadAccessTriggerSecurityContextRoleDeleteMember() throws Exception
	{
		Organization parentOrg = new Organization();
		parentOrg.setActive(true);
		parentOrg.addIdentifier().setSystem(ORGANIZATION_IDENTIFIER_SYSTEM).setValue("parent.com");

		Organization memberOrg = new Organization();
		memberOrg.setActive(true);
		memberOrg.addIdentifier().setSystem(ORGANIZATION_IDENTIFIER_SYSTEM).setValue("member.com");

		OrganizationDao orgDao = new OrganizationDaoJdbc(defaultDataSource, permanentDeleteDataSource, fhirContext);
		Organization createdParentOrg = orgDao.create(parentOrg);
		Organization createdMemberOrg = orgDao.create(memberOrg);

		OrganizationAffiliation aff = new OrganizationAffiliation();
		aff.setActive(true);
		aff.getCodeFirstRep().getCodingFirstRep().setSystem("http://dsf.dev/fhir/CodeSystem/organization-role")
				.setCode("DIC");
		aff.getOrganization().setReference("Organization/" + createdParentOrg.getIdElement().getIdPart());
		aff.getParticipatingOrganization().setReference("Organization/" + createdMemberOrg.getIdElement().getIdPart());

		final OrganizationAffiliationDaoJdbc orgAffDao = new OrganizationAffiliationDaoJdbc(defaultDataSource,
				permanentDeleteDataSource, fhirContext);

		OrganizationAffiliation createdAff = orgAffDao.create(aff);

		ResearchStudy rS = new ResearchStudy();
		new ReadAccessHelperImpl().addRole(rS, "parent.com", "http://dsf.dev/fhir/CodeSystem/organization-role", "DIC");
		ResearchStudy createdRs = new ResearchStudyDaoJdbc(defaultDataSource, permanentDeleteDataSource, fhirContext)
				.create(rS);

		Binary b = createResource();
		b.setSecurityContext(new Reference(createdRs.getIdElement().toUnqualifiedVersionless()));
		Binary createdB = dao.create(b);

		assertReadAccessEntryCount(4, 1, createdRs, READ_ACCESS_TAG_VALUE_LOCAL);
		assertReadAccessEntryCount(4, 1, createdB, READ_ACCESS_TAG_VALUE_LOCAL);

		assertReadAccessEntryCount(4, 1, createdRs, READ_ACCESS_TAG_VALUE_ROLE, createdMemberOrg, createdAff);
		assertReadAccessEntryCount(4, 1, createdB, READ_ACCESS_TAG_VALUE_ROLE, createdMemberOrg, createdAff);

		orgDao.delete(UUID.fromString(createdMemberOrg.getIdElement().getIdPart()));

		assertReadAccessEntryCount(2, 1, createdRs, READ_ACCESS_TAG_VALUE_LOCAL);
		assertReadAccessEntryCount(2, 1, createdB, READ_ACCESS_TAG_VALUE_LOCAL);

		assertReadAccessEntryCount(2, 0, createdRs, READ_ACCESS_TAG_VALUE_ROLE, createdMemberOrg, createdAff);
		assertReadAccessEntryCount(2, 0, createdB, READ_ACCESS_TAG_VALUE_ROLE, createdMemberOrg, createdAff);
	}

	@Test
	public void testReadAccessTriggerSecurityContextRoleDeleteParent() throws Exception
	{
		Organization parentOrg = new Organization();
		parentOrg.setActive(true);
		parentOrg.addIdentifier().setSystem(ORGANIZATION_IDENTIFIER_SYSTEM).setValue("parent.com");

		Organization memberOrg = new Organization();
		memberOrg.setActive(true);
		memberOrg.addIdentifier().setSystem(ORGANIZATION_IDENTIFIER_SYSTEM).setValue("member.com");

		OrganizationDao orgDao = new OrganizationDaoJdbc(defaultDataSource, permanentDeleteDataSource, fhirContext);
		Organization createdParentOrg = orgDao.create(parentOrg);
		Organization createdMemberOrg = orgDao.create(memberOrg);

		OrganizationAffiliation aff = new OrganizationAffiliation();
		aff.setActive(true);
		aff.getCodeFirstRep().getCodingFirstRep().setSystem("http://dsf.dev/fhir/CodeSystem/organization-role")
				.setCode("DIC");
		aff.getOrganization().setReference("Organization/" + createdParentOrg.getIdElement().getIdPart());
		aff.getParticipatingOrganization().setReference("Organization/" + createdMemberOrg.getIdElement().getIdPart());

		final OrganizationAffiliationDaoJdbc orgAffDao = new OrganizationAffiliationDaoJdbc(defaultDataSource,
				permanentDeleteDataSource, fhirContext);

		OrganizationAffiliation createdAff = orgAffDao.create(aff);

		ResearchStudy rS = new ResearchStudy();
		new ReadAccessHelperImpl().addRole(rS, "parent.com", "http://dsf.dev/fhir/CodeSystem/organization-role", "DIC");
		ResearchStudy createdRs = new ResearchStudyDaoJdbc(defaultDataSource, permanentDeleteDataSource, fhirContext)
				.create(rS);

		Binary b = createResource();
		b.setSecurityContext(new Reference(createdRs.getIdElement().toUnqualifiedVersionless()));
		Binary createdB = dao.create(b);

		assertReadAccessEntryCount(4, 1, createdRs, READ_ACCESS_TAG_VALUE_LOCAL);
		assertReadAccessEntryCount(4, 1, createdB, READ_ACCESS_TAG_VALUE_LOCAL);

		assertReadAccessEntryCount(4, 1, createdRs, READ_ACCESS_TAG_VALUE_ROLE, createdMemberOrg, createdAff);
		assertReadAccessEntryCount(4, 1, createdB, READ_ACCESS_TAG_VALUE_ROLE, createdMemberOrg, createdAff);

		orgDao.delete(UUID.fromString(createdParentOrg.getIdElement().getIdPart()));

		assertReadAccessEntryCount(2, 1, createdRs, READ_ACCESS_TAG_VALUE_LOCAL);
		assertReadAccessEntryCount(2, 1, createdB, READ_ACCESS_TAG_VALUE_LOCAL);

		assertReadAccessEntryCount(2, 0, createdRs, READ_ACCESS_TAG_VALUE_ROLE, createdMemberOrg, createdAff);
		assertReadAccessEntryCount(2, 0, createdB, READ_ACCESS_TAG_VALUE_ROLE, createdMemberOrg, createdAff);
	}

	@Test
	public void testReadAccessTriggerSecurityContextRoleDeleteMemberAndParent() throws Exception
	{
		Organization parentOrg = new Organization();
		parentOrg.setActive(true);
		parentOrg.addIdentifier().setSystem(ORGANIZATION_IDENTIFIER_SYSTEM).setValue("parent.com");

		Organization memberOrg = new Organization();
		memberOrg.setActive(true);
		memberOrg.addIdentifier().setSystem(ORGANIZATION_IDENTIFIER_SYSTEM).setValue("member.com");

		OrganizationDao orgDao = new OrganizationDaoJdbc(defaultDataSource, permanentDeleteDataSource, fhirContext);
		Organization createdParentOrg = orgDao.create(parentOrg);
		Organization createdMemberOrg = orgDao.create(memberOrg);

		OrganizationAffiliation aff = new OrganizationAffiliation();
		aff.setActive(true);
		aff.getCodeFirstRep().getCodingFirstRep().setSystem("http://dsf.dev/fhir/CodeSystem/organization-role")
				.setCode("DIC");
		aff.getOrganization().setReference("Organization/" + createdParentOrg.getIdElement().getIdPart());
		aff.getParticipatingOrganization().setReference("Organization/" + createdMemberOrg.getIdElement().getIdPart());

		final OrganizationAffiliationDaoJdbc orgAffDao = new OrganizationAffiliationDaoJdbc(defaultDataSource,
				permanentDeleteDataSource, fhirContext);

		OrganizationAffiliation createdAff = orgAffDao.create(aff);

		ResearchStudy rS = new ResearchStudy();
		new ReadAccessHelperImpl().addRole(rS, "parent.com", "http://dsf.dev/fhir/CodeSystem/organization-role", "DIC");
		ResearchStudy createdRs = new ResearchStudyDaoJdbc(defaultDataSource, permanentDeleteDataSource, fhirContext)
				.create(rS);

		Binary b = createResource();
		b.setSecurityContext(new Reference(createdRs.getIdElement().toUnqualifiedVersionless()));
		Binary createdB = dao.create(b);

		assertReadAccessEntryCount(4, 1, createdRs, READ_ACCESS_TAG_VALUE_LOCAL);
		assertReadAccessEntryCount(4, 1, createdB, READ_ACCESS_TAG_VALUE_LOCAL);

		assertReadAccessEntryCount(4, 1, createdRs, READ_ACCESS_TAG_VALUE_ROLE, createdMemberOrg, createdAff);
		assertReadAccessEntryCount(4, 1, createdB, READ_ACCESS_TAG_VALUE_ROLE, createdMemberOrg, createdAff);

		orgDao.delete(UUID.fromString(createdMemberOrg.getIdElement().getIdPart()));
		orgDao.delete(UUID.fromString(createdParentOrg.getIdElement().getIdPart()));

		assertReadAccessEntryCount(2, 1, createdRs, READ_ACCESS_TAG_VALUE_LOCAL);
		assertReadAccessEntryCount(2, 1, createdB, READ_ACCESS_TAG_VALUE_LOCAL);

		assertReadAccessEntryCount(2, 0, createdRs, READ_ACCESS_TAG_VALUE_ROLE, createdMemberOrg, createdAff);
		assertReadAccessEntryCount(2, 0, createdB, READ_ACCESS_TAG_VALUE_ROLE, createdMemberOrg, createdAff);
	}

	@Test
	public void testCreateLarge() throws Exception
	{
		// making sure logging is enabled to test for PostgreSQL bug https://github.com/pgjdbc/pgjdbc/issues/3365
		org.apache.logging.log4j.Logger psL = LogManager.getLogger(PreparedStatementWithLogger.class);
		Level oldLevel = psL.getLevel();

		if (psL instanceof org.apache.logging.log4j.core.Logger l)
			l.setLevel(Level.DEBUG);

		Binary binary = new Binary();
		binary.setContentType(MediaType.APPLICATION_JSON);
		binary.setData(("{\"data\": \"" + "a".repeat(14999999) + "\"}").getBytes());

		Binary created = dao.create(binary);
		assertNotNull(created);
		assertNotNull(created.getData());
		assertEquals(binary.getData().length, created.getData().length);

		if (psL instanceof org.apache.logging.log4j.core.Logger l)
			l.setLevel(oldLevel);
	}

	@Test
	public void testCreateRead4Gib() throws Exception
	{
		long payloadSize = RandomInputStream.ONE_GIBIBYTE * 4;

		logger.info(
				"Executing create / read test for binary with {} GiB payload, test will run for about 2 minutes ...",
				(payloadSize / RandomInputStream.ONE_GIBIBYTE));

		Binary resource = new Binary();
		resource.setContentType("text/plain");
		resource.setDataElement(new StreamableBase64BinaryType(RandomInputStream.zeros(payloadSize)));

		Binary binary = dao.create(resource);
		assertNotNull(binary);
		assertNotNull(binary.getIdElement());
		assertNotNull(binary.getIdElement().getIdPart());

		Optional<Binary> read = dao.read(UUID.fromString(binary.getIdElement().getIdPart()));
		assertTrue(read.isPresent());
		assertTrue(read.get().getDataElement() instanceof DeferredBase64BinaryType);
		DeferredBase64BinaryType deferred = (DeferredBase64BinaryType) read.get().getDataElement();
		CountingOutputStream countingOutputStream = new CountingOutputStream(NullOutputStream.INSTANCE);
		deferred.writeExternal(countingOutputStream);
		long count = countingOutputStream.getByteCount();
		assertEquals(payloadSize, count);

		Optional<Binary> readFirst2000Bytes = dao.read(UUID.fromString(binary.getIdElement().getIdPart()),
				new RangeRequestImpl(0L, 1999L));
		assertTrue(readFirst2000Bytes.isPresent());
		assertTrue(readFirst2000Bytes.get().getDataElement() instanceof DeferredBase64BinaryType);
		DeferredBase64BinaryType deferredFirst2000Bytes = (DeferredBase64BinaryType) readFirst2000Bytes.get()
				.getDataElement();
		CountingOutputStream countingOutputStreamFirst2000Bytes = new CountingOutputStream(NullOutputStream.INSTANCE);
		deferredFirst2000Bytes.writeExternal(countingOutputStreamFirst2000Bytes);
		long countFirst2000Bytes = countingOutputStreamFirst2000Bytes.getByteCount();
		assertEquals(2000, countFirst2000Bytes);

		Optional<Binary> readLast2000Bytes = dao.read(UUID.fromString(binary.getIdElement().getIdPart()),
				new RangeRequestImpl(null, -2000L));
		assertTrue(readLast2000Bytes.isPresent());
		assertTrue(readLast2000Bytes.get().getDataElement() instanceof DeferredBase64BinaryType);
		DeferredBase64BinaryType deferredLast2000Bytes = (DeferredBase64BinaryType) readLast2000Bytes.get()
				.getDataElement();
		CountingOutputStream countingOutputStreamLast2000Bytes = new CountingOutputStream(NullOutputStream.INSTANCE);
		deferredLast2000Bytes.writeExternal(countingOutputStreamLast2000Bytes);
		long countLast2000Bytes = countingOutputStreamLast2000Bytes.getByteCount();
		assertEquals(2000, countLast2000Bytes);

		Optional<Binary> readAllFrom4GibMinus100Bytes = dao.read(UUID.fromString(binary.getIdElement().getIdPart()),
				new RangeRequestImpl(payloadSize - 100, null));
		assertTrue(readAllFrom4GibMinus100Bytes.isPresent());
		assertTrue(readAllFrom4GibMinus100Bytes.get().getDataElement() instanceof DeferredBase64BinaryType);
		DeferredBase64BinaryType deferredAllFrom4GibMinus100Bytes = (DeferredBase64BinaryType) readAllFrom4GibMinus100Bytes
				.get().getDataElement();
		CountingOutputStream countingOutputStreamAllFrom4GibMinus100Bytes = new CountingOutputStream(
				NullOutputStream.INSTANCE);
		deferredAllFrom4GibMinus100Bytes.writeExternal(countingOutputStreamAllFrom4GibMinus100Bytes);
		long countAllFrom4GibMinus100Bytes = countingOutputStreamAllFrom4GibMinus100Bytes.getByteCount();
		assertEquals(100, countAllFrom4GibMinus100Bytes);
	}

	@Test
	public void testCreateReadTestRanges() throws Exception
	{
		byte[] data = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };

		Binary resource = new Binary();
		resource.setContentType("text/plain");
		resource.setDataElement(new StreamableBase64BinaryType(new ByteArrayInputStream(data)));

		Binary binary = dao.create(resource);
		assertNotNull(binary);
		assertNotNull(binary.getIdElement());
		assertNotNull(binary.getIdElement().getIdPart());

		Optional<Binary> readRange01 = dao.read(UUID.fromString(binary.getIdElement().getIdPart()),
				new RangeRequestImpl(0L, 1L));
		assertTrue(readRange01.isPresent());
		assertTrue(readRange01.get().getDataElement() instanceof DeferredBase64BinaryType);
		DeferredBase64BinaryType deferredRange01Bytes = (DeferredBase64BinaryType) readRange01.get().getDataElement();
		ByteArrayOutputStream outRange01 = new ByteArrayOutputStream();
		deferredRange01Bytes.writeExternal(outRange01);
		byte[] readOutRange01 = outRange01.toByteArray();
		assertEquals(2, readOutRange01.length);
		assertEquals(0, readOutRange01[0]);
		assertEquals(1, readOutRange01[1]);

		Optional<Binary> readRange23 = dao.read(UUID.fromString(binary.getIdElement().getIdPart()),
				new RangeRequestImpl(2L, 3L));
		assertTrue(readRange23.isPresent());
		assertTrue(readRange23.get().getDataElement() instanceof DeferredBase64BinaryType);
		DeferredBase64BinaryType deferredRange23Bytes = (DeferredBase64BinaryType) readRange23.get().getDataElement();
		ByteArrayOutputStream outRange23 = new ByteArrayOutputStream();
		deferredRange23Bytes.writeExternal(outRange23);
		byte[] readOutRange23 = outRange23.toByteArray();
		assertEquals(2, readOutRange23.length);
		assertEquals(2, readOutRange23[0]);
		assertEquals(3, readOutRange23[1]);

		Optional<Binary> readLast2Bytes = dao.read(UUID.fromString(binary.getIdElement().getIdPart()),
				new RangeRequestImpl(null, -2L));
		assertTrue(readLast2Bytes.isPresent());
		assertTrue(readLast2Bytes.get().getDataElement() instanceof DeferredBase64BinaryType);
		DeferredBase64BinaryType deferredLast2Bytes = (DeferredBase64BinaryType) readLast2Bytes.get().getDataElement();
		ByteArrayOutputStream outLast2Bytes = new ByteArrayOutputStream();
		deferredLast2Bytes.writeExternal(outLast2Bytes);
		byte[] last2Bytes = outLast2Bytes.toByteArray();
		assertEquals(2, last2Bytes.length);
		assertEquals(8, last2Bytes[0]);
		assertEquals(9, last2Bytes[1]);

		Optional<Binary> readRange4End = dao.read(UUID.fromString(binary.getIdElement().getIdPart()),
				new RangeRequestImpl(4L, null));
		assertTrue(readRange4End.isPresent());
		assertTrue(readRange4End.get().getDataElement() instanceof DeferredBase64BinaryType);
		DeferredBase64BinaryType deferredRange4EndBytes = (DeferredBase64BinaryType) readRange4End.get()
				.getDataElement();
		ByteArrayOutputStream outRange4EndBytes = new ByteArrayOutputStream();
		deferredRange4EndBytes.writeExternal(outRange4EndBytes);
		byte[] readOutRange4EndBytes = outRange4EndBytes.toByteArray();
		assertEquals(6, readOutRange4EndBytes.length);
		assertEquals(4, readOutRange4EndBytes[0]);
		assertEquals(5, readOutRange4EndBytes[1]);
		assertEquals(6, readOutRange4EndBytes[2]);
		assertEquals(7, readOutRange4EndBytes[3]);
		assertEquals(8, readOutRange4EndBytes[4]);
		assertEquals(9, readOutRange4EndBytes[5]);
	}
}
