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
package dev.dsf.fhir.event;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventManagerImpl implements EventManager
{
	private static final Logger logger = LoggerFactory.getLogger(EventManagerImpl.class);

	private final List<EventHandler> eventHandlers = Collections.synchronizedList(new ArrayList<>());

	public EventManagerImpl(Collection<? extends EventHandler> eventHandlers)
	{
		if (eventHandlers != null)
			this.eventHandlers.addAll(eventHandlers);
	}

	@Override
	public void handleEvent(Event event)
	{
		if (event != null)
			eventHandlers.forEach(doHandleEvent(event));
	}

	private Consumer<? super EventHandler> doHandleEvent(Event event)
	{
		return e ->
		{
			try
			{
				e.handleEvent(event);
			}
			catch (Exception ex)
			{
				logger.debug("Error while handling {} with {}", event.getClass().getSimpleName(),
						e.getClass().getName(), ex);
				logger.warn("Error while handling {} with {}: {} - {}", event.getClass().getSimpleName(),
						e.getClass().getName(), ex.getClass().getName(), ex.getMessage());
			}
		};
	}

	@Override
	public void handleEvents(List<Event> events)
	{
		if (events != null)
			eventHandlers.forEach(doHandleEvents(events));
	}

	private Consumer<? super EventHandler> doHandleEvents(List<Event> events)
	{
		return e ->
		{
			try
			{
				e.handleEvents(events);
			}
			catch (Exception ex)
			{
				logger.debug("Error while handling {} event{} with {}", events.size(), events.size() != 1 ? "s" : "",
						e.getClass().getName(), ex);
				logger.warn("Error while handling {} event{} with {}: {} - {}", events.size(),
						events.size() != 1 ? "s" : "", e.getClass().getName(), ex.getClass().getName(),
						ex.getMessage());
			}
		};
	}

	@Override
	public Runnable addHandler(EventHandler eventHandler)
	{
		eventHandlers.add(eventHandler);

		return () -> removeHandler(eventHandler);
	}

	@Override
	public void removeHandler(EventHandler eventHandler)
	{
		eventHandlers.remove(eventHandler);
	}
}
