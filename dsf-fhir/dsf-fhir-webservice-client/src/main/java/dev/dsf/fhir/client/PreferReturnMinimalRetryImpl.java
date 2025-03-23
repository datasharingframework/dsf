package dev.dsf.fhir.client;

import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.prefer.PreferReturnType;
import jakarta.ws.rs.core.MediaType;

class PreferReturnMinimalRetryImpl extends AbstractFhirWebserviceClientJerseyWithRetry implements PreferReturnMinimal
{
	PreferReturnMinimalRetryImpl(FhirWebserviceClientJersey delegate, int nTimes, Duration delay)
	{
		super(delegate, nTimes, delay);
	}

	@Override
	public IdType create(Resource resource)
	{
		return retry(() -> delegate.create(PreferReturnType.MINIMAL, resource).getId());
	}

	@Override
	public IdType createConditionaly(Resource resource, String ifNoneExistCriteria)
	{
		return retry(
				() -> delegate.createConditionaly(PreferReturnType.MINIMAL, resource, ifNoneExistCriteria).getId());
	}

	@Override
	public IdType createBinary(InputStream in, MediaType mediaType, String securityContextReference)
	{
		return retry(
				() -> delegate.createBinary(PreferReturnType.MINIMAL, in, mediaType, securityContextReference).getId());
	}

	@Override
	public IdType update(Resource resource)
	{
		return retry(() -> delegate.update(PreferReturnType.MINIMAL, resource).getId());
	}

	@Override
	public IdType updateConditionaly(Resource resource, Map<String, List<String>> criteria)
	{
		return retry(() -> delegate.updateConditionaly(PreferReturnType.MINIMAL, resource, criteria).getId());
	}

	@Override
	public IdType updateBinary(String id, InputStream in, MediaType mediaType, String securityContextReference)
	{
		return retry(() -> delegate.updateBinary(PreferReturnType.MINIMAL, id, in, mediaType, securityContextReference)
				.getId());
	}

	@Override
	public Bundle postBundle(Bundle bundle)
	{
		return retry(() -> delegate.postBundle(PreferReturnType.MINIMAL, bundle));
	}
}