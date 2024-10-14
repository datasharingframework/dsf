package dev.dsf.bpe.v2.service;

import java.util.Objects;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.ParameterComponent;
import org.hl7.fhir.r4.model.Task.TaskOutputComponent;
import org.hl7.fhir.r4.model.Type;

public class TaskHelperImpl implements TaskHelper
{
	private final String serverBaseUrl;

	/**
	 * @param serverBaseUrl
	 *            not <code>null</code>
	 */
	public TaskHelperImpl(String serverBaseUrl)
	{
		this.serverBaseUrl = serverBaseUrl;
	}

	@Override
	public String getLocalVersionlessAbsoluteUrl(Task task)
	{
		if (task == null)
			return null;

		return task.getIdElement().toVersionless().withServerBase(serverBaseUrl, ResourceType.Task.name()).getValue();
	}

	@Override
	public Stream<String> getInputParameterStringValues(Task task, Coding coding)
	{
		return getInputParameterValues(task, coding, StringType.class).map(StringType::getValue);
	}

	@Override
	public Stream<String> getInputParameterStringValues(Task task, String system, String code)
	{
		return getInputParameterValues(task, system, code, StringType.class).map(StringType::getValue);
	}

	@Override
	public <T extends Type> Stream<T> getInputParameterValues(Task task, Coding coding, Class<T> expectedType)
	{
		return getInputParameters(task, coding, expectedType).filter(ParameterComponent::hasValue)
				.map(c -> expectedType.cast(c.getValue()));
	}

	@Override
	public <T extends Type> Stream<T> getInputParameterValues(Task task, String system, String code,
			Class<T> expectedType)
	{
		return getInputParameters(task, system, code, expectedType).filter(ParameterComponent::hasValue)
				.map(c -> expectedType.cast(c.getValue()));
	}

	@Override
	public Stream<ParameterComponent> getInputParametersWithExtension(Task task, Coding coding,
			Class<? extends Type> expectedType, String extensionUrl)
	{
		return getInputParameters(task, coding, expectedType).filter(ParameterComponent::hasExtension)
				.filter(c -> c.getExtension().stream().anyMatch(e -> Objects.equals(extensionUrl, e.getUrl())));
	}

	@Override
	public Stream<ParameterComponent> getInputParametersWithExtension(Task task, String system, String code,
			Class<? extends Type> expectedType, String extensionUrl)
	{
		return getInputParameters(task, system, code, expectedType).filter(ParameterComponent::hasExtension)
				.filter(c -> c.getExtension().stream().anyMatch(e -> Objects.equals(extensionUrl, e.getUrl())));
	}

	@Override
	public Stream<ParameterComponent> getInputParameters(Task task, Coding coding, Class<? extends Type> expectedType)
	{
		if (coding == null)
			return Stream.empty();

		return getInputParameters(task, coding.getSystem(), coding.getCode(), expectedType);
	}

	@Override
	public Stream<ParameterComponent> getInputParameters(Task task, String system, String code,
			Class<? extends Type> expectedType)
	{
		Objects.requireNonNull(expectedType, "expectedType");

		if (task == null)
			return Stream.empty();

		return task.getInput().stream().filter(c -> c.hasType() && c.getType().hasCoding())
				.filter(c -> c.getType().getCoding().stream()
						.anyMatch(co -> Objects.equals(system, co.getSystem()) && Objects.equals(code, co.getCode())))
				.filter(c -> c.hasValue() && expectedType.isInstance(c.getValue()));
	}

	@Override
	public ParameterComponent createInput(Type value, Coding coding)
	{
		return new ParameterComponent(new CodeableConcept(coding), value);
	}

	@Override
	public ParameterComponent createInput(Type value, String system, String code)
	{
		return createInput(value, new Coding(system, code, null));
	}

	@Override
	public TaskOutputComponent createOutput(Type value, Coding coding)
	{
		return new TaskOutputComponent(new CodeableConcept(coding), value);
	}

	@Override
	public TaskOutputComponent createOutput(Type value, String system, String code)
	{
		return createOutput(value, new Coding(system, code, null));
	}
}
