package org.highmed.dsf.bpe.service;

import java.nio.file.Paths;
import java.security.KeyStore;

import org.junit.Ignore;
import org.junit.Test;

import de.rwh.utils.crypto.io.CertificateReader;

@Ignore
public class SmtpMailServiceTest
{
	@Test
	public void testSend() throws Exception
	{
		new SmtpMailService("from@localhost", new String[] { "to@localhost" }, "localhost", 1025).send("test subject",
				"test message");
	}

	@Test
	public void testSendReplyAndCc() throws Exception
	{
		new SmtpMailService("from@localhost", new String[] { "to1@localhost", "to2@localhost" },
				new String[] { "cc1@localhost", "cc2@localhost" },
				new String[] { "replyTo1@localhost", "replyTo2@localhost" }, false, "localhost", 1025, null, null, null,
				null, null, null, null).send("test subject", "test message");
	}

	@Test
	public void testSendSigned() throws Exception
	{
		char[] signStorePassword = "password".toCharArray();
		KeyStore signStore = CertificateReader.fromPkcs12(Paths.get("cert.p12"), signStorePassword);

		new SmtpMailService("from@localhost", new String[] { "to@localhost" }, null, null, false, "localhost", 1025,
				null, null, null, null, null, signStore, signStorePassword).send("test subject", "test message");
	}
}
