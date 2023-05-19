package dev.dsf.bpe.camunda;

import org.camunda.bpm.engine.impl.variable.serializer.VariableSerializerFactory;

public interface FallbackSerializerFactory extends VariableSerializerFactory, ProcessPluginConsumer
{
}
