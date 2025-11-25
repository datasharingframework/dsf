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
package dev.dsf.bpe.v2.plugin;

import java.nio.file.Path;
import java.util.stream.Stream;

import org.operaton.bpm.engine.impl.variable.serializer.TypedValueSerializer;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import dev.dsf.bpe.api.listener.ListenerFactory;
import dev.dsf.bpe.api.plugin.AbstractProcessPluginFactory;
import dev.dsf.bpe.api.plugin.ProcessPlugin;
import dev.dsf.bpe.api.plugin.ProcessPluginFactory;
import dev.dsf.bpe.v2.ProcessPluginDefinition;

public class ProcessPluginFactoryImpl extends AbstractProcessPluginFactory implements ProcessPluginFactory
{
	public static final int API_VERSION = 2;

	public ProcessPluginFactoryImpl(ClassLoader apiClassLoader, ApplicationContext apiApplicationContext,
			ConfigurableEnvironment environment, String serverBaseUrl)
	{
		super(API_VERSION, apiClassLoader, apiApplicationContext, environment, serverBaseUrl,
				ProcessPluginDefinition.class);
	}

	@Override
	protected ProcessPlugin createProcessPlugin(Object processPluginDefinition, boolean draft, Path jarFile,
			ClassLoader pluginClassLoader)
	{
		return new ProcessPluginImpl((ProcessPluginDefinition) processPluginDefinition, API_VERSION, draft, jarFile,
				pluginClassLoader, environment, apiApplicationContext, serverBaseUrl);
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Stream<TypedValueSerializer> getSerializer()
	{
		return apiApplicationContext.getBeansOfType(TypedValueSerializer.class).values().stream();
	}

	@Override
	public ListenerFactory getListenerFactory()
	{
		return apiApplicationContext.getBean(ListenerFactory.class);
	}
}
