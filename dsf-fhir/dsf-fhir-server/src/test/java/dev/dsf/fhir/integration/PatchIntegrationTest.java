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
package dev.dsf.fhir.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Endpoint.EndpointStatus;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.StringType;
import org.junit.Test;

import dev.dsf.fhir.authentication.OrganizationProvider;

public class PatchIntegrationTest extends AbstractIntegrationTest
{
	private Endpoint createEndpoint(String identifierValue, String name)
	{
		OrganizationProvider organizationProvider = getSpringWebApplicationContext()
				.getBean(OrganizationProvider.class);
		Organization localOrganization = organizationProvider.getLocalOrganization().get();

		Endpoint endpoint = new Endpoint();
		endpoint.addIdentifier().setSystem("http://dsf.dev/sid/endpoint-identifier").setValue(identifierValue);
		endpoint.setStatus(EndpointStatus.ACTIVE);
		endpoint.getConnectionType().setSystem("http://terminology.hl7.org/CodeSystem/endpoint-connection-type")
				.setCode("hl7-fhir-rest");
		endpoint.setName(name);
		endpoint.getPayloadTypeFirstRep().getCodingFirstRep().setSystem("http://hl7.org/fhir/resource-types")
				.setCode("Task");
		endpoint.addPayloadMimeType("application/fhir+json");
		endpoint.addPayloadMimeType("application/fhir+xml");
		endpoint.setAddress("https://foo-bar-baz.test.bla-bla.de/fhir");
		endpoint.getManagingOrganization()
				.setReferenceElement(localOrganization.getIdElement().toUnqualifiedVersionless());

		getReadAccessHelper().addLocal(endpoint);
		return endpoint;
	}

	private static Parameters replacePatch(String path, String value)
	{
		Parameters patch = new Parameters();
		ParametersParameterComponent op = patch.addParameter().setName("operation");
		op.addPart().setName("type").setValue(new CodeType("replace"));
		op.addPart().setName("path").setValue(new StringType(path));
		op.addPart().setName("value").setValue(new StringType(value));
		return patch;
	}

	@Test
	public void testPatchEndpointReplaceName() throws Exception
	{
		Endpoint created = getWebserviceClient().create(createEndpoint("patch.test.1", "Original Name"));
		assertEquals("Original Name", created.getName());
		assertEquals("1", created.getIdElement().getVersionIdPart());

		Endpoint patched = getWebserviceClient().patch(Endpoint.class, created.getIdElement().getIdPart(),
				replacePatch("Endpoint.name", "Patched Name"));

		assertNotNull(patched);
		assertEquals("Patched Name", patched.getName());
		assertEquals("2", patched.getIdElement().getVersionIdPart());

		// other fields must be unchanged
		assertEquals(EndpointStatus.ACTIVE, patched.getStatus());
		assertEquals("patch.test.1", patched.getIdentifierFirstRep().getValue());

		// verify persisted
		Endpoint read = getWebserviceClient().read(Endpoint.class, created.getIdElement().getIdPart());
		assertEquals("Patched Name", read.getName());
		assertEquals("2", read.getIdElement().getVersionIdPart());
	}

	@Test
	public void testConditionalPatchEndpointByIdentifier() throws Exception
	{
		getWebserviceClient().create(createEndpoint("patch.test.conditional", "Original Name"));

		Endpoint patched = getWebserviceClient().patchConditionaly(Endpoint.class,
				replacePatch("Endpoint.name", "Conditionally Patched"),
				Map.of("identifier", List.of("http://dsf.dev/sid/endpoint-identifier|patch.test.conditional")));

		assertNotNull(patched);
		assertEquals("Conditionally Patched", patched.getName());
		assertEquals("2", patched.getIdElement().getVersionIdPart());
	}

	@Test
	public void testPatchEndpointViaTransactionBundle() throws Exception
	{
		Endpoint created = getWebserviceClient().create(createEndpoint("patch.test.bundle", "Original Name"));

		Bundle bundle = new Bundle();
		bundle.setType(BundleType.TRANSACTION);
		BundleEntryComponent entry = bundle.addEntry();
		entry.setResource(replacePatch("Endpoint.name", "Bundle Patched Name"));
		entry.getRequest().setMethod(HTTPVerb.PATCH).setUrl("Endpoint/" + created.getIdElement().getIdPart());

		Bundle response = getWebserviceClient().postBundle(bundle);

		assertNotNull(response);
		assertEquals(BundleType.TRANSACTIONRESPONSE, response.getType());
		assertEquals(1, response.getEntry().size());
		assertTrue(response.getEntryFirstRep().getResponse().getStatus().startsWith("200"));

		Endpoint read = getWebserviceClient().read(Endpoint.class, created.getIdElement().getIdPart());
		assertEquals("Bundle Patched Name", read.getName());
		assertEquals("2", read.getIdElement().getVersionIdPart());
	}

	@Test
	public void testPatchNonExistentEndpointReturnsNotFound() throws Exception
	{
		expectNotFound(() -> getWebserviceClient().patch(Endpoint.class, UUID.randomUUID().toString(),
				replacePatch("Endpoint.name", "does not matter")));
	}

	@Test
	public void testPatchWithUnknownElementReturnsBadRequest() throws Exception
	{
		Endpoint created = getWebserviceClient().create(createEndpoint("patch.test.badrequest", "Original Name"));

		expectBadRequest(() -> getWebserviceClient().patch(Endpoint.class, created.getIdElement().getIdPart(),
				replacePatch("Endpoint.doesNotExist", "x")));
	}
}
