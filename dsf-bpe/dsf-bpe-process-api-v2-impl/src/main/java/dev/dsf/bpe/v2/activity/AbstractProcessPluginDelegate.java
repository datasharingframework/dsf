package dev.dsf.bpe.v2.activity;

import java.util.Objects;
import java.util.function.Function;

import org.operaton.bpm.engine.delegate.DelegateExecution;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.variables.Variables;

public abstract class AbstractProcessPluginDelegate<D>
{
	protected final ProcessPluginApi api;
	protected final D delegate;

	private final Function<DelegateExecution, Variables> variablesFactory;

	public AbstractProcessPluginDelegate(ProcessPluginApi api, Function<DelegateExecution, Variables> variablesFactory,
			D delegate)
	{
		this.api = Objects.requireNonNull(api, "api");
		this.variablesFactory = Objects.requireNonNull(variablesFactory, "variablesFactory");
		this.delegate = Objects.requireNonNull(delegate, "delegate");
	}

	protected Variables createVariables(DelegateExecution execution)
	{
		return variablesFactory.apply(execution);
	}
}
