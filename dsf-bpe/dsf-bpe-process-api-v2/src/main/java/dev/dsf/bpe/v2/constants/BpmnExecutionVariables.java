package dev.dsf.bpe.v2.constants;

import dev.dsf.bpe.v2.activity.MessageEndEvent;
import dev.dsf.bpe.v2.activity.MessageIntermediateThrowEvent;
import dev.dsf.bpe.v2.activity.MessageSendTask;
import dev.dsf.bpe.v2.activity.task.BusinessKeyStrategies;
import dev.dsf.bpe.v2.variables.Target;
import dev.dsf.bpe.v2.variables.Variables;

/**
 * Defines names of standard process engine variables used by the bpe
 *
 * @see Variables
 */
public final class BpmnExecutionVariables
{
	private BpmnExecutionVariables()
	{
	}

	/**
	 * Values from the <code>target</code> variable are used to configure {@link MessageEndEvent},
	 * {@link MessageIntermediateThrowEvent} and {@link MessageSendTask} activities for sending Task resource messages
	 *
	 * @see Variables#createTarget(String, String, String, String)
	 * @see Variables#createTarget(String, String, String)
	 * @see Variables#setTarget(dev.dsf.bpe.v2.variables.Target)
	 * @see Variables#getTarget()
	 */
	public static final String TARGET = "target";

	/**
	 * The <code>targets</code> variable is typically used to iterate over {@link Target} variables in multi instance
	 * send/receive tasks or multi instance subprocesses
	 *
	 * @see Variables#createTargets(java.util.List)
	 * @see Variables#createTargets(dev.dsf.bpe.v2.variables.Target...)
	 * @see Variables#setTargets(dev.dsf.bpe.v2.variables.Targets)
	 * @see Variables#getTargets()
	 */
	public static final String TARGETS = "targets";

	/**
	 * Value of the <code>correlationKey</code> variable is used to correlated incoming Task resources to waiting multi
	 * instance process activities
	 *
	 * @see Target#getCorrelationKey()
	 */
	public static final String CORRELATION_KEY = "correlationKey";

	/**
	 * Value of the <code>alternativeBusinessKey</code> variable is used to correlated incoming Task resource to a
	 * waiting process instance if an alternative business-key was created for a communication target.
	 *
	 * @see MessageEndEvent#getBusinessKeyStrategy()
	 * @see MessageIntermediateThrowEvent#getBusinessKeyStrategy()
	 * @see MessageEndEvent#getBusinessKeyStrategy()
	 * @see BusinessKeyStrategies#ALTERNATIVE
	 */
	public static final String ALTERNATIVE_BUSINESS_KEY = "alternativeBusinessKey";
}
