package dev.dsf.bpe.v1.service;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hl7.fhir.instance.model.api.IBaseBundle.LINK_NEXT;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntrySearchComponent;
import org.hl7.fhir.r4.model.Bundle.BundleLinkComponent;
import org.hl7.fhir.r4.model.Bundle.SearchEntryMode;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.OrganizationAffiliation;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import dev.dsf.fhir.client.FhirWebserviceClient;

@RunWith(MockitoJUnitRunner.class)
public class OrganizationProviderImplTest
{

	private static final String ENDPOINT = "endpoint";
	private static final BundleEntrySearchComponent INCLUDE_MODE = new BundleEntrySearchComponent()
			.setMode(SearchEntryMode.INCLUDE);
	private static final BundleEntrySearchComponent MATCH_MODE = new BundleEntrySearchComponent()
			.setMode(SearchEntryMode.MATCH);

	@Mock
	private FhirWebserviceClientProvider clientProvider;
	@Mock
	private FhirWebserviceClient client;

	private OrganizationProviderImpl organizationProvider;

	@Captor
	ArgumentCaptor<Map<String, List<String>>> parametersCaptor;


	@Before
	public void setup()
	{
		organizationProvider = new OrganizationProviderImpl(clientProvider, ENDPOINT);
	}

	@Test
	public void getOrganizationsWithOneOrganizationAndOrganizationAffiliation() throws Exception
	{
		String system = "foo";
		String code = "bar";
		Identifier identifier = new Identifier().setSystem(system).setValue(code);
		Organization org = new Organization().setActive(true);
		OrganizationAffiliation affiliation = new OrganizationAffiliation().setActive(true);
		Bundle results = new Bundle();
		results.addEntry().setSearch(MATCH_MODE).setResource(affiliation);
		results.addEntry().setSearch(INCLUDE_MODE).setResource(org);
		results.setTotal(1);
		when(clientProvider.getLocalWebserviceClient()).thenReturn(client);
		when(client.searchWithStrictHandling(Mockito.eq(OrganizationAffiliation.class), parametersCaptor.capture()))
				.thenReturn(results);

		List<Organization> organizations = organizationProvider.getOrganizations(identifier);

		assertThat(parametersCaptor.getAllValues().size(), is(1));
		assertTrue("Parameters do not contain 'primary-organization:identifier'.",
				parametersCaptor.getValue().containsKey("primary-organization:identifier"));
		assertThat(parametersCaptor.getValue().get("primary-organization:identifier").size(), is(1));
		assertThat(parametersCaptor.getValue().get("primary-organization:identifier").get(0), is(system + "|" + code));
		assertThat(organizations.size(), is(1));
		assertThat(organizations, hasItem(org));
	}

	@Test
	public void getOrganizationsWithOneOrganizationAndOrganizationAffiliationSearchingWithRole() throws Exception
	{
		String systemIdentifier = "foo";
		String systemRole = "bar";
		String code = "baz";
		Identifier identifier = new Identifier().setSystem(systemIdentifier).setValue(code);
		Coding role = new Coding().setSystem(systemRole).setCode(code);
		Organization org = new Organization().setActive(true);
		OrganizationAffiliation affiliation = new OrganizationAffiliation().setActive(true);
		Bundle results = new Bundle();
		results.addEntry().setSearch(MATCH_MODE).setResource(affiliation);
		results.addEntry().setSearch(INCLUDE_MODE).setResource(org);
		results.setTotal(1);
		when(clientProvider.getLocalWebserviceClient()).thenReturn(client);
		when(client.searchWithStrictHandling(Mockito.eq(OrganizationAffiliation.class), parametersCaptor.capture()))
				.thenReturn(results);

		List<Organization> organizations = organizationProvider.getOrganizations(identifier, role);

		assertThat(parametersCaptor.getAllValues().size(), is(1));
		assertTrue("Parameters do not contain 'primary-organization:identifier'.",
				parametersCaptor.getValue().containsKey("primary-organization:identifier"));
		assertThat(parametersCaptor.getValue().get("primary-organization:identifier").size(), is(1));
		assertThat(parametersCaptor.getValue().get("primary-organization:identifier").get(0),
				is(systemIdentifier + "|" + code));
		assertTrue("Parameters do not contain 'role'.", parametersCaptor.getValue().containsKey("role"));
		assertThat(parametersCaptor.getValue().get("role").size(), is(1));
		assertThat(parametersCaptor.getValue().get("role").get(0), is(systemRole + "|" + code));
		assertThat(organizations.size(), is(1));
		assertThat(organizations, hasItem(org));
	}

	@Test
	public void getOrganizationsWithSameNumberOfOrganizationsAndOrganizationAffiliations() throws Exception
	{
		Identifier identifier = new Identifier().setSystem("foo").setValue("bar");
		Organization org = new Organization().setActive(true);
		OrganizationAffiliation affiliation = new OrganizationAffiliation().setActive(true);
		Bundle results = new Bundle();
		int count = 10;
		for (int i = 0; i < count; i++)
		{
			results.addEntry().setSearch(MATCH_MODE).setResource(affiliation);
			results.addEntry().setSearch(INCLUDE_MODE).setResource(org);
		}
		results.setTotal(10);
		when(clientProvider.getLocalWebserviceClient()).thenReturn(client);
		when(client.searchWithStrictHandling(Mockito.eq(OrganizationAffiliation.class), parametersCaptor.capture()))
				.thenReturn(results);

		List<Organization> organizations = organizationProvider.getOrganizations(identifier);

		assertThat(parametersCaptor.getAllValues().size(), is(1));
		assertThat(organizations.size(), is(count));
	}

	@Test
	public void getOrganizationsWithNumberOfOrganizationsLessThanNumberOfOrganizationAffiliations() throws Exception
	{
		Identifier identifier = new Identifier().setSystem("foo").setValue("bar");
		Organization org = new Organization().setActive(true);
		OrganizationAffiliation affiliation = new OrganizationAffiliation().setActive(true);
		Bundle results = new Bundle();
		int countOrganizations = 8;
		int countAffiliations = 10;
		for (int i = 0; i < countOrganizations; i++)
		{
			results.addEntry().setSearch(INCLUDE_MODE).setResource(org);
		}
		for (int i = 0; i < countAffiliations; i++)
		{
			results.addEntry().setSearch(MATCH_MODE).setResource(affiliation);
		}
		results.setTotal(countAffiliations);
		when(clientProvider.getLocalWebserviceClient()).thenReturn(client);
		when(client.searchWithStrictHandling(Mockito.eq(OrganizationAffiliation.class), parametersCaptor.capture()))
				.thenReturn(results);

		List<Organization> organizations = organizationProvider.getOrganizations(identifier);

		assertThat(parametersCaptor.getAllValues().size(), is(1));
		assertThat(organizations.size(), is(countOrganizations));
	}

	@Test
	public void getOrganizationsWithSameNumberOfOrganizationsAndOrganizationAffiliationsOnMultiplePages()
			throws Exception
	{
		Identifier identifier = new Identifier().setSystem("foo").setValue("bar");
		Organization org = new Organization().setActive(true);
		OrganizationAffiliation affiliation = new OrganizationAffiliation().setActive(true);
		Bundle firstPageResults = new Bundle();
		Bundle secondPageResults = new Bundle();
		Bundle morePageResults = new Bundle();
		BundleLinkComponent nextLink = new BundleLinkComponent().setRelation(LINK_NEXT).setUrl("foo.bar");
		int count = 10;
		for (int i = 0; i < count / 2; i++)
		{
			firstPageResults.addEntry().setSearch(MATCH_MODE).setResource(affiliation);
			firstPageResults.addEntry().setSearch(INCLUDE_MODE).setResource(org);
		}
		for (int i = count / 2; i < count; i++)
		{
			secondPageResults.addEntry().setSearch(MATCH_MODE).setResource(affiliation);
			secondPageResults.addEntry().setSearch(INCLUDE_MODE).setResource(org);
		}
		firstPageResults.setTotal(count);
		secondPageResults.setTotal(count);
		morePageResults.setTotal(count);
		firstPageResults.setLink(List.of(nextLink));
		when(clientProvider.getLocalWebserviceClient()).thenReturn(client);
		when(client.searchWithStrictHandling(Mockito.eq(OrganizationAffiliation.class), parametersCaptor.capture()))
				.thenReturn(firstPageResults, secondPageResults, morePageResults);

		List<Organization> organizations = organizationProvider.getOrganizations(identifier);

		assertThat(parametersCaptor.getAllValues().size(), is(2));
		assertThat(parametersCaptor.getAllValues().get(1).keySet(), hasItem("_page"));
		assertThat(parametersCaptor.getAllValues().get(1).get("_page").size(), is(1));
		assertThat(parametersCaptor.getAllValues().get(1).get("_page").get(0), is("2"));
		assertThat(organizations.size(), is(count));
	}

	@Test
	public void getOrganizationsWithNumberOfOrganizationsLessThanNumberOfOrganizationAffiliationsOnMultiplePages()
			throws Exception
	{
		Identifier identifier = new Identifier().setSystem("foo").setValue("bar");
		Organization org = new Organization().setActive(true);
		OrganizationAffiliation affiliation = new OrganizationAffiliation().setActive(true);
		int countOrganizations = 8;
		int countAffiliations = 10;
		Bundle firstPageResults = new Bundle();
		Bundle secondPageResults = new Bundle();
		Bundle morePageResults = new Bundle();
		BundleLinkComponent nextLink = new BundleLinkComponent().setRelation(LINK_NEXT).setUrl("foo.bar");
		for (int i = 0; i < countOrganizations / 2; i++)
		{
			firstPageResults.addEntry().setSearch(INCLUDE_MODE).setResource(org);
		}
		for (int i = 0; i < countAffiliations / 2; i++)
		{
			firstPageResults.addEntry().setSearch(MATCH_MODE).setResource(affiliation);
		}
		for (int i = countOrganizations / 2; i < countOrganizations; i++)
		{
			secondPageResults.addEntry().setSearch(INCLUDE_MODE).setResource(org);
		}
		for (int i = countAffiliations / 2; i < countAffiliations; i++)
		{
			secondPageResults.addEntry().setSearch(MATCH_MODE).setResource(affiliation);
		}
		firstPageResults.setTotal(countAffiliations);
		secondPageResults.setTotal(countAffiliations);
		morePageResults.setTotal(countAffiliations);
		firstPageResults.setLink(List.of(nextLink));
		when(clientProvider.getLocalWebserviceClient()).thenReturn(client);
		when(client.searchWithStrictHandling(Mockito.eq(OrganizationAffiliation.class), parametersCaptor.capture()))
				.thenReturn(firstPageResults, secondPageResults, morePageResults);

		List<Organization> organizations = organizationProvider.getOrganizations(identifier);

		assertThat(parametersCaptor.getAllValues().size(), is(2));
		assertThat(parametersCaptor.getAllValues().get(1).keySet(), hasItem("_page"));
		assertThat(parametersCaptor.getAllValues().get(1).get("_page").size(), is(1));
		assertThat(parametersCaptor.getAllValues().get(1).get("_page").get(0), is("2"));
		assertThat(organizations.size(), is(countOrganizations));
	}

}
