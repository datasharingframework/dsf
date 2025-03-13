package dev.dsf.bpe.v2.client.fhir;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.ProxyAuthenticationStrategy;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.client.apache.ApacheHttpClient;
import ca.uhn.fhir.rest.client.api.Header;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.IHttpClient;
import ca.uhn.fhir.rest.client.api.IRestfulClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.impl.GenericClient;
import ca.uhn.fhir.rest.client.impl.RestfulClientFactory;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import dev.dsf.bpe.v2.client.fhir.ClientConfig.BasicAuthentication;
import dev.dsf.bpe.v2.client.fhir.ClientConfig.BearerAuthentication;
import dev.dsf.bpe.v2.client.fhir.ClientConfig.OidcAuthentication;
import dev.dsf.bpe.v2.client.oidc.OidcClient;
import dev.dsf.bpe.v2.client.oidc.OidcInterceptor;
import dev.dsf.bpe.v2.service.OidcClientProvider;

public class FhirClientFactory extends RestfulClientFactory
{
	private final OidcClientProvider oidcClientProvider;
	private final ClientConfig config;
	private final FhirContext fhirContext;
	private final String userAgent;

	private final AtomicReference<HttpClient> httpClientReference = new AtomicReference<>();

	/**
	 * @param oidcClientProvider
	 *            not <code>null</code>
	 * @param config
	 *            not <code>null</code>
	 * @param fhirContext
	 *            not <code>null</code>
	 * @param userAgent
	 *            not <code>null</code>
	 */
	public FhirClientFactory(OidcClientProvider oidcClientProvider, ClientConfig config, FhirContext fhirContext,
			String userAgent)
	{
		super();

		Objects.requireNonNull(oidcClientProvider, "oidcClientProvider");
		Objects.requireNonNull(config, "config");
		Objects.requireNonNull(fhirContext, "fhirContext");
		Objects.requireNonNull(userAgent, "userAgent");

		this.oidcClientProvider = oidcClientProvider;
		this.config = config;
		this.fhirContext = new FhirContextDelegate(fhirContext);
		this.fhirContext.setRestfulClientFactory(this);
		this.userAgent = userAgent;

		super.setFhirContext(fhirContext);
	}

	@Override
	public IGenericClient newGenericClient(String serverBase)
	{
		return configureClient(new GenericClient(fhirContext, getHttpClient(serverBase), serverBase, this));
	}

	@Override
	public IHttpClient getHttpClient(StringBuilder url, Map<String, List<String>> ifNoneExistParams,
			String ifNoneExistString, RequestTypeEnum requestType, List<Header> headers)
	{
		return new ApacheHttpClient(getHttpClient(), url, ifNoneExistParams, ifNoneExistString, requestType, headers);
	}

	@Override
	protected IHttpClient getHttpClient(String theServerBase)
	{
		return getHttpClient(new StringBuilder(theServerBase), null, null, null, null);
	}

	private HttpClient getHttpClient()
	{
		HttpClient httpClient = httpClientReference.get();
		if (httpClient == null)
		{
			HttpClient c = createHttpClient();
			if (httpClientReference.compareAndSet(httpClient, c))
				return c;
			else
				return httpClientReference.get();
		}
		else
			return httpClient;
	}

	private HttpClient createHttpClient()
	{
		Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory> create()
				.register("http", PlainConnectionSocketFactory.getSocketFactory())
				.register("https", new SSLConnectionSocketFactory(createSslContext())).build();

		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(
				socketFactoryRegistry, null, null, null, 5000, TimeUnit.MILLISECONDS);

		connectionManager.setMaxTotal(getPoolMaxTotal());
		connectionManager.setDefaultMaxPerRoute(getPoolMaxPerRoute());

		Builder requestConfigBuilder = RequestConfig.custom().setSocketTimeout(getSocketTimeout())
				.setConnectTimeout(getConnectTimeout()).setConnectionRequestTimeout(getConnectionRequestTimeout());

		HttpHost proxyHost = config.getProxy() == null ? null : HttpHost.create(config.getProxy().getUrl());
		if (proxyHost != null)
			requestConfigBuilder = requestConfigBuilder.setProxy(proxyHost);

		RequestConfig defaultRequestConfig = requestConfigBuilder.build();

		HttpClientBuilder builder = HttpClients.custom().setConnectionManager(connectionManager)
				.setSSLContext(createSslContext()).setDefaultRequestConfig(defaultRequestConfig)
				.disableCookieManagement().setUserAgent(userAgent);

		if (proxyHost != null && config.getProxy().getUsername() != null && !config.getProxy().getUsername().isBlank()
				&& config.getProxy().getPassword() != null)
		{
			CredentialsProvider credsProvider = new BasicCredentialsProvider();
			credsProvider.setCredentials(new AuthScope(proxyHost.getHostName(), proxyHost.getPort()),
					new UsernamePasswordCredentials(config.getProxy().getUsername(),
							String.valueOf(config.getProxy().getPassword())));
			builder.setProxyAuthenticationStrategy(new ProxyAuthenticationStrategy());
			builder.setDefaultCredentialsProvider(credsProvider);
		}

		return builder.build();
	}

	private IGenericClient configureClient(IGenericClient client)
	{
		BasicAuthentication basic = config.getBasicAuthentication();
		if (basic != null)
			client.registerInterceptor(
					new BasicAuthInterceptor(basic.getUsername(), String.valueOf(basic.getPassword())));

		BearerAuthentication bearer = config.getBearerAuthentication();
		if (bearer != null)
			client.registerInterceptor(new BearerTokenAuthInterceptor(String.valueOf(bearer.getToken())));

		OidcAuthentication oidc = config.getOidcAuthentication();
		if (oidc != null)
		{
			OidcClient oidcClient = oidcClientProvider.getOidcClient(oidc);
			client.registerInterceptor(new OidcInterceptor(oidcClient));
		}

		if (config.getEnableDebugLogging())
			client.registerInterceptor(new LoggingInterceptor(config));

		return client;
	}

	private SSLContext createSslContext()
	{
		try
		{
			TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init(config.getTrustStore());

			KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());

			if (config.getCertificateAuthentication() != null)
				kmf.init(config.getCertificateAuthentication().getKeyStore(),
						config.getCertificateAuthentication().getKeyStorePassword());
			else
				kmf.init(null, null);

			SSLContext sc = SSLContext.getInstance("TLS");
			sc.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

			return sc;
		}
		catch (UnrecoverableKeyException | KeyManagementException | NoSuchAlgorithmException | KeyStoreException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public int getConnectionRequestTimeout()
	{
		return DEFAULT_CONNECTION_REQUEST_TIMEOUT;
	}

	@Override
	public int getConnectTimeout()
	{
		// max value check in yaml validation
		return (int) config.getConnectTimeout().toMillis();
	}

	@Override
	public ServerValidationModeEnum getServerValidationModeEnum()
	{
		return getServerValidationMode();
	}

	@Override
	public ServerValidationModeEnum getServerValidationMode()
	{
		return ServerValidationModeEnum.NEVER;
	}

	@Override
	public int getSocketTimeout()
	{
		// max value check in yaml validation
		return (int) config.getReadTimeout().toMillis();
	}

	@Override
	public int getPoolMaxTotal()
	{
		return DEFAULT_POOL_MAX;
	}

	@Override
	public int getPoolMaxPerRoute()
	{
		return DEFAULT_POOL_MAX_PER_ROUTE;
	}

	private RuntimeException notSupported()
	{
		return new RuntimeException("not supported");
	}

	@Override
	public <T extends IRestfulClient> T newClient(Class<T> theClientType, String theServerBase)
	{
		throw notSupported();
	}

	@Override
	public void setConnectionRequestTimeout(int theConnectionRequestTimeout)
	{
		throw notSupported();
	}

	@Override
	public void setConnectTimeout(int theConnectTimeout)
	{
		throw notSupported();
	}

	@Override
	public <T> void setHttpClient(T theHttpClient)
	{
		throw notSupported();
	}

	@Override
	public void setProxy(String theHost, Integer thePort)
	{
		throw notSupported();
	}

	@Override
	public void setProxyCredentials(String theUsername, String thePassword)
	{
		throw notSupported();
	}

	@Override
	public void setServerValidationModeEnum(ServerValidationModeEnum theServerValidationMode)
	{
		throw notSupported();
	}

	@Override
	public void setServerValidationMode(ServerValidationModeEnum theServerValidationMode)
	{
		throw notSupported();
	}

	@Override
	public void setSocketTimeout(int theSocketTimeout)
	{
		throw notSupported();
	}

	@Override
	public void setPoolMaxTotal(int thePoolMaxTotal)
	{
		throw notSupported();
	}

	@Override
	public void setPoolMaxPerRoute(int thePoolMaxPerRoute)
	{
		throw notSupported();
	}

	@Override
	public void validateServerBase(String theServerBase, IHttpClient theHttpClient, IRestfulClient theClient)
	{
		// do nothing
	}

	@Override
	public void validateServerBaseIfConfiguredToDoSo(String theServerBase, IHttpClient theHttpClient,
			IRestfulClient theClient)
	{
		// do nothing
	}

	@Override
	protected void resetHttpClient()
	{
		throw notSupported();
	}
}
