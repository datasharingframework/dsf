package dev.dsf.bpe.v2.activity;

import java.util.function.Function;

import org.operaton.bpm.engine.delegate.DelegateExecution;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.values.SendTaskValues;
import dev.dsf.bpe.v2.variables.Variables;

public class AbstractMessageDelegate<D> extends AbstractProcessPluginDelegate<D>
{
	protected final SendTaskValues sendTaskValues;

	public AbstractMessageDelegate(ProcessPluginApi api, Function<DelegateExecution, Variables> variablesFactory,
			D delegate, SendTaskValues sendTaskValues)
	{
		super(api, variablesFactory, delegate);

		this.sendTaskValues = sendTaskValues;
	}
}
