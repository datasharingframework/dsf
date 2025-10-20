package dev.dsf.bpe.v2.listener;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.function.Function;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.ParameterComponent;
import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.springframework.beans.factory.InitializingBean;

public abstract class AbstractListener implements ExecutionListener, InitializingBean
{
	private final String serverBaseUrl;
	private final Function<DelegateExecution, ListenerVariables> variablesFactory;

	public AbstractListener(String serverBaseUrl, Function<DelegateExecution, ListenerVariables> variablesFactory)
	{
		this.serverBaseUrl = serverBaseUrl;
		this.variablesFactory = variablesFactory;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(serverBaseUrl, "serverBaseUrl");
		Objects.requireNonNull(variablesFactory, "variablesFactory");
	}

	@Override
	public final void notify(DelegateExecution execution) throws Exception
	{
		doNotify(execution, variablesFactory.apply(execution));
	}

	protected abstract void doNotify(DelegateExecution execution, ListenerVariables variables) throws Exception;

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
