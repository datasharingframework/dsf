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
package dev.dsf.fhir.webservice.secure;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import org.hl7.fhir.r4.model.ActivityDefinition;
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
import org.hl7.fhir.r4.model.Subscription;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import dev.dsf.fhir.dao.ActivityDefinitionDao;
import dev.dsf.fhir.dao.BundleDao;
import dev.dsf.fhir.dao.CodeSystemDao;
import dev.dsf.fhir.dao.DocumentReferenceDao;
import dev.dsf.fhir.dao.EndpointDao;
import dev.dsf.fhir.dao.GroupDao;
import dev.dsf.fhir.dao.HealthcareServiceDao;
import dev.dsf.fhir.dao.LibraryDao;
import dev.dsf.fhir.dao.LocationDao;
import dev.dsf.fhir.dao.MeasureDao;
import dev.dsf.fhir.dao.MeasureReportDao;
import dev.dsf.fhir.dao.NamingSystemDao;
import dev.dsf.fhir.dao.OrganizationAffiliationDao;
import dev.dsf.fhir.dao.OrganizationDao;
import dev.dsf.fhir.dao.PatientDao;
import dev.dsf.fhir.dao.PractitionerDao;
import dev.dsf.fhir.dao.PractitionerRoleDao;
import dev.dsf.fhir.dao.ProvenanceDao;
import dev.dsf.fhir.dao.QuestionnaireDao;
import dev.dsf.fhir.dao.QuestionnaireResponseDao;
import dev.dsf.fhir.dao.ResearchStudyDao;
import dev.dsf.fhir.dao.ResourceDao;
import dev.dsf.fhir.dao.SubscriptionDao;
import dev.dsf.fhir.dao.ValueSetDao;
import dev.dsf.fhir.webservice.specification.ActivityDefinitionService;
import dev.dsf.fhir.webservice.specification.BasicResourceService;
import dev.dsf.fhir.webservice.specification.BundleService;
import dev.dsf.fhir.webservice.specification.CodeSystemService;
import dev.dsf.fhir.webservice.specification.DocumentReferenceService;
import dev.dsf.fhir.webservice.specification.EndpointService;
import dev.dsf.fhir.webservice.specification.GroupService;
import dev.dsf.fhir.webservice.specification.HealthcareServiceService;
import dev.dsf.fhir.webservice.specification.LibraryService;
import dev.dsf.fhir.webservice.specification.LocationService;
import dev.dsf.fhir.webservice.specification.MeasureReportService;
import dev.dsf.fhir.webservice.specification.MeasureService;
import dev.dsf.fhir.webservice.specification.NamingSystemService;
import dev.dsf.fhir.webservice.specification.OrganizationAffiliationService;
import dev.dsf.fhir.webservice.specification.OrganizationService;
import dev.dsf.fhir.webservice.specification.PatientService;
import dev.dsf.fhir.webservice.specification.PractitionerRoleService;
import dev.dsf.fhir.webservice.specification.PractitionerService;
import dev.dsf.fhir.webservice.specification.ProvenanceService;
import dev.dsf.fhir.webservice.specification.QuestionnaireResponseService;
import dev.dsf.fhir.webservice.specification.QuestionnaireService;
import dev.dsf.fhir.webservice.specification.ResearchStudyService;
import dev.dsf.fhir.webservice.specification.SubscriptionService;
import dev.dsf.fhir.webservice.specification.ValueSetService;

@RunWith(Parameterized.class)
public class ResourceServiceSecureTest
		extends AbstractResourceServiceSecureTest<Resource, BasicResourceService<Resource>, ResourceDao<Resource>>
{
	@Parameters(name = "{0}")
	public static Collection<Object[]> data()
	{
		return List.of(new Object[][] {

				{ "ActivityDefinition", ActivityDefinition.class, ActivityDefinitionService.class,
						ActivityDefinitionDao.class, (Supplier<ActivityDefinition>) ActivityDefinition::new,
						(ResourceServiceSecureFactory<ActivityDefinition, ActivityDefinitionService, ActivityDefinitionDao>) ActivityDefinitionServiceSecure::new },

				{ "Bundle", Bundle.class, BundleService.class, BundleDao.class, (Supplier<Bundle>) Bundle::new,
						(ResourceServiceSecureFactory<Bundle, BundleService, BundleDao>) BundleServiceSecure::new },

				{ "CodeSystem", CodeSystem.class, CodeSystemService.class, CodeSystemDao.class,
						(Supplier<CodeSystem>) CodeSystem::new,
						(ResourceServiceSecureFactory<CodeSystem, CodeSystemService, CodeSystemDao>) CodeSystemServiceSecure::new },

				{ "DocumentReference", DocumentReference.class, DocumentReferenceService.class,
						DocumentReferenceDao.class, (Supplier<DocumentReference>) DocumentReference::new,
						(ResourceServiceSecureFactory<DocumentReference, DocumentReferenceService, DocumentReferenceDao>) DocumentReferenceServiceSecure::new },

				{ "Endpoint", Endpoint.class, EndpointService.class, EndpointDao.class,
						(Supplier<Endpoint>) Endpoint::new,
						(ResourceServiceSecureFactory<Endpoint, EndpointService, EndpointDao>) EndpointServiceSecure::new },

				{ "Group", Group.class, GroupService.class, GroupDao.class, (Supplier<Group>) Group::new,
						(ResourceServiceSecureFactory<Group, GroupService, GroupDao>) GroupServiceSecure::new },

				{ "HealthcareService", HealthcareService.class, HealthcareServiceService.class,
						HealthcareServiceDao.class, (Supplier<HealthcareService>) HealthcareService::new,
						(ResourceServiceSecureFactory<HealthcareService, HealthcareServiceService, HealthcareServiceDao>) HealthcareServiceServiceSecure::new },

				{ "Library", Library.class, LibraryService.class, LibraryDao.class, (Supplier<Library>) Library::new,
						(ResourceServiceSecureFactory<Library, LibraryService, LibraryDao>) LibraryServiceSecure::new },

				{ "Location", Location.class, LocationService.class, LocationDao.class,
						(Supplier<Location>) Location::new,
						(ResourceServiceSecureFactory<Location, LocationService, LocationDao>) LocationServiceSecure::new },

				{ "Measure", Measure.class, MeasureService.class, MeasureDao.class, (Supplier<Measure>) Measure::new,
						(ResourceServiceSecureFactory<Measure, MeasureService, MeasureDao>) MeasureServiceSecure::new },

				{ "MeasureReport", MeasureReport.class, MeasureReportService.class, MeasureReportDao.class,
						(Supplier<MeasureReport>) MeasureReport::new,
						(ResourceServiceSecureFactory<MeasureReport, MeasureReportService, MeasureReportDao>) MeasureReportServiceSecure::new },

				{ "NamingSystem", NamingSystem.class, NamingSystemService.class, NamingSystemDao.class,
						(Supplier<NamingSystem>) NamingSystem::new,
						(ResourceServiceSecureFactory<NamingSystem, NamingSystemService, NamingSystemDao>) NamingSystemServiceSecure::new },

				{ "OrganizationAffiliation", OrganizationAffiliation.class, OrganizationAffiliationService.class,
						OrganizationAffiliationDao.class,
						(Supplier<OrganizationAffiliation>) OrganizationAffiliation::new,
						(ResourceServiceSecureFactory<OrganizationAffiliation, OrganizationAffiliationService, OrganizationAffiliationDao>) OrganizationAffiliationServiceSecure::new },

				{ "Organization", Organization.class, OrganizationService.class, OrganizationDao.class,
						(Supplier<Organization>) Organization::new,
						(ResourceServiceSecureFactory<Organization, OrganizationService, OrganizationDao>) OrganizationServiceSecure::new },

				{ "Patient", Patient.class, PatientService.class, PatientDao.class, (Supplier<Patient>) Patient::new,
						(ResourceServiceSecureFactory<Patient, PatientService, PatientDao>) PatientServiceSecure::new },

				{ "Practitioner", Practitioner.class, PractitionerService.class, PractitionerDao.class,
						(Supplier<Practitioner>) Practitioner::new,
						(ResourceServiceSecureFactory<Practitioner, PractitionerService, PractitionerDao>) PractitionerServiceSecure::new },

				{ "PractitionerRole", PractitionerRole.class, PractitionerRoleService.class, PractitionerRoleDao.class,
						(Supplier<PractitionerRole>) PractitionerRole::new,
						(ResourceServiceSecureFactory<PractitionerRole, PractitionerRoleService, PractitionerRoleDao>) PractitionerRoleServiceSecure::new },

				{ "Provenance", Provenance.class, ProvenanceService.class, ProvenanceDao.class,
						(Supplier<Provenance>) Provenance::new,
						(ResourceServiceSecureFactory<Provenance, ProvenanceService, ProvenanceDao>) ProvenanceServiceSecure::new },

				{ "Questionnaire", Questionnaire.class, QuestionnaireService.class, QuestionnaireDao.class,
						(Supplier<Questionnaire>) Questionnaire::new,
						(ResourceServiceSecureFactory<Questionnaire, QuestionnaireService, QuestionnaireDao>) QuestionnaireServiceSecure::new },

				{ "QuestionnaireResponse", QuestionnaireResponse.class, QuestionnaireResponseService.class,
						QuestionnaireResponseDao.class, (Supplier<QuestionnaireResponse>) QuestionnaireResponse::new,
						(ResourceServiceSecureFactory<QuestionnaireResponse, QuestionnaireResponseService, QuestionnaireResponseDao>) QuestionnaireResponseServiceSecure::new },

				{ "ResearchStudy", ResearchStudy.class, ResearchStudyService.class, ResearchStudyDao.class,
						(Supplier<ResearchStudy>) ResearchStudy::new,
						(ResourceServiceSecureFactory<ResearchStudy, ResearchStudyService, ResearchStudyDao>) ResearchStudyServiceSecure::new },

				{ "Subscription", Subscription.class, SubscriptionService.class, SubscriptionDao.class,
						(Supplier<Subscription>) Subscription::new,
						(ResourceServiceSecureFactory<Subscription, SubscriptionService, SubscriptionDao>) SubscriptionServiceSecure::new },

				{ "ValueSet", ValueSet.class, ValueSetService.class, ValueSetDao.class,
						(Supplier<ValueSet>) ValueSet::new,
						(ResourceServiceSecureFactory<ValueSet, ValueSetService, ValueSetDao>) ValueSetServiceSecure::new },

		});
	}

	public ResourceServiceSecureTest(String label, Class<Resource> resourceClass,
			Class<BasicResourceService<Resource>> serviceClass, Class<ResourceDao<Resource>> daoClass,
			Supplier<Resource> resouceSupplier,
			ResourceServiceSecureFactory<Resource, BasicResourceService<Resource>, ResourceDao<Resource>> resourceServiceSecureFactory)
	{
		super(resourceClass, serviceClass, daoClass, resouceSupplier, resourceServiceSecureFactory);
	}
}
