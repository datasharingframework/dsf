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
package dev.dsf.fhir.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.Test;

import dev.dsf.fhir.dao.ActivityDefinitionDao;

public class ActivityDefinitionIntegrationTest extends AbstractIntegrationTest
{
	private void testSearchByUrl(String createUrl, String searchParameter, String searchUrl,
			Consumer<Bundle> checkResult) throws SQLException
	{
		ActivityDefinition aD = new ActivityDefinition().setUrl(createUrl);
		readAccessHelper.addAll(aD);

		ActivityDefinitionDao dao = getSpringWebApplicationContext().getBean(ActivityDefinitionDao.class);
		dao.create(aD);

		Bundle resultWithStrictHandling = getWebserviceClient().searchWithStrictHandling(ActivityDefinition.class,
				Map.of(searchParameter, List.of(searchUrl)));

		assertNotNull(resultWithStrictHandling);
		checkResult.accept(resultWithStrictHandling);

		Bundle result = getWebserviceClient().search(ActivityDefinition.class,
				Map.of(searchParameter, List.of(searchUrl)));

		assertNotNull(result);
		checkResult.accept(result);
	}

	private Consumer<Bundle> assertFound(String expectedUrl)
	{
		return b ->
		{
			assertEquals(1, b.getTotal());
			assertEquals(1, b.getEntry().size());
			assertNotNull(b.getEntry().get(0));
			assertTrue(b.getEntry().get(0).hasResource());
			assertNotNull(b.getEntry().get(0).getResource());
			assertTrue(b.getEntry().get(0).getResource() instanceof ActivityDefinition);
			assertEquals(expectedUrl, ((ActivityDefinition) (b.getEntry().get(0).getResource())).getUrl());
		};
	}

	private void assertNotFound(Bundle b)
	{
		assertEquals(0, b.getTotal());
		assertEquals(0, b.getEntry().size());
	}

	@Test
	public void testSearchByUrlFound() throws Exception
	{
		String url = "http://test.com/fhir/ActivityDefinition/test";

		testSearchByUrl(url, "url", url, assertFound(url));
	}

	@Test
	public void testSearchByUrlNotFound() throws Exception
	{
		String createUrl = "http://test.com/fhir/ActivityDefinition/test";
		String searchUrl = "http://somethingelse.com/fhir/ActivityDefinition/test";

		testSearchByUrl(createUrl, "url", searchUrl, this::assertNotFound);
	}

	@Test
	public void testSearchByUrlBelowFound() throws Exception
	{
		String createUrl = "http://test.com/fhir/ActivityDefinition/test";
		String searchUrl = "http://test.com/fhir/ActivityDefinition";

		testSearchByUrl(createUrl, "url:below", searchUrl, assertFound(createUrl));
	}

	@Test
	public void testSearchByUrlBelowNotFound() throws Exception
	{
		String createUrl = "http://test.com/fhir/ActivityDefinition/test";
		String searchUrl = "http://somethingelse.com/fhir/ActivityDefinition";

		testSearchByUrl(createUrl, "url:below", searchUrl, this::assertNotFound);
	}
}
