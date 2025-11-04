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
package dev.dsf.bpe.config;

import java.util.List;

import org.glassfish.jersey.servlet.init.JerseyServletContainerInitializer;
import org.springframework.web.SpringServletContainerInitializer;

import dev.dsf.common.config.AbstractHttpsJettyConfig;
import jakarta.servlet.ServletContainerInitializer;

public class BpeHttpsJettyConfig extends AbstractHttpsJettyConfig
{
	@Override
	protected String mavenServerModuleName()
	{
		return "bpe-server";
	}

	@Override
	protected List<Class<? extends ServletContainerInitializer>> servletContainerInitializers()
	{
		return List.of(JerseyServletContainerInitializer.class, SpringServletContainerInitializer.class);
	}
}
