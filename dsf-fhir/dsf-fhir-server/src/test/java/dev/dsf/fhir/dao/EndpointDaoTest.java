package dev.dsf.fhir.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import java.util.UUID;

import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Endpoint.EndpointStatus;
import org.hl7.fhir.r4.model.StringType;
import org.junit.Test;

import dev.dsf.fhir.dao.jdbc.EndpointDaoJdbc;

public class EndpointDaoTest extends AbstractReadAccessDaoTest<Endpoint, EndpointDao>
{
	private static final String name = "Demo Endpoint Name";
	private static final String address = "https://foo.bar/baz";

	public EndpointDaoTest()
	{
		super(Endpoint.class, EndpointDaoJdbc::new);
	}

	@Override
	public Endpoint createResource()
	{
		Endpoint endpoint = new Endpoint();
		endpoint.setName(name);
		return endpoint;
	}

	@Override
	protected void checkCreated(Endpoint resource)
	{
		assertEquals(name, resource.getName());
	}

	@Override
	protected Endpoint updateResource(Endpoint resource)
	{
		resource.setAddress(address);
		return resource;
	}

	@Override
	protected void checkUpdates(Endpoint resource)
	{
		assertEquals(address, resource.getAddress());
	}

	@Test
	public void testExistsActiveNotDeletedByAddress() throws Exception
	{
		String address = "http://test/fhir";

		Endpoint e = new Endpoint();
		e.setStatus(EndpointStatus.ACTIVE);
		e.setAddress(address);

		Endpoint created = dao.create(e);
		assertNotNull(created);

		assertTrue(dao.existsActiveNotDeletedByAddress(address));
	}

	@Test
	public void testExistsActiveNotDeletedByAddressNotActive() throws Exception
	{
		String address = "http://test/fhir";

		Endpoint e = new Endpoint();
		e.setStatus(EndpointStatus.OFF);
		e.setAddress(address);

		Endpoint created = dao.create(e);
		assertNotNull(created);

		assertFalse(dao.existsActiveNotDeletedByAddress(address));
	}

	@Test
	public void testExistsActiveNotDeletedByAddressDeleted() throws Exception
	{
		String address = "http://test/fhir";

		Endpoint e = new Endpoint();
		e.setStatus(EndpointStatus.ACTIVE);
		e.setAddress(address);

		Endpoint created = dao.create(e);
		assertNotNull(created);
		dao.delete(UUID.fromString(created.getIdElement().getIdPart()));

		assertFalse(dao.existsActiveNotDeletedByAddress(address));
	}

	@Test
	public void testReadActiveNotDeletedByAddress() throws Exception
	{
		String address = "http://test/fhir";

		Endpoint e = new Endpoint();
		e.setStatus(EndpointStatus.ACTIVE);
		e.setAddress(address);

		Endpoint created = dao.create(e);
		assertNotNull(created);

		Optional<Endpoint> readE = dao.readActiveNotDeletedByAddress(address);
		assertNotNull(readE);
		assertTrue(readE.isPresent());
		assertEquals(address, readE.map(Endpoint::getAddress).get());
	}

	@Test
	public void testReadActiveNotDeletedByAddressNotActive() throws Exception
	{
		String address = "http://test/fhir";

		Endpoint e = new Endpoint();
		e.setStatus(EndpointStatus.OFF);
		e.setAddress(address);

		Endpoint created = dao.create(e);
		assertNotNull(created);

		Optional<Endpoint> readE = dao.readActiveNotDeletedByAddress(address);
		assertNotNull(readE);
		assertTrue(readE.isEmpty());
	}

	@Test
	public void testReadActiveNotDeletedByAddressDeleted() throws Exception
	{
		String address = "http://test/fhir";

		Endpoint e = new Endpoint();
		e.setStatus(EndpointStatus.ACTIVE);
		e.setAddress(address);

		Endpoint created = dao.create(e);
		assertNotNull(created);
		dao.delete(UUID.fromString(created.getIdElement().getIdPart()));

		Optional<Endpoint> readE = dao.readActiveNotDeletedByAddress(address);
		assertNotNull(readE);
		assertTrue(readE.isEmpty());
	}

	@Test
	public void testReadActiveNotDeletedByThumbprint() throws Exception
	{
		String thumbprint = "00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";

		Endpoint e = new Endpoint();
		e.setStatus(EndpointStatus.ACTIVE);
		e.setAddress(address);
		e.addExtension().setUrl("http://dsf.dev/fhir/StructureDefinition/extension-certificate-thumbprint")
				.setValue(new StringType(thumbprint));

		Endpoint created = dao.create(e);
		assertNotNull(created);

		Optional<Endpoint> readE = dao.readActiveNotDeletedByAddress(address);
		assertNotNull(readE);
		assertTrue(readE.isPresent());
		assertEquals(address, readE.map(Endpoint::getAddress).get());
		assertEquals(thumbprint,
				((StringType) readE.get()
						.getExtensionByUrl("http://dsf.dev/fhir/StructureDefinition/extension-certificate-thumbprint")
						.getValue()).getValue());
	}

	@Test
	public void testReadActiveNotDeletedByThumbprintNotActive() throws Exception
	{
		String thumbprint = "00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";

		Endpoint e = new Endpoint();
		e.setStatus(EndpointStatus.OFF);
		e.setAddress(address);
		e.addExtension().setUrl("http://dsf.dev/fhir/StructureDefinition/extension-certificate-thumbprint")
				.setValue(new StringType(thumbprint));

		Endpoint created = dao.create(e);
		assertNotNull(created);

		Optional<Endpoint> readE = dao.readActiveNotDeletedByAddress(address);
		assertNotNull(readE);
		assertTrue(readE.isEmpty());
	}

	@Test
	public void testReadActiveNotDeletedByThumbprintDeleted() throws Exception
	{
		String thumbprint = "00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";

		Endpoint e = new Endpoint();
		e.setStatus(EndpointStatus.ACTIVE);
		e.setAddress(address);
		e.addExtension().setUrl("http://dsf.dev/fhir/StructureDefinition/extension-certificate-thumbprint")
				.setValue(new StringType(thumbprint));

		Endpoint created = dao.create(e);
		assertNotNull(created);
		dao.delete(UUID.fromString(created.getIdElement().getIdPart()));

		Optional<Endpoint> readE = dao.readActiveNotDeletedByAddress(address);
		assertNotNull(readE);
		assertTrue(readE.isEmpty());
	}
}
