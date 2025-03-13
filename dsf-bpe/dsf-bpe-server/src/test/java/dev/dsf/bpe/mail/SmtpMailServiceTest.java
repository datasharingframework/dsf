package dev.dsf.bpe.mail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.List;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message.RecipientType;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import com.icegreen.greenmail.junit4.GreenMailRule;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;

import de.hsheilbronn.mi.utils.crypto.io.KeyStoreReader;
import de.hsheilbronn.mi.utils.crypto.io.PemReader;
import de.hsheilbronn.mi.utils.crypto.keystore.KeyStoreCreator;

public class SmtpMailServiceTest
{
	private static final String FROM = "from@localhost";

	private final ServerSetup setup = ServerSetupTest.SMTP;

	@Rule
	public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTP);

	@Test
	public void testSend() throws Exception
	{
		final String subject = "test subject";
		final String recipient = "to@localhost";
		final String message = "test message";

		new SmtpMailService(FROM, List.of(recipient), "localhost", setup.getPort()).send(subject, message);

		MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
		assertEquals(1, receivedMessages.length);

		Address[] from = receivedMessages[0].getFrom();
		assertNotNull(from);
		assertEquals(1, from.length);
		assertEquals(FROM, from[0].toString());

		Address[] to = receivedMessages[0].getRecipients(RecipientType.TO);
		assertNotNull(to);
		assertEquals(1, to.length);
		assertEquals(recipient, to[0].toString());

		assertEquals(subject, receivedMessages[0].getSubject());

		Object messagContent = receivedMessages[0].getContent();
		assertTrue(messagContent instanceof MimeMultipart);
		assertEquals(1, ((MimeMultipart) messagContent).getCount());
		BodyPart bodyPart = ((MimeMultipart) messagContent).getBodyPart(0);
		assertTrue(bodyPart.getContent() instanceof String);
		assertEquals(message, bodyPart.getContent());
	}

	@Test
	public void testSendTo() throws Exception
	{
		final String defaultRecipient = "to@localhost";
		final String subject = "test subject";
		final String message = "test message";
		final String recipient = "to-test@localhost";

		new SmtpMailService(FROM, List.of(defaultRecipient), "localhost", setup.getPort()).send(subject, message,
				recipient);

		MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
		assertEquals(1, receivedMessages.length);

		Address[] from = receivedMessages[0].getFrom();
		assertNotNull(from);
		assertEquals(1, from.length);
		assertEquals(FROM, from[0].toString());

		Address[] to = receivedMessages[0].getRecipients(RecipientType.TO);
		assertNotNull(to);
		assertEquals(1, to.length);
		assertEquals(recipient, to[0].toString());

		assertEquals(subject, receivedMessages[0].getSubject());

		Object messagContent = receivedMessages[0].getContent();
		assertTrue(messagContent instanceof MimeMultipart);
		assertEquals(1, ((MimeMultipart) messagContent).getCount());
		BodyPart bodyPart = ((MimeMultipart) messagContent).getBodyPart(0);
		assertTrue(bodyPart.getContent() instanceof String);
		assertEquals(message, bodyPart.getContent());
	}

	@Test
	public void testSendReplyAndCc() throws Exception
	{
		final String subject = "test subject";
		final String message = "test message";
		final List<String> recipientsTo = List.of("to1@localhost", "to2@localhost");
		final List<String> recipientsCc = List.of("cc1@localhost", "cc2@localhost");
		final List<String> replyTo = List.of("replyTo1@localhost", "replyTo2@localhost");

		new SmtpMailService(FROM, recipientsTo, recipientsCc, replyTo, false, "localhost", setup.getPort(), null, null,
				null, null, null, null, null, false, 0, SmtpMailService.DEFAULT_DEBUG_LOG_LOCATION)
				.send(subject, message);

		MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
		assertEquals(4, receivedMessages.length);

		for (MimeMessage m : receivedMessages)
		{
			Address[] from = m.getFrom();
			assertNotNull(from);
			assertEquals(1, from.length);
			assertEquals(FROM, from[0].toString());

			Address[] to = m.getRecipients(RecipientType.TO);
			assertNotNull(to);
			assertEquals(2, to.length);
			assertEquals(recipientsTo, List.of(to).stream().map(Address::toString).sorted().toList());

			Address[] cc = m.getRecipients(RecipientType.CC);
			assertNotNull(cc);
			assertEquals(2, cc.length);
			assertEquals(recipientsCc, List.of(cc).stream().map(Address::toString).sorted().toList());

			Address[] rTo = m.getReplyTo();
			assertNotNull(rTo);
			assertEquals(2, rTo.length);
			assertEquals(replyTo, List.of(rTo).stream().map(Address::toString).sorted().toList());

			assertEquals(subject, m.getSubject());

			Object messagContent = m.getContent();
			assertTrue(messagContent instanceof MimeMultipart);
			assertEquals(1, ((MimeMultipart) messagContent).getCount());
			BodyPart bodyPart = ((MimeMultipart) messagContent).getBodyPart(0);
			assertTrue(bodyPart.getContent() instanceof String);
			assertEquals(message, bodyPart.getContent());
		}
	}

	@Ignore
	@Test
	public void testSendSigned() throws Exception
	{
		char[] signStorePassword = "password".toCharArray();
		KeyStore signStore = KeyStoreReader.readPkcs12(Paths.get("cert.p12"), signStorePassword);

		new SmtpMailService("from@localhost", List.of("to@localhost"), null, null, false, "localhost", setup.getPort(),
				null, null, null, null, null, signStore, signStorePassword, false, 0,
				SmtpMailService.DEFAULT_DEBUG_LOG_LOCATION).send("test subject", "test message");
	}

	@Ignore
	@Test
	public void testSendViaSmtps() throws Exception
	{
		KeyStore trustStore = KeyStoreCreator.jksForTrustedCertificates(PemReader.readCertificates("cert.pem"));
		new SmtpMailService("from@localhost", List.of("to@localhost"), null, null, true, "localhost", 465, null, null,
				trustStore, null, null, null, null, false, 0, SmtpMailService.DEFAULT_DEBUG_LOG_LOCATION)
				.send("test subject", "test message");

	}

	@Ignore
	@Test
	public void testSendViaGmail() throws Exception
	{
		new SmtpMailService("foo@gmail.com", List.of("foo@gmail.com"), null, null, true, "smtp.gmail.com", 465, "foo",
				"password".toCharArray(), null, null, null, null, null, false, 0,
				SmtpMailService.DEFAULT_DEBUG_LOG_LOCATION).send("test subject", "test message");
	}
}
