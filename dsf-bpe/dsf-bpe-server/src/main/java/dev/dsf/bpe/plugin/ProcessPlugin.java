package dev.dsf.bpe.plugin;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.impl.variable.serializer.TypedValueSerializer;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.context.ApplicationContext;

public interface ProcessPlugin<D, A, L extends TaskListener>
{
	String MODEL_ATTRIBUTE_PROCESS_API_VERSION = "dsf.process.api.version";

	boolean initializeAndValidateResources(String localOrganizationIdentifierValue);

	D getProcessPluginDefinition();

	A getProcessPluginApi();

	L getDefaultUserTaskListener();

	boolean isDraft();

	Path getJarFile();

	ClassLoader getProcessPluginClassLoader();

	ApplicationContext getApplicationContext();

	@SuppressWarnings("rawtypes")
	List<TypedValueSerializer> getTypedValueSerializers();

	List<ProcessIdAndVersion> getProcessKeysAndVersions();

	Map<ProcessIdAndVersion, List<Resource>> getFhirResources();

	List<BpmnFileAndModel> getProcessModels();

}
