package dev.dsf.bpe.spring.config;

import java.io.IOException;
import java.security.KeyStore;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter.Result;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import de.hsheilbronn.mi.utils.crypto.cert.CertificateFormatter.X500PrincipalFormat;
import de.hsheilbronn.mi.utils.crypto.keystore.KeyStoreFormatter;
import dev.dsf.bpe.api.service.BpeMailService;
import dev.dsf.bpe.mail.LoggingMailService;
import dev.dsf.bpe.mail.SmtpMailService;
import dev.dsf.common.build.BuildInfoReader;

@Configuration
public class MailConfig implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(MailConfig.class);

	@Autowired
	private PropertiesConfig propertiesConfig;

	@Autowired
	private BuildInfoReaderConfig buildInfoReaderConfig;

	@Bean
	public BpeMailService bpeMailService()
	{
		if (isConfigured())
			return newSmptMailService();
		else
			return new LoggingMailService();
	}

	private boolean isConfigured()
	{
		return propertiesConfig.getMailServerHostname() != null && propertiesConfig.getMailServerPort() > 0;
	}

	private BpeMailService newSmptMailService()
	{
		String fromAddress = propertiesConfig.getMailFromAddress();
		List<String> toAddresses = propertiesConfig.getMailToAddresses();
		List<String> toAddressesCc = propertiesConfig.getMailToAddressesCc();
		List<String> replyToAddresses = propertiesConfig.getMailReplyToAddresses();

		boolean useSmtps = propertiesConfig.getMailUseSmtps();

		String mailServerHostname = propertiesConfig.getMailServerHostname();
		int mailServerPort = propertiesConfig.getMailServerPort();

		String mailServerUsername = propertiesConfig.getMailServerUsername();
		char[] mailServerPassword = propertiesConfig.getMailServerPassword();

		KeyStore trustStore = propertiesConfig.getMailServerTrustStore();
		char[] keyStorePassword = UUID.randomUUID().toString().toCharArray();
		KeyStore keyStore = propertiesConfig.getMailClientKeyStore(keyStorePassword);
		KeyStore signStore = propertiesConfig.getMailSmimeSigingKeyStore();

		return new SmtpMailService(fromAddress, toAddresses, toAddressesCc, replyToAddresses, useSmtps,
				mailServerHostname, mailServerPort, mailServerUsername, mailServerPassword, trustStore, keyStore,
				keyStorePassword, signStore, propertiesConfig.getMailSmimeSigingKeyStorePassword(),
				propertiesConfig.getSendMailOnErrorLogEvent(), propertiesConfig.getMailOnErrorLogEventBufferSize(),
				propertiesConfig.getMailOnErrorLogEventDebugLogLocation());
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		if (isConfigured())
		{
			logger.info(
					"Mail client config: {fromAddress: {}, toAddresses: {}, toAddressesCc: {}, replyToAddresses: {},"
							+ " useSmtps: {}, mailServerHostname: {}, mailServerPort: {}, mailServerUsername: {},"
							+ " mailServerPassword: {}, trustStore: {}, clientCertificate: {}, clientCertificatePrivateKey: {},"
							+ " clientCertificatePrivateKeyPassword: {}, smimeSigingKeyStore: {}, smimeSigingKeyStorePassword: {},"
							+ " sendTestMailOnStartup: {}, sendMailOnErrorLogEvent: {}, mailOnErrorLogEventBufferSize: {},"
							+ " mailOnErrorLogEventDebugLogLocation: {}}",
					propertiesConfig.getMailFromAddress(), propertiesConfig.getMailToAddresses(),
					propertiesConfig.getMailToAddressesCc(), propertiesConfig.getMailReplyToAddresses(),
					propertiesConfig.getMailUseSmtps(), propertiesConfig.getMailServerHostname(),
					propertiesConfig.getMailServerPort(), propertiesConfig.getMailServerUsername(),
					propertiesConfig.getMailServerPassword() != null ? "***" : "null",
					propertiesConfig.getMailServerTrustStoreFileOrFolder(),
					propertiesConfig.getMailClientCertificateFile(),
					propertiesConfig.getMailClientCertificatePrivateKeyFile(),
					propertiesConfig.getMailClientCertificatePrivateKeyFilePassword() != null ? "***" : "null",
					propertiesConfig.getMailSmimeSigingKeyStoreFile(),
					propertiesConfig.getMailSmimeSigingKeyStorePassword() != null ? "***" : "null",
					propertiesConfig.getSendTestMailOnStartup(), propertiesConfig.getSendMailOnErrorLogEvent(),
					propertiesConfig.getMailOnErrorLogEventBufferSize(),
					propertiesConfig.getMailOnErrorLogEventDebugLogLocation());

			if (propertiesConfig.getMailUseSmtps())
			{
				logger.info("Using trust-store with {} to validate mail server certificate",
						KeyStoreFormatter
								.toSubjectsFromCertificates(propertiesConfig.getDsfClientTrustedServerCas(),
										X500PrincipalFormat.RFC1779)
								.values().stream().collect(Collectors.joining("; ", "[", "]")));
			}
		}
		else
		{
			logger.info(
					"Mail client config: SMTP client not configured, sending mails to debug log, configure at least SMTP server host and port");
		}

		if (isConfigured())
		{
			Appender appender = ((SmtpMailService) bpeMailService()).getLog4jAppender();
			if (appender != null)
			{
				appender.start();

				LoggerContext context = (LoggerContext) LogManager.getContext(false);
				context.getConfiguration().getRootLogger().addAppender(appender, Level.INFO,
						ThresholdFilter.createFilter(Level.INFO, Result.ACCEPT, Result.DENY));
			}
		}
	}

	@EventListener({ ContextRefreshedEvent.class })
	public void onContextRefreshedEvent(ContextRefreshedEvent event) throws IOException
	{
		if (propertiesConfig.getSendTestMailOnStartup())
		{
			DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

			BuildInfoReader buildInfoReader = buildInfoReaderConfig.buildInfoReader();
			bpeMailService().send("DSF BPE Test Mail",
					"BPE startup test mail\n\nArtifact: " + buildInfoReader.getProjectArtifact() + "\nVersion: "
							+ buildInfoReader.getProjectVersion() + "\nBuild: "
							+ buildInfoReader.getBuildDate().withZoneSameInstant(ZoneId.systemDefault())
									.format(formatter)
							+ "\nBranch: " + buildInfoReader.getBuildBranch() + "\nCommit: "
							+ buildInfoReader.getBuildNumber() + "\n\nSend on "
							+ ZonedDateTime.now().withZoneSameInstant(ZoneId.systemDefault()).format(formatter));
		}
	}
}
