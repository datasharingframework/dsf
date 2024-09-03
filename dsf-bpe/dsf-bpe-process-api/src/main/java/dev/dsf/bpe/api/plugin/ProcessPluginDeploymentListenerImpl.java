package dev.dsf.bpe.api.plugin;

import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

public class ProcessPluginDeploymentListenerImpl<L> implements ProcessPluginDeploymentListener
{
	private static final Logger logger = LoggerFactory.getLogger(ProcessPluginDeploymentListenerImpl.class);

	private final Supplier<ApplicationContext> applicationContext;
	private final Supplier<List<ProcessIdAndVersion>> processKeysAndVersions;
	private final Class<? extends L> listenerClass;
	private final BiConsumer<L, List<String>> onProcessesDeployed;

	public ProcessPluginDeploymentListenerImpl(Supplier<ApplicationContext> applicationContext,
			Supplier<List<ProcessIdAndVersion>> processKeysAndVersions, Class<? extends L> listenerClass,
			BiConsumer<L, List<String>> onProcessesDeployed)
	{
		this.applicationContext = Objects.requireNonNull(applicationContext, "applicationContext");
		this.processKeysAndVersions = Objects.requireNonNull(processKeysAndVersions, "processKeysAndVersions");
		this.listenerClass = Objects.requireNonNull(listenerClass, "listenerClass");
		this.onProcessesDeployed = Objects.requireNonNull(onProcessesDeployed, "onProcessesDeployed");
	}

	@Override
	public void onProcessesDeployed(Set<ProcessIdAndVersion> allActiveProcesses)
	{
		List<String> activePluginProcesses = processKeysAndVersions.get().stream().filter(allActiveProcesses::contains)
				.map(ProcessIdAndVersion::getId).toList();

		applicationContext.get().getBeansOfType(listenerClass).entrySet()
				.forEach(executeOnProcessesDeployed(activePluginProcesses));
	}

	private Consumer<Entry<String, ? extends L>> executeOnProcessesDeployed(List<String> activePluginProcesses)
	{
		return entry ->
		{
			try
			{
				onProcessesDeployed.accept(entry.getValue(), activePluginProcesses);
			}
			catch (Exception e)
			{
				logger.debug("Error while executing {} bean of type {}", entry.getKey(),
						entry.getValue().getClass().getName(), e);
				logger.warn("Error while executing {} bean of type {}: {} - {}", entry.getKey(),
						entry.getValue().getClass().getName(), e.getClass().getName(), e.getMessage());
			}
		};
	}
}
