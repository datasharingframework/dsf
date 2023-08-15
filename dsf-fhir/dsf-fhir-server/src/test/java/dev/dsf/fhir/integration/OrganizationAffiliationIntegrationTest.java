package dev.dsf.fhir.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.UUID;

import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Endpoint.EndpointStatus;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.OrganizationAffiliation;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.Test;

import dev.dsf.fhir.dao.EndpointDao;
import dev.dsf.fhir.dao.OrganizationDao;

public class OrganizationAffiliationIntegrationTest extends AbstractIntegrationTest
{
	private final OrganizationDao organizationDao = getSpringWebApplicationContext().getBean(OrganizationDao.class);
	private final EndpointDao endpointDao = getSpringWebApplicationContext().getBean(EndpointDao.class);

	@Test
	public void testCreateAllowed() throws Exception
	{
		Organization p = organizationDao.create(createParentOrganization());
		assertNotNull(p);
		assertTrue(p.hasIdElement());
		Organization m = organizationDao.create(createMemberOrganization());
		assertNotNull(m);
		assertTrue(m.hasIdElement());
		Endpoint eDic = endpointDao.create(createEndpoint("dic.endpoint"));
		assertNotNull(eDic);
		assertTrue(eDic.hasIdElement());
		Endpoint eDms = endpointDao.create(createEndpoint("dms.endpoint"));
		assertNotNull(eDms);
		assertTrue(eDms.hasIdElement());

		OrganizationAffiliation aDic = getWebserviceClient().create(createOrganizationAffiliation(p, m, eDic, "DIC"));
		assertNotNull(aDic);
		assertTrue(aDic.hasIdElement());

		OrganizationAffiliation aDms = getWebserviceClient().create(createOrganizationAffiliation(p, m, eDms, "DMS"));
		assertNotNull(aDms);
		assertTrue(aDms.hasIdElement());
	}

	@Test
	public void testCreateForbiddenResourceExists1() throws Exception
	{
		Organization p = organizationDao.create(createParentOrganization());
		assertNotNull(p);
		assertTrue(p.hasIdElement());
		Organization m = organizationDao.create(createMemberOrganization());
		assertNotNull(m);
		assertTrue(m.hasIdElement());
		Endpoint e = endpointDao.create(createEndpoint("endpoint"));
		assertNotNull(e);
		assertTrue(e.hasIdElement());

		OrganizationAffiliation aDic = getWebserviceClient().create(createOrganizationAffiliation(p, m, e, "DIC"));
		assertNotNull(aDic);
		assertTrue(aDic.hasIdElement());

		expectForbidden(() -> getWebserviceClient().create(createOrganizationAffiliation(p, m, e, "DMS")));
	}

	@Test
	public void testCreateForbiddenResourceExists2() throws Exception
	{
		Organization p = organizationDao.create(createParentOrganization());
		assertNotNull(p);
		assertTrue(p.hasIdElement());
		Organization m = organizationDao.create(createMemberOrganization());
		assertNotNull(m);
		assertTrue(m.hasIdElement());
		Endpoint eDic = endpointDao.create(createEndpoint("dic.endpoint"));
		assertNotNull(eDic);
		assertTrue(eDic.hasIdElement());
		Endpoint eDms = endpointDao.create(createEndpoint("dms.endpoint"));
		assertNotNull(eDms);
		assertTrue(eDms.hasIdElement());

		OrganizationAffiliation aDic = getWebserviceClient().create(createOrganizationAffiliation(p, m, eDic, "DIC"));
		assertNotNull(aDic);
		assertTrue(aDic.hasIdElement());

		expectForbidden(() -> getWebserviceClient().create(createOrganizationAffiliation(p, m, eDms, "DIC")));
	}

	@Test
	public void testUpdateAllowed() throws Exception
	{
		Organization p = organizationDao.create(createParentOrganization());
		assertNotNull(p);
		assertTrue(p.hasIdElement());
		Organization m = organizationDao.create(createMemberOrganization());
		assertNotNull(m);
		assertTrue(m.hasIdElement());
		Endpoint eDic = endpointDao.create(createEndpoint("dic.endpoint"));
		assertNotNull(eDic);
		assertTrue(eDic.hasIdElement());
		Endpoint eDms = endpointDao.create(createEndpoint("dms.endpoint"));
		assertNotNull(eDms);
		assertTrue(eDms.hasIdElement());

		OrganizationAffiliation aDic = getWebserviceClient().create(createOrganizationAffiliation(p, m, eDic, "DIC"));
		assertNotNull(aDic);
		assertTrue(aDic.hasIdElement());

		OrganizationAffiliation aDms = getWebserviceClient().create(createOrganizationAffiliation(p, m, eDms, "DMS"));
		assertNotNull(aDms);
		assertTrue(aDms.hasIdElement());

		aDic.getCodeFirstRep().getCodingFirstRep().setCode("TTP");
		OrganizationAffiliation updated = getWebserviceClient().update(aDic);
		assertNotNull(updated);
		assertEquals("2", updated.getIdElement().getVersionIdPart());
	}

	@Test
	public void testUpdateForbiddenResourceExists() throws Exception
	{
		Organization p = organizationDao.create(createParentOrganization());
		assertNotNull(p);
		assertTrue(p.hasIdElement());
		Organization m = organizationDao.create(createMemberOrganization());
		assertNotNull(m);
		assertTrue(m.hasIdElement());
		Endpoint eDicDms = endpointDao.create(createEndpoint("dic.endpoint"));
		assertNotNull(eDicDms);
		assertTrue(eDicDms.hasIdElement());
		Endpoint eTtp = endpointDao.create(createEndpoint("ttp.endpoint"));
		assertNotNull(eTtp);
		assertTrue(eTtp.hasIdElement());

		OrganizationAffiliation aDicDms = getWebserviceClient()
				.create(createOrganizationAffiliation(p, m, eDicDms, "DIC", "DMS"));
		assertNotNull(aDicDms);
		assertTrue(aDicDms.hasIdElement());

		OrganizationAffiliation aTtp = getWebserviceClient().create(createOrganizationAffiliation(p, m, eTtp, "TTP"));
		assertNotNull(aTtp);
		assertTrue(aTtp.hasIdElement());

		aTtp.getCodeFirstRep().getCodingFirstRep().setCode("DMS");
		expectForbidden(() -> getWebserviceClient().update(aTtp));
	}

	@Test
	public void testUpdateForbiddenParentModified() throws Exception
	{
		Organization p = organizationDao.create(createParentOrganization());
		assertNotNull(p);
		assertTrue(p.hasIdElement());
		Organization m = organizationDao.create(createMemberOrganization());
		assertNotNull(m);
		assertTrue(m.hasIdElement());
		Endpoint e = endpointDao.create(createEndpoint("endpoint"));
		assertNotNull(e);

		OrganizationAffiliation a = getWebserviceClient().create(createOrganizationAffiliation(p, m, e, "DIC"));
		assertNotNull(a);
		assertTrue(a.hasIdElement());

		a.getOrganization().setReference("Organization/" + UUID.randomUUID().toString());
		expectForbidden(() -> getWebserviceClient().update(a));
	}

	@Test
	public void testUpdateForbiddenMemberModified() throws Exception
	{
		Organization p = organizationDao.create(createParentOrganization());
		assertNotNull(p);
		assertTrue(p.hasIdElement());
		Organization m = organizationDao.create(createMemberOrganization());
		assertNotNull(m);
		assertTrue(m.hasIdElement());
		Endpoint e = endpointDao.create(createEndpoint("endpoint"));
		assertNotNull(e);

		OrganizationAffiliation a = getWebserviceClient().create(createOrganizationAffiliation(p, m, e, "DIC"));
		assertNotNull(a);
		assertTrue(a.hasIdElement());

		a.getParticipatingOrganization().setReference("Organization/" + UUID.randomUUID().toString());
		expectForbidden(() -> getWebserviceClient().update(a));
	}

	@Test
	public void testUpdateForbiddenEndpointModified() throws Exception
	{
		Organization p = organizationDao.create(createParentOrganization());
		assertNotNull(p);
		assertTrue(p.hasIdElement());
		Organization m = organizationDao.create(createMemberOrganization());
		assertNotNull(m);
		assertTrue(m.hasIdElement());
		Endpoint e = endpointDao.create(createEndpoint("endpoint"));
		assertNotNull(e);

		OrganizationAffiliation a = getWebserviceClient().create(createOrganizationAffiliation(p, m, e, "DIC"));
		assertNotNull(a);
		assertTrue(a.hasIdElement());

		a.getEndpointFirstRep().setReference("Endpoint/" + UUID.randomUUID().toString());
		expectForbidden(() -> getWebserviceClient().update(a));
	}

	private OrganizationAffiliation createOrganizationAffiliation(Organization parent, Organization member,
			Endpoint endpoint, String... codes)
	{
		OrganizationAffiliation a = new OrganizationAffiliation();
		a.setActive(true);
		a.getOrganization().setType(ResourceType.Organization.name())
				.setReference(parent.getIdElement().toVersionless().getValue());
		a.getParticipatingOrganization().setType(ResourceType.Organization.name())
				.setReference(member.getIdElement().toVersionless().getValue());
		a.addEndpoint().setType(ResourceType.Endpoint.name())
				.setReference(endpoint.getIdElement().toVersionless().getValue());

		Arrays.stream(codes).forEach(
				c -> a.addCode().addCoding().setSystem("http://dsf.dev/fhir/CodeSystem/organization-role").setCode(c));

		return getReadAccessHelper().addAll(a);
	}

	private Organization createParentOrganization()
	{
		Organization p = new Organization();
		p.addIdentifier().setSystem("http://dsf.dev/sid/organization-identifier").setValue("parent.org");
		p.setActive(true);

		return getReadAccessHelper().addAll(p);
	}

	private Organization createMemberOrganization()
	{
		Organization m = new Organization();
		m.addIdentifier().setSystem("http://dsf.dev/sid/organization-identifier").setValue("member.org");
		m.setActive(true);

		return getReadAccessHelper().addAll(m);
	}

	private Endpoint createEndpoint(String subdomain)
	{
		Endpoint e = new Endpoint();
		e.addIdentifier().setSystem("http://dsf.dev/sid/endpoint-identifier").setValue(subdomain + ".member.org");
		e.setAddress("https://" + subdomain + ".member.org/fhir");

		e.setStatus(EndpointStatus.ACTIVE);
		e.getConnectionType().setSystem("http://terminology.hl7.org/CodeSystem/endpoint-connection-type")
				.setCode("hl7-fhir-rest");
		e.getPayloadTypeFirstRep().getCodingFirstRep().setSystem("http://hl7.org/fhir/resource-types").setCode("Task");

		return getReadAccessHelper().addAll(e);
	}
}
