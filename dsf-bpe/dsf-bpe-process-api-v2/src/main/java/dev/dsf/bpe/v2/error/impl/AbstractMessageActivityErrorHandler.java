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
package dev.dsf.bpe.v2.error.impl;

import java.util.function.Function;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.values.SendTaskValues;
import dev.dsf.bpe.v2.variables.Target;
import dev.dsf.bpe.v2.variables.Variables;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.StatusType;

public class AbstractMessageActivityErrorHandler extends AbstractErrorHandler
{
	protected String createErrorMessage(ProcessPluginApi api, Variables variables, Exception exception,
			SendTaskValues sendTaskValues, Function<Exception, String> getExceptionMessage)
	{
		Target target = variables.getTarget();

		return "Task " + sendTaskValues.instantiatesCanonical() + " send failed [recipient: "
				+ target.getOrganizationIdentifierValue() + ", endpoint: " + target.getEndpointIdentifierValue()
				+ ", businessKey: " + variables.getBusinessKey()
				+ (target.getCorrelationKey() == null ? "" : ", correlationKey: " + target.getCorrelationKey())
				+ ", message: " + sendTaskValues.messageName() + ", error: " + exception.getClass().getName() + " - "
				+ getExceptionMessage.apply(exception) + "]";
	}

	protected String getExceptionMessage(Exception exception)
	{
		String exceptionMessage = exception.getMessage();
		if (exception instanceof WebApplicationException w
				&& (exception.getMessage() == null || exception.getMessage().isBlank()))
		{
			StatusType statusInfo = w.getResponse().getStatusInfo();
			exceptionMessage = statusInfo.getStatusCode() + " " + statusInfo.getReasonPhrase();
		}

		return exceptionMessage;
	}
}
