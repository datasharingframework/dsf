package dev.dsf.bpe.v2.activity;

import java.util.List;

import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.ParameterComponent;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.task.BusinessKeyStrategies;
import dev.dsf.bpe.v2.activity.task.BusinessKeyStrategy;
import dev.dsf.bpe.v2.activity.task.DefaultTaskSender;
import dev.dsf.bpe.v2.activity.task.TaskSender;
import dev.dsf.bpe.v2.activity.values.SendTaskValues;
import dev.dsf.bpe.v2.variables.Target;
import dev.dsf.bpe.v2.variables.Variables;

public interface MessageActivity extends Activity
{
	/**
	 * Default implementation uses a {@link TaskSender} from
	 * {@link #getTaskSender(ProcessPluginApi, Variables, SendTaskValues)} to send {@link Task} resources with the
	 * {@link BusinessKeyStrategy} from {@link #getBusinessKeyStrategy()}.
	 *
	 * @param api
	 *            not <code>null</code>
	 * @param variables
	 *            not <code>null</code>
	 * @param sendTaskValues
	 *            not <code>null</code>
	 * @throws Exception
	 *             if the {@link Task} could not be send
	 */
	default void execute(ProcessPluginApi api, Variables variables, SendTaskValues sendTaskValues) throws Exception
	{
		getTaskSender(api, variables, sendTaskValues).send();
	}

	/**
	 * @param api
	 *            not <code>null</code>
	 * @param variables
	 *            not <code>null</code>
	 * @param sendTaskValues
	 *            not <code>null</code>
	 * @return {@link TaskSender} implementation to send {@link Task} resources
	 */
	default TaskSender getTaskSender(ProcessPluginApi api, Variables variables, SendTaskValues sendTaskValues)
	{
		return new DefaultTaskSender(api, variables, sendTaskValues, getBusinessKeyStrategy(),
				target -> getAdditionalInputParameters(api, variables, sendTaskValues, target));
	}

	/**
	 * @return {@link BusinessKeyStrategy} to use when sending {@link Task} resource
	 */
	default BusinessKeyStrategy getBusinessKeyStrategy()
	{
		return BusinessKeyStrategies.SAME;
	}

	/**
	 * @param api
	 *            not <code>null</code>
	 * @param variables
	 *            not <code>null</code>
	 * @param sendTaskValues
	 *            not <code>null</code>
	 * @param target
	 *            not <code>null</code>
	 * @return may be <code>null</code>
	 */
	default List<ParameterComponent> getAdditionalInputParameters(ProcessPluginApi api, Variables variables,
			SendTaskValues sendTaskValues, Target target)
	{
		return List.of();
	}
}
