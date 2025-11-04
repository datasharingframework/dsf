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
package dev.dsf.bpe.camunda;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.operaton.bpm.engine.impl.variable.serializer.AbstractTypedValueSerializer;
import org.operaton.bpm.engine.impl.variable.serializer.TypedValueSerializer;
import org.operaton.bpm.engine.impl.variable.serializer.ValueFields;
import org.operaton.bpm.engine.variable.impl.value.UntypedValueImpl;
import org.operaton.bpm.engine.variable.value.PrimitiveValue;
import org.operaton.bpm.engine.variable.value.TypedValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.bpe.api.plugin.ProcessIdAndVersion;
import dev.dsf.bpe.api.plugin.ProcessPlugin;

public class FallbackSerializerFactoryImpl implements FallbackSerializerFactory
{
	public static final class TypedValueSerializerWrapper<T extends TypedValue> extends AbstractTypedValueSerializer<T>
	{
		final TypedValueSerializer<T> delegate;

		TypedValueSerializerWrapper(TypedValueSerializer<T> delegate)
		{
			super(delegate.getType());

			this.delegate = delegate;
		}

		ClassLoader getClassLoader()
		{
			return delegate.getClass().getClassLoader();
		}

		@Override
		public String getName()
		{
			return getClassLoader().getName() + "/" + delegate.getName();
		}

		@Override
		public void writeValue(T value, ValueFields valueFields)
		{
			delegate.writeValue(value, valueFields);
		}

		@Override
		public T readValue(ValueFields valueFields, boolean deserializeValue, boolean isTransient)
		{
			return delegate.readValue(valueFields, deserializeValue, isTransient);
		}

		@Override
		public T convertToTypedValue(UntypedValueImpl untypedValue)
		{
			return delegate.convertToTypedValue(untypedValue);
		}

		@Override
		public boolean canHandle(TypedValue value)
		{
			return delegate.canHandle(value);
		}

		@Override
		protected boolean canWriteValue(TypedValue value)
		{
			throw new UnsupportedOperationException("canWriteValue method not supported");
		}
	}

	private static final Logger logger = LoggerFactory.getLogger(FallbackSerializerFactoryImpl.class);

	@SuppressWarnings("rawtypes")
	private final Map<ClassLoader, List<TypedValueSerializerWrapper>> serializersByClassLoader = new HashMap<>();
	@SuppressWarnings("rawtypes")
	private final Map<String, TypedValueSerializerWrapper> serializersByName = new HashMap<>();

	@Override
	public void setProcessPlugins(List<ProcessPlugin> plugins,
			Map<ProcessIdAndVersion, ProcessPlugin> processPluginsByProcessIdAndVersion)
	{
		@SuppressWarnings({ "unchecked", "rawtypes" })
		List<TypedValueSerializerWrapper> serializers = plugins.stream()
				.flatMap(ProcessPlugin::getTypedValueSerializers).map(TypedValueSerializerWrapper::new)
				.collect(Collectors.toList());

		serializersByName.putAll(
				serializers.stream().collect(Collectors.toMap(TypedValueSerializer::getName, Function.identity())));
		serializersByClassLoader
				.putAll(serializers.stream().collect(Collectors.groupingBy(s -> s.getType().getClass().getClassLoader(),
						Collectors.mapping(Function.identity(), Collectors.toList()))));
	}

	@Override
	public TypedValueSerializer<?> getSerializer(String serializerName)
	{
		if (serializerName == null)
			return null;

		logger.debug("Getting serializer for {}", serializerName);
		return serializersByName.getOrDefault(serializerName, null);
	}

	@Override
	public TypedValueSerializer<?> getSerializer(TypedValue value)
	{
		if (value == null)
			return null;

		ClassLoader classLoader = getClassLoader(value);
		if (classLoader != null)
		{
			logger.debug("Getting serializer for {} from class loader {}", getName(value), classLoader.getName());

			return serializersByClassLoader.getOrDefault(classLoader, List.of()).stream()
					.filter(s -> s.canHandle(value)).findFirst().orElse(null);
		}
		else
			return null;
	}

	private ClassLoader getClassLoader(TypedValue value)
	{
		if (value == null)
			return null;

		if (value instanceof PrimitiveValue)
			return value.getType().getClass().getClassLoader();
		else if (value.getValue() != null)
			return value.getValue().getClass().getClassLoader();
		else
			return null;
	}

	private String getName(TypedValue value)
	{
		if (value == null)
			return null;

		if (value instanceof PrimitiveValue p)
			return p.getType().getJavaType().getName();
		else if (value.getValue() != null)
			return value.getClass().getName();
		else if (value.getType() != null)
			return value.getType().getName();
		else
			return "?";
	}
}
