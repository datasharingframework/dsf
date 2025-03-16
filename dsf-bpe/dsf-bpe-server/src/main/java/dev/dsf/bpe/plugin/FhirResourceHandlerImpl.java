package dev.dsf.bpe.plugin;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import dev.dsf.bpe.api.plugin.ProcessIdAndVersion;
import dev.dsf.bpe.client.dsf.BasicWebserviceClient;
import dev.dsf.bpe.client.dsf.PreferReturnMinimal;
import dev.dsf.bpe.client.dsf.WebserviceClient;
import dev.dsf.bpe.dao.ProcessPluginResourcesDao;

public class FhirResourceHandlerImpl implements FhirResourceHandler, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(FhirResourceHandlerImpl.class);

	private final WebserviceClient localWebserviceClient;
	private final ProcessPluginResourcesDao dao;
	private final FhirContext fhirContext;
	private final int fhirServerRequestMaxRetries;
	private final long fhirServerRetryDelayMillis;

	public FhirResourceHandlerImpl(WebserviceClient localWebserviceClient, ProcessPluginResourcesDao dao,
			FhirContext fhirContext, int fhirServerRequestMaxRetries, long fhirServerRetryDelayMillis)
	{
		this.localWebserviceClient = localWebserviceClient;
		this.dao = dao;
		this.fhirContext = fhirContext;
		this.fhirServerRequestMaxRetries = fhirServerRequestMaxRetries;
		this.fhirServerRetryDelayMillis = fhirServerRetryDelayMillis;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(localWebserviceClient, "localWebserviceClient");
		Objects.requireNonNull(dao, "dao");
		Objects.requireNonNull(fhirContext, "fhirContext");
		if (fhirServerRequestMaxRetries < -1)
			throw new IllegalArgumentException("fhirServerRequestMaxRetries < -1");
		if (fhirServerRetryDelayMillis < 0)
			throw new IllegalArgumentException("fhirServerRetryDelayMillis < 0");
	}

	private PreferReturnMinimal minimalReturnRetryClient()
	{
		if (fhirServerRequestMaxRetries == WebserviceClient.RETRY_FOREVER)
			return localWebserviceClient.withMinimalReturn().withRetryForever(fhirServerRetryDelayMillis);
		else
			return localWebserviceClient.withMinimalReturn().withRetry(fhirServerRequestMaxRetries,
					fhirServerRetryDelayMillis);
	}

	private BasicWebserviceClient retryClient()
	{
		if (fhirServerRequestMaxRetries == WebserviceClient.RETRY_FOREVER)
			return localWebserviceClient.withRetryForever(fhirServerRetryDelayMillis);
		else
			return localWebserviceClient.withRetry(fhirServerRequestMaxRetries, fhirServerRetryDelayMillis);
	}

	@Override
	public void applyStateChangesAndStoreNewResourcesInDb(Map<ProcessIdAndVersion, List<byte[]>> pluginResources,
			List<ProcessStateChangeOutcome> changes)
	{
		Objects.requireNonNull(pluginResources, "pluginResources");
		Objects.requireNonNull(changes, "changes");

		Map<ProcessIdAndVersion, List<ResourceInfo>> dbResourcesByProcess = getResourceInfosFromDb();

		Map<ResourceInfo, ProcessesResource> resources = new HashMap<>();
		for (ProcessStateChangeOutcome change : changes)
		{
			Stream<ProcessesResource> currentOrOldProcessResources = getCurrentOrOldResources(pluginResources,
					dbResourcesByProcess, change.getProcessKeyAndVersion());

			currentOrOldProcessResources.forEach(res ->
			{
				resources.computeIfPresent(res.getResourceInfo(), (processInfo, processResource) ->
				{
					processResource.addAll(res.getProcesses());

					if (change.getNewProcessState().isHigherPriority(processResource.getNewProcessState()))
						processResource.setNewProcessState(change.getNewProcessState());

					// only override resource state if not special case for previously unknown resource (no resource id)
					if (processResource.getResourceInfo().hasResourceId()
							&& change.getOldProcessState().isHigherPriority(processResource.getOldProcessState()))
						processResource.setOldProcessState(change.getOldProcessState());

					return processResource;
				});

				ProcessesResource nullIfNotNeededByOther = resources.putIfAbsent(res.getResourceInfo(),
						res.setNewProcessState(change.getNewProcessState())
								.setOldProcessState(change.getOldProcessState()));

				if (nullIfNotNeededByOther == null)
				{
					// special DRAFT case for previously unknown resource (no resource id)
					if (ProcessState.DRAFT.equals(change.getOldProcessState())
							&& ProcessState.DRAFT.equals(change.getNewProcessState())
							&& !res.getResourceInfo().hasResourceId())
					{
						logger.info("Adding new resource {}?{}", res.getResourceInfo().getResourceType(),
								res.getResourceInfo().toConditionalUrl());
						res.setOldProcessState(ProcessState.NEW);
					}
				}
			});
		}

		addResourcesRemovedFromDraftProcess(changes, dbResourcesByProcess, resources);

		findMissingResourcesAndModifyOldState(resources.values());

		List<ProcessesResource> resourceValues = new ArrayList<>(
				resources.values().stream().filter(ProcessesResource::hasStateChangeOrDraft)
						.filter(ProcessesResource::notNewToExcludedChange).collect(Collectors.toList()));
		resourceValues.sort(Comparator.comparingInt(this::getSortIndex));

		Bundle batchBundle = new Bundle();
		batchBundle.setType(BundleType.BATCH);

		List<BundleEntryComponent> entries = resourceValues.stream().map(ProcessesResource::toBundleEntry).toList();
		batchBundle.setEntry(entries);

		try
		{
			if (batchBundle.getEntry().isEmpty())
				logger.debug("No transaction bundle to execute");
			else
			{
				logger.debug("Executing process plugin resources bundle");
				logger.trace("Bundle: {}", newJsonParser().encodeResourceToString(batchBundle));

				Bundle returnBundle = minimalReturnRetryClient().postBundle(batchBundle);

				List<UUID> deletedResourcesIds = addIdsAndReturnDeleted(resourceValues, returnBundle);
				List<ProcessIdAndVersion> excludedProcesses = changes.stream()
						.filter(change -> ProcessState.EXCLUDED.equals(change.getNewProcessState()))
						.map(ProcessStateChangeOutcome::getProcessKeyAndVersion).collect(Collectors.toList());
				try
				{
					dao.addOrRemoveResources(resources.values(), deletedResourcesIds, excludedProcesses);
				}
				catch (SQLException e)
				{
					logger.debug("Error while adding process plugin resource to the db", e);
					logger.warn("Error while adding process plugin resource to the db: {} - {}", e.getClass().getName(),
							e.getMessage());

					throw new RuntimeException(e);
				}
			}
		}
		catch (Exception e)
		{
			logger.debug("Error while executing process plugins resource bundle", e);
			logger.warn("Error while executing process plugins resource bundle: {} - {}", e.getClass().getName(),
					e.getMessage());
			logger.warn(
					"Resources in FHIR server may not be consistent, please check resources and execute the following bundle if necessary: {}",
					newJsonParser().encodeResourceToString(batchBundle));

			throw e;
		}
	}

	private IParser newJsonParser()
	{
		IParser p = fhirContext.newJsonParser();
		p.setStripVersionsFromReferences(false);
		p.setOverrideResourceIdWithBundleEntryFullUrl(false);
		return p;
	}

	private int getSortIndex(ProcessesResource resource)
	{
		if (resource.getResource() == null)
			return -1;

		return switch (resource.getResource().getResourceType())
		{
			case ActivityDefinition -> 7;
			case CodeSystem -> 1;
			case Library -> 4;
			case Measure -> 5;
			case NamingSystem -> 0;
			case Questionnaire -> 6;
			case StructureDefinition -> 3;
			case Task -> 8;
			case ValueSet -> 2;
			default ->
				throw new IllegalArgumentException("Unexpected value: " + resource.getResource().getResourceType());
		};
	}

	private void addResourcesRemovedFromDraftProcess(List<ProcessStateChangeOutcome> changes,
			Map<ProcessIdAndVersion, List<ResourceInfo>> dbResourcesByProcess,
			Map<ResourceInfo, ProcessesResource> resources)
	{
		for (ProcessStateChangeOutcome change : changes)
		{
			if (ProcessState.DRAFT.equals(change.getOldProcessState())
					&& ProcessState.DRAFT.equals(change.getNewProcessState()))
			{
				List<ResourceInfo> dbResources = dbResourcesByProcess.getOrDefault(change.getProcessKeyAndVersion(),
						List.of());

				dbResources.forEach(dbRes ->
				{
					ProcessesResource processRes = ProcessesResource.from(dbRes);
					processRes.setOldProcessState(ProcessState.DRAFT);
					processRes.setNewProcessState(ProcessState.EXCLUDED);

					ProcessesResource nullIfNotNeededByOther = resources.putIfAbsent(dbRes, processRes);

					if (nullIfNotNeededByOther == null)
						logger.info("Deleting resource {}?{} with id {} if exists", dbRes.getResourceType(),
								dbRes.toConditionalUrl(), dbRes.getResourceId());
				});
			}
		}
	}

	private void findMissingResourcesAndModifyOldState(Collection<ProcessesResource> resources)
	{
		List<ProcessesResource> resourceValues = resources.stream().filter(ProcessesResource::shouldExist)
				.collect(Collectors.toList());

		Bundle batchBundle = new Bundle();
		batchBundle.setType(BundleType.BATCH);

		batchBundle.setEntry(
				resourceValues.stream().map(ProcessesResource::toSearchBundleEntryCount0).collect(Collectors.toList()));

		if (batchBundle.getEntry().isEmpty())
			return;

		Bundle returnBundle = retryClient().postBundle(batchBundle);

		if (resourceValues.size() != returnBundle.getEntry().size())
			throw new RuntimeException("Return bundle size unexpected, expected " + resourceValues.size() + " got "
					+ returnBundle.getEntry().size());

		for (int i = 0; i < resourceValues.size(); i++)
		{
			ProcessesResource resource = resourceValues.get(i);
			BundleEntryComponent entry = returnBundle.getEntry().get(i);

			if (!entry.getResponse().getStatus().startsWith("200"))
			{
				logger.warn("Response status for {} not 200 OK but {}, missing resource will not be added",
						resource.getSearchBundleEntryUrl(), entry.getResponse().getStatus());
			}
			else if (!entry.hasResource() || !(entry.getResource() instanceof Bundle b)
					|| !BundleType.SEARCHSET.equals(b.getType()))
			{
				logger.warn("Response for {} not a searchset Bundle, missing resource will not be added",
						resource.getSearchBundleEntryUrl());
			}

			Bundle searchBundle = (Bundle) entry.getResource();

			if (searchBundle.getTotal() <= 0)
			{
				resource.setOldProcessState(ProcessState.MISSING);

				logger.warn("Resource {} not found, setting old process state for resource to {}",
						resource.getSearchBundleEntryUrl(), ProcessState.MISSING);
			}
			else
				logger.info("Resource {} found", resource.getSearchBundleEntryUrl());
		}
	}

	private List<UUID> addIdsAndReturnDeleted(List<ProcessesResource> resourceValues, Bundle returnBundle)
	{
		if (resourceValues.size() != returnBundle.getEntry().size())
			throw new RuntimeException("Return bundle size unexpected, expected " + resourceValues.size() + " got "
					+ returnBundle.getEntry().size());

		List<UUID> deletedIds = new ArrayList<>();
		for (int i = 0; i < resourceValues.size(); i++)
		{
			ProcessesResource resource = resourceValues.get(i);
			BundleEntryComponent entry = returnBundle.getEntry().get(i);
			List<String> expectedStatus = resource.getExpectedStatus();

			if (!expectedStatus.stream().anyMatch(eS -> entry.getResponse().getStatus().startsWith(eS)))
			{
				throw new RuntimeException("Return status " + entry.getResponse().getStatus() + " not starting with "
						+ (expectedStatus.size() > 1 ? "one of " : "") + expectedStatus + " for resource "
						+ resource.getResourceInfo().toString() + " of processes " + resource.getProcesses());

			}

			// create or update
			if (!ProcessState.EXCLUDED.equals(resource.getNewProcessState()))
			{
				IdType id = new IdType(entry.getResponse().getLocation());

				if (!resource.getResourceInfo().getResourceType().equals(ResourceType.fromCode(id.getResourceType())))
					throw new RuntimeException("Return resource type unexpected, expected "
							+ resource.getResourceInfo().getResourceType() + " got " + id.getResourceType());

				resource.getResourceInfo().setResourceId(toUuid(id.getIdPart()));
			}

			// delete
			else
			{
				deletedIds.add(resource.getResourceInfo().getResourceId());

				resource.getResourceInfo().setResourceId(null);
			}
		}

		return deletedIds;
	}

	private Stream<ProcessesResource> getCurrentOrOldResources(
			Map<ProcessIdAndVersion, List<byte[]>> pluginResourcesByProcess,
			Map<ProcessIdAndVersion, List<ResourceInfo>> dbResourcesByProcess, ProcessIdAndVersion process)
	{
		List<byte[]> pluginResources = pluginResourcesByProcess.get(process);
		if (pluginResources != null)
		{
			Stream<byte[]> resources = getResources(process, pluginResourcesByProcess);
			return resources.map(r ->
			{
				ProcessesResource resource = ProcessesResource.from(fhirContext, r).add(process);

				Optional<UUID> resourceId = getResourceId(dbResourcesByProcess, process, resource.getResourceInfo());
				resourceId.ifPresent(id -> resource.getResourceInfo().setResourceId(id));
				// not present: new resource, unknown to bpe db

				return resource;
			});
		}
		else
		{
			List<ResourceInfo> resources = dbResourcesByProcess.get(process);
			if (resources == null)
			{
				logger.debug("No resources found in BPE DB for process {}", process);
				resources = List.of();
			}

			return resources.stream().map(info -> ProcessesResource.from(info).add(process));
		}
	}

	private Stream<byte[]> getResources(ProcessIdAndVersion process,
			Map<ProcessIdAndVersion, List<byte[]>> pluginResources)
	{
		List<byte[]> resources = pluginResources.get(process);
		if (resources.isEmpty())
		{
			logger.warn("No FHIR resources found for process {}", process.toString());
			return Stream.empty();
		}
		else
		{
			return resources.stream();
		}
	}

	private Optional<UUID> getResourceId(Map<ProcessIdAndVersion, List<ResourceInfo>> dbResourcesByProcess,
			ProcessIdAndVersion process, ResourceInfo resourceInfo)
	{
		return dbResourcesByProcess.getOrDefault(process, List.of()).stream().filter(r -> r.equals(resourceInfo))
				.findFirst().map(ResourceInfo::getResourceId);
	}

	private Map<ProcessIdAndVersion, List<ResourceInfo>> getResourceInfosFromDb()
	{
		try
		{
			return dao.getResources();
		}
		catch (SQLException e)
		{
			logger.debug("Error while retrieving resource infos from db", e);
			logger.warn("Error while retrieving resource infos from db: {} - {}", e.getClass().getName(),
					e.getMessage());

			throw new RuntimeException(e);
		}
	}

	private UUID toUuid(String id)
	{
		if (id == null)
			return null;

		// TODO control flow by exception
		try
		{
			return UUID.fromString(id);
		}
		catch (IllegalArgumentException e)
		{
			return null;
		}
	}
}
