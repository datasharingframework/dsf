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
package dev.dsf.bpe.v2.client.dsf;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Resource;

import jakarta.ws.rs.core.MediaType;

class PreferReturnOutcomeWithRetryImpl implements PreferReturnOutcomeWithRetry
{
	private final DsfClientJersey delegate;

	PreferReturnOutcomeWithRetryImpl(DsfClientJersey delegate)
	{
		this.delegate = delegate;
	}

	@Override
	public OperationOutcome create(Resource resource)
	{
		return delegate.create(PreferReturnType.OPERATION_OUTCOME, resource).getOperationOutcome();
	}

	@Override
	public OperationOutcome createConditionaly(Resource resource, String ifNoneExistCriteria)
	{
		return delegate.createConditionaly(PreferReturnType.OPERATION_OUTCOME, resource, ifNoneExistCriteria)
				.getOperationOutcome();
	}

	@Override
	public OperationOutcome createBinary(InputStream in, MediaType mediaType, String securityContextReference)
	{
		return delegate.createBinary(PreferReturnType.OPERATION_OUTCOME, in, mediaType, securityContextReference)
				.getOperationOutcome();
	}

	@Override
	public OperationOutcome update(Resource resource)
	{
		return delegate.update(PreferReturnType.OPERATION_OUTCOME, resource).getOperationOutcome();
	}

	@Override
	public OperationOutcome updateConditionaly(Resource resource, Map<String, List<String>> criteria)
	{
		return delegate.updateConditionaly(PreferReturnType.OPERATION_OUTCOME, resource, criteria)
				.getOperationOutcome();
	}

	@Override
	public OperationOutcome updateBinary(String id, InputStream in, MediaType mediaType,
			String securityContextReference)
	{
		return delegate.updateBinary(PreferReturnType.OPERATION_OUTCOME, id, in, mediaType, securityContextReference)
				.getOperationOutcome();
	}

	@Override
	public Bundle postBundle(Bundle bundle)
	{
		return delegate.postBundle(PreferReturnType.OPERATION_OUTCOME, bundle);
	}

	@Override
	public PreferReturnOutcome withRetry(int nTimes, DelayStrategy delayStrategy)
	{
		if (nTimes < 0)
			throw new IllegalArgumentException("nTimes < 0");
		if (delayStrategy == null)
			throw new IllegalArgumentException("delayStrategy null");

		return new PreferReturnOutcomeRetryImpl(delegate, nTimes, delayStrategy);
	}

	@Override
	public PreferReturnOutcome withRetryForever(DelayStrategy delayStrategy)
	{
		if (delayStrategy == null)
			throw new IllegalArgumentException("delayStrategy null");

		return new PreferReturnOutcomeRetryImpl(delegate, RETRY_FOREVER, delayStrategy);
	}
}