package dev.dsf.bpe.plugin;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.NamingSystem;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.ValueSet;

import dev.dsf.bpe.v1.constants.NamingSystems.TaskIdentifier;

public final class ProcessesResource
{
	public static ProcessesResource from(Resource resource)
	{
		Objects.requireNonNull(resource, "resource");

		if (resource instanceof ActivityDefinition a)
			return fromMetadataResource(a);
		else if (resource instanceof CodeSystem c)
			return fromMetadataResource(c);
		else if (resource instanceof Library l)
			return fromMetadataResource(l);
		else if (resource instanceof Measure m)
			return fromMetadataResource(m);
		else if (resource instanceof NamingSystem n)
			return fromNamingSystem(n);
		else if (resource instanceof Questionnaire q)
			return fromMetadataResource(q);
		else if (resource instanceof StructureDefinition s)
			return fromMetadataResource(s);
		else if (resource instanceof Task t)
			return fromTask(t);
		else if (resource instanceof ValueSet v)
			return fromMetadataResource(v);
		else
			throw new IllegalArgumentException(
					"MetadataResource of type " + resource.getClass().getName() + " not supported");
	}

	public static ProcessesResource fromMetadataResource(MetadataResource resource)
	{
		return new ProcessesResource(
				new ResourceInfo(resource.getResourceType(), resource.getUrl(), resource.getVersion(), null, null),
				resource);
	}

	public static ProcessesResource fromNamingSystem(NamingSystem resource)
	{
		return new ProcessesResource(new ResourceInfo(resource.getResourceType(), null, null, resource.getName(), null),
				resource);
	}

	public static ProcessesResource fromTask(Task resource)
	{
		return new ProcessesResource(
				new ResourceInfo(resource.getResourceType(), null, null, null, getIdentifier(resource)), resource);
	}

	private static String getIdentifier(Task resource)
	{
		return TaskIdentifier.findFirst(resource).map(Identifier::getValue).get();
	}

	public static ProcessesResource from(ResourceInfo resourceInfo)
	{
		return new ProcessesResource(resourceInfo, null);
	}

	private final ResourceInfo resourceInfo;
	private final Resource resource;
	private final Set<ProcessIdAndVersion> processes = new HashSet<>();

	private ProcessState oldState;
	private ProcessState newState;

	private ProcessesResource(ResourceInfo resourceInfo, Resource resource)
	{
		this.resourceInfo = resourceInfo;
		this.resource = resource;
	}

	public ResourceInfo getResourceInfo()
	{
		return resourceInfo;
	}

	public Resource getResource()
	{
		return resource;
	}

	public Set<ProcessIdAndVersion> getProcesses()
	{
		return Collections.unmodifiableSet(processes);
	}

	public ProcessesResource add(ProcessIdAndVersion process)
	{
		processes.add(process);

		return this;
	}

	public void addAll(Set<ProcessIdAndVersion> processes)
	{
		this.processes.addAll(processes);
	}

	public ProcessesResource setOldProcessState(ProcessState oldState)
	{
		this.oldState = oldState;

		return this;
	}

	public ProcessState getOldProcessState()
	{
		return oldState;
	}

	public ProcessesResource setNewProcessState(ProcessState newState)
	{
		this.newState = newState;

		return this;
	}

	public ProcessState getNewProcessState()
	{
		return newState;
	}

	public boolean hasStateChangeOrDraft()
	{
		return !Objects.equals(getOldProcessState(), getNewProcessState())
				|| (ProcessState.DRAFT.equals(getOldProcessState()) && ProcessState.DRAFT.equals(getNewProcessState()));
	}

	public boolean notNewToExcludedChange()
	{
		return !(ProcessState.NEW.equals(getOldProcessState()) && ProcessState.EXCLUDED.equals(getNewProcessState()));
	}

	public boolean shouldExist()
	{
		return (ProcessState.ACTIVE.equals(getOldProcessState()) && ProcessState.ACTIVE.equals(getNewProcessState()))
				|| (ProcessState.RETIRED.equals(getOldProcessState())
						&& ProcessState.RETIRED.equals(getNewProcessState()));
	}

	public BundleEntryComponent toBundleEntry()
	{
		return switch (getOldProcessState())
		{
			case MISSING -> fromMissing();
			case NEW -> fromNew();
			case ACTIVE -> fromActive();
			case DRAFT -> fromDraft();
			case RETIRED -> fromRetired();
			case EXCLUDED -> fromExcluded();
		};
	}

	private BundleEntryComponent fromMissing()
	{
		return switch (getNewProcessState())
		{
			case ACTIVE -> createAsActive();
			case RETIRED -> createAsRetired();

			default -> throw new RuntimeException(
					"State change " + getOldProcessState() + " -> " + getNewProcessState() + " not supported");
		};
	}

	private BundleEntryComponent fromNew()
	{
		return switch (getNewProcessState())
		{
			case ACTIVE -> createAsActive();
			case DRAFT -> createAsDraft();
			case RETIRED -> createAsRetired();

			default -> throw new RuntimeException(
					"State change " + getOldProcessState() + " -> " + getNewProcessState() + " not supported");
		};
	}

	private BundleEntryComponent fromActive()
	{
		return switch (getNewProcessState())
		{
			case DRAFT -> updateToDraft();
			case RETIRED -> updateToRetired();
			case EXCLUDED -> delete();

			default -> throw new RuntimeException(
					"State change " + getOldProcessState() + " -> " + getNewProcessState() + " not supported");
		};
	}

	private BundleEntryComponent fromDraft()
	{
		return switch (getNewProcessState())
		{
			case ACTIVE -> updateToActive();
			case DRAFT -> updateToDraft();
			case RETIRED -> updateToRetired();
			case EXCLUDED -> delete();

			default -> throw new RuntimeException(
					"State change " + getOldProcessState() + " -> " + getNewProcessState() + " not supported");
		};
	}

	private BundleEntryComponent fromRetired()
	{
		return switch (getNewProcessState())
		{
			case ACTIVE -> updateToActive();
			case DRAFT -> updateToDraft();
			case EXCLUDED -> delete();

			default -> throw new RuntimeException(
					"State change " + getOldProcessState() + " -> " + getNewProcessState() + " not supported");
		};
	}

	private BundleEntryComponent fromExcluded()
	{
		return switch (getNewProcessState())
		{
			case ACTIVE -> createAsActive();
			case DRAFT -> createAsDraft();
			case RETIRED -> createAsRetired();

			default -> throw new RuntimeException(
					"State change " + getOldProcessState() + " -> " + getNewProcessState() + " not supported");
		};
	}

	private BundleEntryComponent createAsActive()
	{
		if (getResource() instanceof MetadataResource m)
			m.setStatus(PublicationStatus.ACTIVE);

		return create();
	}

	private BundleEntryComponent createAsDraft()
	{
		if (getResource() instanceof MetadataResource m)
			m.setStatus(PublicationStatus.DRAFT);

		return create();
	}

	private BundleEntryComponent createAsRetired()
	{
		if (getResource() instanceof MetadataResource m)
			m.setStatus(PublicationStatus.RETIRED);

		return create();
	}

	private BundleEntryComponent create()
	{
		BundleEntryComponent entry = new BundleEntryComponent();
		entry.setResource(getResource());
		entry.setFullUrl("urn:uuid:" + UUID.randomUUID().toString());

		BundleEntryRequestComponent request = entry.getRequest();
		request.setMethod(HTTPVerb.POST);
		request.setUrl(getResourceInfo().getResourceType().name());
		request.setIfNoneExist(getResourceInfo().toConditionalUrl());

		return entry;
	}

	private BundleEntryComponent updateToActive()
	{
		if (getResource() instanceof MetadataResource m)
			m.setStatus(PublicationStatus.ACTIVE);

		return update();
	}

	private BundleEntryComponent updateToDraft()
	{
		if (getResource() instanceof MetadataResource m)
			m.setStatus(PublicationStatus.DRAFT);

		return update();
	}

	private BundleEntryComponent updateToRetired()
	{
		if (getResource() instanceof MetadataResource m)
			m.setStatus(PublicationStatus.RETIRED);

		return update();
	}

	private BundleEntryComponent update()
	{
		BundleEntryComponent entry = new BundleEntryComponent();
		entry.setResource(getResource());
		entry.setFullUrl("urn:uuid:" + UUID.randomUUID().toString());

		BundleEntryRequestComponent request = entry.getRequest();
		request.setMethod(HTTPVerb.PUT);
		request.setUrl(getResourceInfo().getResourceType().name() + "?" + getResourceInfo().toConditionalUrl());

		return entry;
	}

	private BundleEntryComponent delete()
	{
		BundleEntryComponent entry = new BundleEntryComponent();

		BundleEntryRequestComponent request = entry.getRequest();
		request.setMethod(HTTPVerb.DELETE);
		request.setUrl(getResourceInfo().getResourceType().name() + "?" + getResourceInfo().toConditionalUrl());

		return entry;
	}

	public List<String> getExpectedStatus()
	{
		return switch (getOldProcessState())
		{
			case MISSING -> switch (getNewProcessState())
			{
				// conditional create NamingSystem: name=..., Task: identifier=..., others: url=...&version=...
				case ACTIVE -> Arrays.asList("200", "201");
				// conditional create NamingSystem: name=..., Task: identifier=..., others: url=...&version=...
				case RETIRED -> Arrays.asList("200", "201");

				default -> throw new RuntimeException(
						"State change " + getOldProcessState() + " -> " + getNewProcessState() + " not supported");
			};
			case NEW -> switch (getNewProcessState())
			{
				// conditional create NamingSystem: name=..., Task: identifier=..., others: url=...&version=...
				case ACTIVE -> Arrays.asList("200", "201");
				// conditional create NamingSystem: name=..., Task: identifier=..., others: url=...&version=...
				case DRAFT -> Arrays.asList("200", "201");
				// conditional create NamingSystem: name=..., Task: identifier=..., others: url=...&version=...
				case RETIRED -> Arrays.asList("200", "201");

				default -> throw new RuntimeException(
						"State change " + getOldProcessState() + " -> " + getNewProcessState() + " not supported");
			};
			case ACTIVE -> switch (getNewProcessState())
			{
				// standard update with resource id
				case DRAFT -> Collections.singletonList("200");
				// standard update with resource id
				case RETIRED -> Collections.singletonList("200");
				// standard delete with resource id
				case EXCLUDED -> Arrays.asList("200", "204");

				default -> throw new RuntimeException(
						"State change " + getOldProcessState() + " -> " + getNewProcessState() + " not supported");
			};
			case DRAFT -> switch (getNewProcessState())
			{
				// standard update with resource id
				case ACTIVE -> Collections.singletonList("200");
				// standard update with resource id
				case DRAFT -> Collections.singletonList("200");
				// standard update with resource id
				case RETIRED -> Collections.singletonList("200");
				// standard delete with resource id
				case EXCLUDED -> Arrays.asList("200", "204");

				default -> throw new RuntimeException(
						"State change " + getOldProcessState() + " -> " + getNewProcessState() + " not supported");
			};
			case RETIRED -> switch (getNewProcessState())
			{
				// standard update with resource id
				case ACTIVE -> Collections.singletonList("200");
				// standard update with resource id
				case DRAFT -> Collections.singletonList("200");
				// standard delete with resource id
				case EXCLUDED -> Arrays.asList("200", "204");

				default -> throw new RuntimeException(
						"State change " + getOldProcessState() + " -> " + getNewProcessState() + " not supported");
			};
			case EXCLUDED -> switch (getNewProcessState())
			{
				// conditional create NamingSystem: name=..., Task: identifier=..., others: url=...&version=...
				case ACTIVE -> Arrays.asList("200", "201");
				// conditional create NamingSystem: name=..., Task: identifier=..., others: url=...&version=...
				case DRAFT -> Arrays.asList("200", "201");
				// conditional create NamingSystem: name=..., Task: identifier=..., others: url=...&version=...
				case RETIRED -> Arrays.asList("200", "201");

				default -> throw new RuntimeException(
						"State change " + getOldProcessState() + " -> " + getNewProcessState() + " not supported");
			};
		};
	}

	public BundleEntryComponent toSearchBundleEntryCount0()
	{
		BundleEntryComponent entry = new BundleEntryComponent();

		BundleEntryRequestComponent request = entry.getRequest();
		request.setMethod(HTTPVerb.GET);
		request.setUrl(getSearchBundleEntryUrl() + "&_count=0");

		return entry;
	}

	public String getSearchBundleEntryUrl()
	{
		return getResourceInfo().getResourceType().name() + "?" + getResourceInfo().toConditionalUrl();
	}
}
