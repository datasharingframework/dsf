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
package dev.dsf.fhir.spring.config;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.dsf.fhir.authorization.ActivityDefinitionAuthorizationRule;
import dev.dsf.fhir.authorization.AuthorizationRule;
import dev.dsf.fhir.authorization.AuthorizationRuleProvider;
import dev.dsf.fhir.authorization.AuthorizationRuleProviderImpl;
import dev.dsf.fhir.authorization.BinaryAuthorizationRule;
import dev.dsf.fhir.authorization.BundleAuthorizationRule;
import dev.dsf.fhir.authorization.CodeSystemAuthorizationRule;
import dev.dsf.fhir.authorization.DocumentReferenceAuthorizationRule;
import dev.dsf.fhir.authorization.EndpointAuthorizationRule;
import dev.dsf.fhir.authorization.GroupAuthorizationRule;
import dev.dsf.fhir.authorization.HealthcareServiceAuthorizationRule;
import dev.dsf.fhir.authorization.LibraryAuthorizationRule;
import dev.dsf.fhir.authorization.LocationAuthorizationRule;
import dev.dsf.fhir.authorization.MeasureAuthorizationRule;
import dev.dsf.fhir.authorization.MeasureReportAuthorizationRule;
import dev.dsf.fhir.authorization.NamingSystemAuthorizationRule;
import dev.dsf.fhir.authorization.OrganizationAffiliationAuthorizationRule;
import dev.dsf.fhir.authorization.OrganizationAuthorizationRule;
import dev.dsf.fhir.authorization.PatientAuthorizationRule;
import dev.dsf.fhir.authorization.PractitionerAuthorizationRule;
import dev.dsf.fhir.authorization.PractitionerRoleAuthorizationRule;
import dev.dsf.fhir.authorization.ProvenanceAuthorizationRule;
import dev.dsf.fhir.authorization.QuestionnaireAuthorizationRule;
import dev.dsf.fhir.authorization.QuestionnaireResponseAuthorizationRule;
import dev.dsf.fhir.authorization.ResearchStudyAuthorizationRule;
import dev.dsf.fhir.authorization.RootAuthorizationRule;
import dev.dsf.fhir.authorization.StructureDefinitionAuthorizationRule;
import dev.dsf.fhir.authorization.SubscriptionAuthorizationRule;
import dev.dsf.fhir.authorization.TaskAuthorizationRule;
import dev.dsf.fhir.authorization.ValueSetAuthorizationRule;
import dev.dsf.fhir.authorization.process.ProcessAuthorizationHelper;
import dev.dsf.fhir.authorization.process.ProcessAuthorizationHelperImpl;
import dev.dsf.fhir.authorization.read.ReadAccessHelper;
import dev.dsf.fhir.authorization.read.ReadAccessHelperImpl;
import dev.dsf.fhir.dao.command.AuthorizationHelper;
import dev.dsf.fhir.dao.command.AuthorizationHelperImpl;

@Configuration
public class AuthorizationConfig
{
	@Autowired
	private AuthenticationConfig authenticationConfig;

	@Autowired
	private DaoConfig daoConfig;

	@Autowired
	private HelperConfig helperConfig;

	@Autowired
	private PropertiesConfig propertiesConfig;

	@Autowired
	private ReferenceConfig referenceConfig;

	@Autowired
	private FhirConfig fhirConfig;

	@Bean
	public ReadAccessHelper readAccessHelper()
	{
		return new ReadAccessHelperImpl();
	}

	@Bean
	public ProcessAuthorizationHelper processAuthorizationHelper()
	{
		return new ProcessAuthorizationHelperImpl();
	}

	@Bean
	public AuthorizationRule<ActivityDefinition> activityDefinitionAuthorizationRule()
	{
		return new ActivityDefinitionAuthorizationRule(daoConfig.daoProvider(), propertiesConfig.getDsfServerBaseUrl(),
				referenceConfig.referenceResolver(), authenticationConfig.organizationProvider(), readAccessHelper(),
				helperConfig.parameterConverter(), processAuthorizationHelper());
	}

	@Bean
	public AuthorizationRule<Binary> binaryAuthorizationRule()
	{
		return new BinaryAuthorizationRule(daoConfig.daoProvider(), propertiesConfig.getDsfServerBaseUrl(),
				referenceConfig.referenceResolver(), authenticationConfig.organizationProvider(), readAccessHelper(),
				helperConfig.parameterConverter(),

				// Binary and Task not supported as securityContext rule
				activityDefinitionAuthorizationRule(), bundleAuthorizationRule(), codeSystemAuthorizationRule(),
				documentReferenceAuthorizationRule(), endpointAuthorizationRule(), groupAuthorizationRule(),
				healthcareServiceAuthorizationRule(), libraryAuthorizationRule(), locationAuthorizationRule(),
				measureAuthorizationRule(), measureReportAuthorizationRule(), namingSystemAuthorizationRule(),
				organizationAuthorizationRule(), organizationAffiliationAuthorizationRule(), patientAuthorizationRule(),
				practitionerAuthorizationRule(), practitionerRoleAuthorizationRule(), provenanceAuthorizationRule(),
				questionnaireAuthorizationRule(), questionnaireResponseAuthorizationRule(),
				researchStudyAuthorizationRule(), structureDefinitionAuthorizationRule(),
				subscriptionAuthorizationRule(), valueSetAuthorizationRule());
	}

	@Bean
	public AuthorizationRule<Bundle> bundleAuthorizationRule()
	{
		return new BundleAuthorizationRule(daoConfig.daoProvider(), propertiesConfig.getDsfServerBaseUrl(),
				referenceConfig.referenceResolver(), authenticationConfig.organizationProvider(), readAccessHelper(),
				helperConfig.parameterConverter());
	}

	@Bean
	public AuthorizationRule<CodeSystem> codeSystemAuthorizationRule()
	{
		return new CodeSystemAuthorizationRule(daoConfig.daoProvider(), propertiesConfig.getDsfServerBaseUrl(),
				referenceConfig.referenceResolver(), authenticationConfig.organizationProvider(), readAccessHelper(),
				helperConfig.parameterConverter());
	}

	@Bean
	public AuthorizationRule<DocumentReference> documentReferenceAuthorizationRule()
	{
		return new DocumentReferenceAuthorizationRule(daoConfig.daoProvider(), propertiesConfig.getDsfServerBaseUrl(),
				referenceConfig.referenceResolver(), authenticationConfig.organizationProvider(), readAccessHelper(),
				helperConfig.parameterConverter());
	}

	@Bean
	public AuthorizationRule<Endpoint> endpointAuthorizationRule()
	{
		return new EndpointAuthorizationRule(daoConfig.daoProvider(), propertiesConfig.getDsfServerBaseUrl(),
				referenceConfig.referenceResolver(), authenticationConfig.organizationProvider(), readAccessHelper(),
				helperConfig.parameterConverter());
	}

	@Bean
	public AuthorizationRule<Group> groupAuthorizationRule()
	{
		return new GroupAuthorizationRule(daoConfig.daoProvider(), propertiesConfig.getDsfServerBaseUrl(),
				referenceConfig.referenceResolver(), authenticationConfig.organizationProvider(), readAccessHelper(),
				helperConfig.parameterConverter());
	}

	@Bean
	public AuthorizationRule<HealthcareService> healthcareServiceAuthorizationRule()
	{
		return new HealthcareServiceAuthorizationRule(daoConfig.daoProvider(), propertiesConfig.getDsfServerBaseUrl(),
				referenceConfig.referenceResolver(), authenticationConfig.organizationProvider(), readAccessHelper(),
				helperConfig.parameterConverter());
	}

	@Bean
	public AuthorizationRule<Library> libraryAuthorizationRule()
	{
		return new LibraryAuthorizationRule(daoConfig.daoProvider(), propertiesConfig.getDsfServerBaseUrl(),
				referenceConfig.referenceResolver(), authenticationConfig.organizationProvider(), readAccessHelper(),
				helperConfig.parameterConverter());
	}

	@Bean
	public AuthorizationRule<Location> locationAuthorizationRule()
	{
		return new LocationAuthorizationRule(daoConfig.daoProvider(), propertiesConfig.getDsfServerBaseUrl(),
				referenceConfig.referenceResolver(), authenticationConfig.organizationProvider(), readAccessHelper(),
				helperConfig.parameterConverter());
	}

	@Bean
	public AuthorizationRule<Measure> measureAuthorizationRule()
	{
		return new MeasureAuthorizationRule(daoConfig.daoProvider(), propertiesConfig.getDsfServerBaseUrl(),
				referenceConfig.referenceResolver(), authenticationConfig.organizationProvider(), readAccessHelper(),
				helperConfig.parameterConverter());
	}

	@Bean
	public AuthorizationRule<MeasureReport> measureReportAuthorizationRule()
	{
		return new MeasureReportAuthorizationRule(daoConfig.daoProvider(), propertiesConfig.getDsfServerBaseUrl(),
				referenceConfig.referenceResolver(), authenticationConfig.organizationProvider(), readAccessHelper(),
				helperConfig.parameterConverter());
	}

	@Bean
	public AuthorizationRule<NamingSystem> namingSystemAuthorizationRule()
	{
		return new NamingSystemAuthorizationRule(daoConfig.daoProvider(), propertiesConfig.getDsfServerBaseUrl(),
				referenceConfig.referenceResolver(), authenticationConfig.organizationProvider(), readAccessHelper(),
				helperConfig.parameterConverter());
	}

	@Bean
	public AuthorizationRule<Organization> organizationAuthorizationRule()
	{
		return new OrganizationAuthorizationRule(daoConfig.daoProvider(), propertiesConfig.getDsfServerBaseUrl(),
				referenceConfig.referenceResolver(), authenticationConfig.organizationProvider(), readAccessHelper(),
				helperConfig.parameterConverter());
	}

	@Bean
	public AuthorizationRule<OrganizationAffiliation> organizationAffiliationAuthorizationRule()
	{
		return new OrganizationAffiliationAuthorizationRule(daoConfig.daoProvider(),
				propertiesConfig.getDsfServerBaseUrl(), referenceConfig.referenceResolver(),
				authenticationConfig.organizationProvider(), readAccessHelper(), helperConfig.parameterConverter());
	}

	@Bean
	public AuthorizationRule<Patient> patientAuthorizationRule()
	{
		return new PatientAuthorizationRule(daoConfig.daoProvider(), propertiesConfig.getDsfServerBaseUrl(),
				referenceConfig.referenceResolver(), authenticationConfig.organizationProvider(), readAccessHelper(),
				helperConfig.parameterConverter());
	}

	@Bean
	public AuthorizationRule<Practitioner> practitionerAuthorizationRule()
	{
		return new PractitionerAuthorizationRule(daoConfig.daoProvider(), propertiesConfig.getDsfServerBaseUrl(),
				referenceConfig.referenceResolver(), authenticationConfig.organizationProvider(), readAccessHelper(),
				helperConfig.parameterConverter());
	}

	@Bean
	public AuthorizationRule<PractitionerRole> practitionerRoleAuthorizationRule()
	{
		return new PractitionerRoleAuthorizationRule(daoConfig.daoProvider(), propertiesConfig.getDsfServerBaseUrl(),
				referenceConfig.referenceResolver(), authenticationConfig.organizationProvider(), readAccessHelper(),
				helperConfig.parameterConverter());
	}

	@Bean
	public AuthorizationRule<Provenance> provenanceAuthorizationRule()
	{
		return new ProvenanceAuthorizationRule(daoConfig.daoProvider(), propertiesConfig.getDsfServerBaseUrl(),
				referenceConfig.referenceResolver(), authenticationConfig.organizationProvider(), readAccessHelper(),
				helperConfig.parameterConverter());
	}

	@Bean
	public AuthorizationRule<Questionnaire> questionnaireAuthorizationRule()
	{
		return new QuestionnaireAuthorizationRule(daoConfig.daoProvider(), propertiesConfig.getDsfServerBaseUrl(),
				referenceConfig.referenceResolver(), authenticationConfig.organizationProvider(), readAccessHelper(),
				helperConfig.parameterConverter());
	}

	@Bean
	public AuthorizationRule<QuestionnaireResponse> questionnaireResponseAuthorizationRule()
	{
		return new QuestionnaireResponseAuthorizationRule(daoConfig.daoProvider(),
				propertiesConfig.getDsfServerBaseUrl(), referenceConfig.referenceResolver(),
				authenticationConfig.organizationProvider(), readAccessHelper(), helperConfig.parameterConverter());
	}

	@Bean
	public AuthorizationRule<ResearchStudy> researchStudyAuthorizationRule()
	{
		return new ResearchStudyAuthorizationRule(daoConfig.daoProvider(), propertiesConfig.getDsfServerBaseUrl(),
				referenceConfig.referenceResolver(), authenticationConfig.organizationProvider(), readAccessHelper(),
				helperConfig.parameterConverter());
	}

	@Bean
	public AuthorizationRule<StructureDefinition> structureDefinitionAuthorizationRule()
	{
		return new StructureDefinitionAuthorizationRule(daoConfig.daoProvider(), propertiesConfig.getDsfServerBaseUrl(),
				referenceConfig.referenceResolver(), authenticationConfig.organizationProvider(), readAccessHelper(),
				helperConfig.parameterConverter());
	}

	@Bean
	public AuthorizationRule<Subscription> subscriptionAuthorizationRule()
	{
		return new SubscriptionAuthorizationRule(daoConfig.daoProvider(), propertiesConfig.getDsfServerBaseUrl(),
				referenceConfig.referenceResolver(), authenticationConfig.organizationProvider(), readAccessHelper(),
				helperConfig.parameterConverter());
	}

	@Bean
	public AuthorizationRule<Task> taskAuthorizationRule()
	{
		return new TaskAuthorizationRule(daoConfig.daoProvider(), propertiesConfig.getDsfServerBaseUrl(),
				referenceConfig.referenceResolver(), authenticationConfig.organizationProvider(), readAccessHelper(),
				helperConfig.parameterConverter(), processAuthorizationHelper(), fhirConfig.fhirContext(),
				authenticationConfig.endpointProvider());
	}

	@Bean
	public AuthorizationRule<ValueSet> valueSetAuthorizationRule()
	{
		return new ValueSetAuthorizationRule(daoConfig.daoProvider(), propertiesConfig.getDsfServerBaseUrl(),
				referenceConfig.referenceResolver(), authenticationConfig.organizationProvider(), readAccessHelper(),
				helperConfig.parameterConverter());
	}

	@Bean
	public AuthorizationRuleProvider authorizationRuleProvider()
	{
		return new AuthorizationRuleProviderImpl(activityDefinitionAuthorizationRule(), binaryAuthorizationRule(),
				bundleAuthorizationRule(), codeSystemAuthorizationRule(), documentReferenceAuthorizationRule(),
				endpointAuthorizationRule(), groupAuthorizationRule(), healthcareServiceAuthorizationRule(),
				libraryAuthorizationRule(), locationAuthorizationRule(), measureAuthorizationRule(),
				measureReportAuthorizationRule(), namingSystemAuthorizationRule(), organizationAuthorizationRule(),
				organizationAffiliationAuthorizationRule(), patientAuthorizationRule(), practitionerAuthorizationRule(),
				practitionerRoleAuthorizationRule(), provenanceAuthorizationRule(), questionnaireAuthorizationRule(),
				questionnaireResponseAuthorizationRule(), researchStudyAuthorizationRule(),
				structureDefinitionAuthorizationRule(), subscriptionAuthorizationRule(), taskAuthorizationRule(),
				valueSetAuthorizationRule());
	}

	@Bean
	public AuthorizationHelper authorizationHelper()
	{
		return new AuthorizationHelperImpl(authorizationRuleProvider(), helperConfig.responseGenerator());
	}

	@Bean
	public AuthorizationRule<Resource> rootAuthorizationRule()
	{
		return new RootAuthorizationRule();
	}
}
