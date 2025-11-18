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
import java.util.concurrent.CompletableFuture;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Resource;

import jakarta.ws.rs.core.MediaType;

class PreferReturnMinimalWithRetryImpl implements PreferReturnMinimalWithRetry, AsyncPreferReturnMinimal
{
	private final DsfClientJersey delegate;

	PreferReturnMinimalWithRetryImpl(DsfClientJersey delegate)
	{
		this.delegate = delegate;
	}

	@Override
	public IdType create(Resource resource)
	{
		return delegate.create(PreferReturnType.MINIMAL, Resource.class, resource).id();
	}

	@Override
	public IdType createConditionaly(Resource resource, String ifNoneExistCriteria)
	{
		return delegate.createConditionaly(PreferReturnType.MINIMAL, Resource.class, resource, ifNoneExistCriteria)
				.id();
	}

	@Override
	public IdType createBinary(InputStream in, MediaType mediaType, String securityContextReference)
	{
		return delegate.createBinary(PreferReturnType.MINIMAL, in, mediaType, securityContextReference).id();
	}

	@Override
	public IdType update(Resource resource)
	{
		return delegate.update(PreferReturnType.MINIMAL, Resource.class, resource).id();
	}

	@Override
	public IdType updateConditionaly(Resource resource, Map<String, List<String>> criteria)
	{
		return delegate.updateConditionaly(PreferReturnType.MINIMAL, Resource.class, resource, criteria).id();
	}

	@Override
	public IdType updateBinary(String id, InputStream in, MediaType mediaType, String securityContextReference)
	{
		return delegate.updateBinary(PreferReturnType.MINIMAL, id, in, mediaType, securityContextReference).id();
	}

	@Override
	public Bundle postBundle(Bundle bundle)
	{
		return delegate.postBundle(PreferReturnType.MINIMAL, bundle);
	}

	@Override
	public PreferReturnMinimal withRetry(int nTimes, DelayStrategy delayStrategy)
	{
		if (nTimes < 0)
			throw new IllegalArgumentException("nTimes < 0");
		if (delayStrategy == null)
			throw new IllegalArgumentException("delayStrategy null");

		return new PreferReturnMinimalRetryImpl(delegate, nTimes, delayStrategy);
	}

	@Override
	public PreferReturnMinimal withRetryForever(DelayStrategy delayStrategy)
	{
		if (delayStrategy == null)
			throw new IllegalArgumentException("delayStrategy null");

		return new PreferReturnMinimalRetryImpl(delegate, RETRY_FOREVER, delayStrategy);
	}

	@Override
	public IdType operation(String operationName, Parameters parameters)
	{
		return delegate.operation(PreferReturnType.MINIMAL, operationName, parameters, Resource.class).id();
	}

	@Override
	public <T extends Resource> IdType operation(Class<T> resourceType, String operationName, Parameters parameters)
	{
		return delegate.operation(PreferReturnType.MINIMAL, resourceType, operationName, parameters, Resource.class)
				.id();
	}

	@Override
	public <T extends Resource> IdType operation(Class<T> resourceType, String id, String operationName,
			Parameters parameters)
	{
		return delegate.operation(PreferReturnType.MINIMAL, resourceType, id, operationName, parameters, Resource.class)
				.id();
	}

	@Override
	public <T extends Resource> IdType operation(Class<T> resourceType, String id, String version, String operationName,
			Parameters parameters)
	{
		return delegate.operation(PreferReturnType.MINIMAL, resourceType, id, version, operationName, parameters,
				Resource.class).id();
	}

	@Override
	public CompletableFuture<IdType> operationAsync(DelayStrategy delayStrategy, String operationName,
			Parameters parameters)
	{
		return delegate
				.operationAsync(PreferReturnType.MINIMAL, delayStrategy, operationName, parameters, Resource.class)
				.thenApply(PreferReturn::id);
	}

	@Override
	public <T extends Resource> CompletableFuture<IdType> operationAsync(DelayStrategy delayStrategy,
			Class<T> resourceType, String operationName, Parameters parameters)
	{
		return delegate.operationAsync(PreferReturnType.MINIMAL, delayStrategy, resourceType, operationName, parameters,
				Resource.class).thenApply(PreferReturn::id);
	}

	@Override
	public <T extends Resource> CompletableFuture<IdType> operationAsync(DelayStrategy delayStrategy,
			Class<T> resourceType, String id, String operationName, Parameters parameters)
	{
		return delegate.operationAsync(PreferReturnType.MINIMAL, delayStrategy, resourceType, id, operationName,
				parameters, Resource.class).thenApply(PreferReturn::id);
	}

	@Override
	public <T extends Resource> CompletableFuture<IdType> operationAsync(DelayStrategy delayStrategy,
			Class<T> resourceType, String id, String version, String operationName, Parameters parameters)
	{
		return delegate.operationAsync(PreferReturnType.MINIMAL, delayStrategy, resourceType, id, version,
				operationName, parameters, Resource.class).thenApply(PreferReturn::id);
	}
}