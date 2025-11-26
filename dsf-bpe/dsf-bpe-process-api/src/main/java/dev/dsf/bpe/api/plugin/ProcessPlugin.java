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
package dev.dsf.bpe.api.plugin;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.delegate.JavaDelegate;
import org.operaton.bpm.engine.delegate.TaskListener;
import org.operaton.bpm.engine.delegate.VariableScope;
import org.operaton.bpm.engine.impl.bpmn.parser.FieldDeclaration;
import org.operaton.bpm.engine.impl.variable.serializer.TypedValueSerializer;
import org.operaton.bpm.engine.variable.value.PrimitiveValue;
import org.springframework.context.ApplicationContext;

import dev.dsf.bpe.api.logging.PluginMdc;

public interface ProcessPlugin
{
	String MODEL_ATTRIBUTE_PROCESS_API_VERSION = "dsf.process.api.version";

	boolean initializeAndValidateResources(String localOrganizationIdentifierValue);

	PrimitiveValue<?> createFhirTaskVariable(String taskJson);

	PrimitiveValue<?> createFhirQuestionnaireResponseVariable(String questionnaireResponseJson);

	Path getJarFile();

	ClassLoader getProcessPluginClassLoader();

	ApplicationContext getApplicationContext();

	PluginMdc getPluginMdc();

	@SuppressWarnings("rawtypes")
	Stream<TypedValueSerializer> getTypedValueSerializers();

	List<ProcessIdAndVersion> getProcessKeysAndVersions();

	Map<ProcessIdAndVersion, List<byte[]>> getFhirResources();

	List<BpmnFileAndModel> getProcessModels();

	String getPluginDefinitionPackageName();

	ProcessPluginDeploymentListener getProcessPluginDeploymentListener();

	Class<?> getDefaultUserTaskListenerClass();

	boolean isDefaultUserTaskListenerOrSuperClassOf(String className);

	JavaDelegate getMessageSendTask(String className, List<FieldDeclaration> fieldDeclarations,
			VariableScope variableScope);

	JavaDelegate getServiceTask(String className, List<FieldDeclaration> fieldDeclarations,
			VariableScope variableScope);

	JavaDelegate getMessageEndEvent(String className, List<FieldDeclaration> fieldDeclarations,
			VariableScope variableScope);

	JavaDelegate getMessageIntermediateThrowEvent(String className, List<FieldDeclaration> fieldDeclarations,
			VariableScope variableScope);

	ExecutionListener getExecutionListener(String className, List<FieldDeclaration> fieldDeclarations,
			VariableScope variableScope);

	TaskListener getTaskListener(String className, List<FieldDeclaration> fieldDeclarations,
			VariableScope variableScope);

	FhirResourceModifier getFhirResourceModifier();
}
