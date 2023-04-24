package dev.dsf.bpe.plugin;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.camunda.bpm.engine.delegate.TaskListener;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.SearchEntryMode;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import dev.dsf.bpe.camunda.ProcessPluginConsumer;
import dev.dsf.bpe.v1.ProcessPluginDeplyomentStateListener;
import dev.dsf.bpe.v1.constants.NamingSystems.OrganizationIdentifier;
import dev.dsf.fhir.client.BasicFhirWebserviceClient;
import dev.dsf.fhir.client.FhirWebserviceClient;

public class ProcessPluginManagerImpl implements ProcessPluginManager, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(ProcessPluginManagerImpl.class);

	private final List<ProcessPluginConsumer> processPluginConsumers = new ArrayList<>();

	private final ProcessPluginLoader processPluginLoader;
	private final BpmnProcessStateChangeService bpmnProcessStateChangeService;
	private final FhirResourceHandler fhirResourceHandler;

	private final String localEndpointAddress;
	private final FhirWebserviceClient localWebserviceClient;
	private final int fhirServerRequestMaxRetries;
	private final long fhirServerRetryDelayMillis;

	public ProcessPluginManagerImpl(List<ProcessPluginConsumer> processPluginConsumers,
			ProcessPluginLoader processPluginLoader, BpmnProcessStateChangeService bpmnProcessStateChangeService,
			FhirResourceHandler fhirResourceHandler, String localEndpointAddress,
			FhirWebserviceClient localWebserviceClient, int fhirServerRequestMaxRetries,
			long fhirServerRetryDelayMillis)
	{
		if (processPluginConsumers != null)
			this.processPluginConsumers.addAll(processPluginConsumers);

		this.processPluginLoader = processPluginLoader;
		this.bpmnProcessStateChangeService = bpmnProcessStateChangeService;
		this.fhirResourceHandler = fhirResourceHandler;

		this.localEndpointAddress = localEndpointAddress;
		this.localWebserviceClient = localWebserviceClient;
		this.fhirServerRequestMaxRetries = fhirServerRequestMaxRetries;
		this.fhirServerRetryDelayMillis = fhirServerRetryDelayMillis;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(processPluginLoader, "processPluginLoader");
		Objects.requireNonNull(bpmnProcessStateChangeService, "bpmnProcessStateChangeService");
		Objects.requireNonNull(fhirResourceHandler, "fhirResourceHandler");

		Objects.requireNonNull(localEndpointAddress, "localEndpointAddress");
		Objects.requireNonNull(localWebserviceClient, "localWebserviceClient");
	}

	@Override
	public void loadAndDeployPlugins()
	{
		Optional<String> localOrganizationIdentifierValue = getLocalOrganizationIdentifierValue();
		if (localOrganizationIdentifierValue.isEmpty())
			logger.warn("Local organization identifier unknown, check DSF FHIR server allow list");

		List<ProcessPlugin<?, ?, ? extends TaskListener>> plugins = removeDuplicates(processPluginLoader.loadPlugins()
				.stream().filter(p -> p.initializeAndValidateResources(localOrganizationIdentifierValue.orElse(null))));

		if (plugins.isEmpty())
		{
			logger.warn("No process plugins deployed");
			return;
		}

		processPluginConsumers.forEach(c -> c.setProcessPlugins(plugins));

		// deploy BPMN models
		List<BpmnFileAndModel> models = plugins.stream().flatMap(p -> p.getProcessModels().stream()).toList();
		List<ProcessStateChangeOutcome> outcomes = bpmnProcessStateChangeService
				.deploySuspendOrActivateProcesses(models);

		// deploy FHIR resources
		Map<ProcessIdAndVersion, List<Resource>> resources = plugins.stream().map(ProcessPlugin::getFhirResources)
				.flatMap(m -> m.entrySet().stream()).collect(Collectors.toMap(Entry::getKey, Entry::getValue));
		fhirResourceHandler.applyStateChangesAndStoreNewResourcesInDb(resources, outcomes);

		onProcessesDeployed(outcomes, plugins);
	}

	private BasicFhirWebserviceClient retryClient()
	{
		if (fhirServerRequestMaxRetries == FhirWebserviceClient.RETRY_FOREVER)
			return localWebserviceClient.withRetryForever(fhirServerRetryDelayMillis);
		else
			return localWebserviceClient.withRetry(fhirServerRequestMaxRetries, fhirServerRetryDelayMillis);
	}

	private Optional<String> getLocalOrganizationIdentifierValue()
	{
		Bundle resultBundle = retryClient().searchWithStrictHandling(Endpoint.class,
				Map.of("status", Collections.singletonList("active"), "address",
						Collections.singletonList(localEndpointAddress), "_include",
						Collections.singletonList("Endpoint:organization")));

		if (resultBundle == null || resultBundle.getEntry() == null || resultBundle.getEntry().size() != 2
				|| resultBundle.getEntry().get(0).getResource() == null
				|| !(resultBundle.getEntry().get(0).getResource() instanceof Endpoint)
				|| resultBundle.getEntry().get(1).getResource() == null
				|| !(resultBundle.getEntry().get(1).getResource() instanceof Organization))
		{
			logger.warn("No active (or more than one) Endpoint found for address '{}'", localEndpointAddress);
			return Optional.empty();
		}
		else if (getActiveOrganizationFromIncludes(resultBundle).count() != 1)
		{
			logger.warn("No active (or more than one) Organization found by active Endpoint with address '{}'",
					localEndpointAddress);
			return Optional.empty();
		}

		return getActiveOrganizationFromIncludes(resultBundle).findFirst().flatMap(OrganizationIdentifier::findFirst)
				.map(Identifier::getValue);
	}

	private Stream<Organization> getActiveOrganizationFromIncludes(Bundle resultBundle)
	{
		return resultBundle.getEntry().stream().filter(BundleEntryComponent::hasSearch)
				.filter(e -> SearchEntryMode.INCLUDE.equals(e.getSearch().getMode()))
				.filter(BundleEntryComponent::hasResource).map(BundleEntryComponent::getResource)
				.filter(r -> r instanceof Organization).map(r -> (Organization) r).filter(Organization::getActive);
	}

	private List<ProcessPlugin<?, ?, ? extends TaskListener>> removeDuplicates(
			Stream<ProcessPlugin<?, ?, ? extends TaskListener>> plugins)
	{
		Map<ProcessIdAndVersion, List<ProcessPlugin<?, ?, ? extends TaskListener>>> pluginsByProcessIdAndVersion = new HashMap<>();
		plugins.forEach(plugin ->
		{
			List<ProcessIdAndVersion> processes = plugin.getProcessKeysAndVersions();
			for (ProcessIdAndVersion process : processes)
			{
				if (pluginsByProcessIdAndVersion.containsKey(process))
					pluginsByProcessIdAndVersion.get(process).add(plugin);
				else
				{
					List<ProcessPlugin<?, ?, ? extends TaskListener>> list = new ArrayList<>();
					list.add(plugin);
					pluginsByProcessIdAndVersion.put(process, list);
				}
			}
		});

		pluginsByProcessIdAndVersion.entrySet().stream().filter(e -> e.getValue().size() > 1).forEach(e ->
		{
			logger.warn(
					"Ignoring process plugins {} with duplicated process {}", e.getValue().stream()
							.map(ProcessPlugin::getJarFile).map(Path::toString).collect(Collectors.joining(", ")),
					e.getKey().toString());
		});

		return pluginsByProcessIdAndVersion.entrySet().stream().filter(e -> e.getValue().size() == 1)
				.flatMap(e -> e.getValue().stream()).distinct().toList();
	}

	private void onProcessesDeployed(List<ProcessStateChangeOutcome> changes,
			List<ProcessPlugin<?, ?, ? extends TaskListener>> plugins)
	{
		Set<ProcessIdAndVersion> activeProcesses = changes.stream()
				.filter(c -> EnumSet.of(ProcessState.ACTIVE, ProcessState.DRAFT).contains(c.getNewProcessState()))
				.map(ProcessStateChangeOutcome::getProcessKeyAndVersion).collect(Collectors.toSet());

		plugins.forEach(plugin ->
		{
			List<String> activePluginProcesses = plugin.getProcessKeysAndVersions().stream()
					.filter(activeProcesses::contains).map(ProcessIdAndVersion::getId).toList();

			plugin.getApplicationContext().getBeansOfType(ProcessPluginDeplyomentStateListener.class).entrySet()
					.forEach(onProcessesDeployed(plugin, activePluginProcesses));
		});
	}

	private Consumer<Entry<String, ProcessPluginDeplyomentStateListener>> onProcessesDeployed(
			ProcessPlugin<?, ?, ? extends TaskListener> plugin, List<String> activePluginProcesses)
	{
		return entry ->
		{
			try
			{
				entry.getValue().onProcessesDeployed(activePluginProcesses);
			}
			catch (Exception e)
			{
				logger.warn("Error while executing {} bean {} for process plugin {}, {} - {}",
						ProcessPluginDeplyomentStateListener.class.getName(), entry.getKey(),
						plugin.getJarFile().toString(), e.getClass().getName(), e.getMessage());
				logger.debug("Error while executing " + ProcessPluginDeplyomentStateListener.class.getName() + " bean "
						+ entry.getKey() + " for process plugin " + plugin.getJarFile().toString(), e);
			}
		};
	}
}
