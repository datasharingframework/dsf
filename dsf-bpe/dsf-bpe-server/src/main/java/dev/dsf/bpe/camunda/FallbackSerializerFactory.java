package dev.dsf.bpe.camunda;

import org.operaton.bpm.engine.impl.variable.serializer.VariableSerializerFactory;

public interface FallbackSerializerFactory extends VariableSerializerFactory, ProcessPluginConsumer
{
}
