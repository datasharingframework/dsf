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
package dev.dsf.fhir.profiles;

import java.util.List;
import java.util.UUID;

import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Enumerations.DocumentReferenceStatus;
import org.junit.ClassRule;
import org.junit.Test;

import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.validation.ResourceValidatorImpl;
import dev.dsf.fhir.validation.ValidationSupportRule;

public class BundleProfileTest extends AbstractMetaTagProfileTest<Bundle>
{
	@ClassRule
	public static final ValidationSupportRule validationRule = new ValidationSupportRule(context,
			List.of("dsf-extension-read-access-organization-2.0.0.xml",
					"dsf-extension-read-access-parent-organization-role-2.0.0.xml", "dsf-meta-2.0.0.xml",
					"dsf-bundle-2.0.0.xml"),
			List.of("dsf-read-access-tag-2.0.0.xml", "dsf-organization-role-2.0.0.xml"),
			List.of("dsf-read-access-tag-2.0.0.xml", "dsf-organization-role-2.0.0.xml"));

	private final ResourceValidator resourceValidator = new ResourceValidatorImpl(validationRule.getFhirContext(),
			validationRule.getValidationSupport());

	@Override
	protected Bundle create()
	{
		Bundle b = new Bundle();
		b.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/bundle");
		b.setType(BundleType.COLLECTION);

		return b;
	}

	@Test
	public void runMetaTagTests() throws Exception
	{
		doRunMetaTagTests(resourceValidator);
	}

	@Test
	public void testBundleWithUrnUuidNull() throws Exception
	{
		Binary binary = new Binary().setContentType("text/plain");

		String binaryUuid = "urn:uuid:" + UUID.randomUUID().toString();

		DocumentReference documentReference = new DocumentReference().setStatus(DocumentReferenceStatus.CURRENT);
		documentReference.addContent().getAttachment().setContentType("text/plain").setUrl(binaryUuid);

		Bundle bundle = new Bundle().setType(BundleType.COLLECTION);
		bundle.getIdentifier().setSystem("http://medizininformatik-initiative.de/fhir/CodeSystem/cryptography")
				.setValue("public-key");
		bundle.addEntry().setResource(documentReference).setFullUrl("urn:uuid:null");
		bundle.addEntry().setResource(binary).setFullUrl(binaryUuid);

		testValid(resourceValidator, bundle);
	}
}
