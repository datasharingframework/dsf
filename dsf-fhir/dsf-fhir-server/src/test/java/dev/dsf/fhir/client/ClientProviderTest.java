package dev.dsf.fhir.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.security.KeyStore;
import java.time.Duration;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.common.build.BuildInfoReader;
import dev.dsf.common.config.ProxyConfigImpl;
import dev.dsf.fhir.dao.EndpointDao;
import dev.dsf.fhir.function.SupplierWithSqlException;
import dev.dsf.fhir.help.ExceptionHandler;
import dev.dsf.fhir.service.ReferenceCleaner;

public class ClientProviderTest
{
	private ReferenceCleaner referenceCleaner;
	private EndpointDao endpointDao;
	private ExceptionHandler exceptionHandler;
	private ClientProvider provider;
	private BuildInfoReader buildInfoReader;

	@Before
	public void before() throws Exception
	{
		KeyStore webserviceKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		webserviceKeyStore.load(null);

		KeyStore webserviceTrustStore = KeyStore.getInstance(KeyStore.getDefaultType());
		webserviceTrustStore.load(null);

		char[] webserviceKeyStorePassword = "password".toCharArray();
		Duration remoteReadTimeout = Duration.ZERO;
		Duration remoteConnectTimeout = Duration.ZERO;
		char[] remoteProxyPassword = null;
		String remoteProxyUsername = null;
		String remoteProxySchemeHostPort = null;
		boolean logRequests = false;
		FhirContext fhirContext = mock(FhirContext.class);
		referenceCleaner = mock(ReferenceCleaner.class);
		endpointDao = mock(EndpointDao.class);
		exceptionHandler = mock(ExceptionHandler.class);
		buildInfoReader = mock(BuildInfoReader.class);

		provider = new ClientProviderImpl(webserviceTrustStore, webserviceKeyStore, webserviceKeyStorePassword,
				remoteReadTimeout, remoteConnectTimeout,
				new ProxyConfigImpl(remoteProxySchemeHostPort, remoteProxyUsername, remoteProxyPassword, null),
				logRequests, fhirContext, referenceCleaner, endpointDao, exceptionHandler, buildInfoReader);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testGetClientExisting() throws Exception
	{
		final String serverBase = "http://foo/fhir/";

		when(buildInfoReader.getProjectVersion()).thenReturn("1.2.3-TEST");
		when(exceptionHandler.handleSqlException(any(SupplierWithSqlException.class))).thenReturn(true);

		Optional<FhirWebserviceClient> client = provider.getClient(serverBase);
		assertNotNull(client);
		assertTrue(client.isPresent());
		assertEquals(serverBase, client.get().getBaseUrl());

		verify(buildInfoReader).getProjectVersion();
		verify(exceptionHandler).handleSqlException(any(SupplierWithSqlException.class));
		verifyNoMoreInteractions(referenceCleaner, endpointDao, exceptionHandler, buildInfoReader);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testGetClientExistingNotFound() throws Exception
	{
		when(exceptionHandler.handleSqlException(any(SupplierWithSqlException.class))).thenReturn(false);

		Optional<FhirWebserviceClient> client = provider.getClient("http://does.not/exists/");
		assertNotNull(client);
		assertTrue(client.isEmpty());

		verify(exceptionHandler).handleSqlException(any(SupplierWithSqlException.class));
		verifyNoMoreInteractions(referenceCleaner, endpointDao, exceptionHandler, buildInfoReader);
	}
}
