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
