package dev.dsf.bpe.v2.activity;

import dev.dsf.bpe.v2.error.MessageIntermediateThrowEventErrorHandler;
import dev.dsf.bpe.v2.error.impl.DefaultMessageIntermediateThrowEventErrorHandler;

public interface MessageIntermediateThrowEvent extends MessageActivity
{
	@Override
	default MessageIntermediateThrowEventErrorHandler getErrorHandler()
	{
		return new DefaultMessageIntermediateThrowEventErrorHandler();
	}
}
