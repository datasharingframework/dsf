package dev.dsf.bpe.test.service;

import static dev.dsf.bpe.test.PluginTestExecutor.expectFalse;
import static dev.dsf.bpe.test.PluginTestExecutor.expectNotNull;
import static dev.dsf.bpe.test.PluginTestExecutor.expectNull;
import static dev.dsf.bpe.test.PluginTestExecutor.expectSame;
import static dev.dsf.bpe.test.PluginTestExecutor.expectTrue;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.ArrayList;
import java.util.Collections;

import dev.dsf.bpe.test.AbstractTest;
import dev.dsf.bpe.test.PluginTest;
import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.ServiceTask;
import dev.dsf.bpe.v2.error.ErrorBoundaryEvent;
import dev.dsf.bpe.v2.service.FhirClientConfigProvider;
import dev.dsf.bpe.v2.variables.Variables;

public class FhirClientConfigProviderTest extends AbstractTest implements ServiceTask
{
	@Override
	public void execute(ProcessPluginApi api, Variables variables) throws ErrorBoundaryEvent, Exception
	{
		executeTests(api, variables, api.getFhirClientConfigProvider());
	}

	@PluginTest
	public void getFhirClientProviderNotNull(ProcessPluginApi api) throws Exception
	{
		expectNotNull(api.getFhirClientConfigProvider());
	}

	@PluginTest
	public void getClientConfigDsfFhirServer(FhirClientConfigProvider configProvider) throws Exception
	{
		expectNotNull(configProvider.getClientConfig("dsf-fhir-server"));
		expectTrue(configProvider.getClientConfig("dsf-fhir-server").isPresent());

		configProvider.getClientConfig("dsf-fhir-server").ifPresent(c ->
		{
			expectNotNull(c.getBaseUrl());
			expectNull(c.getBasicAuthentication());
			expectNull(c.getBearerAuthentication());
			expectNotNull(c.getCertificateAuthentication());
			expectNotNull(c.getCertificateAuthentication().getKeyStore());
			expectNotNull(c.getCertificateAuthentication().getKeyStorePassword());
			expectNotNull(c.getConnectTimeout());
			expectFalse(c.isDebugLoggingEnabled());
			expectSame("dsf-fhir-server", c.getFhirServerId());
			expectNull(c.getOidcAuthentication());
			expectNull(c.getProxy());
			expectNotNull(c.getReadTimeout());
			expectTrue(c.isStartupConnectionTestEnabled());
			expectNotNull(c.getTrustStore());

			try
			{
				expectSame(1, Collections.list(c.getTrustStore().aliases()).size());
			}
			catch (KeyStoreException e)
			{
				throw new RuntimeException(e);
			}
		});
	}

	@PluginTest
	public void getClientConfigDsfFhirServerViaEndpointIdentifier(FhirClientConfigProvider configProvider)
			throws Exception
	{
		expectNotNull(configProvider.getClientConfig("#Test_Endpoint"));
		expectTrue(configProvider.getClientConfig("#Test_Endpoint").isPresent());

		configProvider.getClientConfig("#Test_Endpoint").ifPresent(c ->
		{
			expectNotNull(c.getBaseUrl());
			expectNull(c.getBasicAuthentication());
			expectNull(c.getBearerAuthentication());
			expectNotNull(c.getCertificateAuthentication());
			expectNotNull(c.getCertificateAuthentication().getKeyStore());
			expectNotNull(c.getCertificateAuthentication().getKeyStorePassword());
			expectNotNull(c.getConnectTimeout());
			expectFalse(c.isDebugLoggingEnabled());
			expectSame("#Test_Endpoint", c.getFhirServerId());
			expectNull(c.getOidcAuthentication());
			expectNull(c.getProxy());
			expectNotNull(c.getReadTimeout());
			expectFalse(c.isStartupConnectionTestEnabled());
			expectNotNull(c.getTrustStore());

			try
			{
				expectSame(1, Collections.list(c.getTrustStore().aliases()).size());
			}
			catch (KeyStoreException e)
			{
				throw new RuntimeException(e);
			}
		});
	}

	@PluginTest
	public void getClientConfigDsfFhirServerViaLocal(FhirClientConfigProvider configProvider) throws Exception
	{
		expectNotNull(configProvider.getClientConfig("#local"));
		expectTrue(configProvider.getClientConfig("#local").isPresent());

		configProvider.getClientConfig("#local").ifPresent(c ->
		{
			expectNotNull(c.getBaseUrl());
			expectNull(c.getBasicAuthentication());
			expectNull(c.getBearerAuthentication());
			expectNotNull(c.getCertificateAuthentication());
			expectNotNull(c.getCertificateAuthentication().getKeyStore());
			expectNotNull(c.getCertificateAuthentication().getKeyStorePassword());
			expectNotNull(c.getConnectTimeout());
			expectFalse(c.isDebugLoggingEnabled());
			expectSame("#local", c.getFhirServerId());
			expectNull(c.getOidcAuthentication());
			expectNull(c.getProxy());
			expectNotNull(c.getReadTimeout());
			expectFalse(c.isStartupConnectionTestEnabled());
			expectNotNull(c.getTrustStore());

			try
			{
				expectSame(1, Collections.list(c.getTrustStore().aliases()).size());
			}
			catch (KeyStoreException e)
			{
				throw new RuntimeException(e);
			}
		});
	}

	@PluginTest
	public void getClientConfigViaProxy(FhirClientConfigProvider configProvider) throws Exception
	{
		expectNotNull(configProvider.getClientConfig("via-proxy"));
		expectTrue(configProvider.getClientConfig("via-proxy").isPresent());

		configProvider.getClientConfig("via-proxy").ifPresent(c ->
		{
			expectSame("http://via.proxy/fhir", c.getBaseUrl());
			expectNull(c.getBasicAuthentication());
			expectNull(c.getBearerAuthentication());
			expectNull(c.getCertificateAuthentication());
			expectNotNull(c.getConnectTimeout());
			expectFalse(c.isDebugLoggingEnabled());
			expectSame("via-proxy", c.getFhirServerId());
			expectNull(c.getOidcAuthentication());
			expectNotNull(c.getProxy());
			expectSame("proxy_password".toCharArray(), c.getProxy().getPassword());
			expectSame("http://proxy:8080", c.getProxy().getUrl());
			expectSame("proxy_username", c.getProxy().getUsername());
			expectNotNull(c.getReadTimeout());
			expectFalse(c.isStartupConnectionTestEnabled());
			expectNotNull(c.getTrustStore());

			try
			{
				expectSame(1, Collections.list(c.getTrustStore().aliases()).size());
			}
			catch (KeyStoreException e)
			{
				throw new RuntimeException(e);
			}
		});
	}

	@PluginTest
	public void getClientConfigWithNotConfiguredServerId(FhirClientConfigProvider configProvider) throws Exception
	{
		expectNotNull(configProvider.getClientConfig("not-configured"));
		expectFalse(configProvider.getClientConfig("not-configured").isPresent());
	}

	@PluginTest
	public void getDefaultTrustStore(FhirClientConfigProvider configProvider) throws Exception
	{
		KeyStore trustStore = configProvider.createDefaultTrustStore();

		expectNotNull(trustStore);
		ArrayList<String> aliases = Collections.list(trustStore.aliases());
		expectSame(1, aliases.size());
		expectNotNull(trustStore.getCertificate(aliases.get(0)));
	}

	@PluginTest
	public void getDefaultSslContext(FhirClientConfigProvider configProvider) throws Exception
	{
		expectNotNull(configProvider.createDefaultSslContext());
	}
}
