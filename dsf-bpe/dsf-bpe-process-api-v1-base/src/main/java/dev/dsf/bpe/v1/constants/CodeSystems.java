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

import org.hl7.fhir.r4.model.Coding;

/**
 * Constants defining standard DSF CodeSystems
 */
public final class CodeSystems
{
	private CodeSystems()
	{
	}

	public static final class BpmnMessage
	{
		private BpmnMessage()
		{
		}

		public static final String URL = "http://dsf.dev/fhir/CodeSystem/bpmn-message";

		public static final class Codes
		{
			private Codes()
			{
			}

			public static final String MESSAGE_NAME = "message-name";
			public static final String BUSINESS_KEY = "business-key";
			public static final String CORRELATION_KEY = "correlation-key";
			public static final String ERROR = "error";
		}

		public static final Coding messageName()
		{
			return new Coding(URL, Codes.MESSAGE_NAME, null);
		}

		public static final Coding businessKey()
		{
			return new Coding(URL, Codes.BUSINESS_KEY, null);
		}

		public static final Coding correlationKey()
		{
			return new Coding(URL, Codes.CORRELATION_KEY, null);
		}

		public static final Coding error()
		{
			return new Coding(URL, Codes.ERROR, null);
		}
	}

	public static final class BpmnUserTask
	{
		private BpmnUserTask()
		{
		}

		public static final String URL = "http://dsf.dev/fhir/CodeSystem/bpmn-user-task";

		public static final class Codes
		{
			private Codes()
			{
			}

			public static final String BUSINESS_KEY = "business-key";
			public static final String USER_TASK_ID = "user-task-id";
		}

		public static final Coding businessKey()
		{
			return new Coding(URL, Codes.BUSINESS_KEY, null);
		}

		public static final Coding userTaskId()
		{
			return new Coding(URL, Codes.USER_TASK_ID, null);
		}
	}
}