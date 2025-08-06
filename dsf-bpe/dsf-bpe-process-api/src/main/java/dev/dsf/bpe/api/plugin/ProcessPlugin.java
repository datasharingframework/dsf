package dev.dsf.bpe.api.plugin;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.delegate.VariableScope;
import org.camunda.bpm.engine.impl.bpmn.parser.FieldDeclaration;
import org.camunda.bpm.engine.impl.variable.serializer.TypedValueSerializer;
import org.camunda.bpm.engine.variable.value.PrimitiveValue;
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
