package dev.dsf.consent.client.stub;

import static dev.dsf.consent.client.ConsentClient.EHRID_COLUMN_DEFAULT_NAME;
import static dev.dsf.consent.client.ConsentClient.EHRID_COLUMN_DEFAULT_PATH;

import java.util.function.BiFunction;

import dev.dsf.consent.client.ConsentClient;
import dev.dsf.consent.client.ConsentClientFactory;

public class ConsentClientStubFactory implements ConsentClientFactory
{
	@Override
	public ConsentClient createClient(BiFunction<String, String, String> propertyResolver)
	{
		String ehrIdColumnName = propertyResolver.apply("org.highmed.dsf.bpe.openehr.subject.external.id.name",
				EHRID_COLUMN_DEFAULT_NAME);
		String ehrIdColumnPath = propertyResolver.apply("org.highmed.dsf.bpe.openehr.subject.external.id.path",
				EHRID_COLUMN_DEFAULT_PATH);

		return new ConsentClientStub(ehrIdColumnName, ehrIdColumnPath);
	}
}
