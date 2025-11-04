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
package dev.dsf.fhir.thymeleaf;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Locale;
import java.util.Map;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.IContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

public class LearningTest
{
	private static final Logger logger = LoggerFactory.getLogger(LearningTest.class);

	private static record Data(String hello)
	{
	}

	@Test
	public void test() throws Exception
	{
		ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
		resolver.setTemplateMode(TemplateMode.HTML);
		resolver.setPrefix("/thymeleaf/");
		resolver.setSuffix(".html");

		TemplateEngine engine = new TemplateEngine();
		engine.setTemplateResolver(resolver);

		IContext context = new Context(Locale.ENGLISH,
				Map.of("contentTemplate", "content1", "data", new Data("Hello User!")));
		String result = engine.process("main", context);

		assertNotNull(result);
		assertTrue(result.contains("<p>Hello User!</p>"));
		assertFalse(result.contains("<p>Hello Word!</p>"));

		assertTrue(result.contains("<div>Some content from content1 template.</div>"));

		logger.debug(result);
	}
}
