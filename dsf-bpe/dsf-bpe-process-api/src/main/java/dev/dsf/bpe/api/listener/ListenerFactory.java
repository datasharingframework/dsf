package dev.dsf.bpe.api.listener;

import org.operaton.bpm.engine.delegate.ExecutionListener;

public interface ListenerFactory
{
	int getApiVersion();

	ExecutionListener getStartListener();

	ExecutionListener getEndListener();

	ExecutionListener getContinueListener();
}
