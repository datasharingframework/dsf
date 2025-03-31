package dev.dsf.bpe.v2.activity;

import java.util.Objects;

import org.camunda.bpm.engine.delegate.DelegateExecution;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.variables.Variables;
import dev.dsf.bpe.v2.variables.VariablesImpl;

public abstract class AbstractProcessPluginDelegate<D>
{
	protected final ProcessPluginApi api;
	protected final ObjectMapper objectMapper;
	protected final D delegate;

	public AbstractProcessPluginDelegate(ProcessPluginApi api, ObjectMapper objectMapper, D delegate)
	{
		this.api = Objects.requireNonNull(api, "api");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
		this.delegate = Objects.requireNonNull(delegate, "delegate");
	}

	protected Variables createVariables(DelegateExecution execution)
	{
		return new VariablesImpl(execution, objectMapper);
	}
}
