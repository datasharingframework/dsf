package dev.dsf.bpe.camunda;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.delegate.VariableScope;
import org.camunda.bpm.engine.impl.bpmn.parser.FieldDeclaration;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;

import dev.dsf.bpe.api.plugin.ProcessIdAndVersion;
import dev.dsf.bpe.api.plugin.ProcessPlugin;

public class DelegateProviderImpl implements DelegateProvider, InitializingBean
{
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
	public void setProcessPlugins(List<ProcessPlugin> plugins,
			Map<ProcessIdAndVersion, ProcessPlugin> processPluginsByProcessIdAndVersion)
	{
		this.processPluginsByProcessIdAndVersion.putAll(processPluginsByProcessIdAndVersion);
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
		ProcessPlugin plugin = getPlugin(processIdAndVersion);
		JavaDelegate delegate = plugin.getMessageSendTask(className, fieldDeclarations, variableScope);

		return delegateExecution -> plugin.getPluginMdc().executeWithProcessMdc(delegateExecution, delegate::execute);
	}

	@Override
	public JavaDelegate getServiceTask(ProcessIdAndVersion processIdAndVersion, String className,
			List<FieldDeclaration> fieldDeclarations, VariableScope variableScope)
	{
		ProcessPlugin plugin = getPlugin(processIdAndVersion);
		JavaDelegate delegate = plugin.getServiceTask(className, fieldDeclarations, variableScope);

		return delegateExecution -> plugin.getPluginMdc().executeWithProcessMdc(delegateExecution, delegate::execute);
	}

	@Override
	public JavaDelegate getMessageEndEvent(ProcessIdAndVersion processIdAndVersion, String className,
			List<FieldDeclaration> fieldDeclarations, VariableScope variableScope)
	{
		ProcessPlugin plugin = getPlugin(processIdAndVersion);
		JavaDelegate delegate = plugin.getMessageEndEvent(className, fieldDeclarations, variableScope);

		return delegateExecution -> plugin.getPluginMdc().executeWithProcessMdc(delegateExecution, delegate::execute);
	}

	@Override
	public JavaDelegate getMessageIntermediateThrowEvent(ProcessIdAndVersion processIdAndVersion, String className,
			List<FieldDeclaration> fieldDeclarations, VariableScope variableScope)
	{
		ProcessPlugin plugin = getPlugin(processIdAndVersion);
		JavaDelegate delegate = plugin.getMessageIntermediateThrowEvent(className, fieldDeclarations, variableScope);

		return delegateExecution -> plugin.getPluginMdc().executeWithProcessMdc(delegateExecution, delegate::execute);
	}

	@Override
	public ExecutionListener getExecutionListener(ProcessIdAndVersion processIdAndVersion, String className,
			List<FieldDeclaration> fieldDeclarations, VariableScope variableScope)
	{
		ProcessPlugin plugin = getPlugin(processIdAndVersion);
		ExecutionListener executionListener = plugin.getExecutionListener(className, fieldDeclarations, variableScope);

		return delegateExecution -> plugin.getPluginMdc().executeWithProcessMdc(delegateExecution,
				executionListener::notify);
	}

	@Override
	public TaskListener getTaskListener(ProcessIdAndVersion processIdAndVersion, String className,
			List<FieldDeclaration> fieldDeclarations, VariableScope variableScope)
	{
		ProcessPlugin plugin = getPlugin(processIdAndVersion);
		TaskListener taskListener = plugin.getTaskListener(className, fieldDeclarations, variableScope);

		return delegateTask -> plugin.getPluginMdc().executeWithProcessMdc(delegateTask, taskListener::notify);
	}
}
