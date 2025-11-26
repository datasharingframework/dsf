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

import static org.junit.Assert.assertNotNull;

import java.util.UUID;

import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;

public class TaskTest
{
	private static final Logger logger = LoggerFactory.getLogger(TaskTest.class);

	@Test
	public void testRequester() throws Exception
	{
		Task t = new Task();
		t.setRequester(new Reference("Organization/" + UUID.randomUUID().toString()));

		FhirContext context = FhirContext.forR4();

		String txt = context.newJsonParser().setPrettyPrint(true).encodeResourceToString(t);
		assertNotNull(txt);
		logger.debug(txt);
	}

	@Test
	public void testRestrictionRecipient() throws Exception
	{
		Task t = new Task();
		t.getRestriction().addRecipient(new Reference("Organization/" + UUID.randomUUID().toString()));

		FhirContext context = FhirContext.forR4();

		String txt = context.newJsonParser().setPrettyPrint(true).encodeResourceToString(t);
		assertNotNull(txt);
		logger.debug(txt);
	}
}
