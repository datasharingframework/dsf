package dev.dsf.fhir.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collections;
import java.util.UUID;

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
	private static BasicDataSource defaultDataSource;
	private static BasicDataSource permanentDeleteDataSource;

	@ClassRule
	public static final PostgreSqlContainerLiquibaseTemplateClassRule liquibaseRule = new PostgreSqlContainerLiquibaseTemplateClassRule(
			DockerImageName.parse("postgres:15"), ROOT_USER, "fhir", "fhir_template", CHANGE_LOG_FILE,
			CHANGE_LOG_PARAMETERS, true);

	@Rule
	public final PostgresTemplateRule templateRule = new PostgresTemplateRule(liquibaseRule);

	@BeforeClass
	public static void beforeClass() throws Exception
	{
		defaultDataSource = createDefaultDataSource(liquibaseRule.getHost(), liquibaseRule.getMappedPort(5432),
				liquibaseRule.getDatabaseName());
		defaultDataSource.start();

		permanentDeleteDataSource = createPermanentDeleteDataSource(liquibaseRule.getHost(),
				liquibaseRule.getMappedPort(5432), liquibaseRule.getDatabaseName());
		permanentDeleteDataSource.start();
	}

	@AfterClass
	public static void afterClass() throws Exception
	{
		defaultDataSource.close();
		permanentDeleteDataSource.close();
	}

	private final FhirContext fhirContext = FhirContext.forR4();
	private final OrganizationDao orgDao = new OrganizationDaoJdbc(defaultDataSource, permanentDeleteDataSource,
			fhirContext);
	private final HistoryDao dao = new HistroyDaoJdbc(defaultDataSource, fhirContext,
			new BinaryDaoJdbc(defaultDataSource, permanentDeleteDataSource, fhirContext));
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
				new PageAndCount(1, 1000), Collections.singletonList(new AtParameter()), new SinceParameter());
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
				new PageAndCount(1, 1000), Collections.singletonList(new AtParameter()), new SinceParameter(),
				Organization.class);
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
				new PageAndCount(1, 1000), Collections.singletonList(new AtParameter()), new SinceParameter(),
				Organization.class, UUID.fromString(createdOrganization.getIdElement().getIdPart()));

		assertNotNull(history);
		assertEquals(1, history.getTotal());
		assertNotNull(history.getEntries());
		assertEquals(1, history.getEntries().size());
	}
}
