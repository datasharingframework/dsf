package dev.dsf.bpe.listener;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.ParameterComponent;
import org.springframework.beans.factory.InitializingBean;

import dev.dsf.bpe.variables.VariablesImpl;

public abstract class AbstractListener implements ExecutionListener, InitializingBean
{
	private final String serverBaseUrl;

	public AbstractListener(String serverBaseUrl)
	{
		this.serverBaseUrl = serverBaseUrl;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(serverBaseUrl, "serverBaseUrl");
	}

	@Override
	public final void notify(DelegateExecution execution) throws Exception
	{
		doNotify(execution, new VariablesImpl(execution));
	}

	protected abstract void doNotify(DelegateExecution execution, VariablesImpl variables) throws Exception;

	protected final String getLocalVersionlessAbsoluteUrl(Task task)
	{
		return task == null ? null
				: task.getIdElement().toVersionless().withServerBase(serverBaseUrl, ResourceType.Task.name())
						.getValue();
	}

	protected final String getFirstInputParameter(Task task, Coding code)
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

	protected final String getCurrentTime()
	{
		return ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
	}

	protected final String getRequesterIdentifierValue(Task task)
	{
		if (task == null)
			return null;

		return task.getRequester().getIdentifier().getValue();
	}
}
