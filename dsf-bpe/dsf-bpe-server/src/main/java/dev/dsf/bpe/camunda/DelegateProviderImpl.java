package dev.dsf.bpe.camunda;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.delegate.VariableScope;
import org.camunda.bpm.engine.impl.bpmn.parser.FieldDeclaration;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;

import dev.dsf.bpe.api.plugin.ProcessIdAndVersion;
import dev.dsf.bpe.api.plugin.ProcessPlugin;

public class DelegateProviderImpl implements DelegateProvider, ProcessPluginConsumer, InitializingBean
{
	private static record ProcessByIdAndVersion(ProcessIdAndVersion processIdAndVersion, ProcessPlugin plugin)
	{
	}

	private final ClassLoader defaultClassLoader;
	private final ApplicationContext defaultApplicationContext;

	private final Map<ProcessIdAndVersion, ProcessPlugin> processPluginsByProcessIdAndVersion = new HashMap<>();

	public DelegateProviderImpl(ClassLoader mainClassLoader, ApplicationContext mainApplicationContext)
	{
		this.defaultClassLoader = mainClassLoader;
		this.defaultApplicationContext = mainApplicationContext;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(defaultClassLoader, "defaultClassLoader");
		Objects.requireNonNull(defaultApplicationContext, "defaultApplicationContext");
	}

	@Override
	public void setProcessPlugins(List<ProcessPlugin> plugins)
	{
		processPluginsByProcessIdAndVersion.putAll(plugins.stream()
				.flatMap(plugin -> plugin.getProcessKeysAndVersions().stream()
						.map(idAndVersion -> new ProcessByIdAndVersion(idAndVersion, plugin)))
				.collect(Collectors.toMap(ProcessByIdAndVersion::processIdAndVersion, ProcessByIdAndVersion::plugin)));
	}

	private ProcessPlugin getPlugin(ProcessIdAndVersion processIdAndVersion)
	{
		return processPluginsByProcessIdAndVersion.get(processIdAndVersion);
	}

	@Override
	public Class<?> getDefaultUserTaskListenerClass(ProcessIdAndVersion processKeyAndVersion)
	{
		return getPlugin(processKeyAndVersion).getDefaultUserTaskListenerClass();
	}

	@Override
	public boolean isDefaultUserTaskListenerOrSuperClassOf(ProcessIdAndVersion processKeyAndVersion, String className)
	{
		return getPlugin(processKeyAndVersion).isDefaultUserTaskListenerOrSuperClassOf(className);
	}

	@Override
	public JavaDelegate getMessageSendTask(ProcessIdAndVersion processIdAndVersion, String className,
			List<FieldDeclaration> fieldDeclarations, VariableScope variableScope)
	{
		return getPlugin(processIdAndVersion).getMessageSendTask(className, fieldDeclarations, variableScope);
	}

	@Override
	public JavaDelegate getServiceTask(ProcessIdAndVersion processIdAndVersion, String className,
			List<FieldDeclaration> fieldDeclarations, VariableScope variableScope)
	{
		return getPlugin(processIdAndVersion).getServiceTask(className, fieldDeclarations, variableScope);
	}

	@Override
	public JavaDelegate getMessageEndEvent(ProcessIdAndVersion processIdAndVersion, String className,
			List<FieldDeclaration> fieldDeclarations, VariableScope variableScope)
	{
		return getPlugin(processIdAndVersion).getMessageEndEvent(className, fieldDeclarations, variableScope);
	}

	@Override
	public JavaDelegate getMessageIntermediateThrowEvent(ProcessIdAndVersion processIdAndVersion, String className,
			List<FieldDeclaration> fieldDeclarations, VariableScope variableScope)
	{
		return getPlugin(processIdAndVersion).getMessageIntermediateThrowEvent(className, fieldDeclarations,
				variableScope);
	}

	@Override
	public ExecutionListener getExecutionListener(ProcessIdAndVersion processIdAndVersion, String className,
			List<FieldDeclaration> fieldDeclarations, VariableScope variableScope)
	{
		return getPlugin(processIdAndVersion).getExecutionListener(className, fieldDeclarations, variableScope);
	}

	@Override
	public TaskListener getTaskListener(ProcessIdAndVersion processIdAndVersion, String className,
			List<FieldDeclaration> fieldDeclarations, VariableScope variableScope)
	{
		return getPlugin(processIdAndVersion).getTaskListener(className, fieldDeclarations, variableScope);
	}
}
