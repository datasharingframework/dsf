/*
 * Copyright 2018-2025 Heilbronn University of Applied Sciences
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
