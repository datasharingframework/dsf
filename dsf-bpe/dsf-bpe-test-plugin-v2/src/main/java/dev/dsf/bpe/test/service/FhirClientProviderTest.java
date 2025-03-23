package dev.dsf.bpe.test.service;

import static dev.dsf.bpe.test.PluginTestExecutor.expectFalse;
import static dev.dsf.bpe.test.PluginTestExecutor.expectNotNull;
import static dev.dsf.bpe.test.PluginTestExecutor.expectNull;
import static dev.dsf.bpe.test.PluginTestExecutor.expectSame;
import static dev.dsf.bpe.test.PluginTestExecutor.expectTrue;

import java.security.KeyStoreException;
import java.util.Collections;

import org.hl7.fhir.r4.model.CapabilityStatement;

import dev.dsf.bpe.test.PluginTest;
import dev.dsf.bpe.v2.ProcessPluginApi;

public class FhirClientProviderTest extends AbstractTest
{
	public FhirClientProviderTest(ProcessPluginApi api)
	{
		super(api);
	}

	@PluginTest
	public void getFhirClientProviderNotNull() throws Exception
	{
		expectNotNull(api.getFhirClientProvider());
	}

	@PluginTest
	public void getClientConfigDsfFhirServer() throws Exception
	{
		expectNotNull(api.getFhirClientProvider().getClientConfig("dsf-fhir-server"));
		expectTrue(api.getFhirClientProvider().getClientConfig("dsf-fhir-server").isPresent());

		api.getFhirClientProvider().getClientConfig("dsf-fhir-server").ifPresent(c ->
		{
			expectNotNull(c.getBaseUrl());
			expectNull(c.getBasicAuthentication());
			expectNull(c.getBearerAuthentication());
			expectNotNull(c.getCertificateAuthentication());
			expectNotNull(c.getCertificateAuthentication().getKeyStore());
			expectNotNull(c.getCertificateAuthentication().getKeyStorePassword());
			expectNotNull(c.getConnectTimeout());
			expectTrue(c.isDebugLoggingEnabled());
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
	public void getClientConfigDsfFhirServerViaEndpointIdentifier() throws Exception
	{
		expectNotNull(api.getFhirClientProvider().getClientConfig("#Test_Endpoint"));
		expectTrue(api.getFhirClientProvider().getClientConfig("#Test_Endpoint").isPresent());

		api.getFhirClientProvider().getClientConfig("#Test_Endpoint").ifPresent(c ->
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
	public void getClientConfigDsfFhirServerViaLocal() throws Exception
	{
		expectNotNull(api.getFhirClientProvider().getClientConfig("#local"));
		expectTrue(api.getFhirClientProvider().getClientConfig("#local").isPresent());

		api.getFhirClientProvider().getClientConfig("#local").ifPresent(c ->
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
	public void getClientConfigViaProxy() throws Exception
	{
		expectNotNull(api.getFhirClientProvider().getClientConfig("via-proxy"));
		expectTrue(api.getFhirClientProvider().getClientConfig("via-proxy").isPresent());

		api.getFhirClientProvider().getClientConfig("via-proxy").ifPresent(c ->
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
	public void getClientConfigWithNotConfiguredServerId() throws Exception
	{
		expectNotNull(api.getFhirClientProvider().getClientConfig("not-configured"));
		expectFalse(api.getFhirClientProvider().getClientConfig("not-configured").isPresent());
	}

	@PluginTest
	public void getClientWithConfiguredServerId() throws Exception
	{
		expectNotNull(api.getFhirClientProvider().getClient("dsf-fhir-server"));
		expectTrue(api.getFhirClientProvider().getClient("dsf-fhir-server").isPresent());
	}

	@PluginTest
	public void getClientWithNotConfiguredServerId() throws Exception
	{
		expectNotNull(api.getFhirClientProvider().getClient("not-configured"));
		expectFalse(api.getFhirClientProvider().getClient("not-configured").isPresent());
	}

	@PluginTest
	public void getClientConfigTestConnection() throws Exception
	{
		api.getFhirClientProvider().getClient("dsf-fhir-server").ifPresent(client ->
		{
			CapabilityStatement statement = client.capabilities().ofType(CapabilityStatement.class).execute();
			expectNotNull(statement);
			expectSame("Data Sharing Framework", statement.getSoftware().getName());
		});
	}

	@PluginTest
	public void getClientConfigTestConnectionViaEndpointIdentifier() throws Exception
	{
		api.getFhirClientProvider().getClient("#Test_Endpoint").ifPresent(client ->
		{
			CapabilityStatement statement = client.capabilities().ofType(CapabilityStatement.class).execute();
			expectNotNull(statement);
			expectSame("Data Sharing Framework", statement.getSoftware().getName());
		});
	}
}
