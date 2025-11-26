/*
 * Copyright 2018-2025 Heilbronn University of Applied Sciences
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.dsf.bpe.v2.service;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;

/**
 * Service for sending e-mail if a connection to an SMTP mail server is configured. If no connection is configured
 * content will be logged.
 */
public interface MailService
{
	/**
	 * Sends a plain text mail to the BPE wide configured recipients.
	 *
	 * @param subject
	 *            not <code>null</code>
	 * @param message
	 *            not <code>null</code>
	 */
	default void send(String subject, String message)
	{
		send(subject, message, (String) null);
	}

	/**
	 * Sends a plain text mail to the given address (<b>to</b>) if not <code>null</code> or the BPE wide configured
	 * recipients.
	 *
	 * @param subject
	 *            not <code>null</code>
	 * @param message
	 *            not <code>null</code>
	 * @param to
	 *            BPE wide configured recipients if parameter is <code>null</code>
	 */
	default void send(String subject, String message, String to)
	{
		send(subject, message, to == null ? null : List.of(to));
	}

	/**
	 * Sends a plain text mail to the given addresses (<b>to</b>) if not <code>null</code> and not empty or the BPE wide
	 * configured recipients.
	 *
	 * @param subject
	 *            not <code>null</code>
	 * @param message
	 *            not <code>null</code>
	 * @param to
	 *            BPE wide configured recipients if parameter is <code>null</code> or empty
	 */
	default void send(String subject, String message, Collection<String> to)
	{
		try
		{
			MimeBodyPart body = new MimeBodyPart();
			body.setText(message, StandardCharsets.UTF_8.displayName());

			send(subject, body, to);
		}
		catch (MessagingException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Sends the given {@link MimeBodyPart} as content of a mail to the BPE wide configured recipients.
	 *
	 * @param subject
	 *            not <code>null</code>
	 * @param body
	 *            not <code>null</code>
	 */
	default void send(String subject, MimeBodyPart body)
	{
		send(subject, body, (String) null);
	}

	/**
	 * Sends the given {@link MimeBodyPart} as content of a mail to the given address (<b>to</b>) if not
	 * <code>null</code> or the BPE wide configured recipients.
	 *
	 * @param subject
	 *            not <code>null</code>
	 * @param body
	 *            not <code>null</code>
	 * @param to
	 *            BPE wide configured recipients if parameter is <code>null</code>
	 */
	default void send(String subject, MimeBodyPart body, String to)
	{
		send(subject, body, to == null ? null : List.of(to));
	}

	/**
	 * Sends the given {@link MimeBodyPart} as content of a mail to the given addresses (<b>to</b>) if not
	 * <code>null</code> and not empty or the BPE wide configured recipients.
	 *
	 * @param subject
	 *            not <code>null</code>
	 * @param body
	 *            not <code>null</code>
	 * @param to
	 *            BPE wide configured recipients if parameter is <code>null</code> or empty
	 */
	default void send(String subject, MimeBodyPart body, Collection<String> to)
	{
		if (to == null || to.isEmpty())
			send(subject, body, (Consumer<MimeMessage>) null);
		else
			send(subject, body, m ->
			{
				try
				{
					m.setRecipients(RecipientType.TO, to.stream().map(t ->
					{
						try
						{
							return new InternetAddress(t);
						}
						catch (AddressException e)
						{
							throw new RuntimeException(e);
						}
					}).toArray(InternetAddress[]::new));

					m.saveChanges();
				}
				catch (MessagingException e)
				{
					throw new RuntimeException(e);
				}
			});
	}

	/**
	 * Sends the given {@link MimeBodyPart} as content of a mail to the BPE wide configured recipients, the
	 * <b>messageModifier</b> can be used to modify elements of the generated {@link MimeMessage} before it is send to
	 * the SMTP server.
	 *
	 * @param subject
	 *            not <code>null</code>
	 * @param body
	 *            not <code>null</code>
	 * @param messageModifier
	 *            may be <code>null</code>
	 */
	void send(String subject, MimeBodyPart body, Consumer<MimeMessage> messageModifier);
}
