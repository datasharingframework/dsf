package dev.dsf.bpe.api.plugin;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.camunda.bpm.engine.impl.variable.serializer.TypedValueSerializer;
import org.camunda.bpm.engine.variable.value.PrimitiveValue;
import org.springframework.context.ApplicationContext;

public interface ProcessPlugin
{
	String MODEL_ATTRIBUTE_PROCESS_API_VERSION = "dsf.process.api.version";

	boolean initializeAndValidateResources(String localOrganizationIdentifierValue);

	PrimitiveValue<?> createFhirTaskVariable(String taskJson);

	PrimitiveValue<?> createFhirQuestionnaireResponseVariable(String questionnaireResponseJson);

	Path getJarFile();

	ClassLoader getProcessPluginClassLoader();

	ApplicationContext getApplicationContext();

	@SuppressWarnings("rawtypes")
	Stream<TypedValueSerializer> getTypedValueSerializers();

	List<ProcessIdAndVersion> getProcessKeysAndVersions();

	Map<ProcessIdAndVersion, List<byte[]>> getFhirResources();

	List<BpmnFileAndModel> getProcessModels();

	ProcessPluginDeploymentListener getProcessPluginDeploymentListener();
}
