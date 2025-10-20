package dev.dsf.bpe.camunda;

import java.util.List;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.delegate.DelegateTask;
import org.operaton.bpm.engine.delegate.TaskListener;
import org.operaton.bpm.engine.impl.bpmn.parser.FieldDeclaration;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.persistence.entity.TaskEntity;
import org.operaton.bpm.engine.impl.task.delegate.TaskListenerInvocation;
import org.operaton.bpm.engine.impl.task.listener.ClassDelegateTaskListener;

import dev.dsf.bpe.api.plugin.ProcessIdAndVersion;

public class MultiVersionClassDelegateTaskListener extends ClassDelegateTaskListener
{
	private final DelegateProvider delegateProvider;

	public MultiVersionClassDelegateTaskListener(String className, List<FieldDeclaration> fieldDeclarations,
			DelegateProvider delegateProvider)
	{
		super(className, fieldDeclarations);

		this.delegateProvider = delegateProvider;
	}

	@Override
	public void notify(DelegateTask delegateTask)
	{
		try
		{
			TaskEntity te = (TaskEntity) delegateTask;

			ProcessIdAndVersion processKeyAndVersion = new ProcessIdAndVersion(te.getProcessDefinition().getKey(),
					te.getProcessDefinition().getVersionTag());

			TaskListener listener = delegateProvider.getTaskListener(processKeyAndVersion, className, fieldDeclarations,
					te.getExecution());

			Context.getProcessEngineConfiguration().getDelegateInterceptor()
					.handleInvocation(new TaskListenerInvocation(listener, delegateTask));
		}
		catch (Exception e)
		{
			throw new ProcessEngineException("Exception while invoking TaskListener: " + e.getMessage(), e);
		}
	}
}
