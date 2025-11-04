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
package dev.dsf.fhir.webservice.impl;

import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementImplementationComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementKind;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestResourceComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestResourceOperationComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestResourceSearchParamComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementSoftwareComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.ConditionalDeleteStatus;
import org.hl7.fhir.r4.model.CapabilityStatement.ConditionalReadStatus;
import org.hl7.fhir.r4.model.CapabilityStatement.ReferenceHandlingPolicy;
import org.hl7.fhir.r4.model.CapabilityStatement.ResourceVersionPolicy;
import org.hl7.fhir.r4.model.CapabilityStatement.RestfulCapabilityMode;
import org.hl7.fhir.r4.model.CapabilityStatement.TypeRestfulInteraction;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Enumerations.FHIRVersion;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Extension;
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
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.StructureDefinition.StructureDefinitionKind;
import org.hl7.fhir.r4.model.Subscription;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.UrlType;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.model.codesystems.RestfulSecurityService;
import org.springframework.beans.factory.InitializingBean;

import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import ca.uhn.fhir.rest.api.Constants;
import dev.dsf.common.buildinfo.BuildInfoReader;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.help.SummaryMode;
import dev.dsf.fhir.search.IncludeParameterDefinition;
import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.*;
import dev.dsf.fhir.search.parameters.basic.AbstractSearchParameter;
import dev.dsf.fhir.search.parameters.rev.include.AbstractRevIncludeParameter;
import dev.dsf.fhir.search.parameters.rev.include.EndpointOrganizationRevInclude;
import dev.dsf.fhir.search.parameters.rev.include.OrganizationAffiliationParticipatingOrganizationRevInclude;
import dev.dsf.fhir.search.parameters.rev.include.OrganizationAffiliationPrimaryOrganizationRevInclude;
import dev.dsf.fhir.search.parameters.rev.include.OrganizationEndpointRevInclude;
import dev.dsf.fhir.search.parameters.rev.include.ResearchStudyEnrollmentRevInclude;
import dev.dsf.fhir.service.DefaultProfileProvider;
import dev.dsf.fhir.webservice.base.AbstractBasicService;
import dev.dsf.fhir.webservice.specification.ConformanceService;
import dev.dsf.fhir.websocket.ServerEndpoint;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

public class ConformanceServiceImpl extends AbstractBasicService implements ConformanceService, InitializingBean
{
	private static final CodeableConcept OAUTH = new CodeableConcept()
			.addCoding(new Coding(RestfulSecurityService.OAUTH.getSystem(), RestfulSecurityService.OAUTH.toCode(),
					RestfulSecurityService.OAUTH.getDisplay()));
	private static final CodeableConcept CERTIFICATES = new CodeableConcept()
			.addCoding(new Coding(RestfulSecurityService.CERTIFICATES.getSystem(),
					RestfulSecurityService.CERTIFICATES.toCode(), RestfulSecurityService.CERTIFICATES.getDisplay()));

	private static final class StructureDefinitionDistinctByUrl implements Comparable<StructureDefinitionDistinctByUrl>
	{
		final StructureDefinition structureDefinition;
		final String url;

		StructureDefinitionDistinctByUrl(StructureDefinition structureDefinition)
		{
			this.structureDefinition = structureDefinition;
			this.url = structureDefinition.getUrl();
		}

		public StructureDefinition get()
		{
			return structureDefinition;
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(url);
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null || getClass() != obj.getClass())
				return false;
			StructureDefinitionDistinctByUrl other = (StructureDefinitionDistinctByUrl) obj;
			return Objects.equals(url, other.url);
		}

		@Override
		public int compareTo(StructureDefinitionDistinctByUrl o)
		{
			return url.compareTo(o.url);
		}
	}

	private final String serverBase;
	private final int defaultPageCount;
	private final BuildInfoReader buildInfoReader;
	private final ParameterConverter parameterConverter;
	private final IValidationSupport validationSupport;
	private final DefaultProfileProvider defaultProfileProvider;

	private final List<CodeableConcept> securityServices;

	public ConformanceServiceImpl(String serverBase, int defaultPageCount, BuildInfoReader buildInfoReader,
			ParameterConverter parameterConverter, IValidationSupport validationSupport,
			DefaultProfileProvider defaultProfileProvider, boolean oAuthEnabled)
	{
		this.serverBase = serverBase;
		this.defaultPageCount = defaultPageCount;
		this.buildInfoReader = buildInfoReader;
		this.parameterConverter = parameterConverter;
		this.validationSupport = validationSupport;
		this.defaultProfileProvider = defaultProfileProvider;
		this.securityServices = oAuthEnabled ? List.of(CERTIFICATES, OAUTH) : List.of(CERTIFICATES);
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(serverBase, "serverBase");
		Objects.requireNonNull(buildInfoReader, "buildInfoReader");
		Objects.requireNonNull(parameterConverter, "parameterConverter");
		Objects.requireNonNull(validationSupport, "validationSupport");
		Objects.requireNonNull(defaultProfileProvider, "defaultProfileProvider");
	}

	@Override
	public Response getMetadata(String mode, UriInfo uri, HttpHeaders headers)
	{
		return Response
				.ok(createCapabilityStatement(), parameterConverter.getMediaTypeThrowIfNotSupported(uri, headers))
				.build();
	}

	private String getVersion(BuildInfoReader buildInfoReader)
	{
		String branch = buildInfoReader.getBuildBranch();
		String number = buildInfoReader.getBuildNumber();
		number = number.length() >= 7 ? number.substring(0, 7) : number;
		String version = buildInfoReader.getProjectVersion();

		return version + " (" + branch + "/" + number + ")";
	}

	private CapabilityStatement createCapabilityStatement()
	{
		CapabilityStatement statement = new CapabilityStatement();
		statement.setStatus(PublicationStatus.ACTIVE);
		statement.setDate(buildInfoReader.getBuildDateAsDate());
		statement.setPublisher("DSF Developers");
		statement.setKind(CapabilityStatementKind.INSTANCE);
		statement.setSoftware(new CapabilityStatementSoftwareComponent());
		statement.getSoftware().setName("Data Sharing Framework");
		statement.getSoftware().setVersion(getVersion(buildInfoReader));
		statement.setImplementation(new CapabilityStatementImplementationComponent());
		// statement.getImplementation().setDescription("Implementation Description - TODO"); // TODO
		statement.getImplementation().setUrl(serverBase);
		statement.setFhirVersion(FHIRVersion._4_0_1);
		statement.setFormat(List.of(new CodeType(Constants.CT_FHIR_JSON_NEW), new CodeType(Constants.CT_FHIR_XML_NEW)));
		CapabilityStatementRestComponent rest = statement.addRest();
		rest.setMode(RestfulCapabilityMode.SERVER);
		rest.getSecurity().setService(securityServices);
		Extension websocketExtension = rest.addExtension();
		websocketExtension.setUrl("http://hl7.org/fhir/StructureDefinition/capabilitystatement-websocket");
		websocketExtension.setValue(new UrlType(serverBase.replace("http", "ws") + ServerEndpoint.PATH));

		var resources = List.of(ActivityDefinition.class, Binary.class, Bundle.class, CodeSystem.class,
				DocumentReference.class, Endpoint.class, Group.class, HealthcareService.class, Library.class,
				Location.class, Measure.class, MeasureReport.class, NamingSystem.class, Organization.class,
				OrganizationAffiliation.class, Patient.class, PractitionerRole.class, Practitioner.class,
				Provenance.class, Questionnaire.class, QuestionnaireResponse.class, ResearchStudy.class,
				StructureDefinition.class, Subscription.class, Task.class, ValueSet.class);

		var searchParameters = new HashMap<Class<? extends Resource>, List<Class<? extends AbstractSearchParameter<?>>>>();
		var revIncludeParameters = new HashMap<Class<? extends Resource>, List<Class<? extends AbstractRevIncludeParameter>>>();

		searchParameters.put(ActivityDefinition.class,
				List.of(ActivityDefinitionDate.class, ActivityDefinitionUrl.class, ActivityDefinitionIdentifier.class,
						ActivityDefinitionVersion.class, ActivityDefinitionName.class, ActivityDefinitionStatus.class));

		searchParameters.put(Binary.class, List.of(BinaryContentType.class));

		searchParameters.put(Bundle.class, List.of(BundleIdentifier.class));

		searchParameters.put(CodeSystem.class, List.of(CodeSystemDate.class, CodeSystemIdentifier.class,
				CodeSystemName.class, CodeSystemUrl.class, CodeSystemVersion.class, CodeSystemStatus.class));

		searchParameters.put(DocumentReference.class, List.of(DocumentReferenceIdentifier.class));

		searchParameters.put(Endpoint.class, List.of(EndpointAddress.class, EndpointIdentifier.class,
				EndpointName.class, EndpointOrganization.class, EndpointStatus.class));
		revIncludeParameters.put(Endpoint.class, List.of(OrganizationEndpointRevInclude.class));

		searchParameters.put(Group.class, List.of(GroupIdentifier.class));
		revIncludeParameters.put(Group.class, List.of(ResearchStudyEnrollmentRevInclude.class));

		searchParameters.put(HealthcareService.class,
				List.of(HealthcareServiceActive.class, HealthcareServiceIdentifier.class, HealthcareServiceName.class));

		searchParameters.put(Library.class, List.of(LibraryDate.class, LibraryIdentifier.class, LibraryName.class,
				LibraryStatus.class, LibraryUrl.class, LibraryVersion.class));

		searchParameters.put(Location.class, List.of(LocationIdentifier.class, LocationName.class));

		searchParameters.put(Measure.class, List.of(MeasureDate.class, MeasureDependsOn.class, MeasureIdentifier.class,
				MeasureName.class, MeasureStatus.class, MeasureUrl.class, MeasureVersion.class));

		searchParameters.put(MeasureReport.class, List.of(MeasureReportIdentifier.class));

		searchParameters.put(NamingSystem.class,
				List.of(NamingSystemDate.class, NamingSystemName.class, NamingSystemStatus.class));

		searchParameters.put(Organization.class, List.of(OrganizationActive.class, OrganizationEndpoint.class,
				OrganizationIdentifier.class, OrganizationName.class, OrganizationType.class));
		revIncludeParameters.put(Organization.class,
				List.of(EndpointOrganizationRevInclude.class,
						OrganizationAffiliationParticipatingOrganizationRevInclude.class,
						OrganizationAffiliationPrimaryOrganizationRevInclude.class));

		searchParameters.put(OrganizationAffiliation.class,
				List.of(OrganizationAffiliationActive.class, OrganizationAffiliationEndpoint.class,
						OrganizationAffiliationIdentifier.class, OrganizationAffiliationParticipatingOrganization.class,
						OrganizationAffiliationPrimaryOrganization.class, OrganizationAffiliationRole.class));

		searchParameters.put(Patient.class, List.of(PatientActive.class, PatientIdentifier.class));

		searchParameters.put(Practitioner.class, List.of(PractitionerActive.class, PractitionerIdentifier.class));

		searchParameters.put(PractitionerRole.class,
				List.of(PractitionerRoleActive.class, PractitionerRoleIdentifier.class,
						PractitionerRoleOrganization.class, PractitionerRolePractitioner.class));

		searchParameters.put(Questionnaire.class,
				List.of(QuestionnaireDate.class, QuestionnaireIdentifier.class, QuestionnaireName.class,
						QuestionnaireStatus.class, QuestionnaireUrl.class, QuestionnaireVersion.class));

		searchParameters.put(QuestionnaireResponse.class,
				List.of(QuestionnaireResponseAuthor.class, QuestionnaireResponseAuthored.class,
						QuestionnaireResponseIdentifier.class, QuestionnaireResponseStatus.class));

		searchParameters.put(ResearchStudy.class, List.of(ResearchStudyIdentifier.class, ResearchStudyEnrollment.class,
				ResearchStudyPrincipalInvestigator.class));

		searchParameters.put(StructureDefinition.class,
				List.of(StructureDefinitionDate.class, StructureDefinitionIdentifier.class,
						StructureDefinitionName.class, StructureDefinitionStatus.class, StructureDefinitionUrl.class,
						StructureDefinitionVersion.class));

		searchParameters.put(Subscription.class, List.of(SubscriptionCriteria.class, SubscriptionPayload.class,
				SubscriptionStatus.class, SubscriptionType.class));

		searchParameters.put(Task.class, List.of(TaskAuthoredOn.class, TaskIdentifier.class, TaskModified.class,
				TaskRequester.class, TaskStatus.class));

		searchParameters.put(ValueSet.class, List.of(ValueSetDate.class, ValueSetIdentifier.class, ValueSetName.class,
				ValueSetUrl.class, ValueSetVersion.class, ValueSetStatus.class));

		var operations = new HashMap<Class<? extends DomainResource>, List<CapabilityStatementRestResourceOperationComponent>>();

		var snapshotOperation = createOperation("snapshot",
				"http://hl7.org/fhir/OperationDefinition/StructureDefinition-snapshot",
				"Generates a StructureDefinition instance with a snapshot, based on a differential in a specified StructureDefinition");
		operations.put(StructureDefinition.class, List.of(snapshotOperation));

		var standardSortableSearchParameters = List.of(ResourceId.class, ResourceLastUpdated.class,
				ResourceProfile.class);

		Map<String, List<CanonicalType>> profileUrlsByResource = validationSupport.fetchAllStructureDefinitions()
				.stream().filter(r -> r instanceof StructureDefinition).map(r -> (StructureDefinition) r)
				.filter(s -> StructureDefinitionKind.RESOURCE.equals(s.getKind()) && !s.getAbstract()
						&& EnumSet.of(PublicationStatus.ACTIVE, PublicationStatus.DRAFT).contains(s.getStatus())
						&& !s.getUrl().contains("hl7.org"))
				.map(StructureDefinitionDistinctByUrl::new).distinct().sorted()
				.map(StructureDefinitionDistinctByUrl::get).collect(Collectors.groupingBy(StructureDefinition::getType,
						Collectors.mapping(s -> new CanonicalType(s.getUrl()), Collectors.toList())));

		for (Class<? extends Resource> resource : resources)
		{
			ResourceDef resourceDefAnnotation = resource.getAnnotation(ResourceDef.class);
			ResourceType resourceType = ResourceType.valueOf(resourceDefAnnotation.name());

			CapabilityStatementRestResourceComponent r = rest.addResource();
			r.setVersioning(ResourceVersionPolicy.VERSIONED);
			r.setReadHistory(true);
			r.setUpdateCreate(false);
			r.setConditionalCreate(true);
			r.setConditionalRead(ConditionalReadStatus.FULLSUPPORT);
			r.setConditionalUpdate(true);
			r.setConditionalDelete(ConditionalDeleteStatus.SINGLE);
			r.addReferencePolicy(ReferenceHandlingPolicy.LITERAL);
			r.addReferencePolicy(ReferenceHandlingPolicy.LOGICAL);

			Optional<String> defaultProfile = defaultProfileProvider.getDefaultProfile(resourceType);

			r.setType(resourceDefAnnotation.name());
			r.setProfile(defaultProfile.orElse(resourceDefAnnotation.profile()));

			r.addInteraction().setCode(TypeRestfulInteraction.CREATE);
			r.addInteraction().setCode(TypeRestfulInteraction.READ);
			r.addInteraction().setCode(TypeRestfulInteraction.VREAD);
			r.addInteraction().setCode(TypeRestfulInteraction.UPDATE);
			r.addInteraction().setCode(TypeRestfulInteraction.DELETE);
			r.addInteraction().setCode(TypeRestfulInteraction.SEARCHTYPE);

			var resourceSearchParameters = searchParameters.getOrDefault(resource, List.of());
			resourceSearchParameters.stream().map(this::createSearchParameter)
					.sorted(Comparator.comparing(CapabilityStatementRestResourceSearchParamComponent::getName))
					.forEach(r::addSearchParam);

			r.addSearchParam(createCountParameter(defaultPageCount));
			r.addSearchParam(createFormatParameter());
			r.addSearchParam(createIdParameter());

			var includes = resourceSearchParameters.stream().map(p -> p.getAnnotation(IncludeParameterDefinition.class))
					.filter(def -> def != null).collect(Collectors.toList());
			if (!includes.isEmpty())
			{
				r.addSearchParam(createIncludeParameter(includes));
				r.setSearchInclude(includes.stream().flatMap(this::toIncludeParameterNames).sorted()
						.map(StringType::new).collect(Collectors.toList()));
			}

			r.addSearchParam(createLastUpdatedParameter());
			r.addSearchParam(createPageParameter());
			r.addSearchParam(createPrettyParameter());
			r.addSearchParam(createSummaryParameter());
			r.addSearchParam(createProfileParameter());
			r.addSearchParam(createSinceParameter());
			r.addSearchParam(createAtParameter());

			var resourceRevIncludeParameters = revIncludeParameters.getOrDefault(resource, List.of());
			var revIncludes = resourceRevIncludeParameters.stream()
					.map(p -> p.getAnnotation(IncludeParameterDefinition.class)).filter(def -> def != null)
					.collect(Collectors.toList());
			if (!revIncludes.isEmpty())
			{
				r.addSearchParam(createRevIncludeParameter(revIncludes));
				r.setSearchRevInclude(revIncludes.stream().flatMap(this::toIncludeParameterNames).sorted()
						.map(StringType::new).collect(Collectors.toList()));
			}

			r.addSearchParam(createSortParameter(
					Stream.concat(standardSortableSearchParameters.stream(), resourceSearchParameters.stream())));

			r.getSearchParam().sort(Comparator.comparing(CapabilityStatementRestResourceSearchParamComponent::getName));

			operations.getOrDefault(resource, List.of()).forEach(r::addOperation);

			List<CanonicalType> supportedProfiles = profileUrlsByResource.getOrDefault(resourceDefAnnotation.name(),
					List.of());

			Stream<String> fromDb = supportedProfiles.stream().filter(CanonicalType::hasValue)
					.map(CanonicalType::getValue);
			Stream<String> fromProvider = defaultProfileProvider.getSecondaryDefaultProfiles(resourceType).stream();

			r.setSupportedProfile(
					Stream.concat(fromDb, fromProvider).filter(p -> defaultProfile.map(d -> !d.equals(p)).orElse(true))
							.distinct().sorted().map(CanonicalType::new).toList());
		}

		return statement;
	}

	private CapabilityStatementRestResourceSearchParamComponent createIncludeParameter(
			List<IncludeParameterDefinition> includes)
	{
		String values = includes.stream().flatMap(this::toIncludeParameterNames).sorted()
				.collect(Collectors.joining(", ", "[", "]"));
		return createSearchParameter("_include", "", SearchParamType.SPECIAL,
				"Additional resources to return, allowed values: " + values
						+ " (use one _include parameter for every resource to include)");
	}

	private CapabilityStatementRestResourceSearchParamComponent createRevIncludeParameter(
			List<IncludeParameterDefinition> revIncludes)
	{
		String values = revIncludes.stream().flatMap(this::toIncludeParameterNames).sorted()
				.collect(Collectors.joining(", ", "[", "]"));

		return createSearchParameter("_revinclude", "", SearchParamType.SPECIAL,
				"Additional resources to return, allowed values: " + values
						+ " (use one _revinclude parameter for every resource to include)");
	}

	private Stream<String> toIncludeParameterNames(IncludeParameterDefinition def)
	{
		return Arrays.stream(def.targetResourceTypes()).map(target -> target.getAnnotation(ResourceDef.class).name())
				.map(target -> def.resourceType().getAnnotation(ResourceDef.class).name() + ":" + def.parameterName()
						+ ":" + target);
	}

	private CapabilityStatementRestResourceSearchParamComponent createSortParameter(
			@SuppressWarnings("rawtypes") Stream<Class<? extends AbstractSearchParameter>> parameters)
	{
		String values = parameters.map(p -> p.getAnnotation(SearchParameterDefinition.class))
				.map(SearchParameterDefinition::name).sorted().collect(Collectors.joining(", ", "[", "]"));

		return createSearchParameter("_sort", "", SearchParamType.SPECIAL,
				"Specify the returned order, allowed values: " + values
						+ " (one or multiple as comma separated string), prefix with '-' for reversed order");
	}

	private CapabilityStatementRestResourceSearchParamComponent createLastUpdatedParameter()
	{
		return createSearchParameter(ResourceLastUpdated.class);
	}

	private CapabilityStatementRestResourceSearchParamComponent createPageParameter()
	{
		return createSearchParameter("_page", "", SearchParamType.NUMBER,
				"Specify the page number, 1 if not specified");
	}

	private CapabilityStatementRestResourceSearchParamComponent createCountParameter(int defaultPageCount)
	{
		return createSearchParameter("_count", "", SearchParamType.NUMBER,
				"Specify the numer of returned resources per page, " + defaultPageCount + " if not specified");
	}

	private CapabilityStatementRestResourceSearchParamComponent createAtParameter()
	{
		return createSearchParameter("_at", "", SearchParamType.DATE,
				"Only include resource versions that were current at some point during the time period specified in the date time value");
	}

	private CapabilityStatementRestResourceSearchParamComponent createSinceParameter()
	{
		return createSearchParameter("_since", "", SearchParamType.SPECIAL,
				"Only include resource versions that were created at or after the given instant in time");
	}

	private CapabilityStatementRestResourceSearchParamComponent createFormatParameter()
	{
		String formatValues = Stream
				.of(Stream.of(ParameterConverter.JSON_FORMAT), ParameterConverter.JSON_FORMATS.stream(),
						Stream.of(ParameterConverter.XML_FORMAT), ParameterConverter.XML_FORMATS.stream())
				.flatMap(Function.identity()).collect(Collectors.joining(", ", "[", "]"));
		CapabilityStatementRestResourceSearchParamComponent createFormatParameter = createSearchParameter("_format", "",
				SearchParamType.SPECIAL,
				"Specify the returned format of the payload response, allowed values: " + formatValues);
		return createFormatParameter;
	}

	private CapabilityStatementRestResourceSearchParamComponent createIdParameter()
	{
		return createSearchParameter(ResourceId.class);
	}

	private CapabilityStatementRestResourceSearchParamComponent createPrettyParameter()
	{
		CapabilityStatementRestResourceSearchParamComponent createFormatParameter = createSearchParameter("_pretty", "",
				SearchParamType.SPECIAL,
				"Pretty printed response for human convenience, allowed values: [true, false]");
		return createFormatParameter;
	}

	private CapabilityStatementRestResourceSearchParamComponent createSummaryParameter()
	{
		CapabilityStatementRestResourceSearchParamComponent createFormatParameter = createSearchParameter("_summary",
				"", SearchParamType.SPECIAL,
				"Predefined short form of the resource, allowed values: " + Arrays.stream(SummaryMode.values())
						.map(SummaryMode::toString).collect(Collectors.joining(", ", "[", "]")));
		return createFormatParameter;
	}

	private CapabilityStatementRestResourceSearchParamComponent createProfileParameter()
	{
		return createSearchParameter(ResourceProfile.class);
	}

	private CapabilityStatementRestResourceOperationComponent createOperation(String name, String definition,
			String documentation)
	{
		return new CapabilityStatementRestResourceOperationComponent().setName(name).setDefinition(definition)
				.setDocumentation(documentation);
	}

	private CapabilityStatementRestResourceSearchParamComponent createSearchParameter(Class<?> parameter)
	{
		SearchParameterDefinition d = parameter.getAnnotation(SearchParameterDefinition.class);
		return createSearchParameter(d.name(), d.definition(), d.type(), d.documentation());
	}

	private CapabilityStatementRestResourceSearchParamComponent createSearchParameter(String name, String definition,
			SearchParamType type, String documentation)
	{
		return new CapabilityStatementRestResourceSearchParamComponent().setName(name).setDefinition(definition)
				.setType(type).setDocumentation(documentation);
	}
}
