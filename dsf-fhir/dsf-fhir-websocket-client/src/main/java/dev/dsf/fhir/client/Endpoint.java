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
package dev.dsf.fhir.client;

import java.util.concurrent.CompletableFuture;

import org.hl7.fhir.r4.model.Subscription;

public interface Endpoint
{
	/**
	 * @return not <code>null</code>
	 */
	Subscription getSubscription();

	/**
	 * Called before a websocket connection attempt is made
	 *
	 * @param subscription
	 *            not <code>null</code>
	 */
	default void beforeConnect(Subscription subscription)
	{
	}

	default void onOpen()
	{
	}

	/**
	 * @param subscription
	 *            not <code>null</code>
	 * @param message
	 *            not <code>null</code>
	 * @return never <code>null</code>
	 */
	CompletableFuture<Void> onMessage(Subscription subscription, String message);

	default void onDisconnect()
	{
	}

	default boolean isRetryableException(Throwable e)
	{
		return true;
	}
}
