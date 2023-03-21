package dev.dsf.fhir.spring.config;

import java.util.List;

import org.hl7.fhir.r4.model.Coding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.dsf.common.auth.conf.IdentityProvider;
import dev.dsf.common.auth.conf.RoleConfig;
import dev.dsf.common.auth.conf.RoleConfigReader;
import dev.dsf.fhir.authentication.FhirServerRole;
import dev.dsf.fhir.authentication.IdentityProviderImpl;
import dev.dsf.fhir.authentication.OrganizationProvider;
import dev.dsf.fhir.authentication.OrganizationProviderImpl;
import dev.dsf.fhir.authentication.PractitionerProvider;
import dev.dsf.fhir.authentication.PractitionerProviderImpl;

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
		return new OrganizationProviderImpl(daoConfig.organizationDao(), helperConfig.exceptionHandler(),
				propertiesConfig.getOrganizationIdentifierValue());
	}

	@Bean
	public PractitionerProvider practitionerProvider()
	{
		List<String> configuredUserThumbprints = roleConfig().getEntries().stream()
				.flatMap(e -> e.getThumbprints().stream()).distinct().toList();
		return new PractitionerProviderImpl(configuredUserThumbprints);
	}

	@Bean
	public IdentityProvider identityProvider()
	{
		return new IdentityProviderImpl(organizationProvider(), practitionerProvider(),
				propertiesConfig.getOrganizationIdentifierValue(), roleConfig());
	}

	@Bean
	public RoleConfig roleConfig()
	{
		String roleConfig = propertiesConfig.getRoleConfig();
		if (roleConfig != null)
		{
			RoleConfig config = new RoleConfigReader().read(roleConfig,
					role -> FhirServerRole.isValid(role) ? FhirServerRole.valueOf(role) : null,
					this::practionerRoleFactory);

			logger.info("Role config: {}", config.toString());
			return config;
		}
		else
			throw new RuntimeException("Roles not configured");
	}

	// TODO implement practitioner role factory that logs non existing codes as warning
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
