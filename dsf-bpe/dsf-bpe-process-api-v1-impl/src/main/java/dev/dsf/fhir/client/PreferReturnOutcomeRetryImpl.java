package dev.dsf.fhir.client;

import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.prefer.PreferReturnType;
import jakarta.ws.rs.core.MediaType;

class PreferReturnOutcomeRetryImpl extends AbstractFhirWebserviceClientJerseyWithRetry implements PreferReturnOutcome
{
	PreferReturnOutcomeRetryImpl(FhirWebserviceClientJersey delegate, int nTimes, Duration delay)
	{
		super(delegate, nTimes, delay);
	}

	@Override
	public OperationOutcome create(Resource resource)
	{
		return retry(() -> delegate.create(PreferReturnType.OPERATION_OUTCOME, resource).getOperationOutcome());
	}

	@Override
	public OperationOutcome createConditionaly(Resource resource, String ifNoneExistCriteria)
	{
		return retry(
				() -> delegate.createConditionaly(PreferReturnType.OPERATION_OUTCOME, resource, ifNoneExistCriteria)
						.getOperationOutcome());
	}

	@Override
	public OperationOutcome createBinary(InputStream in, MediaType mediaType, String securityContextReference)
	{
		return retry(
				() -> delegate.createBinary(PreferReturnType.OPERATION_OUTCOME, in, mediaType, securityContextReference)
						.getOperationOutcome());
	}

	@Override
	public OperationOutcome update(Resource resource)
	{
		return retry(() -> delegate.update(PreferReturnType.OPERATION_OUTCOME, resource).getOperationOutcome());
	}

	@Override
	public OperationOutcome updateConditionaly(Resource resource, Map<String, List<String>> criteria)
	{
		return retry(() -> delegate.updateConditionaly(PreferReturnType.OPERATION_OUTCOME, resource, criteria)
				.getOperationOutcome());
	}

	@Override
	public OperationOutcome updateBinary(String id, InputStream in, MediaType mediaType,
			String securityContextReference)
	{
		return retry(() -> delegate
				.updateBinary(PreferReturnType.OPERATION_OUTCOME, id, in, mediaType, securityContextReference)
				.getOperationOutcome());
	}

	@Override
	public Bundle postBundle(Bundle bundle)
	{
		return retry(() -> delegate.postBundle(PreferReturnType.OPERATION_OUTCOME, bundle));
	}
}