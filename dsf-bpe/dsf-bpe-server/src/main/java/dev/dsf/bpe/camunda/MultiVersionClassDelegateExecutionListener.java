package dev.dsf.bpe.camunda;

import java.util.List;

import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.impl.bpmn.delegate.ExecutionListenerInvocation;
import org.camunda.bpm.engine.impl.bpmn.listener.ClassDelegateExecutionListener;
import org.camunda.bpm.engine.impl.bpmn.parser.FieldDeclaration;
import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;

import dev.dsf.bpe.api.plugin.ProcessIdAndVersion;

public class MultiVersionClassDelegateExecutionListener extends ClassDelegateExecutionListener
{
	private final DelegateProvider delegateProvider;

	public MultiVersionClassDelegateExecutionListener(String className, List<FieldDeclaration> fieldDeclarations,
			DelegateProvider delegateProvider)
	{
		super(className, fieldDeclarations);

		this.delegateProvider = delegateProvider;
	}

	@Override
	public void notify(DelegateExecution execution)
	{
		try
		{
			ExecutionEntity e = (ExecutionEntity) execution;

			ProcessIdAndVersion processKeyAndVersion = new ProcessIdAndVersion(e.getProcessDefinition().getKey(),
					e.getProcessDefinition().getVersionTag());

			ExecutionListener listener = delegateProvider.getExecutionListener(processKeyAndVersion, className,
					fieldDeclarations, e);

			Context.getProcessEngineConfiguration().getDelegateInterceptor()
					.handleInvocation(new ExecutionListenerInvocation(listener, execution));
		}
		catch (Exception exception)
		{
			throw new ProcessEngineException("Exception while invoking ExecutionListener: " + exception.getMessage(),
					exception);
		}
	}
}
