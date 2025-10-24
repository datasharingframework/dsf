package dev.dsf.bpe.api.plugin;

import java.nio.file.Path;
import java.util.stream.Stream;

import org.camunda.bpm.engine.impl.variable.serializer.TypedValueSerializer;

import dev.dsf.bpe.api.listener.ListenerFactory;

public interface ProcessPluginFactory
{
	int getApiVersion();

	@SuppressWarnings("rawtypes")
	Stream<TypedValueSerializer> getSerializer();

	ListenerFactory getListenerFactory();

	ProcessPlugin load(Path pluginPath);
}