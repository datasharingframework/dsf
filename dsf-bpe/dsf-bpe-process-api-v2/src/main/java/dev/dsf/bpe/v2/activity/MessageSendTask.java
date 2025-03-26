package dev.dsf.bpe.v2.activity;

import dev.dsf.bpe.v2.error.MessageSendTaskErrorHandler;
import dev.dsf.bpe.v2.error.impl.DefaultMessageSendTaskErrorHandler;

public interface MessageSendTask extends MessageActivity
{
	@Override
	default MessageSendTaskErrorHandler getErrorHandler()
	{
		return new DefaultMessageSendTaskErrorHandler();
	}
}
