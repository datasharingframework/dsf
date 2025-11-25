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
package dev.dsf.fhir.hapi;

import static org.junit.Assert.assertEquals;

import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.junit.Test;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

public class SerializationTest
{
	@Test
	public void testSerializationReferenceWithVersionJson() throws Exception
	{
		FhirContext context = FhirContext.forR4();

		Reference ref = new Reference("http://foo.bar/fhir/Organization/id/_history/vid");
		Task task = new Task().setRequester(ref);
		IParser parser = context.newJsonParser();

		// versions will be striped unless we set stripVersionsFromReferences to false
		parser.setStripVersionsFromReferences(false);

		String json = parser.encodeResourceToString(task);
		Task task2 = parser.parseResource(Task.class, json);

		assertEquals(task.getRequester().getReference(), task2.getRequester().getReference());
	}

	@Test
	public void testSerializationReferenceWithVersionXml() throws Exception
	{
		FhirContext context = FhirContext.forR4();

		Reference ref = new Reference("http://foo.bar/fhir/Organization/id/_history/vid");
		Task task = new Task().setRequester(ref);
		IParser parser = context.newXmlParser();

		// versions will be striped unless we set stripVersionsFromReferences to false
		parser.setStripVersionsFromReferences(false);

		String json = parser.encodeResourceToString(task);
		Task task2 = parser.parseResource(Task.class, json);

		assertEquals(task.getRequester().getReference(), task2.getRequester().getReference());
	}

	@Test
	public void testSerializationReferenceDifferentServerJson() throws Exception
	{
		FhirContext context = FhirContext.forR4();

		Reference ref = new Reference("http://foo.bar/fhir/Organization/id");
		Task task = new Task().setRequester(ref);
		IParser parser = context.newJsonParser();
		parser.setServerBaseUrl("http://baz.bar/fhir");

		String json = parser.encodeResourceToString(task);
		Task task2 = parser.parseResource(Task.class, json);

		assertEquals(task.getRequester().getReference(), task2.getRequester().getReference());
	}

	@Test
	public void testSerializationReferenceSameServerJson() throws Exception
	{
		FhirContext context = FhirContext.forR4();

		Reference ref = new Reference("http://foo.bar/fhir/Organization/id");
		Task task = new Task().setRequester(ref);
		IParser parser = context.newJsonParser();
		parser.setServerBaseUrl("http://foo.bar/fhir");

		String json = parser.encodeResourceToString(task);
		Task task2 = parser.parseResource(Task.class, json);

		assertEquals(task.getRequester().getReference(), "http://foo.bar/fhir/" + task2.getRequester().getReference());
	}
}
