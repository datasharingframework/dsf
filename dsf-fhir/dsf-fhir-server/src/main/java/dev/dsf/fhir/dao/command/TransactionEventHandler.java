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
package dev.dsf.fhir.dao.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import dev.dsf.fhir.event.Event;
import dev.dsf.fhir.event.EventHandler;

public class TransactionEventHandler implements EventHandler
{
	private final List<Event> cachedEvents = new ArrayList<>();
	private final EventHandler commitDelegate;
	private final EventHandler delegate;

	public TransactionEventHandler(EventHandler commitDelegate, EventHandler delegate)
	{
		this.commitDelegate = Objects.requireNonNull(commitDelegate, "commitDelegate");
		this.delegate = delegate; // may be null
	}

	@Override
	public void handleEvent(Event event)
	{
		cachedEvents.add(event);

		if (delegate != null)
			delegate.handleEvent(event);
	}

	@Override
	public void handleEvents(List<Event> events)
	{
		cachedEvents.addAll(events);

		if (delegate != null)
			delegate.handleEvents(events);
	}

	public void commitEvents()
	{
		commitDelegate.handleEvents(cachedEvents);
	}
}
