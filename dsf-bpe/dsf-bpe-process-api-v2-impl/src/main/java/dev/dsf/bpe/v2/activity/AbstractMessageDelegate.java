package dev.dsf.bpe.v2.activity;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.values.SendTaskValues;

public class AbstractMessageDelegate<D> extends AbstractProcessPluginDelegate<D>
{
	protected final SendTaskValues sendTaskValues;

	public AbstractMessageDelegate(ProcessPluginApi api, D delegate, SendTaskValues sendTaskValues)
	{
		super(api, delegate);

		this.sendTaskValues = sendTaskValues;
	}
}
