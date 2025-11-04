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
package dev.dsf.bpe.test.fhir;

import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.StringType;

import dev.dsf.bpe.test.TestProcessPluginDefinition;
import dev.dsf.bpe.v2.constants.CodeSystems.ProcessAuthorization;
import dev.dsf.bpe.v2.fhir.AbstractFhirResourceModifier;

public class FhirResourceModifierImpl extends AbstractFhirResourceModifier
{
	@Override
	public ActivityDefinition modifyActivityDefinition(String filename, ActivityDefinition resource)
	{
		if ("fhir/ActivityDefinition/dsf-test.xml".equals(filename))
			return addProcessAuthorization(resource);
		else
			return super.modifyActivityDefinition(filename, resource);
	}

	private ActivityDefinition addProcessAuthorization(ActivityDefinition resource)
	{
		Extension processAuthorization = resource.addExtension();
		processAuthorization.setUrl("http://dsf.dev/fhir/StructureDefinition/extension-process-authorization");
		processAuthorization.addExtension().setUrl("message-name").setValue(new StringType("continue-send-test"));
		processAuthorization.addExtension().setUrl("task-profile")
				.setValue(new CanonicalType("http://dsf.dev/fhir/StructureDefinition/task-continue-send-test|"
						+ new TestProcessPluginDefinition().getResourceVersion()));
		processAuthorization.addExtension().setUrl("requester").setValue(ProcessAuthorization.localAll());
		processAuthorization.addExtension().setUrl("recipient").setValue(ProcessAuthorization.localAll());

		return resource;
	}
}
