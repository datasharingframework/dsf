package dev.dsf.bpe.v1.logging;

import java.util.Objects;
import java.util.function.Function;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.ParameterComponent;
import org.operaton.bpm.engine.delegate.DelegateExecution;

import dev.dsf.bpe.api.Constants;
import dev.dsf.bpe.api.logging.AbstractPluginMdc;
import dev.dsf.bpe.v1.constants.CodeSystems.BpmnMessage;
import dev.dsf.bpe.v1.variables.Variables;

public class PluginMdcImpl extends AbstractPluginMdc
{
	private final String serverBaseUrl;
	private final Function<DelegateExecution, Variables> variablesFactory;

	/**
	 * @param apiVersion
	 * @param name
	 *            not <code>null</code>
	 * @param version
	 *            not <code>null</code>
	 * @param jar
	 *            not <code>null</code>
	 * @param serverBaseUrl
	 *            not <code>null</code>
	 * @param variablesFactory
	 *            not <code>null</code>
	 */
	public PluginMdcImpl(int apiVersion, String name, String version, String jar, String serverBaseUrl,
			Function<DelegateExecution, Variables> variablesFactory)
	{
		super(apiVersion, name, version, jar);

		this.serverBaseUrl = Objects.requireNonNull(serverBaseUrl, "serverBaseUrl");
		this.variablesFactory = Objects.requireNonNull(variablesFactory, "variablesFactory");
	}

	@Override
	protected ProcessValues getProcessValues(DelegateExecution delegateExecution)
	{
		Variables variables = variablesFactory.apply(delegateExecution);

		Task startTask = variables.getStartTask();
		if (startTask == null)
			startTask = variables.getResource(Constants.TASK_VARIABLE);

		Task latestTask = variables.getLatestTask();
		if (startTask == latestTask)
			latestTask = null;

		return new ProcessValues(startTask == null ? null : startTask.getInstantiatesCanonical(),
				getLocalVersionlessAbsoluteUrl(startTask), getRequesterIdentifierValue(startTask),
				getFirstInputParameter(latestTask, BpmnMessage.correlationKey()),
				getLocalVersionlessAbsoluteUrl(latestTask), getRequesterIdentifierValue(latestTask));
	}

	private String getLocalVersionlessAbsoluteUrl(Task task)
	{
		return task == null ? null
				: task.getIdElement().toVersionless().withServerBase(serverBaseUrl, ResourceType.Task.name())
						.getValue();
	}

	private String getRequesterIdentifierValue(Task task)
	{
		if (task == null)
			return null;

		return task.getRequester().getIdentifier().getValue();
	}

	private String getFirstInputParameter(Task task, Coding code)
	{
		if (task == null || code == null)
			return null;

		return task.getInput().stream().filter(ParameterComponent::hasType)
				.filter(c -> c.getType().getCoding().stream()
						.anyMatch(co -> co != null && Objects.equals(code.getSystem(), co.getSystem())
								&& Objects.equals(code.getCode(), co.getCode())))
				.filter(ParameterComponent::hasValue).map(ParameterComponent::getValue)
				.filter(v -> v instanceof StringType).map(v -> (StringType) v).map(StringType::getValue).findFirst()
				.orElse(null);
	}
}
