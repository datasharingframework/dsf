package dev.dsf.bpe.spring.config;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.dsf.bpe.authentication.BpeServerRole;
import dev.dsf.bpe.authentication.IdentityProviderImpl;
import dev.dsf.bpe.service.LocalOrganizationProvider;
import dev.dsf.bpe.service.LocalOrganizationProviderImpl;
import dev.dsf.common.auth.conf.IdentityProvider;
import dev.dsf.common.auth.conf.RoleConfig;
import dev.dsf.common.auth.conf.RoleConfigReader;

@Configuration
public class AuthenticationConfig
{
	private static final Logger logger = LoggerFactory.getLogger(AuthenticationConfig.class);

	@Autowired
	private FhirClientConfig fhirClientConfig;

	@Autowired
	private PropertiesConfig propertiesConfig;

	@Bean
	public LocalOrganizationProvider localOrganizationProvider()
	{
		return new LocalOrganizationProviderImpl(Duration.ofSeconds(30), fhirClientConfig.clientProvider(),
				propertiesConfig.getFhirServerBaseUrl());
	}

	@Bean
	public IdentityProvider identityProvider()
	{
		return new IdentityProviderImpl(roleConfig(), localOrganizationProvider());
	}

	@Bean
	public RoleConfig roleConfig()
	{
		RoleConfig config = new RoleConfigReader().read(propertiesConfig.getRoleConfig(),
				role -> BpeServerRole.isValid(role) ? BpeServerRole.valueOf(role) : null, s -> null);

		logger.info("Role config: {}", config.toString());
		return config;
	}
}
