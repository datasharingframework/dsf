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

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Resource;

public interface AsyncDsfClient
{
	/**
	 * Uses {@link DelayStrategy#EXPONENTIAL_BACKOFF}.<br>
	 * <br>
	 * Send "Prefer: respond-async" header and handles async response
	 *
	 * @param resourceType
	 *            not <code>null</code>
	 * @param parameters
	 *            may be <code>null</code>
	 * @return async search result
	 */
	default CompletableFuture<Bundle> searchAsync(Class<? extends Resource> resourceType,
			Map<String, List<String>> parameters)
	{
		return searchAsync(DelayStrategy.EXPONENTIAL_BACKOFF, resourceType, parameters);
	}

	/**
	 * Send "Prefer: respond-async" header and handles async response
	 *
	 * @param delayStrategy
	 *            not <code>null</code>
	 * @param resourceType
	 *            not <code>null</code>
	 * @param parameters
	 *            may be <code>null</code>
	 * @return async search result
	 */
	CompletableFuture<Bundle> searchAsync(DelayStrategy delayStrategy, Class<? extends Resource> resourceType,
			Map<String, List<String>> parameters);

	/**
	 * Uses {@link DelayStrategy#EXPONENTIAL_BACKOFF} unless the server sends <i>Retry-After</i> headers.<br>
	 * <br>
	 * Send "Prefer: respond-async" header and handles async response
	 *
	 * @param url
	 *            not <code>null</code>, not empty, expected to contain path with a valid FHIR resource name and
	 *            optional query parameters
	 * @return async search result
	 */
	default CompletableFuture<Bundle> searchAsync(String url)
	{
		return searchAsync(DelayStrategy.EXPONENTIAL_BACKOFF, url);
	}

	/**
	 * Send "Prefer: respond-async" header and handles async response
	 *
	 * @param delayStrategy
	 *            not <code>null</code>, will be ignored if the server sends <i>Retry-After</i> headers
	 * @param url
	 *            not <code>null</code>, not empty, expected to contain path with a valid FHIR resource name and
	 *            optional query parameters
	 * @return async search result
	 */
	CompletableFuture<Bundle> searchAsync(DelayStrategy delayStrategy, String url);

	/**
	 * Uses {@link DelayStrategy#EXPONENTIAL_BACKOFF} unless the server sends <i>Retry-After</i> headers.<br>
	 * <br>
	 * Send "Prefer: respond-async, handling=strict" header and handles async response
	 *
	 * @param resourceType
	 *            not <code>null</code>
	 * @param parameters
	 *            may be <code>null</code>
	 * @return async search result
	 */
	default CompletableFuture<Bundle> searchAsyncWithStrictHandling(Class<? extends Resource> resourceType,
			Map<String, List<String>> parameters)
	{
		return searchAsyncWithStrictHandling(DelayStrategy.EXPONENTIAL_BACKOFF, resourceType, parameters);
	}

	/**
	 * Send "Prefer: respond-async, handling=strict" header and handles async response
	 *
	 * @param delayStrategy
	 *            not <code>null</code>, will be ignored if the server sends <i>Retry-After</i> headers
	 * @param resourceType
	 *            not <code>null</code>
	 * @param parameters
	 *            may be <code>null</code>
	 * @return async search result
	 */
	CompletableFuture<Bundle> searchAsyncWithStrictHandling(DelayStrategy delayStrategy,
			Class<? extends Resource> resourceType, Map<String, List<String>> parameters);


	/**
	 * Uses {@link DelayStrategy#EXPONENTIAL_BACKOFF} unless the server sends <i>Retry-After</i> headers.<br>
	 * <br>
	 * Send "Prefer: respond-async, handling=strict" header and handles async response
	 *
	 * @param url
	 *            not <code>null</code>, not empty, expected to contain path with a valid FHIR resource name and
	 *            optional query parameters
	 * @return async search result
	 */
	default CompletableFuture<Bundle> searchAsyncWithStrictHandling(String url)
	{
		return searchAsyncWithStrictHandling(DelayStrategy.EXPONENTIAL_BACKOFF, url);
	}

	/**
	 * Send "Prefer: respond-async, handling=strict" header and handles async response
	 *
	 * @param delayStrategy
	 *            not <code>null</code>, will be ignored if the server sends <i>Retry-After</i> headers
	 * @param url
	 *            not <code>null</code>, not empty, expected to contain path with a valid FHIR resource name and
	 *            optional query parameters
	 * @return async search result
	 */
	CompletableFuture<Bundle> searchAsyncWithStrictHandling(DelayStrategy delayStrategy, String url);

	/**
	 * Uses {@link DelayStrategy#EXPONENTIAL_BACKOFF} unless the server sends <i>Retry-After</i> headers.<br>
	 * <br>
	 * Send "Prefer: respond-async, return=representation" header and handles async response
	 *
	 * @param <R>
	 *            return resource type
	 * @param operationName
	 *            not <code>null</code>, <code>$</code> will be prepended if not present
	 * @param parameters
	 *            may be <code>null</code>
	 * @param returnType
	 *            not <code>null</code>
	 * @return async operation result
	 */
	default <R extends Resource> CompletableFuture<R> operationAsync(String operationName, Parameters parameters,
			Class<R> returnType)
	{
		return operationAsync(DelayStrategy.EXPONENTIAL_BACKOFF, operationName, parameters, returnType);
	}

	/**
	 * Send "Prefer: respond-async, return=representation" header and handles async response
	 *
	 * @param <R>
	 *            return resource type
	 * @param delayStrategy
	 *            not <code>null</code>, will be ignored if the server sends <i>Retry-After</i> headers
	 * @param operationName
	 *            not <code>null</code>, <code>$</code> will be prepended if not present
	 * @param parameters
	 *            may be <code>null</code>
	 * @param returnType
	 *            not <code>null</code>
	 * @return async operation result
	 */
	<R extends Resource> CompletableFuture<R> operationAsync(DelayStrategy delayStrategy, String operationName,
			Parameters parameters, Class<R> returnType);

	/**
	 * Uses {@link DelayStrategy#EXPONENTIAL_BACKOFF} unless the server sends <i>Retry-After</i> headers.<br>
	 * <br>
	 * Send "Prefer: respond-async, return=representation" header and handles async response
	 *
	 * @param <R>
	 *            return resource type
	 * @param <T>
	 *            request path resource type
	 * @param resourceType
	 *            not <code>null</code>
	 * @param operationName
	 *            not <code>null</code>, <code>$</code> will be prepended if not present
	 * @param parameters
	 *            may be <code>null</code>
	 * @param returnType
	 *            not <code>null</code>
	 * @return async operation result
	 */
	default <R extends Resource, T extends Resource> CompletableFuture<R> operationAsync(Class<T> resourceType,
			String operationName, Parameters parameters, Class<R> returnType)
	{
		return operationAsync(DelayStrategy.EXPONENTIAL_BACKOFF, resourceType, operationName, parameters, returnType);
	}

	/**
	 * Send "Prefer: respond-async, return=representation" header and handles async response
	 *
	 * @param <R>
	 *            return resource type
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
	 * @param returnType
	 *            not <code>null</code>
	 * @return async operation result
	 */
	<R extends Resource, T extends Resource> CompletableFuture<R> operationAsync(DelayStrategy delayStrategy,
			Class<T> resourceType, String operationName, Parameters parameters, Class<R> returnType);

	/**
	 * Uses {@link DelayStrategy#EXPONENTIAL_BACKOFF} unless the server sends <i>Retry-After</i> headers.<br>
	 * <br>
	 * Send "Prefer: respond-async, return=representation" header and handles async response
	 *
	 * @param <R>
	 *            return resource type
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
	 * @param returnType
	 *            not <code>null</code>
	 * @return async operation result
	 */
	default <R extends Resource, T extends Resource> CompletableFuture<R> operationAsync(Class<T> resourceType,
			String id, String operationName, Parameters parameters, Class<R> returnType)
	{
		return operationAsync(DelayStrategy.EXPONENTIAL_BACKOFF, resourceType, id, operationName, parameters,
				returnType);
	}

	/**
	 * Send "Prefer: respond-async, return=representation" header and handles async response
	 *
	 * @param <R>
	 *            return resource type
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
	 * @param returnType
	 *            not <code>null</code>
	 * @return async operation result
	 */
	<R extends Resource, T extends Resource> CompletableFuture<R> operationAsync(DelayStrategy delayStrategy,
			Class<T> resourceType, String id, String operationName, Parameters parameters, Class<R> returnType);

	/**
	 * Uses {@link DelayStrategy#EXPONENTIAL_BACKOFF} unless the server sends <i>Retry-After</i> headers.<br>
	 * <br>
	 * Send "Prefer: respond-async, return=representation" header and handles async response
	 *
	 * @param <R>
	 *            return resource type
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
	 * @param returnType
	 *            not <code>null</code>
	 * @return async operation result
	 */
	default <R extends Resource, T extends Resource> CompletableFuture<R> operationAsync(Class<T> resourceType,
			String id, String version, String operationName, Parameters parameters, Class<R> returnType)
	{
		return operationAsync(DelayStrategy.EXPONENTIAL_BACKOFF, resourceType, id, version, operationName, parameters,
				returnType);
	}

	/**
	 * Send "Prefer: respond-async, return=representation" header and handles async response
	 *
	 * @param <R>
	 *            return resource type
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
	 * @param returnType
	 *            not <code>null</code>
	 * @return async operation result
	 */
	<R extends Resource, T extends Resource> CompletableFuture<R> operationAsync(DelayStrategy delayStrategy,
			Class<T> resourceType, String id, String version, String operationName, Parameters parameters,
			Class<R> returnType);
}
