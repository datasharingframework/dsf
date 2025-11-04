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

		public static final String SYSTEM = "http://dsf.dev/fhir/CodeSystem/bpmn-message";

		public static Coding withCode(String code)
		{
			return new Coding().setSystem(SYSTEM).setCode(code);
		}

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
			return new Coding(SYSTEM, Codes.MESSAGE_NAME, null);
		}

		public static final Coding businessKey()
		{
			return new Coding(SYSTEM, Codes.BUSINESS_KEY, null);
		}

		public static final Coding correlationKey()
		{
			return new Coding(SYSTEM, Codes.CORRELATION_KEY, null);
		}

		public static final Coding error()
		{
			return new Coding(SYSTEM, Codes.ERROR, null);
		}

		public static final boolean isMessageName(Coding coding)
		{
			return isSame(SYSTEM, Codes.MESSAGE_NAME, coding);
		}

		public static final boolean isBusinessKey(Coding coding)
		{
			return isSame(SYSTEM, Codes.BUSINESS_KEY, coding);
		}

		public static final boolean isCorrelationKey(Coding coding)
		{
			return isSame(SYSTEM, Codes.CORRELATION_KEY, coding);
		}

		public static final boolean isError(Coding coding)
		{
			return isSame(SYSTEM, Codes.ERROR, coding);
		}
	}

	public static final class BpmnUserTask
	{
		private BpmnUserTask()
		{
		}

		public static final String SYSTEM = "http://dsf.dev/fhir/CodeSystem/bpmn-user-task";

		public static Coding withCode(String code)
		{
			return new Coding().setSystem(SYSTEM).setCode(code);
		}

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
			return new Coding(SYSTEM, Codes.BUSINESS_KEY, null);
		}

		public static final Coding userTaskId()
		{
			return new Coding(SYSTEM, Codes.USER_TASK_ID, null);
		}

		public static final boolean isBusinessKey(Coding coding)
		{
			return isSame(SYSTEM, Codes.BUSINESS_KEY, coding);
		}

		public static final boolean isUserTaskId(Coding coding)
		{
			return isSame(SYSTEM, Codes.USER_TASK_ID, coding);
		}
	}

	public static final class ProcessAuthorization
	{
		private ProcessAuthorization()
		{
		}

		public static final String SYSTEM = "http://dsf.dev/fhir/CodeSystem/process-authorization";

		public static Coding withCode(String code)
		{
			return new Coding().setSystem(SYSTEM).setCode(code);
		}

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
			return new Coding(SYSTEM, Codes.LOCAL_ORGANIZATION, null);
		}

		public static final Coding localOrganizationPractitioner()
		{
			return new Coding(SYSTEM, Codes.LOCAL_ORGANIZATION_PRACTITIONER, null);
		}

		public static final Coding remoteOrganization()
		{
			return new Coding(SYSTEM, Codes.REMOTE_ORGANIZATION, null);
		}

		public static final Coding localRole()
		{
			return new Coding(SYSTEM, Codes.LOCAL_ROLE, null);
		}

		public static final Coding localRolePractitioner()
		{
			return new Coding(SYSTEM, Codes.LOCAL_ROLE_PRACTITIONER, null);
		}

		public static final Coding remoteRole()
		{
			return new Coding(SYSTEM, Codes.REMOTE_ROLE, null);
		}

		public static final Coding localAll()
		{
			return new Coding(SYSTEM, Codes.LOCAL_ALL, null);
		}

		public static final Coding localAllPractitioner()
		{
			return new Coding(SYSTEM, Codes.LOCAL_ALL_PRACTITIONER, null);
		}

		public static final Coding remoteAll()
		{
			return new Coding(SYSTEM, Codes.REMOTE_ALL, null);
		}

		public static final boolean isLocalOrganization(Coding coding)
		{
			return isSame(SYSTEM, Codes.LOCAL_ORGANIZATION, coding);
		}

		public static final boolean isLocalOrganizationPractitioner(Coding coding)
		{
			return isSame(SYSTEM, Codes.LOCAL_ORGANIZATION_PRACTITIONER, coding);
		}

		public static final boolean isRemoteOrganization(Coding coding)
		{
			return isSame(SYSTEM, Codes.REMOTE_ORGANIZATION, coding);
		}

		public static final boolean isLocalRole(Coding coding)
		{
			return isSame(SYSTEM, Codes.LOCAL_ROLE, coding);
		}

		public static final boolean isLocalRolePractitioner(Coding coding)
		{
			return isSame(SYSTEM, Codes.LOCAL_ROLE_PRACTITIONER, coding);
		}

		public static final boolean isRemoteRole(Coding coding)
		{
			return isSame(SYSTEM, Codes.REMOTE_ROLE, coding);
		}

		public static final boolean isLocalAll(Coding coding)
		{
			return isSame(SYSTEM, Codes.LOCAL_ALL, coding);
		}

		public static final boolean isLocalAllPractitioner(Coding coding)
		{
			return isSame(SYSTEM, Codes.LOCAL_ALL_PRACTITIONER, coding);
		}

		public static final boolean isRemoteAll(Coding coding)
		{
			return isSame(SYSTEM, Codes.REMOTE_ALL, coding);
		}
	}

	public static final class OrganizationRole
	{
		private OrganizationRole()
		{
		}

		public static final String SYSTEM = "http://dsf.dev/fhir/CodeSystem/organization-role";

		public static Coding withCode(String code)
		{
			return new Coding().setSystem(SYSTEM).setCode(code);
		}

		public static final class Codes
		{
			private Codes()
			{
			}

			public static final String UAC = "UAC";
			public static final String COS = "COS";
			public static final String CRR = "CRR";
			public static final String DIC = "DIC";
			public static final String DMS = "DMS";
			public static final String DTS = "DTS";
			public static final String HRP = "HRP";
			public static final String TTP = "TTP";
			public static final String AMS = "AMS";
			public static final String ASP = "ASP";
			public static final String SPR = "SPR";
			public static final String TSP = "TSP";
			public static final String PPH = "PPH";
			public static final String BIO = "BIO";
		}

		public static final Coding uac()
		{
			return new Coding(SYSTEM, Codes.UAC, "Use-and-Access Committee");
		}

		public static final Coding cos()
		{
			return new Coding(SYSTEM, Codes.COS, "Coordinating Site");
		}

		public static final Coding crr()
		{
			return new Coding(SYSTEM, Codes.CRR, "Central Research Repository");
		}

		public static final Coding dic()
		{
			return new Coding(SYSTEM, Codes.DIC, "Data Integration Center");
		}

		public static final Coding dms()
		{
			return new Coding(SYSTEM, Codes.DMS, "Data Management Site");
		}

		public static final Coding dts()
		{
			return new Coding(SYSTEM, Codes.DTS, "Data Transfer Site");
		}

		public static final Coding hrp()
		{
			return new Coding(SYSTEM, Codes.HRP, "Health Research Platform");
		}

		public static final Coding ttp()
		{
			return new Coding(SYSTEM, Codes.TTP, "Trusted Third Party");
		}

		public static final Coding ams()
		{
			return new Coding(SYSTEM, Codes.AMS, "Allowlist Management Site");
		}

		public static final Coding asp()
		{
			return new Coding(SYSTEM, Codes.ASP, "Analysis Service Provider");
		}

		public static final Coding SPR()
		{
			return new Coding(SYSTEM, Codes.SPR, "Service Provider Registry");
		}

		public static final Coding TSP()
		{
			return new Coding(SYSTEM, Codes.TSP, "Terminology Service Provider");
		}

		public static final Coding PPH()
		{
			return new Coding(SYSTEM, Codes.PPH, "Process Plugin Hub");
		}

		public static final Coding BIO()
		{
			return new Coding(SYSTEM, Codes.BIO, "Biobank");
		}

		public static final boolean isUac(Coding coding)
		{
			return isSame(SYSTEM, Codes.UAC, coding);
		}

		public static final boolean isCos(Coding coding)
		{
			return isSame(SYSTEM, Codes.COS, coding);
		}

		public static final boolean isCrr(Coding coding)
		{
			return isSame(SYSTEM, Codes.CRR, coding);
		}

		public static final boolean isDic(Coding coding)
		{
			return isSame(SYSTEM, Codes.DIC, coding);
		}

		public static final boolean isDms(Coding coding)
		{
			return isSame(SYSTEM, Codes.DMS, coding);
		}

		public static final boolean isDts(Coding coding)
		{
			return isSame(SYSTEM, Codes.DTS, coding);
		}

		public static final boolean isHrp(Coding coding)
		{
			return isSame(SYSTEM, Codes.HRP, coding);
		}

		public static final boolean isTtp(Coding coding)
		{
			return isSame(SYSTEM, Codes.TTP, coding);
		}

		public static final boolean isAms(Coding coding)
		{
			return isSame(SYSTEM, Codes.AMS, coding);
		}

		public static final boolean isAsp(Coding coding)
		{
			return isSame(SYSTEM, Codes.ASP, coding);
		}

		public static final boolean isSpr(Coding coding)
		{
			return isSame(SYSTEM, Codes.SPR, coding);
		}

		public static final boolean isTsp(Coding coding)
		{
			return isSame(SYSTEM, Codes.TSP, coding);
		}

		public static final boolean isPph(Coding coding)
		{
			return isSame(SYSTEM, Codes.PPH, coding);
		}

		public static final boolean isBio(Coding coding)
		{
			return isSame(SYSTEM, Codes.BIO, coding);
		}
	}

	public static final class PractitionerRole
	{
		private PractitionerRole()
		{
		}

		public static final String SYSTEM = "http://dsf.dev/fhir/CodeSystem/practitioner-role";

		public static Coding withCode(String code)
		{
			return new Coding().setSystem(SYSTEM).setCode(code);
		}

		public static final class Codes
		{
			private Codes()
			{
			}

			public static final String UAC_USER = "UAC_USER";
			public static final String COS_USER = "COS_USER";
			public static final String CRR_USER = "CRR_USER";
			public static final String DIC_USER = "DIC_USER";
			public static final String DMS_USER = "DMS_USER";
			public static final String DTS_USER = "DTS_USER";
			public static final String HRP_USER = "HRP_USER";
			public static final String TTP_USER = "TTP_USER";
			public static final String AMS_USER = "AMS_USER";
			public static final String ASP_USER = "ASP_USER";
			public static final String SPR_USER = "SPR_USER";
			public static final String DSF_ADMIN = "DSF_ADMIN";
		}

		public static final Coding uacUser()
		{
			return new Coding(SYSTEM, Codes.UAC_USER, "Use-and-Access Committee Member");
		}

		public static final Coding cosUser()
		{
			return new Coding(SYSTEM, Codes.COS_USER, "Coordinating Site Member");
		}

		public static final Coding crrUser()
		{
			return new Coding(SYSTEM, Codes.CRR_USER, "Central Research Repository Member");
		}

		public static final Coding dicUser()
		{
			return new Coding(SYSTEM, Codes.DIC_USER, "Data Integration Center Member");
		}

		public static final Coding dmsUser()
		{
			return new Coding(SYSTEM, Codes.DMS_USER, "Data Management Site Member");
		}

		public static final Coding dtsUser()
		{
			return new Coding(SYSTEM, Codes.DTS_USER, "Data Transfer Site Member");
		}

		public static final Coding hrpUser()
		{
			return new Coding(SYSTEM, Codes.HRP_USER, "Health Research Platform Member");
		}

		public static final Coding ttpUser()
		{
			return new Coding(SYSTEM, Codes.TTP_USER, "Trusted Third Party Member");
		}

		public static final Coding amsUser()
		{
			return new Coding(SYSTEM, Codes.AMS_USER, "Allowlist Management Site Member");
		}

		public static final Coding aspUser()
		{
			return new Coding(SYSTEM, Codes.ASP_USER, "Analysis Service Provider Member");
		}

		public static final Coding sprUser()
		{
			return new Coding(SYSTEM, Codes.SPR_USER, "Service Provider Registry Member");
		}

		public static final Coding dsfAdmin()
		{
			return new Coding(SYSTEM, Codes.DSF_ADMIN, "DSF Administrator");
		}

		public static final boolean isUacUser(Coding coding)
		{
			return isSame(SYSTEM, Codes.UAC_USER, coding);
		}

		public static final boolean isCosUser(Coding coding)
		{
			return isSame(SYSTEM, Codes.COS_USER, coding);
		}

		public static final boolean isCrrUser(Coding coding)
		{
			return isSame(SYSTEM, Codes.CRR_USER, coding);
		}

		public static final boolean isDicUser(Coding coding)
		{
			return isSame(SYSTEM, Codes.DIC_USER, coding);
		}

		public static final boolean isDmsUser(Coding coding)
		{
			return isSame(SYSTEM, Codes.DMS_USER, coding);
		}

		public static final boolean isDtsUser(Coding coding)
		{
			return isSame(SYSTEM, Codes.DTS_USER, coding);
		}

		public static final boolean isHrpUser(Coding coding)
		{
			return isSame(SYSTEM, Codes.HRP_USER, coding);
		}

		public static final boolean isTtpUser(Coding coding)
		{
			return isSame(SYSTEM, Codes.TTP_USER, coding);
		}

		public static final boolean isAmsUser(Coding coding)
		{
			return isSame(SYSTEM, Codes.AMS_USER, coding);
		}

		public static final boolean isAspUser(Coding coding)
		{
			return isSame(SYSTEM, Codes.ASP_USER, coding);
		}

		public static final boolean isSprUser(Coding coding)
		{
			return isSame(SYSTEM, Codes.SPR_USER, coding);
		}

		public static final boolean isDsfAdmin(Coding coding)
		{
			return isSame(SYSTEM, Codes.DSF_ADMIN, coding);
		}
	}

	public static final class ReadAccessTag
	{
		private ReadAccessTag()
		{
		}

		public static final String SYSTEM = "http://dsf.dev/fhir/CodeSystem/read-access-tag";

		public static Coding withCode(String code)
		{
			return new Coding().setSystem(SYSTEM).setCode(code);
		}

		public static final class Codes
		{
			private Codes()
			{
			}

			public static final String LOCAL = "LOCAL";
			public static final String ORGANIZATION = "ORGANIZATION";
			public static final String ROLE = "ROLE";
			public static final String ALL = "ALL";
		}

		public static final Coding local()
		{
			return new Coding(SYSTEM, Codes.LOCAL, "Read access for local users");
		}

		public static final Coding organization()
		{
			return new Coding(SYSTEM, Codes.ORGANIZATION,
					"Read access for organization specified via extension http://dsf.dev/fhir/StructureDefinition/extension-read-access-organization");
		}

		public static final Coding role()
		{
			return new Coding(SYSTEM, Codes.ROLE,
					"Read access for member organizations with role in consortium (parent organization) specified via extension http://dsf.dev/fhir/StructureDefinition/extension-read-access-consortium-role");
		}

		public static final Coding all()
		{
			return new Coding(SYSTEM, Codes.ALL, "Read access for remote and local users");
		}
	}
}