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

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import dev.dsf.bpe.client.dsf.WebserviceClient;
import dev.dsf.bpe.dao.LastEventTimeDao;
import jakarta.ws.rs.core.UriBuilder;

public class ExistingResourceLoaderImpl<R extends Resource> implements ExistingResourceLoader<R>
{
	private static final Logger logger = LoggerFactory.getLogger(ExistingResourceLoaderImpl.class);

	private static final String PARAM_LAST_UPDATED = "_lastUpdated";
	private static final String PARAM_COUNT = "_count";
	private static final String PARAM_PAGE = "_page";
	private static final String PARAM_SORT = "_sort";
	private static final int RESULT_PAGE_COUNT = 20;

	private final LastEventTimeDao lastEventTimeDao;
	private final WebserviceClient webserviceClient;
	private final ResourceHandler<R> handler;
	private final String resourceName;
	private final Class<R> resourceClass;

	public ExistingResourceLoaderImpl(LastEventTimeDao lastEventTimeDao, ResourceHandler<R> handler,
			WebserviceClient webserviceClient, String resourceName, Class<R> resourceClass)
	{
		this.lastEventTimeDao = lastEventTimeDao;
		this.handler = handler;
		this.webserviceClient = webserviceClient;
		this.resourceName = resourceName;
		this.resourceClass = resourceClass;
	}

	@Override
	public void readExistingResources(Map<String, List<String>> searchCriteriaQueryParameters)
	{
		// executing search until call results in no more found tasks
		while (doReadExistingResources(searchCriteriaQueryParameters))
			;
	}

	private boolean doReadExistingResources(Map<String, List<String>> searchCriteriaQueryParameters)
	{
		Map<String, List<String>> queryParams = new HashMap<>(searchCriteriaQueryParameters);
		Optional<LocalDateTime> readLastEventTime = readLastEventTime();

		readLastEventTime.ifPresent(lastEventTime -> queryParams.put(PARAM_LAST_UPDATED,
				List.of("gt" + lastEventTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))));

		queryParams.put(PARAM_COUNT, List.of(String.valueOf(RESULT_PAGE_COUNT)));
		queryParams.put(PARAM_PAGE, List.of("1"));
		queryParams.put(PARAM_SORT, List.of(PARAM_LAST_UPDATED));

		UriBuilder builder = UriBuilder.fromPath(resourceName);
		queryParams.forEach((k, v) -> builder.replaceQueryParam(k, v.toArray()));

		logger.debug("Executing search {}", builder.toString());
		Bundle bundle = webserviceClient.searchWithStrictHandling(resourceClass, queryParams);

		if (bundle.getTotal() <= 0)
		{
			logger.debug("Result bundle.total <= 0");
			return false;
		}

		for (BundleEntryComponent entry : bundle.getEntry())
		{
			if (entry.hasResource())
			{
				if (resourceClass.isInstance(entry.getResource()))
				{
					@SuppressWarnings("unchecked")
					R resource = (R) entry.getResource();
					handler.onResource(resource);
					writeLastEventTime(resource.getMeta().getLastUpdated());
				}
				else
				{
					logger.warn("Ignoring resource of type {}",
							entry.getResource().getClass().getAnnotation(ResourceDef.class).name());
				}
			}
			else
			{
				logger.warn("Bundle entry did not contain resource");
			}
		}

		return true;
	}

	private Optional<LocalDateTime> readLastEventTime()
	{
		try
		{
			return lastEventTimeDao.readLastEventTime();
		}
		catch (SQLException e)
		{
			logger.debug("Unable to read last event time from db", e);
			logger.warn("Unable to read last event time from db: {} - {}", e.getClass().getName(), e.getMessage());

			throw new RuntimeException(e);
		}
	}

	private void writeLastEventTime(Date lastUpdated)
	{
		try
		{
			lastEventTimeDao.writeLastEventTime(lastUpdated);
		}
		catch (SQLException e)
		{
			logger.debug("Unable to write last event time to db", e);
			logger.warn("Unable to write last event time to db: {} - {}", e.getClass().getName(), e.getMessage());

			throw new RuntimeException(e);
		}
	}
}
