package dev.dsf.bpe.mail;

import org.junit.Test;

public class LoggingMailServiceTest
{
	@Test
	public void testSend() throws Exception
	{
		new LoggingMailService().send("subject test", "message test");
	}
}
