package dev.dsf.bpe.plugin;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.SearchEntryMode;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import dev.dsf.bpe.api.plugin.BpmnFileAndModel;
import dev.dsf.bpe.api.plugin.ProcessIdAndVersion;
import dev.dsf.bpe.api.plugin.ProcessPlugin;
import dev.dsf.bpe.camunda.ProcessPluginConsumer;
import dev.dsf.bpe.client.dsf.BasicWebserviceClient;
import dev.dsf.bpe.client.dsf.WebserviceClient;

public class ProcessPluginManagerImpl implements ProcessPluginManager, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(ProcessPluginManagerImpl.class);

	public static final String ORGANIZATION_IDENTIFIER_SID = "http://dsf.dev/sid/organization-identifier";

	private static record ProcessByIdAndVersion(ProcessIdAndVersion idAndVersion, ProcessPlugin plugin)
	{
	}

	private final List<ProcessPluginConsumer> processPluginConsumers = new ArrayList<>();

	private final ProcessPluginLoader processPluginLoader;
	private final BpmnProcessStateChangeService bpmnProcessStateChangeService;
	private final FhirResourceHandler fhirResourceHandler;

	private final String localEndpointAddress;
	private final WebserviceClient localWebserviceClient;
	private final int fhirServerRequestMaxRetries;
	private final Duration fhirServerRetryDelay;

	private Map<ProcessIdAndVersion, ProcessPlugin> pluginsByProcessIdAndVersion;

	public ProcessPluginManagerImpl(List<ProcessPluginConsumer> processPluginConsumers,
			ProcessPluginLoader processPluginLoader, BpmnProcessStateChangeService bpmnProcessStateChangeService,
			FhirResourceHandler fhirResourceHandler, String localEndpointAddress,
			WebserviceClient localWebserviceClient, int fhirServerRequestMaxRetries, Duration fhirServerRetryDelay)
	{
		if (processPluginConsumers != null)
			this.processPluginConsumers.addAll(processPluginConsumers);

		this.processPluginLoader = processPluginLoader;
		this.bpmnProcessStateChangeService = bpmnProcessStateChangeService;
		this.fhirResourceHandler = fhirResourceHandler;

		this.localEndpointAddress = localEndpointAddress;
		this.localWebserviceClient = localWebserviceClient;
		this.fhirServerRequestMaxRetries = fhirServerRequestMaxRetries;
		this.fhirServerRetryDelay = fhirServerRetryDelay;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(processPluginLoader, "processPluginLoader");
		Objects.requireNonNull(bpmnProcessStateChangeService, "bpmnProcessStateChangeService");
		Objects.requireNonNull(fhirResourceHandler, "fhirResourceHandler");

		Objects.requireNonNull(localEndpointAddress, "localEndpointAddress");
		Objects.requireNonNull(localWebserviceClient, "localWebserviceClient");

		if (fhirServerRequestMaxRetries < -1)
			throw new IllegalArgumentException("fhirServerRequestMaxRetries < -1");
		Objects.requireNonNull(fhirServerRetryDelay, "fhirServerRetryDelay");
	}

	@Override
	public void loadAndDeployPlugins()
	{
		Optional<String> localOrganizationIdentifierValue = getLocalOrganizationIdentifierValue();
		if (localOrganizationIdentifierValue.isEmpty())
			logger.warn("Local organization identifier unknown, check DSF FHIR server allow list");

		List<ProcessPlugin> loadedPlugins = processPluginLoader.loadPlugins();

		// set log level to debug for logger with plugin definition package name
		loadedPlugins.stream().map(ProcessPlugin::getPluginDefinitionPackageName)
				.forEach(name -> Configurator.setLevel(name, Level.DEBUG));

		List<ProcessPlugin> plugins = removeDuplicates(
				loadedPlugins.stream().filter(p -> p.getPluginMdc().executeWithPluginMdc(
						() -> p.initializeAndValidateResources(localOrganizationIdentifierValue.orElse(null)))));

		if (plugins.isEmpty())
			logger.warn("No process plugins deployed");

		pluginsByProcessIdAndVersion = plugins.stream()
				.flatMap(p -> p.getProcessKeysAndVersions().stream().map(iAV -> new ProcessByIdAndVersion(iAV, p)))
				.collect(Collectors.toMap(ProcessByIdAndVersion::idAndVersion, ProcessByIdAndVersion::plugin));
		processPluginConsumers.forEach(c -> c.setProcessPlugins(plugins, pluginsByProcessIdAndVersion));

		// deploy BPMN models
		List<BpmnFileAndModel> models = plugins.stream().flatMap(p -> p.getProcessModels().stream()).toList();
		List<ProcessStateChangeOutcome> outcomes = bpmnProcessStateChangeService
				.deploySuspendOrActivateProcesses(models);

		// deploy FHIR resources
		Map<ProcessIdAndVersion, List<byte[]>> resources = plugins.stream().map(ProcessPlugin::getFhirResources)
				.flatMap(m -> m.entrySet().stream()).collect(Collectors.toMap(Entry::getKey, Entry::getValue));
		fhirResourceHandler.applyStateChangesAndStoreNewResourcesInDb(resources, outcomes);

		onProcessesDeployed(outcomes, plugins);
	}

	private BasicWebserviceClient retryClient()
	{
		if (fhirServerRequestMaxRetries == WebserviceClient.RETRY_FOREVER)
			return localWebserviceClient.withRetryForever(fhirServerRetryDelay);
		else
			return localWebserviceClient.withRetry(fhirServerRequestMaxRetries, fhirServerRetryDelay);
	}

	private Optional<String> getLocalOrganizationIdentifierValue()
	{
		Bundle resultBundle = retryClient().searchWithStrictHandling(Endpoint.class, Map.of("status", List.of("active"),
				"address", List.of(localEndpointAddress), "_include", List.of("Endpoint:organization")));

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

		return getActiveOrganizationFromIncludes(resultBundle).findFirst()
				.flatMap(o -> o.getIdentifier().stream()
						.filter(i -> i.hasSystemElement() && i.getSystemElement().hasValue()
								&& ORGANIZATION_IDENTIFIER_SID.equals(i.getSystem()))
						.findFirst())
				.filter(i -> i.hasValueElement() && i.getValueElement().hasValue()).map(Identifier::getValue);
	}

	private Stream<Organization> getActiveOrganizationFromIncludes(Bundle resultBundle)
	{
		return resultBundle.getEntry().stream().filter(BundleEntryComponent::hasSearch)
				.filter(e -> SearchEntryMode.INCLUDE.equals(e.getSearch().getMode()))
				.filter(BundleEntryComponent::hasResource).map(BundleEntryComponent::getResource)
				.filter(r -> r instanceof Organization).map(r -> (Organization) r).filter(Organization::getActive);
	}

	private List<ProcessPlugin> removeDuplicates(Stream<ProcessPlugin> plugins)
	{
		Map<ProcessIdAndVersion, List<ProcessPlugin>> pluginsByProcessIdAndVersion = new HashMap<>();
		plugins.forEach(plugin ->
		{
			List<ProcessIdAndVersion> processes = plugin.getProcessKeysAndVersions();
			for (ProcessIdAndVersion process : processes)
			{
				if (pluginsByProcessIdAndVersion.containsKey(process))
					pluginsByProcessIdAndVersion.get(process).add(plugin);
				else
				{
					List<ProcessPlugin> list = new ArrayList<>();
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

	private void onProcessesDeployed(List<ProcessStateChangeOutcome> changes, List<ProcessPlugin> plugins)
	{
		Set<ProcessIdAndVersion> activeProcesses = changes.stream()
				.filter(c -> EnumSet.of(ProcessState.ACTIVE, ProcessState.DRAFT).contains(c.getNewProcessState()))
				.map(ProcessStateChangeOutcome::getProcessKeyAndVersion).collect(Collectors.toSet());

		plugins.stream().forEach(p -> p.getPluginMdc().executeWithPluginMdc(
				() -> p.getProcessPluginDeploymentListener().onProcessesDeployed(activeProcesses)));
	}

	@Override
	public Optional<ProcessPlugin> getProcessPlugin(ProcessIdAndVersion processIdAndVersion)
	{
		if (pluginsByProcessIdAndVersion == null)
			return Optional.empty();
		else
			return Optional.ofNullable(pluginsByProcessIdAndVersion.get(processIdAndVersion));
	}
}
