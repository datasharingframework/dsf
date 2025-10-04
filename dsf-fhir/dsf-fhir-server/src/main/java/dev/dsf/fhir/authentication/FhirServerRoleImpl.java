package dev.dsf.fhir.authentication;

import static dev.dsf.fhir.authentication.FhirServerRoleImpl.Operation.CREATE;
import static dev.dsf.fhir.authentication.FhirServerRoleImpl.Operation.DELETE;
import static dev.dsf.fhir.authentication.FhirServerRoleImpl.Operation.HISTORY;
import static dev.dsf.fhir.authentication.FhirServerRoleImpl.Operation.PERMANENT_DELETE;
import static dev.dsf.fhir.authentication.FhirServerRoleImpl.Operation.READ;
import static dev.dsf.fhir.authentication.FhirServerRoleImpl.Operation.SEARCH;
import static dev.dsf.fhir.authentication.FhirServerRoleImpl.Operation.UPDATE;
import static dev.dsf.fhir.authentication.FhirServerRoleImpl.Operation.WEBSOCKET;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.Subscription;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.ValueSet;

import dev.dsf.common.auth.conf.DsfRole;
import dev.dsf.common.auth.conf.RoleConfig.RoleKeyAndValues;

public record FhirServerRoleImpl(Operation operation, List<ResourceType> resourceTypes) implements FhirServerRole
{
	public static enum Operation
	{
		CREATE, READ, UPDATE, DELETE, SEARCH, HISTORY, PERMANENT_DELETE, WEBSOCKET;

		public FhirServerRole toFhirServerRoleAllResources()
		{
			return new FhirServerRoleImpl(this);
		}

		public static boolean isValid(String operation)
		{
			return operation != null && !operation.isBlank()
					&& Stream.of(Operation.values()).map(Enum::name).anyMatch(n -> n.equals(operation));
		}
	}

	public static final Set<FhirServerRole> LOCAL_ORGANIZATION = EnumSet
			.of(CREATE, READ, UPDATE, DELETE, SEARCH, HISTORY, PERMANENT_DELETE, WEBSOCKET).stream()
			.map(Operation::toFhirServerRoleAllResources).collect(Collectors.toSet());

	public static final Set<FhirServerRole> REMOTE_ORGANIZATION = EnumSet
			.of(CREATE, READ, UPDATE, DELETE, SEARCH, HISTORY).stream().map(Operation::toFhirServerRoleAllResources)
			.collect(Collectors.toSet());

	public static final Set<FhirServerRole> INITIAL_DATA_LOADER = EnumSet.of(CREATE, DELETE, UPDATE).stream()
			.map(Operation::toFhirServerRoleAllResources).collect(Collectors.toSet());

	private static ResourceType forResourceClass(Class<? extends Resource> resourceClass)
	{
		if (ActivityDefinition.class.equals(resourceClass))
			return ResourceType.ActivityDefinition;
		else if (Binary.class.equals(resourceClass))
			return ResourceType.Binary;
		else if (Bundle.class.equals(resourceClass))
			return ResourceType.Bundle;
		else if (CodeSystem.class.equals(resourceClass))
			return ResourceType.CodeSystem;
		else if (DocumentReference.class.equals(resourceClass))
			return ResourceType.DocumentReference;
		else if (Endpoint.class.equals(resourceClass))
			return ResourceType.Endpoint;
		else if (Group.class.equals(resourceClass))
			return ResourceType.Group;
		else if (HealthcareService.class.equals(resourceClass))
			return ResourceType.HealthcareService;
		else if (Library.class.equals(resourceClass))
			return ResourceType.Library;
		else if (Location.class.equals(resourceClass))
			return ResourceType.Location;
		else if (Measure.class.equals(resourceClass))
			return ResourceType.Measure;
		else if (MeasureReport.class.equals(resourceClass))
			return ResourceType.MeasureReport;
		else if (NamingSystem.class.equals(resourceClass))
			return ResourceType.NamingSystem;
		else if (OrganizationAffiliation.class.equals(resourceClass))
			return ResourceType.OrganizationAffiliation;
		else if (Organization.class.equals(resourceClass))
			return ResourceType.Organization;
		else if (Patient.class.equals(resourceClass))
			return ResourceType.Patient;
		else if (Practitioner.class.equals(resourceClass))
			return ResourceType.Practitioner;
		else if (PractitionerRole.class.equals(resourceClass))
			return ResourceType.PractitionerRole;
		else if (Provenance.class.equals(resourceClass))
			return ResourceType.Provenance;
		else if (Questionnaire.class.equals(resourceClass))
			return ResourceType.Questionnaire;
		else if (QuestionnaireResponse.class.equals(resourceClass))
			return ResourceType.QuestionnaireResponse;
		else if (ResearchStudy.class.equals(resourceClass))
			return ResourceType.ResearchStudy;
		else if (StructureDefinition.class.equals(resourceClass))
			return ResourceType.StructureDefinition;
		else if (Subscription.class.equals(resourceClass))
			return ResourceType.Subscription;
		else if (Task.class.equals(resourceClass))
			return ResourceType.Task;
		else if (ValueSet.class.equals(resourceClass))
			return ResourceType.ValueSet;
		else
			throw new IllegalArgumentException("Resource class '" + resourceClass.getName() + "' not supported");
	}

	public static FhirServerRole create(Class<? extends Resource> resourceClass)
	{
		return new FhirServerRoleImpl(CREATE, forResourceClass(resourceClass));
	}

	public static FhirServerRole read(Class<? extends Resource> resourceClass)
	{
		return read(forResourceClass(resourceClass));
	}

	public static FhirServerRole read(ResourceType resourceType)
	{
		return new FhirServerRoleImpl(READ, resourceType);
	}

	public static FhirServerRole update(Class<? extends Resource> resourceClass)
	{
		return new FhirServerRoleImpl(UPDATE, forResourceClass(resourceClass));
	}

	public static FhirServerRole delete(Class<? extends Resource> resourceClass)
	{
		return new FhirServerRoleImpl(DELETE, forResourceClass(resourceClass));
	}

	public static FhirServerRole search(Class<? extends Resource> resourceClass)
	{
		return search(forResourceClass(resourceClass));
	}

	public static FhirServerRole search(ResourceType resourceType)
	{
		return new FhirServerRoleImpl(SEARCH, resourceType);
	}

	public static FhirServerRole history(Class<? extends Resource> resourceClass)
	{
		return history(forResourceClass(resourceClass));
	}

	public static FhirServerRole history(ResourceType resourceType)
	{
		return new FhirServerRoleImpl(HISTORY, resourceType);
	}

	public static FhirServerRole permanentDelete(Class<? extends Resource> resourceClass)
	{
		return permanentDelete(forResourceClass(resourceClass));
	}

	public static FhirServerRole permanentDelete(ResourceType resourceType)
	{
		return new FhirServerRoleImpl(PERMANENT_DELETE, resourceType);
	}

	public static FhirServerRole websocket(Class<? extends Resource> resourceClass)
	{
		return new FhirServerRoleImpl(WEBSOCKET, forResourceClass(resourceClass));
	}

	private static final Set<String> SUPPORTED_RESOURCES = Set.of(ResourceType.ActivityDefinition, ResourceType.Binary,
			ResourceType.Bundle, ResourceType.CodeSystem, ResourceType.DocumentReference, ResourceType.Endpoint,
			ResourceType.Group, ResourceType.HealthcareService, ResourceType.Library, ResourceType.Location,
			ResourceType.Measure, ResourceType.MeasureReport, ResourceType.NamingSystem,
			ResourceType.OrganizationAffiliation, ResourceType.Organization, ResourceType.Patient,
			ResourceType.Practitioner, ResourceType.PractitionerRole, ResourceType.Provenance,
			ResourceType.Questionnaire, ResourceType.QuestionnaireResponse, ResourceType.ResearchStudy,
			ResourceType.StructureDefinition, ResourceType.Task, ResourceType.ValueSet).stream().map(Enum::name)
			.collect(Collectors.toSet());

	private static boolean isSupportedResource(String resource)
	{
		return resource != null && !resource.isBlank() && SUPPORTED_RESOURCES.contains(resource);
	}

	public static FhirServerRoleImpl from(RoleKeyAndValues keyAndValues)
	{
		if (Operation.isValid(keyAndValues.key())
				&& keyAndValues.values().stream().allMatch(FhirServerRoleImpl::isSupportedResource))
		{
			Operation operation = Operation.valueOf(keyAndValues.key());
			List<ResourceType> resourceTypes = keyAndValues.values().stream().map(ResourceType::valueOf).toList();

			return new FhirServerRoleImpl(operation, resourceTypes);
		}
		else
			return null;
	}

	public FhirServerRoleImpl(Operation operation, List<ResourceType> resourceTypes)
	{
		Objects.requireNonNull(operation, "operation");
		Objects.requireNonNull(resourceTypes, "resourceTypes");

		this.operation = operation;
		this.resourceTypes = resourceTypes;
	}

	public FhirServerRoleImpl(Operation operation, ResourceType... resourceTypes)
	{
		this(operation, List.of(resourceTypes));
	}

	@Override
	public String name()
	{
		return operation.name();
	}

	@Override
	public boolean matches(DsfRole role)
	{
		if (this == role)
			return true;

		return role instanceof FhirServerRoleImpl i
				? operation == i.operation && (resourceTypes.isEmpty() || resourceTypes.containsAll(i.resourceTypes))
				: false;
	}

	@Override
	public String toString()
	{
		if (!resourceTypes.isEmpty())
			return operation.name() + " "
					+ resourceTypes.stream().map(ResourceType::name).collect(Collectors.joining(", ", "[", "]"));
		else
			return operation.name();
	}
}
