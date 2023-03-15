package dev.dsf.bpe.authentication;

import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import javax.security.auth.x500.X500Principal;

import org.eclipse.jetty.security.openid.OpenIdCredentials;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Practitioner;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.common.auth.conf.IdentityProvider;
import dev.dsf.common.auth.conf.OrganizationIdentity;
import dev.dsf.common.auth.conf.PractitionerIdentity;
import dev.dsf.common.auth.conf.Role;

public class IdentityProviderImpl implements IdentityProvider
{
	@Override
	public Identity getIdentity(OpenIdCredentials credentials)
	{
		return new PractitionerIdentity()
		{
			@Override
			public String getName()
			{
				return credentials.getUserId();
			}

			@Override
			public String getDisplayName()
			{
				return getName();
			}

			@Override
			public boolean isLocalIdentity()
			{
				return true;
			}

			@Override
			public boolean hasRole(String role)
			{
				return BpeServerRole.ORGANIZATION.name().equals(role);
			}


			@Override
			public boolean hasRole(Role role)
			{
				return BpeServerRole.ORGANIZATION.equals(role);
			}

			@Override
			public Set<Role> getRoles()
			{
				return Collections.singleton(BpeServerRole.ORGANIZATION);
			}

			@Override
			public String getOrganizationIdentifierValue()
			{
				return "";
			}

			@Override
			public Organization getOrganization()
			{
				return null;
			}

			@Override
			public Practitioner getPractitioner()
			{
				return null;
			}

			@Override
			public Optional<OpenIdCredentials> getCredentials()
			{
				return Optional.of(credentials);
			}

			@Override
			public Optional<X509Certificate> getCertificate()
			{
				return Optional.empty();
			}
		};
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
			public String getDisplayName()
			{
				return getName();
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

			@Override
			public Optional<X509Certificate> getCertificate()
			{
				return Optional.of(certificates[0]);
			}
		};
	}
}
