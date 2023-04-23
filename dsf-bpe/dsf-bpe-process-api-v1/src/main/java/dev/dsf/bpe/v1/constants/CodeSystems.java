package dev.dsf.bpe.v1.constants;

import org.hl7.fhir.r4.model.Coding;

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