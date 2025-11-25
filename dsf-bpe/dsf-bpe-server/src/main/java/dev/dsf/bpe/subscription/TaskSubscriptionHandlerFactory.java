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
package dev.dsf.bpe.subscription;

import java.util.Objects;

import org.hl7.fhir.r4.model.Task;
import org.springframework.beans.factory.InitializingBean;

import dev.dsf.bpe.client.dsf.WebserviceClient;
import dev.dsf.bpe.dao.LastEventTimeDao;

public class TaskSubscriptionHandlerFactory implements SubscriptionHandlerFactory<Task>, InitializingBean
{
	private final ResourceHandler<Task> resourceHandler;
	private final LastEventTimeDao lastEventTimeDao;

	public TaskSubscriptionHandlerFactory(ResourceHandler<Task> resourceHandler, LastEventTimeDao lastEventTimeDao)
	{
		this.resourceHandler = resourceHandler;
		this.lastEventTimeDao = lastEventTimeDao;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(resourceHandler, "resourceHandler");
		Objects.requireNonNull(lastEventTimeDao, "lastEventTimeDao");
	}

	@Override
	public ExistingResourceLoader<Task> createExistingResourceLoader(WebserviceClient client)
	{
		return new ExistingResourceLoaderImpl<>(lastEventTimeDao, resourceHandler, client, "Task", Task.class);
	}

	@Override
	public EventResourceHandler<Task> createEventResourceHandler()
	{
		return new EventResourceHandlerImpl<>(lastEventTimeDao, resourceHandler, Task.class);
	}

	@Override
	public PingEventResourceHandler<Task> createPingEventResourceHandler(
			ExistingResourceLoader<Task> existingResourceLoader)
	{
		return new PingEventResourceHandlerImpl<>(existingResourceLoader);
	}
}
