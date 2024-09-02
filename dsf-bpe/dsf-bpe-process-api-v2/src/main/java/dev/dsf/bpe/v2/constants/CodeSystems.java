package dev.dsf.bpe.v2.constants;

import org.hl7.fhir.r4.model.Coding;

/**
 * Constants defining standard DSF CodeSystems
 */
public final class CodeSystems
{
	private CodeSystems()
	{
	}

	private static boolean isSame(String system, String code, Coding coding)
	{
		return system != null && code != null && coding != null && coding.hasSystem()
				&& system.equals(coding.getSystem()) && coding.hasCode() && code.equals(coding.getCode());
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

	public static final class ProcessAuthorization
	{
		private ProcessAuthorization()
		{
		}

		public static final String URL = "http://dsf.dev/fhir/CodeSystem/process-authorization";

		public static final class Codes
		{
			private Codes()
			{
			}

			public static final String LOCAL_ORGANIZATION = "LOCAL_ORGANIZATION";
			public static final String LOCAL_ORGANIZATION_PRACTITIONER = "LOCAL_ORGANIZATION_PRACTITIONER";
			public static final String REMOTE_ORGANIZATION = "REMOTE_ORGANIZATION";
			public static final String LOCAL_ROLE = "LOCAL_ROLE";
			public static final String LOCAL_ROLE_PRACTITIONER = "LOCAL_ROLE_PRACTITIONER";
			public static final String REMOTE_ROLE = "REMOTE_ROLE";
			public static final String LOCAL_ALL = "LOCAL_ALL";
			public static final String LOCAL_ALL_PRACTITIONER = "LOCAL_ALL_PRACTITIONER";
			public static final String REMOTE_ALL = "REMOTE_ALL";
		}

		public static final Coding localOrganization()
		{
			return new Coding(URL, Codes.LOCAL_ORGANIZATION, null);
		}

		public static final Coding localOrganizationPractitioner()
		{
			return new Coding(URL, Codes.LOCAL_ORGANIZATION_PRACTITIONER, null);
		}

		public static final Coding remoteOrganization()
		{
			return new Coding(URL, Codes.REMOTE_ORGANIZATION, null);
		}

		public static final Coding localRole()
		{
			return new Coding(URL, Codes.LOCAL_ROLE, null);
		}

		public static final Coding localRolePractitioner()
		{
			return new Coding(URL, Codes.LOCAL_ROLE_PRACTITIONER, null);
		}

		public static final Coding remoteRole()
		{
			return new Coding(URL, Codes.REMOTE_ROLE, null);
		}

		public static final Coding localAll()
		{
			return new Coding(URL, Codes.LOCAL_ALL, null);
		}

		public static final Coding localAllPractitioner()
		{
			return new Coding(URL, Codes.LOCAL_ALL_PRACTITIONER, null);
		}

		public static final Coding remoteAll()
		{
			return new Coding(URL, Codes.REMOTE_ALL, null);
		}

		public static final boolean isLocalOrganization(Coding coding)
		{
			return isSame(URL, Codes.LOCAL_ORGANIZATION, coding);
		}

		public static final boolean isLocalOrganizationPractitioner(Coding coding)
		{
			return isSame(URL, Codes.LOCAL_ORGANIZATION_PRACTITIONER, coding);
		}

		public static final boolean isRemoteOrganization(Coding coding)
		{
			return isSame(URL, Codes.REMOTE_ORGANIZATION, coding);
		}

		public static final boolean isLocalRole(Coding coding)
		{
			return isSame(URL, Codes.LOCAL_ROLE, coding);
		}

		public static final boolean isLocalRolePractitioner(Coding coding)
		{
			return isSame(URL, Codes.LOCAL_ROLE_PRACTITIONER, coding);
		}

		public static final boolean isRemoteRole(Coding coding)
		{
			return isSame(URL, Codes.REMOTE_ROLE, coding);
		}

		public static final boolean isLocalAll(Coding coding)
		{
			return isSame(URL, Codes.LOCAL_ALL, coding);
		}

		public static final boolean isLocalAllPractitioner(Coding coding)
		{
			return isSame(URL, Codes.LOCAL_ALL_PRACTITIONER, coding);
		}

		public static final boolean isRemoteAll(Coding coding)
		{
			return isSame(URL, Codes.REMOTE_ALL, coding);
		}
	}
}