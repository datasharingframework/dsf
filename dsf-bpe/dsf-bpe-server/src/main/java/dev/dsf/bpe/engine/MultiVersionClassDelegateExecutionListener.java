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
package dev.dsf.bpe.engine;

import java.util.List;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.impl.bpmn.delegate.ExecutionListenerInvocation;
import org.operaton.bpm.engine.impl.bpmn.listener.ClassDelegateExecutionListener;
import org.operaton.bpm.engine.impl.bpmn.parser.FieldDeclaration;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;

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
