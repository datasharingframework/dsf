package dev.dsf.pseudonymization.client.stub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.openehr.model.structure.ResultSet;
import dev.dsf.pseudonymization.client.PseudonymizationClient;

public class PseudonymizationClientStub implements PseudonymizationClient
{
	private static final Logger logger = LoggerFactory.getLogger(PseudonymizationClientStub.class);

	@Override
	public ResultSet pseudonymize(ResultSet resultSet)
	{
		logger.warn("No pseudonymization applied, ResultSet will be returned as provided");

		return resultSet;
	}
}
