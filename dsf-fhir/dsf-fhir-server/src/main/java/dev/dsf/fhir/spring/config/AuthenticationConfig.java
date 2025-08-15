package dev.dsf.fhir.spring.config;

import org.hl7.fhir.r4.model.Coding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.dsf.common.auth.conf.IdentityProvider;
import dev.dsf.common.auth.conf.RoleConfig;
import dev.dsf.common.auth.conf.RoleConfigReader;
import dev.dsf.fhir.authentication.EndpointProvider;
import dev.dsf.fhir.authentication.EndpointProviderImpl;
import dev.dsf.fhir.authentication.FhirServerRole;
import dev.dsf.fhir.authentication.IdentityProviderImpl;
import dev.dsf.fhir.authentication.OrganizationProvider;
import dev.dsf.fhir.authentication.OrganizationProviderImpl;

@Configuration
public class AuthenticationConfig
{
	private static final Logger logger = LoggerFactory.getLogger(AuthenticationConfig.class);

	@Autowired
	private DaoConfig daoConfig;

	@Autowired
	private HelperConfig helperConfig;

	@Autowired
	private PropertiesConfig propertiesConfig;

	@Bean
	public OrganizationProvider organizationProvider()
	{
		return new OrganizationProviderImpl(helperConfig.exceptionHandler(), daoConfig.organizationDao(),
				propertiesConfig.getOrganizationIdentifierValue());
	}

	@Bean
	public EndpointProvider endpointProvider()
	{
		return new EndpointProviderImpl(helperConfig.exceptionHandler(), daoConfig.endpointDao(),
				propertiesConfig.getDsfServerBaseUrl());
	}

	@Bean
	public IdentityProvider identityProvider()
	{
		return new IdentityProviderImpl(roleConfig(), organizationProvider(), endpointProvider(),
				propertiesConfig.getOrganizationIdentifierValue());
	}

	@Bean
	public RoleConfig roleConfig()
	{
		RoleConfig config = new RoleConfigReader().read(propertiesConfig.getRoleConfig(),
				role -> FhirServerRole.isValid(role) ? FhirServerRole.valueOf(role) : null,
				this::practionerRoleFactory);

		logger.info("Role config: {}", config.toString());
		return config;
	}

	// TODO implement role factory that only allows existing roles
	private Coding practionerRoleFactory(String role)
	{
		if (role != null)
		{
			String[] roleParts = role.split("\\|");
			if (roleParts.length == 2)
				return new Coding().setSystem(roleParts[0]).setCode(roleParts[1]);
		}

		return null;
	}
}
