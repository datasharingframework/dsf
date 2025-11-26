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

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.UUID;

import org.hl7.fhir.r4.model.OrganizationAffiliation;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.ValidationResult;
import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.validation.ResourceValidatorImpl;
import dev.dsf.fhir.validation.ValidationSupportRule;

public class OrganizationAffiliationProfileTest extends AbstractMetaTagProfileTest<OrganizationAffiliation>
{
	private static final Logger logger = LoggerFactory.getLogger(OrganizationAffiliationProfileTest.class);

	@ClassRule
	public static final ValidationSupportRule validationRule = new ValidationSupportRule(
			List.of("dsf-extension-read-access-organization-2.0.0.xml",
					"dsf-extension-read-access-parent-organization-role-2.0.0.xml", "dsf-meta-2.0.0.xml",
					"dsf-organization-2.0.0.xml", "dsf-organization-parent-2.0.0.xml",
					"dsf-organization-affiliation-2.0.0.xml"),
			List.of("dsf-read-access-tag-2.0.0.xml", "dsf-organization-role-2.0.0.xml"),
			List.of("dsf-read-access-tag-2.0.0.xml", "dsf-organization-role-2.0.0.xml"));

	private final ResourceValidator resourceValidator = new ResourceValidatorImpl(validationRule.getFhirContext(),
			validationRule.getValidationSupport());

	@Override
	protected OrganizationAffiliation create()
	{
		OrganizationAffiliation a = new OrganizationAffiliation();
		a.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/organization-affiliation");
		a.setActive(true);
		a.getOrganization().setReference("Organization/" + UUID.randomUUID().toString());
		a.getParticipatingOrganization().setReference("Organization/" + UUID.randomUUID().toString());
		a.getCodeFirstRep().getCodingFirstRep().setSystem("http://dsf.dev/fhir/CodeSystem/organization-role")
				.setCode("DIC");
		a.getEndpointFirstRep().setReference("Endpoint/" + UUID.randomUUID().toString());

		return a;
	}

	@Test
	public void runMetaTagTests() throws Exception
	{
		doRunMetaTagTests(resourceValidator);
	}

	@Test
	public void testOrganizationAffiliationProfileValid() throws Exception
	{
		OrganizationAffiliation a = create();
		a.getMeta().addTag().setSystem("http://dsf.dev/fhir/CodeSystem/read-access-tag").setCode("ALL");

		ValidationResult result = resourceValidator.validate(a);
		ValidationSupportRule.logValidationMessages(logger::debug, result);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	@Test
	public void testOrganizationAffiliationProfileNotValidMultipleEndpoints() throws Exception
	{
		OrganizationAffiliation a = new OrganizationAffiliation();
		a.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/organization-affiliation");
		a.getMeta().addTag().setSystem("http://dsf.dev/fhir/CodeSystem/read-access-tag").setCode("ALL");
		a.setActive(true);
		a.getOrganization().setReference("Organization/" + UUID.randomUUID().toString());
		a.getParticipatingOrganization().setReference("Organization/" + UUID.randomUUID().toString());
		a.getCodeFirstRep().getCodingFirstRep().setSystem("http://dsf.dev/fhir/CodeSystem/organization-role")
				.setCode("DIC");
		a.addEndpoint().setReference("Endpoint/" + UUID.randomUUID().toString());
		a.addEndpoint().setReference("Endpoint/" + UUID.randomUUID().toString());

		ValidationResult result = resourceValidator.validate(a);
		ValidationSupportRule.logValidationMessages(logger::debug, result);

		assertEquals(1, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	@Test
	public void testOrganizationAffiliationProfileNoDsfOrganizationRole() throws Exception
	{
		OrganizationAffiliation a = create();
		a.getMeta().addTag().setSystem("http://dsf.dev/fhir/CodeSystem/read-access-tag").setCode("ALL");

		a.getCodeFirstRep().getCodingFirstRep().setSystem("http://test.org/fhir/CodeSystem/special-role")
				.setCode("Foo");

		ValidationResult result = resourceValidator.validate(a);
		ValidationSupportRule.logValidationMessages(logger::debug, result);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}
}
