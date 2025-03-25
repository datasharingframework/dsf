package dev.dsf.bpe.v2.activity;

import java.util.Objects;

import dev.dsf.bpe.v2.ProcessPluginApi;

public abstract class AbstractProcessPluginDelegate<D>
{
	protected final ProcessPluginApi api;
	protected final D delegate;

	public AbstractProcessPluginDelegate(ProcessPluginApi api, D delegate)
	{
		this.api = Objects.requireNonNull(api, "api");
		this.delegate = Objects.requireNonNull(delegate, "delegate");
	}
}
