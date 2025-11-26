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
package dev.dsf.bpe.v2.activity.task;

import java.util.UUID;

import dev.dsf.bpe.v2.variables.Target;
import dev.dsf.bpe.v2.variables.Variables;

public enum BusinessKeyStrategies implements BusinessKeyStrategy
{
	/**
	 * Uses the business-key from the current process instance when sending Task resources.
	 * <p>
	 * The target can reply to this process instance with the send key.
	 */
	SAME
	{
		@Override
		public String get(Variables variables, Target target)
		{
			return variables.getBusinessKey();
		}
	},
	/**
	 * Generates an alternative buisness-key for the current process instance and uses the alternative when sending Task
	 * resources.
	 * <p>
	 * This can be used to hide the current business-key from the target, but allows the target to reply using the send
	 * alternative business-key.
	 */
	ALTERNATIVE
	{
		@Override
		public String get(Variables variables, Target target)
		{
			String alternativeBusinessKey = createBusinessKey();

			variables.setAlternativeBusinessKey(alternativeBusinessKey);

			return alternativeBusinessKey;
		}
	},
	/**
	 * Generates a new business-key for every Task send.
	 * <p>
	 * This does not allow the target to reply to the current process instance.
	 */
	NEW
	{
		@Override
		public String get(Variables variables, Target target)
		{
			return createBusinessKey();
		}

	};

	private static String createBusinessKey()
	{
		return UUID.randomUUID().toString();
	}
}
