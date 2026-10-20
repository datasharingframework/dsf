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
package dev.dsf.fhir.history.filter;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.function.Function;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import dev.dsf.common.auth.conf.Identity;

@RunWith(Parameterized.class)
public class HistoryIdentityFilterTest
{
	@Parameters(name = "{1}")
	public static List<Object[]> data()
	{
		return List.of(row(ActivityDefinitionHistoryIdentityFilter::new, "ActivityDefinition"),
				row(BinaryHistoryIdentityFilter::new, "Binary"), row(BundleHistoryIdentityFilter::new, "Bundle"),
				row(CodeSystemHistoryIdentityFilter::new, "CodeSystem"),
				row(DocumentReferenceHistoryIdentityFilter::new, "DocumentReference"),
				row(EndpointHistoryIdentityFilter::new, "Endpoint"), row(GroupHistoryIdentityFilter::new, "Group"),
				row(HealthcareServiceHistoryIdentityFilter::new, "HealthcareService"),
				row(LibraryHistoryIdentityFilter::new, "Library"), row(LocationHistoryIdentityFilter::new, "Location"),
				row(MeasureHistoryIdentityFilter::new, "Measure"),
				row(MeasureReportHistoryIdentityFilter::new, "MeasureReport"),
				row(NamingSystemHistoryIdentityFilter::new, "NamingSystem"),
				row(OrganizationAffiliationHistoryIdentityFilter::new, "OrganizationAffiliation"),
				row(OrganizationHistoryIdentityFilter::new, "Organization"),
				row(PatientHistoryIdentityFilter::new, "Patient"),
				row(PractitionerHistoryIdentityFilter::new, "Practitioner"),
				row(PractitionerRoleHistoryIdentityFilter::new, "PractitionerRole"),
				row(ProvenanceHistoryIdentityFilter::new, "Provenance"),
				row(QuestionnaireHistoryIdentityFilter::new, "Questionnaire"),
				row(QuestionnaireResponseHistoryIdentityFilter::new, "QuestionnaireResponse"),
				row(ResearchStudyHistoryIdentityFilter::new, "ResearchStudy"),
				row(StructureDefinitionHistoryIdentityFilter::new, "StructureDefinition"),
				row(SubscriptionHistoryIdentityFilter::new, "Subscription"),
				row(TaskHistoryIdentityFilter::new, "Task"), row(ValueSetHistoryIdentityFilter::new, "ValueSet"));
	}

	private static Object[] row(Function<Identity, HistoryIdentityFilter> factory, String resourceType)
	{
		return new Object[] { factory, resourceType };
	}

	@Parameter(0)
	public Function<Identity, HistoryIdentityFilter> filterFactory;

	@Parameter(1)
	public String resourceType;

	@Test
	public void filterQueryScopesToResourceType()
	{
		HistoryIdentityFilter filter = filterFactory.apply(mock(Identity.class));

		String query = filter.getFilterQuery();
		assertNotNull(query);
		assertTrue("filter query must scope to type '" + resourceType + "' but was: " + query,
				query.contains("type = '" + resourceType + "'"));
	}

	@Test
	public void filterQueryIsDefinedAndParameterCountNonNegative()
	{
		HistoryIdentityFilter filter = filterFactory.apply(mock(Identity.class));

		assertTrue(filter.isDefined());
		assertTrue(filter.getSqlParameterCount() >= 0);
	}
}
