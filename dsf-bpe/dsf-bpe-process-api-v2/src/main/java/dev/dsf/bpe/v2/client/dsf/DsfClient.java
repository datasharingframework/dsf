package dev.dsf.bpe.v2.client.dsf;

public interface DsfClient extends BasicDsfClient, RetryClient<BasicDsfClient>
{
	String getBaseUrl();

	PreferReturnOutcomeWithRetry withOperationOutcomeReturn();

	PreferReturnMinimalWithRetry withMinimalReturn();
}
