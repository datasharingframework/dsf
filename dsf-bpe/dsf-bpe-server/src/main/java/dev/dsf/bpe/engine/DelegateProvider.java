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

import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.delegate.JavaDelegate;
import org.operaton.bpm.engine.delegate.TaskListener;
import org.operaton.bpm.engine.delegate.VariableScope;
import org.operaton.bpm.engine.impl.bpmn.parser.FieldDeclaration;

import dev.dsf.bpe.api.plugin.ProcessIdAndVersion;

public interface DelegateProvider extends ProcessPluginConsumer
{
	Class<?> getDefaultUserTaskListenerClass(ProcessIdAndVersion processKeyAndVersion);

	boolean isDefaultUserTaskListenerOrSuperClassOf(ProcessIdAndVersion processKeyAndVersion, String className);

	JavaDelegate getMessageSendTask(ProcessIdAndVersion processIdAndVersion, String className,
			List<FieldDeclaration> fieldDeclarations, VariableScope variableScope);

	JavaDelegate getServiceTask(ProcessIdAndVersion processIdAndVersion, String className,
			List<FieldDeclaration> fieldDeclarations, VariableScope variableScope);

	JavaDelegate getMessageEndEvent(ProcessIdAndVersion processIdAndVersion, String className,
			List<FieldDeclaration> fieldDeclarations, VariableScope variableScope);

	JavaDelegate getMessageIntermediateThrowEvent(ProcessIdAndVersion processIdAndVersion, String className,
			List<FieldDeclaration> fieldDeclarations, VariableScope variableScope);

	ExecutionListener getExecutionListener(ProcessIdAndVersion processIdAndVersion, String className,
			List<FieldDeclaration> fieldDeclarations, VariableScope variableScope);

	TaskListener getTaskListener(ProcessIdAndVersion processIdAndVersion, String className,
			List<FieldDeclaration> fieldDeclarations, VariableScope variableScope);
}
