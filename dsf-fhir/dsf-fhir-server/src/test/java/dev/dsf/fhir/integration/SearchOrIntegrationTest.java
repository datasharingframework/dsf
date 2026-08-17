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
import java.util.Set;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Endpoint.EndpointStatus;
import org.hl7.fhir.r4.model.Organization;
import org.junit.Test;

import dev.dsf.fhir.authentication.OrganizationProvider;

public class SearchOrIntegrationTest extends AbstractIntegrationTest
{
	private static final String IDENTIFIER_SYSTEM = "http://dsf.dev/sid/endpoint-identifier";

	private Endpoint createEndpoint(String identifierValue)
	{
		OrganizationProvider organizationProvider = getSpringWebApplicationContext()
				.getBean(OrganizationProvider.class);
		Organization localOrganization = organizationProvider.getLocalOrganization().get();

		Endpoint endpoint = new Endpoint();
		endpoint.addIdentifier().setSystem(IDENTIFIER_SYSTEM).setValue(identifierValue);
		endpoint.setStatus(EndpointStatus.ACTIVE);
		endpoint.getConnectionType().setSystem("http://terminology.hl7.org/CodeSystem/endpoint-connection-type")
				.setCode("hl7-fhir-rest");
		endpoint.setName(name(identifierValue));
		endpoint.getPayloadTypeFirstRep().getCodingFirstRep().setSystem("http://hl7.org/fhir/resource-types")
				.setCode("Task");
		endpoint.addPayloadMimeType("application/fhir+json");
		endpoint.addPayloadMimeType("application/fhir+xml");
		endpoint.setAddress("https://foo-bar-baz.test.bla-bla.de/fhir/" + identifierValue);
		endpoint.getManagingOrganization()
				.setReferenceElement(localOrganization.getIdElement().toUnqualifiedVersionless());

		getReadAccessHelper().addLocal(endpoint);
		return endpoint;
	}

	private String name(String identifierValue)
	{
		return "Endpoint " + identifierValue;
	}

	private Set<String> identifierValues(Bundle bundle)
	{
		return bundle.getEntry().stream().map(Bundle.BundleEntryComponent::getResource)
				.filter(r -> r instanceof Endpoint).map(r -> (Endpoint) r)
				.map(e -> e.getIdentifierFirstRep().getValue()).collect(Collectors.toSet());
	}

	private String token(String value)
	{
		return IDENTIFIER_SYSTEM + "|" + value;
	}

	@Test
	public void testSearchWithOrOnTokenReturnsUnionOfMatches() throws Exception
	{
		getWebserviceClient().create(createEndpoint("ora"));
		getWebserviceClient().create(createEndpoint("orb"));
		getWebserviceClient().create(createEndpoint("orc"));

		// identifier=<a>,<b> -> a OR b
		Bundle result = getWebserviceClient().search(Endpoint.class,
				Map.of("identifier", List.of(token("ora") + "," + token("orb"))));

		assertNotNull(result);
		assertEquals(2, result.getTotal());
		assertEquals(Set.of("ora", "orb"), identifierValues(result));
	}

	@Test
	public void testSearchWithOrOnThreeValues() throws Exception
	{
		getWebserviceClient().create(createEndpoint("ora"));
		getWebserviceClient().create(createEndpoint("orb"));
		getWebserviceClient().create(createEndpoint("orc"));
		getWebserviceClient().create(createEndpoint("ord"));

		Bundle result = getWebserviceClient().search(Endpoint.class,
				Map.of("identifier", List.of(token("ora") + "," + token("orb") + "," + token("orc"))));

		assertEquals(3, result.getTotal());
		assertEquals(Set.of("ora", "orb", "orc"), identifierValues(result));
	}

	@Test
	public void testSeparatelyRepeatedParameterStaysAnd() throws Exception
	{
		getWebserviceClient().create(createEndpoint("ora"));
		getWebserviceClient().create(createEndpoint("orb"));

		// identifier=<a>&identifier=<b> -> a AND b; no endpoint carries both identifiers -> no match
		Bundle result = getWebserviceClient().search(Endpoint.class,
				Map.of("identifier", List.of(token("ora"), token("orb"))));

		assertEquals(0, result.getTotal());
	}

	@Test
	public void testOrValueMatchingNoResource() throws Exception
	{
		getWebserviceClient().create(createEndpoint("ora"));

		// a OR (non existing) -> only a
		Bundle result = getWebserviceClient().search(Endpoint.class,
				Map.of("identifier", List.of(token("ora") + "," + token("doesnotexist"))));

		assertEquals(1, result.getTotal());
		assertEquals(Set.of("ora"), identifierValues(result));
	}

	@Test
	public void testSelfLinkKeepsOrValuesCommaJoined() throws Exception
	{
		getWebserviceClient().create(createEndpoint("ora"));
		getWebserviceClient().create(createEndpoint("orb"));

		Bundle result = getWebserviceClient().search(Endpoint.class,
				Map.of("identifier", List.of(token("ora") + "," + token("orb"))));

		String selfLink = result.getLink(Bundle.LINK_SELF).getUrl();
		assertNotNull(selfLink);
		// both OR values are kept within a single identifier parameter (comma separated), not two AND parameters
		assertTrue("self link should contain both or values: " + selfLink,
				selfLink.contains("ora") && selfLink.contains("orb"));
		assertEquals("self link should contain exactly one identifier parameter: " + selfLink, 1,
				selfLink.split("identifier=", -1).length - 1);
	}

	@Test
	public void testSearchResultSortOrder() throws Exception
	{
		getWebserviceClient().create(createEndpoint("ora"));
		getWebserviceClient().create(createEndpoint("orb"));

		Bundle result1 = getWebserviceClient().search(Endpoint.class,
				Map.of("identifier", List.of(token("ora") + "," + token("orb")), "_sort", List.of("name")));

		assertNotNull(result1);
		assertEquals(2, result1.getTotal());
		assertNotNull(result1.getEntry());
		assertNotNull(result1.getEntry().get(0));
		assertNotNull(result1.getEntry().get(0).getResource());
		assertTrue(result1.getEntry().get(0).getResource() instanceof Endpoint);
		assertNotNull(result1.getEntry().get(1));
		assertNotNull(result1.getEntry().get(1).getResource());
		assertTrue(result1.getEntry().get(1).getResource() instanceof Endpoint);

		assertEquals(name("ora"), ((Endpoint) result1.getEntry().get(0).getResource()).getName());
		assertEquals(name("orb"), ((Endpoint) result1.getEntry().get(1).getResource()).getName());

		Bundle result2 = getWebserviceClient().search(Endpoint.class,
				Map.of("identifier", List.of(token("ora") + "," + token("orb")), "_sort", List.of("-name")));

		assertNotNull(result2);
		assertEquals(2, result2.getTotal());
		assertNotNull(result1.getEntry());
		assertNotNull(result1.getEntry().get(0));
		assertNotNull(result1.getEntry().get(0).getResource());
		assertTrue(result1.getEntry().get(0).getResource() instanceof Endpoint);
		assertNotNull(result1.getEntry().get(1));
		assertNotNull(result1.getEntry().get(1).getResource());
		assertTrue(result1.getEntry().get(1).getResource() instanceof Endpoint);

		assertEquals(name("orb"), ((Endpoint) result2.getEntry().get(0).getResource()).getName());
		assertEquals(name("ora"), ((Endpoint) result2.getEntry().get(1).getResource()).getName());
	}
}
