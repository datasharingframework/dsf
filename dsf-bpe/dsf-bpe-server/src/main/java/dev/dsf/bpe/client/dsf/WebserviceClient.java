package dev.dsf.bpe.client.dsf;

public interface WebserviceClient extends BasicWebserviceClient, RetryClient<BasicWebserviceClient>
{
	PreferReturnMinimalWithRetry withMinimalReturn();
}
