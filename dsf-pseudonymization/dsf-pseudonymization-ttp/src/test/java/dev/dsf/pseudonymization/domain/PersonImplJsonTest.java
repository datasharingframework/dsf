package dev.dsf.pseudonymization.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.BitSet;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.openehr.model.datatypes.DoubleRowElement;
import dev.dsf.openehr.model.datatypes.StringRowElement;
import dev.dsf.openehr.model.datatypes.ZonedDateTimeRowElement;
import dev.dsf.pseudonymization.domain.impl.FhirMdatContainer;
import dev.dsf.pseudonymization.domain.impl.MedicIdImpl;
import dev.dsf.pseudonymization.domain.impl.OpenEhrMdatContainer;
import dev.dsf.pseudonymization.domain.impl.PersonImpl;
import dev.dsf.pseudonymization.domain.json.TtpObjectMapperFactory;
import dev.dsf.pseudonymization.recordlinkage.MedicId;

public class PersonImplJsonTest
{
	private static final Logger logger = LoggerFactory.getLogger(PersonImplJsonTest.class);

	private ObjectMapper objectMapper = TtpObjectMapperFactory.createObjectMapper(FhirContext.forR4());

	@Test
	public void testWriteReadOpenEhr() throws Exception
	{
		MedicId medicId = new MedicIdImpl("org", "value");
		BitSet recordBloomFilter = new BitSet(5000);
		recordBloomFilter.set(200);
		recordBloomFilter.set(4850);
		OpenEhrMdatContainer mdatContainer = new OpenEhrMdatContainer(Arrays.asList(new StringRowElement("string"),
				new DoubleRowElement(0.1), new ZonedDateTimeRowElement(ZonedDateTime.now())));

		PersonWithMdat person = new PersonImpl(medicId, recordBloomFilter, mdatContainer);

		String value = objectMapper.writeValueAsString(person);
		assertNotNull(value);

		logger.debug("person: {}", value);

		PersonWithMdat readPerson = objectMapper.readValue(value, PersonImpl.class);
		assertNotNull(readPerson);

		assertNotNull(readPerson.getMedicId());
		assertEquals(person.getMedicId().getOrganization(), readPerson.getMedicId().getOrganization());
		assertEquals(person.getMedicId().getValue(), readPerson.getMedicId().getValue());
		assertNotNull(readPerson.getRecordBloomFilter());
		assertEquals(person.getRecordBloomFilter(), readPerson.getRecordBloomFilter());
		assertNotNull(readPerson.getMdatContainer());
		assertEquals(person.getMdatContainer().getClass(), readPerson.getMdatContainer().getClass());
		OpenEhrMdatContainer readMdatContainer = (OpenEhrMdatContainer) readPerson.getMdatContainer();
		assertNotNull(readMdatContainer.getElements());
		assertEquals(mdatContainer.getElements().size(), readMdatContainer.getElements().size());
		for (int i = 0; i < mdatContainer.getElements().size(); i++)
		{
			assertNotNull(readMdatContainer.getElements().get(0));
			assertEquals(mdatContainer.getElements().get(i).getClass(),
					readMdatContainer.getElements().get(i).getClass());
		}
	}

	@Test
	public void testWriteReadFhir() throws Exception
	{
		MedicId medicId = new MedicIdImpl("org", "value");
		BitSet recordBloomFilter = new BitSet(5000);
		recordBloomFilter.set(200);
		recordBloomFilter.set(4850);
		FhirMdatContainer mdatContainer = new FhirMdatContainer(
				Arrays.asList(new Bundle().setType(BundleType.SEARCHSET), new Bundle().setType(BundleType.SEARCHSET)));

		PersonWithMdat person = new PersonImpl(medicId, recordBloomFilter, mdatContainer);

		String value = objectMapper.writeValueAsString(person);
		assertNotNull(value);

		logger.debug("person: {}", value);

		PersonWithMdat readPerson = objectMapper.readValue(value, PersonImpl.class);
		assertNotNull(readPerson);

		assertNotNull(readPerson.getMedicId());
		assertEquals(person.getMedicId().getOrganization(), readPerson.getMedicId().getOrganization());
		assertEquals(person.getMedicId().getValue(), readPerson.getMedicId().getValue());
		assertNotNull(readPerson.getRecordBloomFilter());
		assertEquals(person.getRecordBloomFilter(), readPerson.getRecordBloomFilter());
		assertNotNull(readPerson.getMdatContainer());
		assertEquals(person.getMdatContainer().getClass(), readPerson.getMdatContainer().getClass());
		FhirMdatContainer readMdatContainer = (FhirMdatContainer) readPerson.getMdatContainer();
		assertNotNull(readMdatContainer.getElements());
		assertEquals(mdatContainer.getElements().size(), readMdatContainer.getElements().size());
		for (int i = 0; i < mdatContainer.getElements().size(); i++)
		{
			assertNotNull(readMdatContainer.getElements().get(0));
			assertEquals(mdatContainer.getElements().get(i).getClass(),
					readMdatContainer.getElements().get(i).getClass());
		}
	}
}
