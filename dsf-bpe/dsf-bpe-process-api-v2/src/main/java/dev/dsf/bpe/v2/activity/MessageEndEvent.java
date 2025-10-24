package dev.dsf.bpe.v2.activity;

import dev.dsf.bpe.v2.error.MessageEndEventErrorHandler;
import dev.dsf.bpe.v2.error.impl.DefaultMessageEndEventErrorHandler;

public interface MessageEndEvent extends MessageActivity
{
	@Override
	default MessageEndEventErrorHandler getErrorHandler()
	{
		return new DefaultMessageEndEventErrorHandler();
	}
}
