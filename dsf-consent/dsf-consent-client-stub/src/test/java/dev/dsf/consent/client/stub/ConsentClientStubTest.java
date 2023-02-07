package dev.dsf.consent.client.stub;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.dsf.consent.client.ConsentClient;
import dev.dsf.openehr.json.OpenEhrObjectMapperFactory;
import dev.dsf.openehr.model.structure.ResultSet;

public class ConsentClientStubTest
{
	@Test
	public void testConsentClientStub() throws Exception
	{
		ObjectMapper openEhrObjectMapper = OpenEhrObjectMapperFactory.createObjectMapper();
		ResultSet resultSet = openEhrObjectMapper
				.readValue(Files.readAllBytes(Paths.get("src/test/resources/result.json")), ResultSet.class);

		ConsentClient consentClient = new ConsentClientStubFactory()
				.createClient((String key, String defaultValue) -> defaultValue);

		assertNotNull(consentClient);

		int initialColumnsSize = resultSet.getColumns().size();
		int initialRowsSize = resultSet.getRows().size();

		ResultSet filteredResultSet = consentClient.removeRowsWithoutConsent(resultSet);

		assertNotNull(filteredResultSet);
		assertEquals(initialColumnsSize, filteredResultSet.getColumns().size());
		assertEquals(initialRowsSize, filteredResultSet.getRows().size());
	}
}
