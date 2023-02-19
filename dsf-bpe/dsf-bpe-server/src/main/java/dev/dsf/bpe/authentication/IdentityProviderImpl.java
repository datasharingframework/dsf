package dev.dsf.bpe.authentication;

import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Set;

import javax.security.auth.x500.X500Principal;

import org.hl7.fhir.r4.model.Organization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.common.auth.Identity;
import dev.dsf.common.auth.IdentityProvider;
import dev.dsf.common.auth.OrganizationIdentity;
import dev.dsf.common.auth.Role;

public class IdentityProviderImpl implements IdentityProvider
{
	private static final Logger logger = LoggerFactory.getLogger(IdentityProviderImpl.class);

	@Override
	public Identity getIdentity(String jwtToken)
	{
		logger.warn("JWT token based login not implemented");
		return null;
	}

	@Override
	public Identity getIdentity(X509Certificate[] certificates)
	{
		return new OrganizationIdentity()
		{
			@Override
			public String getName()
			{
				return certificates[0].getSubjectX500Principal().getName(X500Principal.RFC1779);
			}

			@Override
			public Set<Role> getRoles()
			{
				return Collections.singleton(BpeServerRole.ORGANIZATION);
			}

			@Override
			public Organization getOrganization()
			{
				return null;
			}

			@Override
			public boolean isLocalIdentity()
			{
				return true;
			}

			@Override
			public String getOrganizationIdentifierValue()
			{
				return "";
			}

			@Override
			public boolean hasRole(Role role)
			{
				return BpeServerRole.ORGANIZATION.equals(role);
			}

			@Override
			public boolean hasRole(String role)
			{
				return BpeServerRole.ORGANIZATION.name().equals(role);
			}
		};
	}
}
