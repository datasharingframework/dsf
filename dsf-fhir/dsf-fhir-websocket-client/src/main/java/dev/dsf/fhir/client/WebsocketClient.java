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

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface WebsocketClient extends AutoCloseable
{
	/**
	 * @return <code>true</code> if the client is currently connected
	 */
	boolean isConnected();

	/**
	 * Initiates a connection or returns immediately if already connected
	 *
	 * @return a future completed when the connection is established
	 */
	CompletableFuture<Void> connect();

	/**
	 * Returns the last 10 ping/pong turnaround times in milliseconds, oldest first
	 */
	List<Long> getPingHistory();

	/**
	 * Initiates a shutdown without waiting for completion
	 */
	@Override
	void close();

	/**
	 * Initiates an asynchronous shutdown
	 *
	 * @return a future completed when the shutdown operation finishes
	 */
	CompletableFuture<Void> closeAsync();
}