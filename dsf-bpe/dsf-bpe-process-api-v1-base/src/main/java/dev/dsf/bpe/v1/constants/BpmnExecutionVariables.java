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
package dev.dsf.bpe.v1.constants;

import dev.dsf.bpe.v1.variables.Target;

/**
 * Defines names of standard process engine variables used by the bpe
 *
 * @see dev.dsf.bpe.v1.variables.Variables
 */
public final class BpmnExecutionVariables
{
	private BpmnExecutionVariables()
	{
	}

	/**
	 * Values from the <code>target</code> variable are used to configure
	 * {@link dev.dsf.bpe.v1.activity.AbstractTaskMessageSend} activities for sending Task resource messages
	 *
	 * @see dev.dsf.bpe.v1.variables.Variables#createTarget(String, String, String, String)
	 * @see dev.dsf.bpe.v1.variables.Variables#createTarget(String, String, String)
	 * @see dev.dsf.bpe.v1.variables.Variables#setTarget(dev.dsf.bpe.v1.variables.Target)
	 * @see dev.dsf.bpe.v1.variables.Variables#getTarget()
	 */
	public static final String TARGET = "target";

	/**
	 * The <code>targets</code> variable is typically used to iterate over {@link Target} variables in multi instance
	 * send/receive tasks or multi instance subprocesses
	 *
	 * @see dev.dsf.bpe.v1.variables.Variables#createTargets(java.util.List)
	 * @see dev.dsf.bpe.v1.variables.Variables#createTargets(dev.dsf.bpe.v1.variables.Target...)
	 * @see dev.dsf.bpe.v1.variables.Variables#setTargets(dev.dsf.bpe.v1.variables.Targets)
	 * @see dev.dsf.bpe.v1.variables.Variables#getTargets()
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
	 * waiting process instance if an alternative business-key was created for a communication target. See corresponding
	 * <code>protected</code> method in {@link dev.dsf.bpe.v1.activity.AbstractTaskMessageSend} on how to create and use
	 * an alternative business-key.
	 *
	 * @see dev.dsf.bpe.v1.activity.AbstractTaskMessageSend
	 */
	public static final String ALTERNATIVE_BUSINESS_KEY = "alternativeBusinessKey";
}
