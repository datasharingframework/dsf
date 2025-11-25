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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.HealthcareService;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.NamingSystem;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.OrganizationAffiliation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Provenance;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.ResearchStudy;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.Subscription;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.ValueSet;

import dev.dsf.common.auth.conf.Identity;

public class HistoryIdentityFilterFactoryImpl implements HistoryIdentityFilterFactory
{
	private final Map<Class<? extends Resource>, Function<Identity, HistoryIdentityFilter>> filtersByResource = new HashMap<>();

	public HistoryIdentityFilterFactoryImpl()
	{
		filtersByResource.put(ActivityDefinition.class, ActivityDefinitionHistoryIdentityFilter::new);
		filtersByResource.put(Binary.class, BinaryHistoryIdentityFilter::new);
		filtersByResource.put(Bundle.class, BundleHistoryIdentityFilter::new);
		filtersByResource.put(CodeSystem.class, CodeSystemHistoryIdentityFilter::new);
		filtersByResource.put(DocumentReference.class, DocumentReferenceHistoryIdentityFilter::new);
		filtersByResource.put(Endpoint.class, EndpointHistoryIdentityFilter::new);
		filtersByResource.put(Group.class, GroupHistoryIdentityFilter::new);
		filtersByResource.put(HealthcareService.class, HealthcareServiceHistoryIdentityFilter::new);
		filtersByResource.put(Library.class, LibraryHistoryIdentityFilter::new);
		filtersByResource.put(Location.class, LocationHistoryIdentityFilter::new);
		filtersByResource.put(Measure.class, MeasureHistoryIdentityFilter::new);
		filtersByResource.put(MeasureReport.class, MeasureReportHistoryIdentityFilter::new);
		filtersByResource.put(NamingSystem.class, NamingSystemHistoryIdentityFilter::new);
		filtersByResource.put(OrganizationAffiliation.class, OrganizationAffiliationHistoryIdentityFilter::new);
		filtersByResource.put(Organization.class, OrganizationHistoryIdentityFilter::new);
		filtersByResource.put(Patient.class, PatientHistoryIdentityFilter::new);
		filtersByResource.put(Practitioner.class, PractitionerHistoryIdentityFilter::new);
		filtersByResource.put(PractitionerRole.class, PractitionerRoleHistoryIdentityFilter::new);
		filtersByResource.put(Provenance.class, ProvenanceHistoryIdentityFilter::new);
		filtersByResource.put(Questionnaire.class, QuestionnaireHistoryIdentityFilter::new);
		filtersByResource.put(QuestionnaireResponse.class, QuestionnaireResponseHistoryIdentityFilter::new);
		filtersByResource.put(ResearchStudy.class, ResearchStudyHistoryIdentityFilter::new);
		filtersByResource.put(StructureDefinition.class, StructureDefinitionHistoryIdentityFilter::new);
		filtersByResource.put(Subscription.class, SubscriptionHistoryIdentityFilter::new);
		filtersByResource.put(Task.class, TaskHistoryIdentityFilter::new);
		filtersByResource.put(ValueSet.class, ValueSetHistoryIdentityFilter::new);
	}

	@Override
	public HistoryIdentityFilter getIdentityFilter(Identity identity, Class<? extends Resource> resourceType)
	{
		Function<Identity, HistoryIdentityFilter> factory = filtersByResource.get(resourceType);
		if (factory == null)
			throw new IllegalArgumentException(HistoryIdentityFilter.class.getSimpleName() + " for "
					+ resourceType.getClass().getName() + " not found");
		else
			return factory.apply(identity);
	}

	@Override
	public List<HistoryIdentityFilter> getIdentityFilters(Identity identity)
	{
		return filtersByResource.values().stream().map(f -> f.apply(identity)).collect(Collectors.toList());
	}
}
