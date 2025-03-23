package dev.dsf.bpe.v2.client.oidc;

import java.io.IOException;

import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import jakarta.ws.rs.core.HttpHeaders;

public class OidcInterceptor implements IClientInterceptor
{
	private final OidcClient oidcClient;

	public OidcInterceptor(OidcClient oidcClient)
	{
		this.oidcClient = oidcClient;
	}

	@Override
	public void interceptRequest(IHttpRequest request)
	{
		char[] accessToken = oidcClient.getAccessToken();
		request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + String.valueOf(accessToken));
	}

	@Override
	public void interceptResponse(IHttpResponse response) throws IOException
	{
		// nothing to do
	}
}
