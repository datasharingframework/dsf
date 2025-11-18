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

import java.util.concurrent.CompletableFuture;

import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Resource;

public interface AsyncPreferReturnMinimal
{
	/**
	 * Uses {@link DelayStrategy#TRUNCATED_EXPONENTIAL_BACKOFF} unless the server sends <i>Retry-After</i> headers.<br>
	 * <br>
	 * Send "Prefer: respond-async, return=minimal" header and handles async response
	 *
	 * @param operationName
	 *            not <code>null</code>, <code>$</code> will be prepended if not present
	 * @param parameters
	 *            may be <code>null</code>
	 * @return location value from async operation result
	 */
	default CompletableFuture<IdType> operationAsync(String operationName, Parameters parameters)
	{
		return operationAsync(DelayStrategy.TRUNCATED_EXPONENTIAL_BACKOFF, operationName, parameters);
	}

	/**
	 * Send "Prefer: respond-async, return=minimal" header and handles async response
	 *
	 * @param delayStrategy
	 *            not <code>null</code>, will be ignored if the server sends <i>Retry-After</i> headers
	 * @param operationName
	 *            not <code>null</code>, <code>$</code> will be prepended if not present
	 * @param parameters
	 *            may be <code>null</code>
	 * @return location value from async operation result
	 */
	CompletableFuture<IdType> operationAsync(DelayStrategy delayStrategy, String operationName, Parameters parameters);

	/**
	 * Uses {@link DelayStrategy#TRUNCATED_EXPONENTIAL_BACKOFF} unless the server sends <i>Retry-After</i> headers.<br>
	 * <br>
	 * Send "Prefer: respond-async, return=minimal" header and handles async response
	 *
	 * @param <T>
	 *            request path resource type
	 * @param resourceType
	 *            not <code>null</code>
	 * @param operationName
	 *            not <code>null</code>, <code>$</code> will be prepended if not present
	 * @param parameters
	 *            may be <code>null</code>
	 * @return location value from async operation result
	 */
	default <T extends Resource> CompletableFuture<IdType> operationAsync(Class<T> resourceType, String operationName,
			Parameters parameters)
	{
		return operationAsync(DelayStrategy.TRUNCATED_EXPONENTIAL_BACKOFF, resourceType, operationName, parameters);
	}

	/**
	 * Send "Prefer: respond-async, return=minimal" header and handles async response
	 *
	 * @param <T>
	 *            request path resource type
	 * @param delayStrategy
	 *            not <code>null</code>, will be ignored if the server sends <i>Retry-After</i> headers
	 * @param resourceType
	 *            not <code>null</code>
	 * @param operationName
	 *            not <code>null</code>, <code>$</code> will be prepended if not present
	 * @param parameters
	 *            may be <code>null</code>
	 * @return location value from async operation result
	 */
	<T extends Resource> CompletableFuture<IdType> operationAsync(DelayStrategy delayStrategy, Class<T> resourceType,
			String operationName, Parameters parameters);

	/**
	 * Uses {@link DelayStrategy#TRUNCATED_EXPONENTIAL_BACKOFF} unless the server sends <i>Retry-After</i> headers.<br>
	 * <br>
	 * Send "Prefer: respond-async, return=minimal" header and handles async response
	 *
	 * @param <T>
	 *            request path resource type
	 * @param resourceType
	 *            not <code>null</code>
	 * @param id
	 *            not <code>null</code>
	 * @param operationName
	 *            not <code>null</code>, <code>$</code> will be prepended if not present
	 * @param parameters
	 *            may be <code>null</code>
	 * @return location value from async operation result
	 */
	default <T extends Resource> CompletableFuture<IdType> operationAsync(Class<T> resourceType, String id,
			String operationName, Parameters parameters)
	{
		return operationAsync(DelayStrategy.TRUNCATED_EXPONENTIAL_BACKOFF, resourceType, id, operationName, parameters);
	}

	/**
	 * Send "Prefer: respond-async, return=minimal" header and handles async response
	 *
	 * @param <T>
	 *            request path resource type
	 * @param delayStrategy
	 *            not <code>null</code>, will be ignored if the server sends <i>Retry-After</i> headers
	 * @param resourceType
	 *            not <code>null</code>
	 * @param id
	 *            not <code>null</code>
	 * @param operationName
	 *            not <code>null</code>, <code>$</code> will be prepended if not present
	 * @param parameters
	 *            may be <code>null</code>
	 * @return location value from async operation result
	 */
	<T extends Resource> CompletableFuture<IdType> operationAsync(DelayStrategy delayStrategy, Class<T> resourceType,
			String id, String operationName, Parameters parameters);

	/**
	 * Uses {@link DelayStrategy#TRUNCATED_EXPONENTIAL_BACKOFF} unless the server sends <i>Retry-After</i> headers.<br>
	 * <br>
	 * Send "Prefer: respond-async, return=minimal" header and handles async response
	 *
	 * @param <T>
	 *            request path resource type
	 * @param resourceType
	 *            not <code>null</code>
	 * @param id
	 *            not <code>null</code>
	 * @param version
	 *            not <code>null</code>
	 * @param operationName
	 *            not <code>null</code>, <code>$</code> will be prepended if not present
	 * @param parameters
	 *            may be <code>null</code>
	 * @return location value from async operation result
	 */
	default <T extends Resource> CompletableFuture<IdType> operationAsync(Class<T> resourceType, String id,
			String version, String operationName, Parameters parameters)
	{
		return operationAsync(DelayStrategy.TRUNCATED_EXPONENTIAL_BACKOFF, resourceType, id, version, operationName,
				parameters);
	}

	/**
	 * Send "Prefer: respond-async, return=minimal" header and handles async response
	 *
	 * @param <T>
	 *            request path resource type
	 * @param delayStrategy
	 *            not <code>null</code>, will be ignored if the server sends <i>Retry-After</i> headers
	 * @param resourceType
	 *            not <code>null</code>
	 * @param id
	 *            not <code>null</code>
	 * @param version
	 *            not <code>null</code>
	 * @param operationName
	 *            not <code>null</code>, <code>$</code> will be prepended if not present
	 * @param parameters
	 *            may be <code>null</code>
	 * @return location value from async operation result
	 */
	<T extends Resource> CompletableFuture<IdType> operationAsync(DelayStrategy delayStrategy, Class<T> resourceType,
			String id, String version, String operationName, Parameters parameters);
}