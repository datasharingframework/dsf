package dev.dsf.bpe.v1.service;

import java.util.Objects;
import java.util.function.Consumer;

import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.InitializingBean;

public class MailServiceImpl implements MailService, InitializingBean
{
	private MailService delegate;

	public MailServiceImpl(MailService delegate)
	{
		this.delegate = delegate;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(delegate, "delegate");
	}

	@Override
	public void send(String subject, MimeBodyPart body, Consumer<MimeMessage> messageModifier)
	{
		delegate.send(subject, body, messageModifier);
	}
}
