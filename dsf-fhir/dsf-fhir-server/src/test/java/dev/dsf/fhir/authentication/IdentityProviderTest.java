package dev.dsf.fhir.authentication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Practitioner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import de.rwh.utils.crypto.CertificateAuthority;
import de.rwh.utils.crypto.CertificateHelper;
import de.rwh.utils.crypto.CertificationRequestBuilder;
import dev.dsf.common.auth.DsfOpenIdCredentials;
import dev.dsf.common.auth.conf.Identity;
import dev.dsf.common.auth.conf.IdentityProvider;
import dev.dsf.common.auth.conf.OrganizationIdentity;
import dev.dsf.common.auth.conf.PractitionerIdentity;
import dev.dsf.common.auth.conf.RoleConfig;

public class IdentityProviderTest
{
	private static final String LOCAL_ORGANIZATION_NAME = "Local Organization";
	private static final String LOCAL_ORGANIZATION_COMMON_NAME = "local.org";
	private static final String LOCAL_ORGANIZATION_IDENTIFIER_VALUE = "id.local.org";
	private static final String REMOTE_ORGANIZATION_NAME = "Remote Organization";
	private static final String REMOTE_ORGANIZATION_COMMON_NAME = "remote.org";
	private static final String REMOTE_ORGANIZATION_IDENTIFIER_VALUE = "id.remote.org";
	private static final String LOCAL_PRACTITIONER_NAME_GIVEN = "Tyler";
	private static final String LOCAL_PRACTITIONER_NAME_FAMILY = "Tester";
	private static final String LOCAL_PRACTITIONER_COMMON_NAME = "Tyler Tester";
	private static final String LOCAL_PRACTITIONER_MAIL = "tyler.tester@local.org";

	private static final Coding PRACTIONER_ROLE1 = new Coding()
			.setSystem("http://local.org/CodeSystem/PractitionerRoles").setCode("pr_role1");
	private static final Coding PRACTIONER_ROLE2 = new Coding()
			.setSystem("http://local.org/CodeSystem/PractitionerRoles").setCode("pr_role2");
	private static final Coding PRACTIONER_ROLE3 = new Coding()
			.setSystem("http://local.org/CodeSystem/PractitionerRoles").setCode("pr_role3");
	private static final Coding PRACTIONER_ROLE4 = new Coding()
			.setSystem("http://local.org/CodeSystem/PractitionerRoles").setCode("pr_role4");
	private static final String TOKEN_ROLE1 = "t_role1";
	private static final String TOKEN_ROLE2 = "t_role2";
	private static final String TOKEN_ROLE2_CLIENT = "t_role2_client";
	private static final String TOKEN_GROUP = "group";

	private static final CertificateAuthority CA;

	private static final Organization LOCAL_ORGANIZATION = new Organization();
	private static final X509Certificate LOCAL_ORGANIZATION_CERTIFICATE;
	private static final Organization REMOTE_ORGANIZATION = new Organization();
	private static final X509Certificate REMOTE_ORGANIZATION_CERTIFICATE;
	private static final Practitioner LOCAL_PRACTITIONER = new Practitioner();
	private static final X509Certificate LOCAL_PRACTITIONER_CERTIFICATE;
	private static final String LOCAL_PRACTITIONER_CERTIFICATE_THUMBPRINT;

	static
	{
		CertificateAuthority.registerBouncyCastleProvider();

		try
		{
			CA = new CertificateAuthority("DE", null, null, null, null, "CA");
			CA.initialize(LocalDateTime.now(), LocalDateTime.now().plusDays(1), 1024,
					CertificateHelper.DEFAULT_SIGNATURE_ALGORITHM);

			X500Name localOrgSubject = CertificationRequestBuilder.createSubject("DE", null, null, null, null,
					LOCAL_ORGANIZATION_COMMON_NAME);
			KeyPair localOrgKeyPair = CertificateHelper.createKeyPair(CertificateHelper.DEFAULT_KEY_ALGORITHM, 1024);
			JcaPKCS10CertificationRequest localOrgReq = CertificationRequestBuilder
					.createClientCertificationRequest(localOrgSubject, localOrgKeyPair, "email@local.org");
			LOCAL_ORGANIZATION_CERTIFICATE = CA.signWebClientCertificate(localOrgReq);

			X500Name remoteOrgSubject = CertificationRequestBuilder.createSubject("DE", null, null, null, null,
					REMOTE_ORGANIZATION_COMMON_NAME);
			KeyPair remoteOrgKeyPair = CertificateHelper.createKeyPair(CertificateHelper.DEFAULT_KEY_ALGORITHM, 1024);
			JcaPKCS10CertificationRequest remoteOrgReq = CertificationRequestBuilder
					.createClientCertificationRequest(remoteOrgSubject, remoteOrgKeyPair, "email@remote.org");
			REMOTE_ORGANIZATION_CERTIFICATE = CA.signWebClientCertificate(remoteOrgReq);

			X500Name localPractitionerSubject = CertificationRequestBuilder.createSubject("DE", null, null, null, null,
					LOCAL_PRACTITIONER_COMMON_NAME);
			KeyPair localPractitionerKeyPair = CertificateHelper.createKeyPair(CertificateHelper.DEFAULT_KEY_ALGORITHM,
					1024);
			JcaPKCS10CertificationRequest localPractitionerReq = CertificationRequestBuilder
					.createClientCertificationRequest(localPractitionerSubject, localPractitionerKeyPair,
							LOCAL_PRACTITIONER_MAIL);
			LOCAL_PRACTITIONER_CERTIFICATE = CA.signWebClientCertificate(localPractitionerReq);
			LOCAL_PRACTITIONER_CERTIFICATE_THUMBPRINT = Hex.encodeHexString(
					MessageDigest.getInstance("SHA-512").digest(LOCAL_PRACTITIONER_CERTIFICATE.getEncoded()));

		}
		catch (InvalidKeyException | NoSuchAlgorithmException | KeyStoreException | CertificateException | IOException
				| OperatorCreationException | IllegalStateException | InvalidKeySpecException e)
		{
			throw new RuntimeException(e);
		}

		LOCAL_ORGANIZATION.addIdentifier().setSystem(OrganizationProvider.ORGANIZATION_IDENTIFIER_SYSTEM)
				.setValue(LOCAL_ORGANIZATION_IDENTIFIER_VALUE);
		LOCAL_ORGANIZATION.setName(LOCAL_ORGANIZATION_NAME);

		REMOTE_ORGANIZATION.addIdentifier().setSystem(OrganizationProvider.ORGANIZATION_IDENTIFIER_SYSTEM)
				.setValue(REMOTE_ORGANIZATION_IDENTIFIER_VALUE);
		REMOTE_ORGANIZATION.setName(REMOTE_ORGANIZATION_NAME);

		LOCAL_PRACTITIONER.addIdentifier().setSystem(PractitionerProvider.PRACTITIONER_IDENTIFIER_SYSTEM)
				.setValue(LOCAL_PRACTITIONER_MAIL);
		LOCAL_PRACTITIONER.addName().setFamily(LOCAL_PRACTITIONER_NAME_FAMILY).addGiven(LOCAL_PRACTITIONER_NAME_GIVEN);
	}


	private OrganizationProvider organizationProvider;
	private PractitionerProvider practitionerProvider;
	private RoleConfig roleConfig;
	private DsfOpenIdCredentials credentials;

	private IdentityProvider provider;

	@Before
	public void before() throws Exception
	{
		organizationProvider = mock(OrganizationProvider.class);
		practitionerProvider = mock(PractitionerProvider.class);
		roleConfig = mock(RoleConfig.class);
		credentials = mock(DsfOpenIdCredentials.class);

		provider = new IdentityProviderImpl(organizationProvider, practitionerProvider,
				LOCAL_ORGANIZATION_IDENTIFIER_VALUE, roleConfig);
	}

	@After
	public void after() throws Exception
	{
		verifyNoMoreInteractions(organizationProvider, practitionerProvider, roleConfig);
	}

	@Test
	public void testGetOrganizationIdentityByX509CertificateNull() throws Exception
	{
		Identity i = provider.getIdentity((X509Certificate[]) null);
		assertNull(i);
	}

	@Test
	public void testGetOrganizationIdentityByX509CertificateEmpty() throws Exception
	{
		Identity i = provider.getIdentity(new X509Certificate[0]);
		assertNull(i);
	}

	@Test
	public void testGetOrganizationIdentityByX509CertificateLocalOrganization() throws Exception
	{
		when(organizationProvider.getOrganization(LOCAL_ORGANIZATION_CERTIFICATE)).thenReturn(Optional.of(LOCAL_ORGANIZATION));

		Identity i = provider.getIdentity(new X509Certificate[] { LOCAL_ORGANIZATION_CERTIFICATE });
		assertNotNull(i);
		assertTrue(i instanceof OrganizationIdentity);

		OrganizationIdentity orgI = (OrganizationIdentity) i;
		assertNotNull(orgI.getCertificate());
		assertTrue(orgI.getCertificate().isPresent());
		assertEquals(LOCAL_ORGANIZATION_CERTIFICATE, orgI.getCertificate().get());
		assertEquals(LOCAL_ORGANIZATION_NAME, orgI.getDisplayName());
		assertEquals(FhirServerRole.LOCAL_ORGANIZATION, orgI.getDsfRoles());
		assertEquals(LOCAL_ORGANIZATION_IDENTIFIER_VALUE, orgI.getName());
		assertEquals(LOCAL_ORGANIZATION, orgI.getOrganization());
		assertEquals(LOCAL_ORGANIZATION_IDENTIFIER_VALUE, orgI.getOrganizationIdentifierValue());

		ArgumentCaptor<X509Certificate> cArg1 = ArgumentCaptor.forClass(X509Certificate.class);
		verify(organizationProvider).getOrganization(cArg1.capture());

		assertEquals(LOCAL_ORGANIZATION_CERTIFICATE, cArg1.getValue());
	}

	@Test
	public void testGetOrganizationIdentityByX509CertificateRemoteOrganization() throws Exception
	{
		when(organizationProvider.getOrganization(REMOTE_ORGANIZATION_CERTIFICATE)).thenReturn(Optional.of(REMOTE_ORGANIZATION));

		Identity i = provider.getIdentity(new X509Certificate[] { REMOTE_ORGANIZATION_CERTIFICATE });
		assertNotNull(i);
		assertTrue(i instanceof OrganizationIdentity);

		OrganizationIdentity orgI = (OrganizationIdentity) i;
		assertNotNull(orgI.getCertificate());
		assertTrue(orgI.getCertificate().isPresent());
		assertEquals(REMOTE_ORGANIZATION_CERTIFICATE, orgI.getCertificate().get());
		assertEquals(REMOTE_ORGANIZATION_NAME, orgI.getDisplayName());
		assertEquals(FhirServerRole.REMOTE_ORGANIZATION, orgI.getDsfRoles());
		assertEquals(REMOTE_ORGANIZATION_IDENTIFIER_VALUE, orgI.getName());
		assertEquals(REMOTE_ORGANIZATION, orgI.getOrganization());
		assertEquals(REMOTE_ORGANIZATION_IDENTIFIER_VALUE, orgI.getOrganizationIdentifierValue());

		ArgumentCaptor<X509Certificate> cArg1 = ArgumentCaptor.forClass(X509Certificate.class);
		verify(organizationProvider).getOrganization(cArg1.capture());

		assertEquals(REMOTE_ORGANIZATION_CERTIFICATE, cArg1.getValue());
	}

	@Test
	public void testGetOrganizationIdentityByX509CertificateUnknownOrganization() throws Exception
	{
		when(organizationProvider.getOrganization(REMOTE_ORGANIZATION_CERTIFICATE)).thenReturn(Optional.empty());
		when(practitionerProvider.getPractitioner(REMOTE_ORGANIZATION_CERTIFICATE)).thenReturn(Optional.empty());
		when(organizationProvider.getLocalOrganization()).thenReturn(Optional.of(LOCAL_ORGANIZATION));

		Identity i = provider.getIdentity(new X509Certificate[] { REMOTE_ORGANIZATION_CERTIFICATE });
		assertNull(i);

		ArgumentCaptor<X509Certificate> cArg1 = ArgumentCaptor.forClass(X509Certificate.class);
		verify(organizationProvider).getOrganization(cArg1.capture());
		ArgumentCaptor<X509Certificate> cArg2 = ArgumentCaptor.forClass(X509Certificate.class);
		verify(practitionerProvider).getPractitioner(cArg2.capture());
		verify(organizationProvider).getLocalOrganization();

		assertEquals(REMOTE_ORGANIZATION_CERTIFICATE, cArg1.getValue());
		assertEquals(REMOTE_ORGANIZATION_CERTIFICATE, cArg2.getValue());
	}

	@Test
	public void testGetPractitionerIdentityByX509Certificate() throws Exception
	{
		when(organizationProvider.getOrganization(LOCAL_ORGANIZATION_CERTIFICATE)).thenReturn(Optional.empty());
		when(practitionerProvider.getPractitioner(LOCAL_PRACTITIONER_CERTIFICATE)).thenReturn(Optional.of(LOCAL_PRACTITIONER));
		when(organizationProvider.getLocalOrganization()).thenReturn(Optional.of(LOCAL_ORGANIZATION));
		when(roleConfig.getDsfRolesForEmail(LOCAL_PRACTITIONER_MAIL)).thenReturn(Collections.singletonList(FhirServerRole.CREATE));
		when(roleConfig.getDsfRolesForThumbprint(LOCAL_PRACTITIONER_CERTIFICATE_THUMBPRINT)).thenReturn(Collections.singletonList(FhirServerRole.DELETE));
		when(roleConfig.getPractitionerRolesForEmail(LOCAL_PRACTITIONER_MAIL)).thenReturn(Collections.singletonList(PRACTIONER_ROLE1));
		when(roleConfig.getPractitionerRolesForThumbprint(LOCAL_PRACTITIONER_CERTIFICATE_THUMBPRINT)).thenReturn(Collections.singletonList(PRACTIONER_ROLE2));

		Identity i = provider.getIdentity(new X509Certificate[] { LOCAL_PRACTITIONER_CERTIFICATE });
		assertNotNull(i);
		assertTrue(i instanceof PractitionerIdentity);

		PractitionerIdentity practitionerI = (PractitionerIdentity) i;
		assertNotNull(practitionerI.getCertificate());
		assertTrue(practitionerI.getCertificate().isPresent());
		assertEquals(LOCAL_PRACTITIONER_CERTIFICATE, practitionerI.getCertificate().get());
		assertNotNull(practitionerI.getCredentials());
		assertTrue(practitionerI.getCredentials().isEmpty());
		assertEquals(LOCAL_PRACTITIONER_NAME_GIVEN + " " + LOCAL_PRACTITIONER_NAME_FAMILY, practitionerI.getDisplayName());
		assertEquals(EnumSet.of(FhirServerRole.CREATE, FhirServerRole.DELETE), practitionerI.getDsfRoles());
		assertEquals(LOCAL_ORGANIZATION_IDENTIFIER_VALUE + "/" + LOCAL_PRACTITIONER_MAIL, practitionerI.getName());
		assertEquals(LOCAL_ORGANIZATION, practitionerI.getOrganization());
		assertEquals(LOCAL_ORGANIZATION_IDENTIFIER_VALUE, practitionerI.getOrganizationIdentifierValue());
		assertEquals(Set.of(PRACTIONER_ROLE1, PRACTIONER_ROLE2), practitionerI.getPractionerRoles());
		assertEquals(LOCAL_PRACTITIONER, practitionerI.getPractitioner());

		ArgumentCaptor<X509Certificate> cArg1 = ArgumentCaptor.forClass(X509Certificate.class);
		verify(organizationProvider).getOrganization(cArg1.capture());
		ArgumentCaptor<X509Certificate> cArg2 = ArgumentCaptor.forClass(X509Certificate.class);
		verify(practitionerProvider).getPractitioner(cArg2.capture());
		verify(organizationProvider).getLocalOrganization();

		ArgumentCaptor<String> mArg1 = ArgumentCaptor.forClass(String.class);
		verify(roleConfig).getDsfRolesForEmail(mArg1.capture());
		ArgumentCaptor<String> tArg1 = ArgumentCaptor.forClass(String.class);
		verify(roleConfig).getDsfRolesForThumbprint(tArg1.capture());
		ArgumentCaptor<String> mArg2 = ArgumentCaptor.forClass(String.class);
		verify(roleConfig).getPractitionerRolesForEmail(mArg2.capture());
		ArgumentCaptor<String> tArg2 = ArgumentCaptor.forClass(String.class);
		verify(roleConfig).getPractitionerRolesForThumbprint(tArg2.capture());

		assertEquals(LOCAL_PRACTITIONER_CERTIFICATE, cArg1.getValue());
		assertEquals(LOCAL_PRACTITIONER_CERTIFICATE, cArg2.getValue());
		assertEquals(LOCAL_PRACTITIONER_MAIL, mArg1.getValue());
		assertEquals(LOCAL_PRACTITIONER_MAIL, mArg2.getValue());
		assertEquals(LOCAL_PRACTITIONER_CERTIFICATE_THUMBPRINT, tArg1.getValue());
		assertEquals(LOCAL_PRACTITIONER_CERTIFICATE_THUMBPRINT, tArg2.getValue());
	}

	@Test
	public void testGetPractitionerIdentityByX509CertificateNoLocalOrganization() throws Exception
	{
		when(organizationProvider.getOrganization(LOCAL_ORGANIZATION_CERTIFICATE)).thenReturn(Optional.empty());
		when(practitionerProvider.getPractitioner(LOCAL_PRACTITIONER_CERTIFICATE)).thenReturn(Optional.of(LOCAL_PRACTITIONER));
		when(organizationProvider.getLocalOrganization()).thenReturn(Optional.empty());

		Identity i = provider.getIdentity(new X509Certificate[] { LOCAL_PRACTITIONER_CERTIFICATE });
		assertNull(i);

		ArgumentCaptor<X509Certificate> cArg1 = ArgumentCaptor.forClass(X509Certificate.class);
		verify(organizationProvider).getOrganization(cArg1.capture());
		ArgumentCaptor<X509Certificate> cArg2 = ArgumentCaptor.forClass(X509Certificate.class);
		verify(practitionerProvider).getPractitioner(cArg2.capture());
		verify(organizationProvider).getLocalOrganization();

		assertEquals(LOCAL_PRACTITIONER_CERTIFICATE, cArg1.getValue());
		assertEquals(LOCAL_PRACTITIONER_CERTIFICATE, cArg2.getValue());
	}

	@Test
	public void testGetPractitionerIdentityByOpenIdCredentialsNull() throws Exception
	{
		Identity i = provider.getIdentity((DsfOpenIdCredentials) null);
		assertNull(i);
	}

	@Test
	public void testGetPractitionerIdentityByOpenIdCredentials() throws Exception
	{
		when(practitionerProvider.getPractitioner(credentials)).thenReturn(Optional.of(LOCAL_PRACTITIONER));
		when(organizationProvider.getLocalOrganization()).thenReturn(Optional.of(LOCAL_ORGANIZATION));

		when(credentials.getIdToken()).thenReturn(Map.of("realm_access", Map.of("roles", new String[] {TOKEN_ROLE1})));
		when(credentials.getAccessToken()).thenReturn(Map.of("resource_access",Map.of(TOKEN_ROLE2_CLIENT,  Map.of("roles", new String[] {TOKEN_ROLE2})), "groups", new String[] {TOKEN_GROUP}));

		when(roleConfig.getDsfRolesForEmail(LOCAL_PRACTITIONER_MAIL)).thenReturn(Collections.singletonList(FhirServerRole.CREATE));
		when(roleConfig.getDsfRolesForTokenRole(TOKEN_ROLE1)).thenReturn(Collections.singletonList(FhirServerRole.DELETE));
		when(roleConfig.getDsfRolesForTokenRole(TOKEN_ROLE2_CLIENT + "." + TOKEN_ROLE2)).thenReturn(Collections.singletonList(FhirServerRole.HISTORY));
		when(roleConfig.getDsfRolesForTokenGroup(TOKEN_GROUP)).thenReturn(Collections.singletonList(FhirServerRole.PERMANENT_DELETE));

		when(roleConfig.getPractitionerRolesForEmail(LOCAL_PRACTITIONER_MAIL)).thenReturn(Collections.singletonList(PRACTIONER_ROLE1));
		when(roleConfig.getPractitionerRolesForTokenRole(TOKEN_ROLE1)).thenReturn(Collections.singletonList(PRACTIONER_ROLE2));
		when(roleConfig.getPractitionerRolesForTokenRole(TOKEN_ROLE2_CLIENT + "." + TOKEN_ROLE2)).thenReturn(Collections.singletonList(PRACTIONER_ROLE3));
		when(roleConfig.getPractitionerRolesForTokenGroup(TOKEN_GROUP)).thenReturn(Collections.singletonList(PRACTIONER_ROLE4));

		Identity i = provider.getIdentity(credentials);
		assertNotNull(i);
		assertTrue(i instanceof PractitionerIdentity);

		PractitionerIdentity practitionerI = (PractitionerIdentity) i;
		assertNotNull(practitionerI.getCertificate());
		assertTrue(practitionerI.getCertificate().isEmpty());
		assertNotNull(practitionerI.getCredentials());
		assertTrue(practitionerI.getCredentials().isPresent());
		assertEquals(credentials, practitionerI.getCredentials().get());
		assertEquals(LOCAL_PRACTITIONER_NAME_GIVEN + " " + LOCAL_PRACTITIONER_NAME_FAMILY, practitionerI.getDisplayName());
		assertEquals(EnumSet.of(FhirServerRole.CREATE, FhirServerRole.DELETE, FhirServerRole.HISTORY, FhirServerRole.PERMANENT_DELETE), practitionerI.getDsfRoles());
		assertEquals(LOCAL_ORGANIZATION_IDENTIFIER_VALUE + "/" + LOCAL_PRACTITIONER_MAIL, practitionerI.getName());
		assertEquals(LOCAL_ORGANIZATION, practitionerI.getOrganization());
		assertEquals(LOCAL_ORGANIZATION_IDENTIFIER_VALUE, practitionerI.getOrganizationIdentifierValue());

		assertNotNull(practitionerI.getPractionerRoles());
		Set<Coding> expectedPractitionerRoles = Set.of(PRACTIONER_ROLE1, PRACTIONER_ROLE2, PRACTIONER_ROLE3, PRACTIONER_ROLE4);
		Set<String> expectedRoles = expectedPractitionerRoles.stream().map(c -> c.getSystem() + c.getCode()).collect(Collectors.toSet());
		Set<String> actualRoles = practitionerI.getPractionerRoles().stream().map(c -> c.getSystem() + c.getCode()).collect(Collectors.toSet());
		assertEquals("expected: " + expectedRoles + ", actual: " + actualRoles , expectedPractitionerRoles.size(), practitionerI.getPractionerRoles().size());
		assertEquals(expectedRoles, actualRoles);

		assertEquals(LOCAL_PRACTITIONER, practitionerI.getPractitioner());

		ArgumentCaptor<DsfOpenIdCredentials> cArg2 = ArgumentCaptor.forClass(DsfOpenIdCredentials.class);
		verify(practitionerProvider).getPractitioner(cArg2.capture());
		verify(organizationProvider).getLocalOrganization();

		verify(credentials).getIdToken();
		verify(credentials).getAccessToken();

		ArgumentCaptor<String> mArg1 = ArgumentCaptor.forClass(String.class);
		verify(roleConfig).getDsfRolesForEmail(mArg1.capture());
		ArgumentCaptor<String> rArg1 = ArgumentCaptor.forClass(String.class);
		verify(roleConfig, times(2)).getDsfRolesForTokenRole(rArg1.capture());
		ArgumentCaptor<String> gArg1 = ArgumentCaptor.forClass(String.class);
		verify(roleConfig).getDsfRolesForTokenGroup(gArg1.capture());

		ArgumentCaptor<String> mArg2 = ArgumentCaptor.forClass(String.class);
		verify(roleConfig).getPractitionerRolesForEmail(mArg2.capture());
		ArgumentCaptor<String> rArg2 = ArgumentCaptor.forClass(String.class);
		verify(roleConfig, times(2)).getPractitionerRolesForTokenRole(rArg2.capture());
		ArgumentCaptor<String> gArg2 = ArgumentCaptor.forClass(String.class);
		verify(roleConfig).getPractitionerRolesForTokenGroup(gArg2.capture());

		assertEquals(credentials, cArg2.getValue());
		assertEquals(LOCAL_PRACTITIONER_MAIL, mArg1.getValue());
		assertEquals(List.of(TOKEN_ROLE1, TOKEN_ROLE2_CLIENT + "." + TOKEN_ROLE2), rArg1.getAllValues());
		assertEquals(TOKEN_GROUP, gArg1.getValue());
		assertEquals(LOCAL_PRACTITIONER_MAIL, mArg2.getValue());
		assertEquals(List.of(TOKEN_ROLE1, TOKEN_ROLE2_CLIENT + "." + TOKEN_ROLE2), rArg2.getAllValues());
		assertEquals(TOKEN_GROUP, gArg2.getValue());
	}

	@Test
	public void testGetPractitionerIdentityByOpenIdCredentialsUnknownPractitioner() throws Exception
	{
		when(practitionerProvider.getPractitioner(credentials)).thenReturn(Optional.empty());
		when(organizationProvider.getLocalOrganization()).thenReturn(Optional.of(LOCAL_ORGANIZATION));

		Identity i = provider.getIdentity(credentials);
		assertNull(i);

		ArgumentCaptor<DsfOpenIdCredentials> cArg2 = ArgumentCaptor.forClass(DsfOpenIdCredentials.class);
		verify(practitionerProvider).getPractitioner(cArg2.capture());
		verify(organizationProvider).getLocalOrganization();

		assertEquals(credentials, cArg2.getValue());
	}

	@Test
	public void testGetPractitionerIdentityByOpenIdCredentialsUnknownLocalOrganization() throws Exception
	{
		when(practitionerProvider.getPractitioner(credentials)).thenReturn(Optional.of(LOCAL_PRACTITIONER));
		when(organizationProvider.getLocalOrganization()).thenReturn(Optional.empty());

		Identity i = provider.getIdentity(credentials);
		assertNull(i);

		ArgumentCaptor<DsfOpenIdCredentials> cArg2 = ArgumentCaptor.forClass(DsfOpenIdCredentials.class);
		verify(practitionerProvider).getPractitioner(cArg2.capture());
		verify(organizationProvider).getLocalOrganization();

		assertEquals(credentials, cArg2.getValue());
	}
}
