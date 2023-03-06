package dev.dsf.bpe.spring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.dsf.bpe.authentication.IdentityProviderImpl;
import dev.dsf.common.auth.IdentityProvider;

@Configuration
public class AuthenticationConfig
{
	@Bean
	public IdentityProvider identityProvider()
	{
		return new IdentityProviderImpl();
	}
}
