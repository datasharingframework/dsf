package dev.dsf.bpe.v2.client.dsf;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.BackboneElement;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.DocumentReference.DocumentReferenceContextComponent;
import org.hl7.fhir.r4.model.DocumentReference.DocumentReferenceRelatesToComponent;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.HealthcareService;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupPopulationComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupStratifierComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupPopulationComponent;
import org.hl7.fhir.r4.model.NamingSystem;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.OrganizationAffiliation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Patient.ContactComponent;
import org.hl7.fhir.r4.model.Patient.PatientLinkComponent;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Practitioner.PractitionerQualificationComponent;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Provenance;
import org.hl7.fhir.r4.model.Provenance.ProvenanceAgentComponent;
import org.hl7.fhir.r4.model.Provenance.ProvenanceEntityComponent;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResearchStudy;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.Subscription;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.ValueSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReferenceExtractorImpl implements ReferenceExtractor
{
	private static final Logger logger = LoggerFactory.getLogger(ReferenceExtractorImpl.class);

	private <R extends Resource> Stream<Reference> getReference(R resource, Predicate<R> hasReference,
			Function<R, Reference> getReference)
	{
		return hasReference.test(resource) ? Stream.of(getReference.apply(resource)) : Stream.empty();
	}

	private <R extends Resource> Stream<Reference> getReferences(R resource, Predicate<R> hasReference,
			Function<R, List<Reference>> getReference)
	{
		return hasReference.test(resource) ? Stream.of(getReference.apply(resource)).flatMap(List::stream)
				: Stream.empty();
	}

	private <R extends Resource, E extends BackboneElement> Stream<Reference> getBackboneElementsReference(R resource,
			Predicate<R> hasBackboneElements, Function<R, List<E>> getBackboneElements, Predicate<E> hasReference,
			Function<E, Reference> getReference)
	{
		if (hasBackboneElements.test(resource))
		{
			List<E> backboneElements = getBackboneElements.apply(resource);
			return backboneElements.stream().map(e -> getReference(e, hasReference, getReference))
					.flatMap(Function.identity());
		}
		else
			return Stream.empty();
	}

	private <E extends BackboneElement> Stream<Reference> getReference(E backboneElement, Predicate<E> hasReference,
			Function<E, Reference> getReference)
	{
		return hasReference.test(backboneElement) ? Stream.of(getReference.apply(backboneElement)) : Stream.empty();
	}

	private <R extends DomainResource, E extends BackboneElement> Stream<Reference> getBackboneElementReferences(
			R resource, Predicate<R> hasBackboneElement, Function<R, E> getBackboneElement, Predicate<E> hasReference,
			Function<E, List<Reference>> getReference)
	{
		if (hasBackboneElement.test(resource))
		{
			E backboneElement = getBackboneElement.apply(resource);
			return getReferences(backboneElement, hasReference, getReference);
		}
		else
			return Stream.empty();
	}

	private <R extends DomainResource, E extends BackboneElement> Stream<Reference> getBackboneElementReference(
			R resource, Predicate<R> hasBackboneElement, Function<R, E> getBackboneElement, Predicate<E> hasReference,
			Function<E, Reference> getReference)
	{
		if (hasBackboneElement.test(resource))
		{
			E backboneElement = getBackboneElement.apply(resource);
			return getReference(backboneElement, hasReference, getReference);
		}
		else
			return Stream.empty();
	}

	private <R extends DomainResource, E1 extends BackboneElement, E2 extends BackboneElement> Stream<Reference> getBackboneElements2Reference(
			R resource, Predicate<R> hasBackboneElements1, Function<R, List<E1>> getBackboneElements1,
			Predicate<E1> hasBackboneElements2, Function<E1, List<E2>> getBackboneElements2, Predicate<E2> hasReference,
			Function<E2, Reference> getReference)
	{
		if (hasBackboneElements1.test(resource))
		{
			List<E1> backboneElements1 = getBackboneElements1.apply(resource);
			return backboneElements1.stream().filter(e1 -> hasBackboneElements2.test(e1))
					.flatMap(e1 -> getBackboneElements2.apply(e1).stream())
					.map(e2 -> getReference(e2, hasReference, getReference)).flatMap(Function.identity());
		}
		else
			return Stream.empty();
	}

	private <R extends DomainResource, E1 extends BackboneElement, E2 extends BackboneElement, E3 extends BackboneElement, E4 extends BackboneElement> Stream<Reference> getBackboneElements4Reference(
			R resource, Predicate<R> hasBackboneElements1, Function<R, List<E1>> getBackboneElements1,
			Predicate<E1> hasBackboneElements2, Function<E1, List<E2>> getBackboneElements2,
			Predicate<E2> hasBackboneElements3, Function<E2, List<E3>> getBackboneElements3,
			Predicate<E3> hasBackboneElements4, Function<E3, List<E4>> getBackboneElements4, Predicate<E4> hasReference,
			Function<E4, Reference> getReference)
	{
		if (hasBackboneElements1.test(resource))
		{
			List<E1> backboneElements1 = getBackboneElements1.apply(resource);
			return backboneElements1.stream().filter(e1 -> hasBackboneElements2.test(e1))
					.flatMap(e1 -> getBackboneElements2.apply(e1).stream()).filter(e2 -> hasBackboneElements3.test(e2))
					.flatMap(e2 -> getBackboneElements3.apply(e2).stream()).filter(e3 -> hasBackboneElements4.test(e3))
					.flatMap(e3 -> getBackboneElements4.apply(e3).stream())
					.map(e4 -> getReference(e4, hasReference, getReference)).flatMap(Function.identity());
		}
		else
			return Stream.empty();
	}

	private <E extends BackboneElement> Stream<Reference> getReferences(E backboneElement, Predicate<E> hasReference,
			Function<E, List<Reference>> getReference)
	{
		return hasReference.test(backboneElement) ? Stream.of(getReference.apply(backboneElement)).flatMap(List::stream)
				: Stream.empty();
	}

	private Stream<Reference> getExtensionReferences(DomainResource resource)
	{
		var extensions = resource.getExtension().stream().filter(e -> e.getValue() instanceof Reference)
				.map(e -> (Reference) e.getValue());

		var extensionExtensions = resource.getExtension().stream().flatMap(this::getExtensionReferences);

		return Stream.concat(extensions, extensionExtensions);
	}

	private Stream<Reference> getExtensionReferences(BackboneElement resource)
	{
		var extensions = resource.getExtension().stream().filter(e -> e.getValue() instanceof Reference)
				.map(e -> (Reference) e.getValue());

		var extensionExtensions = resource.getExtension().stream().flatMap(this::getExtensionReferences);

		return Stream.concat(extensions, extensionExtensions);
	}

	private Stream<Reference> getExtensionReferences(Extension resource)
	{
		var extensions = resource.getExtension().stream().filter(e -> e.getValue() instanceof Reference)
				.map(e -> (Reference) e.getValue());

		var extensionExtensions = resource.getExtension().stream().flatMap(this::getExtensionReferences);

		return Stream.concat(extensions, extensionExtensions);
	}

	@SafeVarargs
	private Stream<Reference> concat(Stream<Reference>... streams)
	{
		if (streams.length == 0)
			return Stream.empty();
		else if (streams.length == 1)
			return streams[0];
		else if (streams.length == 2)
			return Stream.concat(streams[0], streams[1]);
		else
			return Arrays.stream(streams).flatMap(Function.identity());
	}

	@Override
	public Stream<Reference> getReferences(Resource resource)
	{
		return switch (resource)
		{
			case null -> Stream.empty();

			case ActivityDefinition ad -> getReferences(ad);

			// not implemented yet, special rules apply for tmp ids
			// case Bundle b -> getReferences(b);

			case Binary b -> getReferences(b);
			case CodeSystem cs -> getReferences(cs);
			case DocumentReference dr -> getReferences(dr);
			case Endpoint e -> getReferences(e);
			case Group g -> getReferences(g);
			case HealthcareService hs -> getReferences(hs);
			case Library l -> getReferences(l);
			case Location l -> getReferences(l);
			case Measure m -> getReferences(m);
			case MeasureReport mr -> getReferences(mr);
			case NamingSystem ns -> getReferences(ns);
			case OperationOutcome oo -> getReferences(oo);
			case Organization o -> getReferences(o);
			case OrganizationAffiliation oa -> getReferences(oa);
			case Patient p -> getReferences(p);
			case Practitioner p -> getReferences(p);
			case PractitionerRole pr -> getReferences(pr);
			case Provenance p -> getReferences(p);
			case Questionnaire q -> getReferences(q);
			case QuestionnaireResponse qr -> getReferences(qr);
			case ResearchStudy rs -> getReferences(rs);
			case StructureDefinition sd -> getReferences(sd);
			case Subscription s -> getReferences(s);
			case Task t -> getReferences(t);
			case ValueSet vs -> getReferences(vs);

			case DomainResource dr -> {
				logger.debug("DomainResource of type {} not supported, returning extension references only",
						dr.getClass().getName());
				yield getExtensionReferences(dr);
			}

			default -> {
				logger.debug("Resource of type {} not supported, returning no references",
						resource.getClass().getName());
				yield Stream.empty();
			}
		};
	}

	private Stream<Reference> getReferences(ActivityDefinition resource)
	{
		var subjectReference = getReference(resource, ActivityDefinition::hasSubjectReference,
				ActivityDefinition::getSubjectReference);
		var location = getReference(resource, ActivityDefinition::hasLocation, ActivityDefinition::getLocation);
		var productReference = getReference(resource, ActivityDefinition::hasProductReference,
				ActivityDefinition::getProductReference);
		var specimenRequirement = getReferences(resource, ActivityDefinition::hasSpecimenRequirement,
				ActivityDefinition::getSpecimenRequirement);
		var observationRequirement = getReferences(resource, ActivityDefinition::hasObservationRequirement,
				ActivityDefinition::getObservationRequirement);
		var observationResultRequirement = getReferences(resource, ActivityDefinition::hasObservationResultRequirement,
				ActivityDefinition::getObservationResultRequirement);

		var extensionReferences = getExtensionReferences(resource);

		return concat(subjectReference, location, productReference, specimenRequirement, observationRequirement,
				observationResultRequirement, extensionReferences);
	}

	private Stream<Reference> getReferences(Binary resource)
	{
		var securityContext = getReference(resource, Binary::hasSecurityContext, Binary::getSecurityContext);

		return securityContext;
	}

	private Stream<Reference> getReferences(CodeSystem resource)
	{
		var extensionReferences = getExtensionReferences(resource);

		return extensionReferences;
	}

	private Stream<Reference> getReferences(DocumentReference resource)
	{
		var subject = getReference(resource, DocumentReference::hasSubject, DocumentReference::getSubject);
		var author = getReferences(resource, DocumentReference::hasAuthor, DocumentReference::getAuthor);
		var authenticator = getReference(resource, DocumentReference::hasAuthenticator,
				DocumentReference::getAuthenticator);
		var custodian = getReference(resource, DocumentReference::hasCustodian, DocumentReference::getCustodian);
		var relatesToTarget = getBackboneElementsReference(resource, DocumentReference::hasRelatesTo,
				DocumentReference::getRelatesTo, DocumentReferenceRelatesToComponent::hasTarget,
				DocumentReferenceRelatesToComponent::getTarget);
		var contextEncounters = getBackboneElementReferences(resource, DocumentReference::hasContent,
				DocumentReference::getContext, DocumentReferenceContextComponent::hasEncounter,
				DocumentReferenceContextComponent::getEncounter);
		var contextSourcePatientInfo = getBackboneElementReference(resource, DocumentReference::hasContent,
				DocumentReference::getContext, DocumentReferenceContextComponent::hasSourcePatientInfo,
				DocumentReferenceContextComponent::getSourcePatientInfo);
		var contextRelated = getBackboneElementReferences(resource, DocumentReference::hasContent,
				DocumentReference::getContext, DocumentReferenceContextComponent::hasRelated,
				DocumentReferenceContextComponent::getRelated);

		var extensionReferences = getExtensionReferences(resource);

		return concat(subject, author, authenticator, custodian, relatesToTarget, contextEncounters,
				contextSourcePatientInfo, contextRelated, extensionReferences);
	}

	private Stream<Reference> getReferences(Endpoint resource)
	{
		var managingOrganization = getReference(resource, Endpoint::hasManagingOrganization,
				Endpoint::getManagingOrganization);

		var extensionReferences = getExtensionReferences(resource);

		return concat(managingOrganization, extensionReferences);
	}

	private Stream<Reference> getReferences(Group resource)
	{
		var managingEntity = getReference(resource, Group::hasManagingEntity, Group::getManagingEntity);
		var memberEntities = getBackboneElementsReference(resource, Group::hasMember, Group::getMember,
				Group.GroupMemberComponent::hasEntity, Group.GroupMemberComponent::getEntity);

		var extensionReferences = getExtensionReferences(resource);

		return concat(managingEntity, memberEntities, extensionReferences);
	}

	private Stream<Reference> getReferences(HealthcareService resource)
	{
		var providedBy = getReference(resource, HealthcareService::hasProvidedBy, HealthcareService::getProvidedBy);
		var locations = getReferences(resource, HealthcareService::hasLocation, HealthcareService::getLocation);
		var coverageAreas = getReferences(resource, HealthcareService::hasCoverageArea,
				HealthcareService::getCoverageArea);
		var endpoints = getReferences(resource, HealthcareService::hasEndpoint, HealthcareService::getEndpoint);

		var extensionReferences = getExtensionReferences(resource);

		return concat(providedBy, locations, coverageAreas, endpoints, extensionReferences);
	}

	private Stream<Reference> getReferences(Library resource)
	{
		var subject = getReference(resource, Library::hasSubjectReference, Library::getSubjectReference);

		var extensionReferences = getExtensionReferences(resource);

		return concat(subject, extensionReferences);
	}

	private Stream<Reference> getReferences(Location resource)
	{
		var managingOrganization = getReference(resource, Location::hasManagingOrganization,
				Location::getManagingOrganization);
		var partOf = getReference(resource, Location::hasPartOf, Location::getPartOf);
		var endpoints = getReferences(resource, Location::hasEndpoint, Location::getEndpoint);

		var extensionReferences = getExtensionReferences(resource);

		return concat(managingOrganization, partOf, endpoints, extensionReferences);
	}

	private Stream<Reference> getReferences(Measure resource)
	{
		var subject = getReference(resource, Measure::hasSubjectReference, Measure::getSubjectReference);

		var extensionReferences = getExtensionReferences(resource);

		return concat(subject, extensionReferences);
	}

	private Stream<Reference> getReferences(MeasureReport resource)
	{
		var subject = getReference(resource, MeasureReport::hasSubject, MeasureReport::getSubject);
		var reporter = getReference(resource, MeasureReport::hasReporter, MeasureReport::getReporter);
		var subjectResults1 = getBackboneElements2Reference(resource, MeasureReport::hasGroup, MeasureReport::getGroup,
				MeasureReportGroupComponent::hasPopulation, MeasureReportGroupComponent::getPopulation,
				MeasureReportGroupPopulationComponent::hasSubjectResults,
				MeasureReportGroupPopulationComponent::getSubjectResults);
		var subjectResults2 = getBackboneElements4Reference(resource, MeasureReport::hasGroup, MeasureReport::getGroup,
				MeasureReportGroupComponent::hasStratifier, MeasureReportGroupComponent::getStratifier,
				MeasureReportGroupStratifierComponent::hasStratum, MeasureReportGroupStratifierComponent::getStratum,
				StratifierGroupComponent::hasPopulation, StratifierGroupComponent::getPopulation,
				StratifierGroupPopulationComponent::hasSubjectResults,
				StratifierGroupPopulationComponent::getSubjectResults);
		var evaluatedResource = getReferences(resource, MeasureReport::hasEvaluatedResource,
				MeasureReport::getEvaluatedResource);

		var extensionReferences = getExtensionReferences(resource);

		return concat(subject, reporter, subjectResults1, subjectResults2, evaluatedResource, extensionReferences);
	}

	private Stream<Reference> getReferences(NamingSystem resource)
	{
		var extensionReferences = getExtensionReferences(resource);

		return extensionReferences;
	}

	private Stream<Reference> getReferences(OperationOutcome resource)
	{
		return getExtensionReferences(resource);
	}

	private Stream<Reference> getReferences(Organization resource)
	{
		var partOf = getReference(resource, Organization::hasPartOf, Organization::getPartOf);
		var endpoints = getReferences(resource, Organization::hasEndpoint, Organization::getEndpoint);

		var extensionReferences = getExtensionReferences(resource);

		return concat(partOf, endpoints, extensionReferences);
	}

	private Stream<Reference> getReferences(OrganizationAffiliation resource)
	{
		var organization = getReference(resource, OrganizationAffiliation::hasOrganization,
				OrganizationAffiliation::getOrganization);
		var participatingOrganization = getReference(resource, OrganizationAffiliation::hasParticipatingOrganization,
				OrganizationAffiliation::getParticipatingOrganization);
		var network = getReferences(resource, OrganizationAffiliation::hasNetwork, OrganizationAffiliation::getNetwork);
		var location = getReferences(resource, OrganizationAffiliation::hasLocation,
				OrganizationAffiliation::getLocation);
		var healthcareService = getReferences(resource, OrganizationAffiliation::hasHealthcareService,
				OrganizationAffiliation::getHealthcareService);
		var endpoint = getReferences(resource, OrganizationAffiliation::hasEndpoint,
				OrganizationAffiliation::getEndpoint);

		var extensionReferences = getExtensionReferences(resource);

		return concat(organization, participatingOrganization, network, location, healthcareService, endpoint,
				extensionReferences);
	}

	private Stream<Reference> getReferences(Patient resource)
	{
		var contactsOrganization = getBackboneElementsReference(resource, Patient::hasContact, Patient::getContact,
				ContactComponent::hasOrganization, ContactComponent::getOrganization);
		var generalPractitioners = getReferences(resource, Patient::hasGeneralPractitioner,
				Patient::getGeneralPractitioner);
		var managingOrganization = getReference(resource, Patient::hasManagingOrganization,
				Patient::getManagingOrganization);
		var linksOther = getBackboneElementsReference(resource, Patient::hasLink, Patient::getLink,
				PatientLinkComponent::hasOther, PatientLinkComponent::getOther);

		var extensionReferences = getExtensionReferences(resource);

		return concat(contactsOrganization, generalPractitioners, managingOrganization, linksOther,
				extensionReferences);
	}

	private Stream<Reference> getReferences(Practitioner resource)
	{
		var qualificationsIssuer = getBackboneElementsReference(resource, Practitioner::hasQualification,
				Practitioner::getQualification, PractitionerQualificationComponent::hasIssuer,
				PractitionerQualificationComponent::getIssuer);

		var extensionReferences = getExtensionReferences(resource);

		return concat(qualificationsIssuer, extensionReferences);
	}

	private Stream<Reference> getReferences(PractitionerRole resource)
	{
		var practitioner = getReference(resource, PractitionerRole::hasPractitioner, PractitionerRole::getPractitioner);
		var organization = getReference(resource, PractitionerRole::hasOrganization, PractitionerRole::getOrganization);
		var locations = getReferences(resource, PractitionerRole::hasLocation, PractitionerRole::getLocation);
		var healthcareServices = getReferences(resource, PractitionerRole::hasHealthcareService,
				PractitionerRole::getHealthcareService);
		var endpoints = getReferences(resource, PractitionerRole::hasEndpoint, PractitionerRole::getEndpoint);

		var extensionReferences = getExtensionReferences(resource);

		return concat(practitioner, organization, locations, healthcareServices, endpoints, extensionReferences);
	}

	private Stream<Reference> getReferences(Provenance resource)
	{
		var targets = getReferences(resource, Provenance::hasTarget, Provenance::getTarget);
		var location = getReference(resource, Provenance::hasLocation, Provenance::getLocation);
		var agentsWho = getBackboneElementsReference(resource, Provenance::hasAgent, Provenance::getAgent,
				ProvenanceAgentComponent::hasWho, ProvenanceAgentComponent::getWho);
		var agentsOnBehalfOf = getBackboneElementsReference(resource, Provenance::hasAgent, Provenance::getAgent,
				ProvenanceAgentComponent::hasOnBehalfOf, ProvenanceAgentComponent::getOnBehalfOf);
		var entitiesWhat = getBackboneElementsReference(resource, Provenance::hasEntity, Provenance::getEntity,
				ProvenanceEntityComponent::hasWhat, ProvenanceEntityComponent::getWhat);

		var extensionReferences = getExtensionReferences(resource);

		return concat(targets, location, agentsWho, agentsOnBehalfOf, entitiesWhat, extensionReferences);
	}

	private Stream<Reference> getReferences(Questionnaire resource)
	{
		var enableWhen = getBackboneElements2Reference(resource, Questionnaire::hasItem, Questionnaire::getItem,
				Questionnaire.QuestionnaireItemComponent::hasEnableWhen,
				Questionnaire.QuestionnaireItemComponent::getEnableWhen,
				Questionnaire.QuestionnaireItemEnableWhenComponent::hasAnswerReference,
				Questionnaire.QuestionnaireItemEnableWhenComponent::getAnswerReference);
		var answerOption = getBackboneElements2Reference(resource, Questionnaire::hasItem, Questionnaire::getItem,
				Questionnaire.QuestionnaireItemComponent::hasAnswerOption,
				Questionnaire.QuestionnaireItemComponent::getAnswerOption,
				Questionnaire.QuestionnaireItemAnswerOptionComponent::hasValueReference,
				Questionnaire.QuestionnaireItemAnswerOptionComponent::getValueReference);
		var initial = getBackboneElements2Reference(resource, Questionnaire::hasItem, Questionnaire::getItem,
				Questionnaire.QuestionnaireItemComponent::hasInitial,
				Questionnaire.QuestionnaireItemComponent::getInitial,
				Questionnaire.QuestionnaireItemInitialComponent::hasValueReference,
				Questionnaire.QuestionnaireItemInitialComponent::getValueReference);

		var extensionReferences = getExtensionReferences(resource);

		return concat(enableWhen, answerOption, initial, extensionReferences);
	}

	private Stream<Reference> getReferences(QuestionnaireResponse resource)
	{
		var author = getReference(resource, QuestionnaireResponse::hasAuthor, QuestionnaireResponse::getAuthor);
		var basedOn = getReferences(resource, QuestionnaireResponse::hasBasedOn, QuestionnaireResponse::getBasedOn);
		var encounter = getReference(resource, QuestionnaireResponse::hasEncounter,
				QuestionnaireResponse::getEncounter);
		var partOf = getReferences(resource, QuestionnaireResponse::hasPartOf, QuestionnaireResponse::getPartOf);
		var source = getReference(resource, QuestionnaireResponse::hasSource, QuestionnaireResponse::getSource);
		var subject = getReference(resource, QuestionnaireResponse::hasSubject, QuestionnaireResponse::getSubject);

		var extensionReferences = getExtensionReferences(resource);

		return concat(author, basedOn, encounter, partOf, source, subject, extensionReferences);
	}

	private Stream<Reference> getReferences(ResearchStudy resource)
	{
		var protocols = getReferences(resource, ResearchStudy::hasProtocol, ResearchStudy::getProtocol);
		var partOfs = getReferences(resource, ResearchStudy::hasPartOf, ResearchStudy::getPartOf);
		var enrollments = getReferences(resource, ResearchStudy::hasEnrollment, ResearchStudy::getEnrollment);
		var sponsor = getReference(resource, ResearchStudy::hasSponsor, ResearchStudy::getSponsor);
		var principalInvestigator = getReference(resource, ResearchStudy::hasPrincipalInvestigator,
				ResearchStudy::getPrincipalInvestigator);
		var sites = getReferences(resource, ResearchStudy::hasSite, ResearchStudy::getSite);

		var extensionReferences = getExtensionReferences(resource);

		return concat(protocols, partOfs, enrollments, sponsor, principalInvestigator, sites, extensionReferences);
	}

	private Stream<Reference> getReferences(StructureDefinition resource)
	{
		var extensionReferences = getExtensionReferences(resource);

		return extensionReferences;
	}

	private Stream<Reference> getReferences(Subscription resource)
	{
		var extensionReferences = getExtensionReferences(resource);

		return extensionReferences;
	}

	private Stream<Reference> getReferences(Task resource)
	{
		var basedOns = getReferences(resource, Task::hasBasedOn, Task::getBasedOn);
		var partOfs = getReferences(resource, Task::hasPartOf, Task::getPartOf);
		var focus = getReference(resource, Task::hasFocus, Task::getFocus);
		var forRef = getReference(resource, Task::hasFor, Task::getFor);
		var encounter = getReference(resource, Task::hasEncounter, Task::getEncounter);
		var requester = getReference(resource, Task::hasRequester, Task::getRequester);
		var owner = getReference(resource, Task::hasOwner, Task::getOwner);
		var location = getReference(resource, Task::hasLocation, Task::getLocation);
		var reasonReference = getReference(resource, Task::hasReasonReference, Task::getReasonReference);
		var insurance = getReferences(resource, Task::hasInsurance, Task::getInsurance);
		var relevanteHistories = getReferences(resource, Task::hasRelevantHistory, Task::getRelevantHistory);
		var restrictionRecipiets = getBackboneElementReferences(resource, Task::hasRestriction, Task::getRestriction,
				Task.TaskRestrictionComponent::hasRecipient, Task.TaskRestrictionComponent::getRecipient);

		var inputReferences = resource.getInput().stream().filter(in -> in.getValue() instanceof Reference)
				.map(in -> (Reference) in.getValue());
		var inputExtensionReferences = resource.getInput().stream().flatMap(this::getExtensionReferences);

		var outputReferences = resource.getOutput().stream().filter(out -> out.getValue() instanceof Reference)
				.map(in -> (Reference) in.getValue());
		var outputExtensionReferences = resource.getOutput().stream().flatMap(this::getExtensionReferences);

		var extensionReferences = getExtensionReferences(resource);

		return concat(basedOns, partOfs, focus, forRef, encounter, requester, owner, location, reasonReference,
				insurance, relevanteHistories, restrictionRecipiets, inputReferences, inputExtensionReferences,
				outputReferences, outputExtensionReferences, extensionReferences);
	}

	private Stream<Reference> getReferences(ValueSet resource)
	{
		var extensionReferences = getExtensionReferences(resource);

		return extensionReferences;
	}
}
