package dev.dsf.bpe.api.listener;

import org.operaton.bpm.engine.delegate.ExecutionListener;

public class ListenerFactoryImpl implements ListenerFactory
{
	private final int apiVersion;
	private final ExecutionListener startListener;
	private final ExecutionListener endListener;
	private final ExecutionListener continueListener;

	public ListenerFactoryImpl(int apiVersion, ExecutionListener startListener, ExecutionListener endListener,
			ExecutionListener continueListener)
	{
		this.apiVersion = apiVersion;
		this.startListener = startListener;
		this.endListener = endListener;
		this.continueListener = continueListener;
	}

	@Override
	public int getApiVersion()
	{
		return apiVersion;
	}

	@Override
	public ExecutionListener getStartListener()
	{
		return startListener;
	}

	@Override
	public ExecutionListener getEndListener()
	{
		return endListener;
	}

	@Override
	public ExecutionListener getContinueListener()
	{
		return continueListener;
	}
}
