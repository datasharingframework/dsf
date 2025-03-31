package dev.dsf.bpe.v2.activity;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.values.SendTaskValues;

public class AbstractMessageDelegate<D> extends AbstractProcessPluginDelegate<D>
{
	protected final SendTaskValues sendTaskValues;

	public AbstractMessageDelegate(ProcessPluginApi api, ObjectMapper objectMapper, D delegate,
			SendTaskValues sendTaskValues)
	{
		super(api, objectMapper, delegate);

		this.sendTaskValues = sendTaskValues;
	}
}
