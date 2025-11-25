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
package dev.dsf.fhir.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.hl7.fhir.r4.model.Organization;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.utility.DockerImageName;

import ca.uhn.fhir.context.FhirContext;
import de.hsheilbronn.mi.utils.test.PostgreSqlContainerLiquibaseTemplateClassRule;
import de.hsheilbronn.mi.utils.test.PostgresTemplateRule;
import dev.dsf.fhir.dao.jdbc.BinaryDaoJdbc;
import dev.dsf.fhir.dao.jdbc.HistroyDaoJdbc;
import dev.dsf.fhir.dao.jdbc.OrganizationDaoJdbc;
import dev.dsf.fhir.history.AtParameter;
import dev.dsf.fhir.history.History;
import dev.dsf.fhir.history.SinceParameter;
import dev.dsf.fhir.history.filter.HistoryIdentityFilterFactory;
import dev.dsf.fhir.history.filter.HistoryIdentityFilterFactoryImpl;
import dev.dsf.fhir.search.PageAndCount;

public class HistoryDaoTest extends AbstractDbTest
{
	private static DataSource defaultDataSource;
	private static DataSource permanentDeleteDataSource;

	@ClassRule
	public static final PostgreSqlContainerLiquibaseTemplateClassRule liquibaseRule = new PostgreSqlContainerLiquibaseTemplateClassRule(
			DockerImageName.parse("postgres:18"), ROOT_USER, "fhir", "fhir_template", CHANGE_LOG_FILE,
			CHANGE_LOG_PARAMETERS, true);

	@Rule
	public final PostgresTemplateRule templateRule = new PostgresTemplateRule(liquibaseRule);

	@BeforeClass
	public static void beforeClass() throws Exception
	{
		defaultDataSource = createDefaultDataSource(liquibaseRule.getHost(), liquibaseRule.getMappedPort(5432),
				liquibaseRule.getDatabaseName());
		defaultDataSource.unwrap(BasicDataSource.class).start();

		permanentDeleteDataSource = createPermanentDeleteDataSource(liquibaseRule.getHost(),
				liquibaseRule.getMappedPort(5432), liquibaseRule.getDatabaseName());
		permanentDeleteDataSource.unwrap(BasicDataSource.class).start();
	}

	@AfterClass
	public static void afterClass() throws Exception
	{
		defaultDataSource.unwrap(BasicDataSource.class).close();
		permanentDeleteDataSource.unwrap(BasicDataSource.class).close();
	}

	private final FhirContext fhirContext = FhirContext.forR4();
	private final OrganizationDao orgDao = new OrganizationDaoJdbc(defaultDataSource, permanentDeleteDataSource,
			fhirContext);
	private final HistoryDao dao = new HistroyDaoJdbc(defaultDataSource, fhirContext,
			new BinaryDaoJdbc(defaultDataSource, permanentDeleteDataSource, fhirContext, DATABASE_USERS_GROUP));
	private final HistoryIdentityFilterFactory filterFactory = new HistoryIdentityFilterFactoryImpl();

	@Test
	public void testReadHistory() throws Exception
	{
		Organization organization = new Organization();
		organization.getMeta().addTag("http://dsf.dev/fhir/CodeSystem/read-access-tag", "ALL", null);
		organization.setName("Test Organization");
		organization.addIdentifier().setSystem("http://dsf.dev/sid/organization-identifier").setValue("test.org");
		Organization createdOrganization = orgDao.create(organization);

		History history = dao.readHistory(
				filterFactory.getIdentityFilters(TestOrganizationIdentity.local(createdOrganization)),
				PageAndCount.from(1, 1000), List.of(new AtParameter()), new SinceParameter());
		assertNotNull(history);
		assertEquals(1, history.getTotal());
		assertNotNull(history.getEntries());
		assertEquals(1, history.getEntries().size());
	}

	@Test
	public void testReadHistoryOrganization() throws Exception
	{
		Organization organization = new Organization();
		organization.getMeta().addTag("http://dsf.dev/fhir/CodeSystem/read-access-tag", "ALL", null);
		organization.setName("Test Organization");
		organization.addIdentifier().setSystem("http://dsf.dev/sid/organization-identifier").setValue("test.org");
		Organization createdOrganization = orgDao.create(organization);

		History history = dao.readHistory(
				filterFactory.getIdentityFilter(TestOrganizationIdentity.local(createdOrganization),
						Organization.class),
				PageAndCount.from(1, 1000), List.of(new AtParameter()), new SinceParameter(), Organization.class);
		assertNotNull(history);
		assertEquals(1, history.getTotal());
		assertNotNull(history.getEntries());
		assertEquals(1, history.getEntries().size());
	}

	@Test
	public void testReadHistoryOrganizationWithId() throws Exception
	{
		Organization organization = new Organization();
		organization.getMeta().addTag("http://dsf.dev/fhir/CodeSystem/read-access-tag", "ALL", null);
		organization.setName("Test Organization");
		organization.addIdentifier().setSystem("http://dsf.dev/sid/organization-identifier").setValue("test.org");
		Organization createdOrganization = orgDao.create(organization);

		History history = dao.readHistory(
				filterFactory.getIdentityFilter(TestOrganizationIdentity.local(createdOrganization),
						Organization.class),
				PageAndCount.from(1, 1000), List.of(new AtParameter()), new SinceParameter(), Organization.class,
				UUID.fromString(createdOrganization.getIdElement().getIdPart()));

		assertNotNull(history);
		assertEquals(1, history.getTotal());
		assertNotNull(history.getEntries());
		assertEquals(1, history.getEntries().size());
	}
}
