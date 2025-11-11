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

import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StructureDefinition;

import jakarta.ws.rs.core.MediaType;

class BasicDsfClientWithRetryImpl extends AbstractDsfClientJerseyWithRetry implements BasicDsfClient
{
	BasicDsfClientWithRetryImpl(DsfClientJersey delegate, int nTimes, DelayStrategy delayStrategy)
	{
		super(delegate, nTimes, delayStrategy);
	}

	@Override
	public <R extends Resource> R updateConditionaly(R resource, Map<String, List<String>> criteria)
	{
		return retry(() -> delegate.updateConditionaly(resource, criteria));
	}

	@Override
	public Binary updateBinary(String id, InputStream in, MediaType mediaType, String securityContextReference)
	{
		return retry(() -> delegate.updateBinary(id, in, mediaType, securityContextReference));
	}

	@Override
	public <R extends Resource> R update(R resource)
	{
		return retry(() -> delegate.update(resource));
	}

	@Override
	public Bundle postBundle(Bundle bundle)
	{
		return retry(() -> delegate.postBundle(bundle));
	}

	@Override
	public <R extends Resource> R createConditionaly(R resource, String ifNoneExistCriteria)
	{
		return retry(() -> delegate.createConditionaly(resource, ifNoneExistCriteria));
	}

	@Override
	public Binary createBinary(InputStream in, MediaType mediaType, String securityContextReference)
	{
		return retry(() -> delegate.createBinary(in, mediaType, securityContextReference));
	}

	@Override
	public <R extends Resource> R create(R resource)
	{
		return retry(() -> delegate.create(resource));
	}

	@Override
	public Bundle searchWithStrictHandling(Class<? extends Resource> resourceType, Map<String, List<String>> parameters)
	{
		return retry(() -> delegate.searchWithStrictHandling(resourceType, parameters));
	}

	@Override
	public Bundle search(Class<? extends Resource> resourceType, Map<String, List<String>> parameters)
	{
		return retry(() -> delegate.search(resourceType, parameters));
	}

	@Override
	public CompletableFuture<Bundle> searchAsync(DelayStrategy delayStrategy, Class<? extends Resource> resourceType,
			Map<String, List<String>> parameters)
	{
		return retry(() -> delegate.searchAsync(delayStrategy, resourceType, parameters));
	}

	@Override
	public CompletableFuture<Bundle> searchAsync(DelayStrategy delayStrategy, String url)
	{
		return retry(() -> delegate.searchAsync(delayStrategy, url));
	}

	@Override
	public CompletableFuture<Bundle> searchAsyncWithStrictHandling(DelayStrategy delayStrategy,
			Class<? extends Resource> resourceType, Map<String, List<String>> parameters)
	{
		return retry(() -> delegate.searchAsyncWithStrictHandling(delayStrategy, resourceType, parameters));
	}

	@Override
	public CompletableFuture<Bundle> searchAsyncWithStrictHandling(DelayStrategy delayStrategy, String url)
	{
		return retry(() -> delegate.searchAsyncWithStrictHandling(delayStrategy, url));
	}

	@Override
	public BinaryInputStream readBinary(String id, String version, MediaType mediaType)
	{
		return retry(() -> delegate.readBinary(id, version, mediaType));
	}

	@Override
	public BinaryInputStream readBinary(String id, String version, MediaType mediaType, Long rangeStart,
			Long rangeEndInclusive, Map<String, String> additionalHeaders)
	{
		return retry(
				() -> delegate.readBinary(id, version, mediaType, rangeStart, rangeEndInclusive, additionalHeaders));
	}

	@Override
	public BinaryInputStream readBinary(String id, MediaType mediaType)
	{
		return retry(() -> delegate.readBinary(id, mediaType));
	}

	@Override
	public BinaryInputStream readBinary(String id, MediaType mediaType, Long rangeStart, Long rangeEndInclusive,
			Map<String, String> additionalHeaders)
	{
		return retry(() -> delegate.readBinary(id, mediaType, rangeStart, rangeEndInclusive, additionalHeaders));
	}

	@Override
	public <R extends Resource> R read(Class<R> resourceType, String id, String version)
	{
		return retry(() -> delegate.read(resourceType, id, version));
	}

	@Override
	public Resource read(String resourceTypeName, String id, String version)
	{
		return retry(() -> delegate.read(resourceTypeName, id, version));
	}

	@Override
	public <R extends Resource> R read(Class<R> resourceType, String id)
	{
		return retry(() -> delegate.read(resourceType, id));
	}

	@Override
	public <R extends Resource> R read(R oldValue)
	{
		return retry(() -> delegate.read(oldValue));
	}

	@Override
	public Resource read(String resourceTypeName, String id)
	{
		return retry(() -> delegate.read(resourceTypeName, id));
	}

	@Override
	public CapabilityStatement getConformance()
	{
		return retry(() -> delegate.getConformance());
	}

	@Override
	public StructureDefinition generateSnapshot(StructureDefinition differential)
	{
		return retry(() -> delegate.generateSnapshot(differential));
	}

	@Override
	public StructureDefinition generateSnapshot(String url)
	{
		return retry(() -> delegate.generateSnapshot(url));
	}

	@Override
	public boolean exists(IdType resourceTypeIdVersion)
	{
		return retry(() -> delegate.exists(resourceTypeIdVersion));
	}

	@Override
	public <R extends Resource> boolean exists(Class<R> resourceType, String id, String version)
	{
		return retry(() -> delegate.exists(resourceType, id, version));
	}

	@Override
	public <R extends Resource> boolean exists(Class<R> resourceType, String id)
	{
		return retry(() -> delegate.exists(resourceType, id));
	}

	@Override
	public void deletePermanently(Class<? extends Resource> resourceClass, String id)
	{
		retry(() ->
		{
			delegate.deletePermanently(resourceClass, id);
			return null;
		});
	}

	@Override
	public void deleteConditionaly(Class<? extends Resource> resourceClass, Map<String, List<String>> criteria)
	{
		retry(() ->
		{
			delegate.deleteConditionaly(resourceClass, criteria);
			return null;
		});
	}

	@Override
	public void delete(Class<? extends Resource> resourceClass, String id)
	{
		retry(() ->
		{
			delegate.delete(resourceClass, id);
			return null;
		});
	}

	@Override
	public Bundle history(Class<? extends Resource> resourceType, String id, int page, int count)
	{
		return retry(() -> delegate.history(resourceType, id, page, count));
	}
}