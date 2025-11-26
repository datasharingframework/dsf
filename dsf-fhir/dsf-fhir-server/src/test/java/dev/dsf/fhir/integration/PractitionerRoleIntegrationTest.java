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

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.function.Function;

import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.Before;
import org.junit.Test;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.fhir.dao.OrganizationDao;
import dev.dsf.fhir.dao.PractitionerDao;

public class PractitionerRoleIntegrationTest extends AbstractIntegrationTest
{
	private static final FhirContext fhirContext = FhirContext.forR4();

	private <R extends Resource> void readAndCreate(Class<R> type, String filename, Function<R, R> modifier)
			throws IOException
	{
		try (InputStream in = Files
				.newInputStream(Paths.get("src/test/resources/integration/practitioner-role", filename)))
		{
			R resource = fhirContext.newXmlParser().parseResource(type, in);

			R created = getWebserviceClient().create(modifier.apply(resource));

			assertNotNull(created);
			assertNotNull(created.getIdElement().getIdPart());
		}
	}

	@Before
	public void before() throws Exception
	{
		readAndCreate(CodeSystem.class, "codesystem-a.xml", Function.identity());
		readAndCreate(ValueSet.class, "valueset-a.xml", Function.identity());
		readAndCreate(StructureDefinition.class, "structuredefinition-a.xml", Function.identity());

		readAndCreate(CodeSystem.class, "codesystem-b.xml", Function.identity());
		readAndCreate(ValueSet.class, "valueset-b.xml", Function.identity());
		readAndCreate(StructureDefinition.class, "structuredefinition-b.xml", Function.identity());
	}

	private Organization createOrganization() throws SQLException
	{
		OrganizationDao organizationDao = getSpringWebApplicationContext().getBean(OrganizationDao.class);
		return organizationDao.create(new Organization());
	}

	private Practitioner createPractitioner() throws SQLException
	{
		PractitionerDao practitionerDao = getSpringWebApplicationContext().getBean(PractitionerDao.class);
		return practitionerDao.create(new Practitioner());
	}

	@Test
	public void testCreateA() throws Exception
	{
		Practitioner p = createPractitioner();
		Organization o = createOrganization();

		readAndCreate(PractitionerRole.class, "practitionerrole-a.xml", pr ->
		{
			pr.getPractitioner().setReferenceElement(p.getIdElement().toVersionless());
			pr.getOrganization().setReferenceElement(o.getIdElement().toVersionless());
			return pr;
		});
	}

	@Test
	public void testCreateB() throws Exception
	{
		Practitioner p = createPractitioner();
		Organization o = createOrganization();

		readAndCreate(PractitionerRole.class, "practitionerrole-b.xml", pr ->
		{
			pr.getPractitioner().setReferenceElement(p.getIdElement().toVersionless());
			pr.getOrganization().setReferenceElement(o.getIdElement().toVersionless());
			return pr;
		});
	}
}
