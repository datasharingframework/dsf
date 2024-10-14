package dev.dsf.bpe.api.listener;

import org.camunda.bpm.engine.delegate.ExecutionListener;

public interface ListenerFactory
{
	int getApiVersion();

	ExecutionListener getStartListener();

	ExecutionListener getEndListener();

	ExecutionListener getContinueListener();
}
