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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.fhir.context.FhirContext;

public class TargetsJsonSerializationTest
{
	private static final Logger logger = LoggerFactory.getLogger(TargetsJsonSerializationTest.class);

	@Test
	public void testEmptyTargetsSerialization() throws Exception
	{
		ObjectMapper mapper = ObjectMapperFactory.createObjectMapper(FhirContext.forR4());

		Targets targets = new TargetsImpl(List.of());

		String targetsAsString = mapper.writeValueAsString(targets);
		assertNotNull(targetsAsString);

		logger.debug("Empty targests json: '{}'", targetsAsString);

		Targets readTargets = mapper.readValue(targetsAsString, TargetsImpl.class);
		assertNotNull(readTargets);
		assertTrue(readTargets.isEmpty());
	}

	@Test
	public void testTargetsWithBiDirectionalTargetSerialization() throws Exception
	{
		ObjectMapper mapper = ObjectMapperFactory.createObjectMapper(FhirContext.forR4());

		TargetImpl target = new TargetImpl("target.org", "endpoint.target.org", "https://endpoint.target.org/fhir",
				UUID.randomUUID().toString());
		Targets targets = new TargetsImpl(List.of(target));

		String targetsAsString = mapper.writeValueAsString(targets);
		assertNotNull(targetsAsString);

		logger.debug("Targets with bi-directional target json: '{}'", targetsAsString);

		Targets readTargets = mapper.readValue(targetsAsString, TargetsImpl.class);
		assertNotNull(readTargets);
		assertFalse(readTargets.isEmpty());
		assertNotNull(readTargets.getEntries());
		assertEquals(1, readTargets.getEntries().size());

		targetEquals(target, readTargets.getEntries().get(0));
	}

	@Test
	public void testTargetsWithUniDirectionalTargetSerialization() throws Exception
	{
		ObjectMapper mapper = ObjectMapperFactory.createObjectMapper(FhirContext.forR4());

		TargetImpl target = new TargetImpl("target.org", "endpoint.target.org", "https://endpoint.target.org/fhir",
				null);
		Targets targets = new TargetsImpl(List.of(target));

		String targetsAsString = mapper.writeValueAsString(targets);
		assertNotNull(targetsAsString);

		logger.debug("Targets with Uni directional target json: '{}'", targetsAsString);

		Targets readTargets = mapper.readValue(targetsAsString, TargetsImpl.class);
		assertNotNull(readTargets);
		assertFalse(readTargets.isEmpty());
		assertNotNull(readTargets.getEntries());
		assertEquals(1, readTargets.getEntries().size());

		targetEquals(target, readTargets.getEntries().get(0));
	}

	private void targetEquals(Target expected, Target actual)
	{
		assertEquals(expected.getCorrelationKey(), actual.getCorrelationKey());
		assertEquals(expected.getEndpointIdentifierValue(), actual.getEndpointIdentifierValue());
		assertEquals(expected.getEndpointUrl(), actual.getEndpointUrl());
		assertEquals(expected.getOrganizationIdentifierValue(), actual.getOrganizationIdentifierValue());
	}

	@Test
	public void testReadOldJson() throws Exception
	{
		ObjectMapper mapper = ObjectMapperFactory.createObjectMapper(FhirContext.forR4());

		String oldJson = "{\"entries\":[{\"targetOrganizationIdentifierValue\":\"target.org\",\"endpointIdentifierValue\":\"endpoint.target.org\",\"targetEndpointUrl\":\"https://endpoint.target.org/fhir\"}]}";

		Targets targets = mapper.readValue(oldJson, TargetsImpl.class);
		assertNotNull(targets);
		assertNotNull(targets.getEntries());
		assertEquals(1, targets.getEntries().size());
		assertEquals("target.org", targets.getEntries().get(0).getOrganizationIdentifierValue());
		assertEquals("endpoint.target.org", targets.getEntries().get(0).getEndpointIdentifierValue());
		assertEquals("https://endpoint.target.org/fhir", targets.getEntries().get(0).getEndpointUrl());
	}

	@Test
	public void testReadNewJson() throws Exception
	{
		ObjectMapper mapper = ObjectMapperFactory.createObjectMapper(FhirContext.forR4());

		String oldJson = "{\"entries\":[{\"organizationIdentifierValue\":\"target.org\",\"endpointIdentifierValue\":\"endpoint.target.org\",\"endpointUrl\":\"https://endpoint.target.org/fhir\"}]}";

		Targets targets = mapper.readValue(oldJson, TargetsImpl.class);
		assertNotNull(targets);
		assertNotNull(targets.getEntries());
		assertEquals(1, targets.getEntries().size());
		assertEquals("target.org", targets.getEntries().get(0).getOrganizationIdentifierValue());
		assertEquals("endpoint.target.org", targets.getEntries().get(0).getEndpointIdentifierValue());
		assertEquals("https://endpoint.target.org/fhir", targets.getEntries().get(0).getEndpointUrl());
	}
}
