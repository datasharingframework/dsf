package dev.dsf.fhir.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.UUID;

import org.apache.commons.dbcp2.BasicDataSource;
import org.hl7.fhir.r4.model.Organization;
import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ca.uhn.fhir.context.FhirContext;
import de.rwh.utils.test.LiquibaseTemplateTestClassRule;
import de.rwh.utils.test.LiquibaseTemplateTestRule;
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
	private static final BasicDataSource adminDataSource = createAdminBasicDataSource();
	private static final BasicDataSource liquibaseDataSource = createLiquibaseDataSource();
	private static final BasicDataSource defaultDataSource = createDefaultDataSource();
	private static final BasicDataSource permanentDeleteDataSource = createPermanentDeleteDataSource();

	@ClassRule
	public static final LiquibaseTemplateTestClassRule liquibaseRule = new LiquibaseTemplateTestClassRule(
			adminDataSource, LiquibaseTemplateTestClassRule.DEFAULT_TEST_DB_NAME,
			AbstractResourceDaoTest.DAO_DB_TEMPLATE_NAME, liquibaseDataSource, CHANGE_LOG_FILE, CHANGE_LOG_PARAMETERS,
			true);

	@AfterClass
	public static void afterClass() throws Exception
	{
		defaultDataSource.close();
		liquibaseDataSource.close();
		adminDataSource.close();
		permanentDeleteDataSource.close();
	}

	@Rule
	public final LiquibaseTemplateTestRule templateRule = new LiquibaseTemplateTestRule(adminDataSource,
			LiquibaseTemplateTestClassRule.DEFAULT_TEST_DB_NAME, AbstractResourceDaoTest.DAO_DB_TEMPLATE_NAME);

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
				new PageAndCount(1, 1000), new AtParameter(), new SinceParameter());
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
				new PageAndCount(1, 1000), new AtParameter(), new SinceParameter(), Organization.class);
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
				new PageAndCount(1, 1000), new AtParameter(), new SinceParameter(), Organization.class,
				UUID.fromString(createdOrganization.getIdElement().getIdPart()));

		assertNotNull(history);
		assertEquals(1, history.getTotal());
		assertNotNull(history.getEntries());
		assertEquals(1, history.getEntries().size());
	}
}
