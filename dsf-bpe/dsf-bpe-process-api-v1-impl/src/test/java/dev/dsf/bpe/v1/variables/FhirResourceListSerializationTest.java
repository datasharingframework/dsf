/*
 * Copyright 2018-2025 Heilbronn University of Applied Sciences
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.dsf.bpe.v1.variables;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Task;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.fhir.context.FhirContext;

public class FhirResourceListSerializationTest
{
	private static final Logger logger = LoggerFactory.getLogger(FhirResourceListSerializationTest.class);

	@Test
	public void testEmptyFhirResourceListSerialization() throws Exception
	{
		ObjectMapper mapper = ObjectMapperFactory.createObjectMapper(FhirContext.forR4());

		FhirResourcesList list = new FhirResourcesList();

		String listAsString = mapper.writeValueAsString(list);
		assertNotNull(listAsString);

		logger.debug("Empty fhir resource list json: '{}'", listAsString);

		FhirResourcesList readList = mapper.readValue(listAsString, FhirResourcesList.class);
		assertNotNull(readList);
		assertNotNull(readList.getResources());
		assertNotNull(readList.getResourcesAndCast());
		assertTrue(readList.getResources().isEmpty());
	}

	@Test
	public void testNonEmptyFhirResourceListSerialization() throws Exception
	{
		ObjectMapper mapper = ObjectMapperFactory.createObjectMapper(FhirContext.forR4());

		Task task = new Task();
		Patient patient = new Patient();
		FhirResourcesList list = new FhirResourcesList(task, patient);

		String listAsString = mapper.writeValueAsString(list);
		assertNotNull(listAsString);

		logger.debug("Non empty fhir resource list json: '{}'", listAsString);

		FhirResourcesList readList = mapper.readValue(listAsString, FhirResourcesList.class);
		assertNotNull(readList);
		assertNotNull(readList.getResources());
		assertNotNull(readList.getResourcesAndCast());
		assertEquals(2, readList.getResources().size());
	}
}
