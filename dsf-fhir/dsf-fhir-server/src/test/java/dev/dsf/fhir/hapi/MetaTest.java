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
import static org.junit.Assert.assertTrue;

import org.hl7.fhir.r4.model.CodeSystem;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;

public class MetaTest
{
	private static final Logger logger = LoggerFactory.getLogger(MetaTest.class);

	@Test
	public void testMetaTag() throws Exception
	{
		CodeSystem c = new CodeSystem();
		c.getMeta().addTag().setSystem("http://system.com/foo").setCode("TAG_CODE");

		FhirContext context = FhirContext.forR4();
		String string = context.newJsonParser().encodeResourceToString(c);

		logger.info(string);

		CodeSystem c2 = context.newJsonParser().parseResource(CodeSystem.class, string);
		assertTrue(c2.hasMeta());
		assertTrue(c2.getMeta().hasTag());
		assertEquals(1, c2.getMeta().getTag().size());
		assertEquals(c.getMeta().getTagFirstRep().getSystem(), c2.getMeta().getTagFirstRep().getSystem());
	}
}
