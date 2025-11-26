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
package dev.dsf.bpe.api;

public final class Constants
{
	public static final String BPMN_MESSAGE_URL = "http://dsf.dev/fhir/CodeSystem/bpmn-message";

	public static final String BPMN_MESSAGE_MESSAGE_NAME = "message-name";
	public static final String BPMN_MESSAGE_BUSINESS_KEY = "business-key";
	public static final String BPMN_MESSAGE_CORRELATION_KEY = "correlation-key";
	public static final String BPMN_MESSAGE_ERROR = "error";

	public static final String TASK_VARIABLE = "dev.dsf.bpe.subscription.TaskHandler.task";

	public static final String CORRELATION_KEY = "correlationKey";
	public static final String ALTERNATIVE_BUSINESS_KEY = "alternativeBusinessKey";

	public static final String QUESTIONNAIRE_RESPONSE_VARIABLE = "dev.dsf.bpe.subscription.QuestionnaireResponseHandler.questionnaireResponse";

	public static final String ITEM_LINK_ID_BUSINESS_KEY = "business-key";
	public static final String ITEM_LINK_ID_USER_TASK_ID = "user-task-id";

	public static final String TASK_IDENTIFIER_SID = "http://dsf.dev/sid/task-identifier";

	private Constants()
	{
	}
}
