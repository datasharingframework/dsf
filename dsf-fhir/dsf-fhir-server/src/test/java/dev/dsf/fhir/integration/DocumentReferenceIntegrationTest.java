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

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Enumerations.DocumentReferenceStatus;
import org.junit.Test;

import dev.dsf.fhir.dao.DocumentReferenceDao;

public class DocumentReferenceIntegrationTest extends AbstractIntegrationTest
{
	private DocumentReference createValidDocumentReference()
	{
		DocumentReference documentReference = new DocumentReference();
		documentReference.setDate(new Date());
		documentReference.setDescription("Demo DocumentReference Description");
		documentReference.addContent().getAttachment().setContentType("text/plain")
				.setData("Test Plain Text Inline Data".getBytes(StandardCharsets.UTF_8));
		documentReference.setStatus(DocumentReferenceStatus.CURRENT);
		return documentReference;
	}

	@Test
	public void testCreateValidByLocalUserReadAccessTagAll() throws Exception
	{
		DocumentReference documentReference = createValidDocumentReference();
		readAccessHelper.addAll(documentReference);

		DocumentReference created = getWebserviceClient().create(documentReference);
		assertNotNull(created);
		assertNotNull(created.getIdElement().getIdPart());
		assertNotNull(created.getIdElement().getVersionIdPart());
	}

	@Test
	public void testCreateValidByLocalUserReadAccessTagLocal() throws Exception
	{
		DocumentReference documentReference = createValidDocumentReference();
		readAccessHelper.addLocal(documentReference);

		DocumentReference created = getWebserviceClient().create(documentReference);
		assertNotNull(created);
		assertNotNull(created.getIdElement().getIdPart());
		assertNotNull(created.getIdElement().getVersionIdPart());
	}

	@Test
	public void testCreateValidByLocalUserReadAccessTagOrganization() throws Exception
	{
		DocumentReference documentReference = createValidDocumentReference();
		readAccessHelper.addLocal(documentReference);
		readAccessHelper.addOrganization(documentReference, "External_Test_Organization");

		DocumentReference created = getWebserviceClient().create(documentReference);
		assertNotNull(created);
		assertNotNull(created.getIdElement().getIdPart());
		assertNotNull(created.getIdElement().getVersionIdPart());
	}

	@Test
	public void testReadWithOrganizationTag() throws Exception
	{
		DocumentReference documentReference = createValidDocumentReference();
		readAccessHelper.addLocal(documentReference);
		readAccessHelper.addOrganization(documentReference, "External_Test_Organization");

		DocumentReferenceDao dao = getSpringWebApplicationContext().getBean(DocumentReferenceDao.class);
		DocumentReference created = dao.create(documentReference);
		assertNotNull(created);
		assertNotNull(created.getIdElement().getIdPart());
		assertNotNull(created.getIdElement().getVersionIdPart());

		DocumentReference read = getExternalWebserviceClient().read(DocumentReference.class,
				created.getIdElement().getIdPart());
		assertNotNull(read);
		assertEquals(created.getIdElement().getIdPart(), read.getIdElement().getIdPart());
		assertEquals(created.getIdElement().getVersionIdPart(), read.getIdElement().getVersionIdPart());
	}

	@Test
	public void testCreateValidByLocalUserReadAccessTagRole() throws Exception
	{
		DocumentReference documentReference = createValidDocumentReference();
		readAccessHelper.addLocal(documentReference);
		readAccessHelper.addRole(documentReference, "Parent_Organization",
				"http://dsf.dev/fhir/CodeSystem/organization-role", "DIC");

		DocumentReference created = getWebserviceClient().create(documentReference);
		assertNotNull(created);
		assertNotNull(created.getIdElement().getIdPart());
		assertNotNull(created.getIdElement().getVersionIdPart());
	}

	@Test
	public void testReadWithRoleTag() throws Exception
	{
		DocumentReference documentReference = createValidDocumentReference();
		readAccessHelper.addLocal(documentReference);
		readAccessHelper.addRole(documentReference, "Parent_Organization",
				"http://dsf.dev/fhir/CodeSystem/organization-role", "TTP");

		DocumentReferenceDao dao = getSpringWebApplicationContext().getBean(DocumentReferenceDao.class);
		DocumentReference created = dao.create(documentReference);
		assertNotNull(created);
		assertNotNull(created.getIdElement().getIdPart());
		assertNotNull(created.getIdElement().getVersionIdPart());

		DocumentReference read = getExternalWebserviceClient().read(DocumentReference.class,
				created.getIdElement().getIdPart());
		assertNotNull(read);
		assertEquals(created.getIdElement().getIdPart(), read.getIdElement().getIdPart());
		assertEquals(created.getIdElement().getVersionIdPart(), read.getIdElement().getVersionIdPart());
	}

	@Test
	public void testSearchByIdentifier() throws Exception
	{
		String system = "http://test.com/fhir/sid/Foo";
		String value1 = "Bar1";
		String value2 = "Bar2";
		DocumentReference documentReference = createValidDocumentReference();
		documentReference.addIdentifier().setSystem(system).setValue(value1);
		documentReference.getMasterIdentifier().setSystem(system).setValue(value2);
		readAccessHelper.addLocal(documentReference);

		DocumentReferenceDao dao = getSpringWebApplicationContext().getBean(DocumentReferenceDao.class);
		DocumentReference created = dao.create(documentReference);
		assertNotNull(created);
		assertNotNull(created.getIdElement().getIdPart());
		assertNotNull(created.getIdElement().getVersionIdPart());

		Bundle bundle1 = getWebserviceClient().search(DocumentReference.class,
				Map.of("identifier", List.of(system + "|" + value1)));
		assertFound(bundle1, created);
		Bundle bundle1not = getWebserviceClient().search(DocumentReference.class,
				Map.of("identifier:not", List.of(system + "|" + value1)));
		assertNotFound(bundle1not);
		Bundle bundle2 = getWebserviceClient().search(DocumentReference.class, Map.of("identifier", List.of(value1)));
		assertFound(bundle2, created);
		Bundle bundle2not = getWebserviceClient().search(DocumentReference.class,
				Map.of("identifier:not", List.of(value1)));
		assertNotFound(bundle2not);
		Bundle bundle3 = getWebserviceClient().search(DocumentReference.class,
				Map.of("identifier:not", List.of(system + "|Baz")));
		assertFound(bundle3, created);
		Bundle bundle4 = getWebserviceClient().search(DocumentReference.class,
				Map.of("identifier:not", List.of("Something|Baz")));
		assertFound(bundle4, created);
		Bundle bundle5 = getWebserviceClient().search(DocumentReference.class,
				Map.of("identifier:not", List.of("Baz")));
		assertFound(bundle5, created);

		Bundle bundle6 = getWebserviceClient().search(DocumentReference.class,
				Map.of("identifier", List.of(system + "|" + value2)));
		assertFound(bundle6, created);
		Bundle bundle6not = getWebserviceClient().search(DocumentReference.class,
				Map.of("identifier:not", List.of(system + "|" + value2)));
		assertNotFound(bundle6not);
		Bundle bundle7 = getWebserviceClient().search(DocumentReference.class, Map.of("identifier", List.of(value2)));
		assertFound(bundle7, created);
		Bundle bundle7not = getWebserviceClient().search(DocumentReference.class,
				Map.of("identifier:not", List.of(value2)));
		assertNotFound(bundle7not);
		Bundle bundle8 = getWebserviceClient().search(DocumentReference.class,
				Map.of("identifier", List.of(system + "|")));
		assertFound(bundle8, created);
		Bundle bundle8not = getWebserviceClient().search(DocumentReference.class,
				Map.of("identifier:not", List.of(system + "|")));
		assertNotFound(bundle8not);
	}

	@Test
	public void testSearchByIdentifierNoSystem() throws Exception
	{
		String value = "Foo";
		DocumentReference documentReference = createValidDocumentReference();
		documentReference.addIdentifier().setValue(value);
		documentReference.getMasterIdentifier().setValue(value);
		readAccessHelper.addLocal(documentReference);

		DocumentReferenceDao dao = getSpringWebApplicationContext().getBean(DocumentReferenceDao.class);
		DocumentReference created = dao.create(documentReference);
		assertNotNull(created);
		assertNotNull(created.getIdElement().getIdPart());
		assertNotNull(created.getIdElement().getVersionIdPart());

		Bundle bundle = getWebserviceClient().search(DocumentReference.class, Map.of("identifier", List.of("|Foo")));
		assertFound(bundle, created);
		Bundle bundleNot = getWebserviceClient().search(DocumentReference.class,
				Map.of("identifier:not", List.of("|Foo")));
		assertNotFound(bundleNot);
	}

	private void assertFound(Bundle bundle, DocumentReference created)
	{
		assertNotNull(bundle);
		assertEquals(1, bundle.getTotal());
		assertEquals(1, bundle.getEntry().size());
		assertNotNull(bundle.getEntry().get(0));
		assertTrue(bundle.getEntry().get(0).hasResource());
		assertTrue(bundle.getEntry().get(0).getResource() instanceof DocumentReference);

		DocumentReference read = (DocumentReference) bundle.getEntry().get(0).getResource();

		assertNotNull(read);
		assertEquals(created.getIdElement().getIdPart(), read.getIdElement().getIdPart());
		assertEquals(created.getIdElement().getVersionIdPart(), read.getIdElement().getVersionIdPart());
	}

	private void assertNotFound(Bundle bundle)
	{
		assertNotNull(bundle);
		assertEquals(0, bundle.getTotal());
	}
}
