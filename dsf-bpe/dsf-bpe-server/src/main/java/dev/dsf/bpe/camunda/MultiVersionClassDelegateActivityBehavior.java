package dev.dsf.bpe.camunda;

import java.util.List;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.delegate.JavaDelegate;
import org.operaton.bpm.engine.impl.bpmn.behavior.ClassDelegateActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.ServiceTaskJavaDelegateActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.parser.FieldDeclaration;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.pvm.delegate.ActivityBehavior;
import org.operaton.bpm.engine.impl.pvm.delegate.ActivityExecution;
import org.operaton.bpm.model.bpmn.instance.EndEvent;
import org.operaton.bpm.model.bpmn.instance.IntermediateThrowEvent;
import org.operaton.bpm.model.bpmn.instance.SendTask;
import org.operaton.bpm.model.bpmn.instance.ServiceTask;

import dev.dsf.bpe.api.plugin.ProcessIdAndVersion;

public class MultiVersionClassDelegateActivityBehavior extends ClassDelegateActivityBehavior
{
	private final DelegateProvider delegateProvider;

	public MultiVersionClassDelegateActivityBehavior(String className, List<FieldDeclaration> fieldDeclarations,
			DelegateProvider delegateProvider)
	{
		super(className, fieldDeclarations);

		this.delegateProvider = delegateProvider;
	}

	@Override
	protected ActivityBehavior getActivityBehaviorInstance(ActivityExecution execution)
	{
		try
		{
			ExecutionEntity e = (ExecutionEntity) execution;
			ProcessIdAndVersion processKeyAndVersion = new ProcessIdAndVersion(e.getProcessDefinition().getKey(),
					e.getProcessDefinition().getVersionTag());

			JavaDelegate delegate = switch (e.getBpmnModelElementInstance())
			{
				case SendTask _ ->
					delegateProvider.getMessageSendTask(processKeyAndVersion, className, fieldDeclarations, execution);
				case ServiceTask _ ->
					delegateProvider.getServiceTask(processKeyAndVersion, className, fieldDeclarations, execution);
				case EndEvent _ ->
					delegateProvider.getMessageEndEvent(processKeyAndVersion, className, fieldDeclarations, execution);
				case IntermediateThrowEvent _ -> delegateProvider.getMessageIntermediateThrowEvent(processKeyAndVersion,
						className, fieldDeclarations, execution);

				default -> throw new IllegalArgumentException("Unexpected value: " + e.getBpmnModelElementInstance());
			};

			return new ServiceTaskJavaDelegateActivityBehavior(delegate);
		}
		catch (Exception e)
		{
			throw new ProcessEngineException(
					"Exception while creating ServiceTaskJavaDelegateActivityBehavior: " + e.getMessage(), e);
		}
	}
}
