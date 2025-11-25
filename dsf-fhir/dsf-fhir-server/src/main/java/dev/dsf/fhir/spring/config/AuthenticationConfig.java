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
import dev.dsf.fhir.authentication.FhirServerRoleImpl;
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
	public RoleConfig<FhirServerRole> roleConfig()
	{
		RoleConfig<FhirServerRole> config = new RoleConfigReader().read(propertiesConfig.getRoleConfig(),
				FhirServerRoleImpl::from, this::practionerRoleFactory);

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
