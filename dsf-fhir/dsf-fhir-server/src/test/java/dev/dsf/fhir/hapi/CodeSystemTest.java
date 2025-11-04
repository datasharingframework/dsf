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

import java.util.Date;

import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeSystem.CodeSystemContentMode;
import org.hl7.fhir.r4.model.CodeSystem.CodeSystemHierarchyMeaning;
import org.hl7.fhir.r4.model.CodeSystem.ConceptDefinitionComponent;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;

public class CodeSystemTest
{
	private static final Logger logger = LoggerFactory.getLogger(CodeSystemTest.class);

	private static FhirContext fhirContext = FhirContext.forR4();

	@Test
	public void testCodeSystem() throws Exception
	{
		CodeSystem codeSystem = new CodeSystem();
		codeSystem.setUrl("http://dsf.dev/fhir/CodeSystem/organization-role");
		codeSystem.setVersion("0.1.0");
		codeSystem.setName("DSF_Organization_Type");
		codeSystem.setTitle("DSF Organization Type");
		codeSystem.setStatus(PublicationStatus.ACTIVE);
		codeSystem.setExperimental(false);
		codeSystem.setDate(new Date());
		codeSystem.setPublisher("DSF");
		codeSystem.setCaseSensitive(true);
		codeSystem.setContent(CodeSystemContentMode.COMPLETE);
		codeSystem.setVersionNeeded(false);
		codeSystem.setHierarchyMeaning(CodeSystemHierarchyMeaning.GROUPEDBY);
		ConceptDefinitionComponent c1 = codeSystem.addConcept();
		c1.setCode("TTP");
		c1.setDefinition("Trusted Third Party");
		ConceptDefinitionComponent c2 = codeSystem.addConcept();
		c2.setCode("DIC");
		c2.setDefinition("Data Integration Center");

		String s = fhirContext.newXmlParser().setPrettyPrint(true).encodeResourceToString(codeSystem);
		logger.debug(s);
	}
}
