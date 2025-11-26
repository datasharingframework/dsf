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

import java.util.Date;
import java.util.List;

import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.ClassRule;
import org.junit.Test;

import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.validation.ResourceValidatorImpl;
import dev.dsf.fhir.validation.ValidationSupportRule;

public class ValueSetProfileTest extends AbstractMetaDataResourceProfileTest<ValueSet>
{
	@ClassRule
	public static final ValidationSupportRule validationRule = new ValidationSupportRule(
			List.of("dsf-extension-read-access-organization-2.0.0.xml",
					"dsf-extension-read-access-parent-organization-role-2.0.0.xml", "dsf-meta-2.0.0.xml",
					"dsf-value-set-2.0.0.xml"),
			List.of("dsf-read-access-tag-2.0.0.xml", "dsf-organization-role-2.0.0.xml"),
			List.of("dsf-read-access-tag-2.0.0.xml", "dsf-organization-role-2.0.0.xml"));

	private final ResourceValidator resourceValidator = new ResourceValidatorImpl(validationRule.getFhirContext(),
			validationRule.getValidationSupport());

	@Override
	protected ValueSet create()
	{
		ValueSet s = new ValueSet();
		s.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/value-set");
		s.setUrl("http://test.com/fhir/test");
		s.setVersion("2.0.0");
		s.setName("Test");
		s.setDate(new Date());
		s.setStatus(PublicationStatus.ACTIVE);
		s.getCompose().addInclude().setSystem(CS_READ_ACCESS_TAG).setVersion("2.0.0");

		return s;
	}

	@Test
	public void runMetaTagTests() throws Exception
	{
		doRunMetaTagTests(resourceValidator);
	}

	@Test
	public void runMetaDataResourceTests() throws Exception
	{
		doRunMetaDataResourceTests(resourceValidator);
	}

	@Test
	public void testBpmnMessage() throws Exception
	{
		ValueSet s = readValidationResource(ValueSet.class, "dsf-bpmn-message-2.0.0.xml");

		testValid(resourceValidator, s);
	}

	@Test
	public void testOrganizationRole() throws Exception
	{
		ValueSet s = readValidationResource(ValueSet.class, "dsf-organization-role-2.0.0.xml");

		testValid(resourceValidator, s);
	}

	@Test
	public void testPractitionerRole() throws Exception
	{
		ValueSet s = readValidationResource(ValueSet.class, "dsf-practitioner-role-2.0.0.xml");

		testValid(resourceValidator, s);
	}

	@Test
	public void testProcessAuthorizationRecipient() throws Exception
	{
		ValueSet s = readValidationResource(ValueSet.class, "dsf-process-authorization-recipient-2.0.0.xml");

		testValid(resourceValidator, s);
	}

	@Test
	public void testProcessAuthorizationRequester() throws Exception
	{
		ValueSet s = readValidationResource(ValueSet.class, "dsf-process-authorization-requester-2.0.0.xml");

		testValid(resourceValidator, s);
	}

	@Test
	public void testReadAccessTag() throws Exception
	{
		ValueSet s = readValidationResource(ValueSet.class, "dsf-read-access-tag-2.0.0.xml");

		testValid(resourceValidator, s);
	}

	@Test
	public void testValuesetMimetypes() throws Exception
	{
		ValueSet s = readValidationResource(ValueSet.class, "valueset-mimetypes.xml");

		testValid(resourceValidator, s);
	}
}
