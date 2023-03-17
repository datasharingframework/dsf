package dev.dsf.bpe;

public interface ConstantsBase
{
	String BPMN_EXECUTION_VARIABLE_INSTANTIATES_URI = "instantiatesUri";
	String BPMN_EXECUTION_VARIABLE_MESSAGE_NAME = "messageName";
	String BPMN_EXECUTION_VARIABLE_PROFILE = "profile";
	String BPMN_EXECUTION_VARIABLE_TARGET = "target";
	String BPMN_EXECUTION_VARIABLE_TARGETS = "targets";
	String BPMN_EXECUTION_VARIABLE_TASK = "task";
	String BPMN_EXECUTION_VARIABLE_LEADING_TASK = "leadingTask";
	String BPMN_EXECUTION_VARIABLE_BUNDLE_ID = "bundleId";
	String BPMN_EXECUTION_VARIABLE_QUERY_PARAMETERS = "queryParameters";
	String BPMN_EXECUTION_VARIABLE_ALTERNATIVE_BUSINESS_KEY = "alternativeBusinessKey";
	String BPMN_EXECUTION_VARIABLE_QUESTIONNAIRE_RESPONSE_ID = "questionnaireResponseId";
	String BPMN_EXECUTION_VARIABLE_QUESTIONNAIRE_RESPONSE_COMPLETED = "questionnaireResponseCompleted";

	/**
	 * Used to distinguish if I am at the moment in a process called by another process by a CallActivity or not
	 */
	String BPMN_EXECUTION_VARIABLE_IN_CALLED_PROCESS = "inCalledProcess";

	String PROCESS_DSF_URI_BASE = "http://dsf.dev/bpe/Process/";

	String CODESYSTEM_DSF_BPMN = "http://dsf.dev/fhir/CodeSystem/bpmn-message";
	String CODESYSTEM_DSF_BPMN_VALUE_MESSAGE_NAME = "message-name";
	String CODESYSTEM_DSF_BPMN_VALUE_BUSINESS_KEY = "business-key";
	String CODESYSTEM_DSF_BPMN_VALUE_CORRELATION_KEY = "correlation-key";
	String CODESYSTEM_DSF_BPMN_VALUE_ERROR = "error";

	String CODESYSTEM_DSF_BPMN_USER_TASK = "http://dsf.dev/fhir/CodeSystem/bpmn-user-task";
	String CODESYSTEM_DSF_BPMN_USER_TASK_VALUE_BUSINESS_KEY = "business-key";
	String CODESYSTEM_DSF_BPMN_USER_TASK_VALUE_USER_TASK_ID = "user-task-id";

	String CODESYSTEM_DSF_ORGANIZATION_ROLE = "http://dsf.dev/fhir/CodeSystem/organization-role";
	String CODESYSTEM_DSF_ORGANIZATION_ROLE_VALUE_TTP = "TTP";
	String CODESYSTEM_DSF_ORGANIZATION_ROLE_VALUE_MEDIC = "MeDIC";
	String CODESYSTEM_DSF_ORGANIZATION_ROLE_VALUE_DTS = "DTS";
	String CODESYSTEM_DSF_ORGANIZATION_ROLE_VALUE_COS = "COS";
	String CODESYSTEM_DSF_ORGANIZATION_ROLE_VALUE_CRR = "CRR";
	String CODESYSTEM_DSF_ORGANIZATION_ROLE_VALUE_HRP = "HRP";

	String NAMINGSYSTEM_DSF_ORGANIZATION_IDENTIFIER = "http://dsf.dev/sid/organization-identifier";
	String NAMINGSYSTEM_DSF_ENDPOINT_IDENTIFIER = "http://dsf.dev/sid/endpoint-identifier";

	String NAMINGSYSTEM_DSF_ORGANIZATION_IDENTIFIER_HIGHMED_CONSORTIUM = "highmed.org";
	String NAMINGSYSTEM_DSF_ORGANIZATION_IDENTIFIER_NUM_CODEX_CONSORTIUM = "netzwerk-universitaetsmedizin.de";
	String NAMINGSYSTEM_DSF_ORGANIZATION_IDENTIFIER_MEDICAL_INFORMATICS_INITIATIVE_CONSORTIUM = "medizininformatik-initiative.de";
}
