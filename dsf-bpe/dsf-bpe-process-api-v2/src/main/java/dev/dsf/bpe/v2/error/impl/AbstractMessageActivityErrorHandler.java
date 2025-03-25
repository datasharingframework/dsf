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
