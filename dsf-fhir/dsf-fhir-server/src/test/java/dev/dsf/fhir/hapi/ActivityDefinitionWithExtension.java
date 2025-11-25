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

import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.ActivityDefinition.ActivityDefinitionKind;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.StringType;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;

public class ActivityDefinitionWithExtension
{
	private static final Logger logger = LoggerFactory.getLogger(ActivityDefinitionWithExtension.class);

	@Test
	public void test() throws Exception
	{
		ActivityDefinition a = new ActivityDefinition();
		a.getMeta().addTag("http://dsf.dev/fhir/CodeSystem/authorization-role", "REMOTE", null);
		a.setUrl("http://dsf.dev/bpe/Process/ping");
		a.setVersion("1.0.0");
		a.setName("PingProcess");
		a.setTitle("PING process");
		a.setSubtitle("Communication Testing Process");
		a.setStatus(PublicationStatus.DRAFT);
		a.setExperimental(true);
		a.setDate(new Date());
		a.setPublisher("DSF");
		a.getContactFirstRep().setName("DSF").getTelecomFirstRep().setSystem(ContactPointSystem.EMAIL)
				.setValue("pmo@dsf.dev");
		a.setDescription(
				"Process to send PING messages to remote Organizations and to receive corresponding PONG message");
		a.setKind(ActivityDefinitionKind.TASK);

		Extension e1 = a.addExtension();
		e1.setUrl("http://dsf.dev/fhir/StructureDefinition/extension-process-authorization");
		e1.addExtension("message-name", new StringType("startPingProcessMessage"));
		e1.addExtension("authorization-role",
				new Coding("http://dsf.dev/fhir/CodeSystem/authorization-role", "LOCAL", null));
		Extension ot12 = e1.addExtension();
		ot12.setUrl("organization-types");
		ot12.addExtension("organization-type",
				new Coding("http://dsf.dev/fhir/CodeSystem/authorization-role", "TTP", null));
		ot12.addExtension("organization-type",
				new Coding("http://dsf.dev/fhir/CodeSystem/authorization-role", "DIC", null));
		e1.addExtension("task-profile",
				new CanonicalType("http://dsf.dev/fhir/StructureDefinition/task-start-ping-process"));

		Extension e2 = a.addExtension();
		e2.setUrl("http://dsf.dev/fhir/StructureDefinition/extension-process-authorization");
		e2.addExtension("message-name", new StringType("pongMessage"));
		e2.addExtension("authorization-role",
				new Coding("http://dsf.dev/fhir/CodeSystem/authorization-role", "REMOTE", null));
		Extension ot22 = e2.addExtension();
		ot22.setUrl("organization-types");
		ot22.addExtension("organization-type",
				new Coding("http://dsf.dev/fhir/CodeSystem/authorization-role", "TTP", null));
		ot22.addExtension("organization-type",
				new Coding("http://dsf.dev/fhir/CodeSystem/authorization-role", "DIC", null));
		e2.addExtension("task-profile", new CanonicalType("http://dsf.dev/fhir/StructureDefinition/task-pong"));

		String xml = FhirContext.forR4().newXmlParser().setPrettyPrint(true).encodeResourceToString(a);
		logger.debug(xml);
	}
}
