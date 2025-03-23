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

class PreferReturnMinimalWithRetryImpl implements PreferReturnMinimalWithRetry
{
	private final FhirWebserviceClientJersey delegate;

	PreferReturnMinimalWithRetryImpl(FhirWebserviceClientJersey delegate)
	{
		this.delegate = delegate;
	}

	@Override
	public IdType create(Resource resource)
	{
		return delegate.create(PreferReturnType.MINIMAL, resource).getId();
	}

	@Override
	public IdType createConditionaly(Resource resource, String ifNoneExistCriteria)
	{
		return delegate.createConditionaly(PreferReturnType.MINIMAL, resource, ifNoneExistCriteria).getId();
	}

	@Override
	public IdType createBinary(InputStream in, MediaType mediaType, String securityContextReference)
	{
		return delegate.createBinary(PreferReturnType.MINIMAL, in, mediaType, securityContextReference).getId();
	}

	@Override
	public IdType update(Resource resource)
	{
		return delegate.update(PreferReturnType.MINIMAL, resource).getId();
	}

	@Override
	public IdType updateConditionaly(Resource resource, Map<String, List<String>> criteria)
	{
		return delegate.updateConditionaly(PreferReturnType.MINIMAL, resource, criteria).getId();
	}

	@Override
	public IdType updateBinary(String id, InputStream in, MediaType mediaType, String securityContextReference)
	{
		return delegate.updateBinary(PreferReturnType.MINIMAL, id, in, mediaType, securityContextReference).getId();
	}

	@Override
	public Bundle postBundle(Bundle bundle)
	{
		return delegate.postBundle(PreferReturnType.MINIMAL, bundle);
	}

	@Override
	public PreferReturnMinimal withRetry(int nTimes, Duration delay)
	{
		if (nTimes < 0)
			throw new IllegalArgumentException("nTimes < 0");
		if (delay == null || delay.isNegative())
			throw new IllegalArgumentException("delay null or negative");

		return new PreferReturnMinimalRetryImpl(delegate, nTimes, delay);
	}

	@Override
	public PreferReturnMinimal withRetryForever(Duration delay)
	{
		if (delay == null || delay.isNegative())
			throw new IllegalArgumentException("delay null or negative");

		return new PreferReturnMinimalRetryImpl(delegate, RETRY_FOREVER, delay);
	}
}