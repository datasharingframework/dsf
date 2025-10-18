package dev.dsf.bpe.spring.config;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.dsf.bpe.authentication.BpeServerRole;
import dev.dsf.bpe.authentication.IdentityProviderImpl;
import dev.dsf.bpe.service.LocalOrganizationAndEndpointProvider;
import dev.dsf.bpe.service.LocalOrganizationAndEndpointProviderImpl;
import dev.dsf.common.auth.conf.IdentityProvider;
import dev.dsf.common.auth.conf.RoleConfig;
import dev.dsf.common.auth.conf.RoleConfigReader;

@Configuration
public class AuthenticationConfig
{
	private static final Logger logger = LoggerFactory.getLogger(AuthenticationConfig.class);

	@Autowired
	private DsfClientConfig dsfClientConfig;

	@Autowired
	private PropertiesConfig propertiesConfig;

	@Bean
	public LocalOrganizationAndEndpointProvider localOrganizationProvider()
	{
		return new LocalOrganizationAndEndpointProviderImpl(Duration.ofSeconds(30), dsfClientConfig.clientProvider(),
				propertiesConfig.getDsfServerBaseUrl());
	}

	@Bean
	public IdentityProvider identityProvider()
	{
		return new IdentityProviderImpl(roleConfig(), localOrganizationProvider());
	}

	@Bean
	public RoleConfig<BpeServerRole> roleConfig()
	{
		RoleConfig<BpeServerRole> config = new RoleConfigReader().read(propertiesConfig.getRoleConfig(),
				BpeServerRole::from, _ -> null);

		logger.info("Role config: {}", config.toString());
		return config;
	}
}
