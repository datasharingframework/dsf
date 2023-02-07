package dev.dsf.openehr.client.impl;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.dsf.openehr.client.OpenEhrClient;
import dev.dsf.openehr.json.OpenEhrObjectMapperFactory;
import dev.dsf.openehr.model.datatypes.JsonNodeRowElement;
import dev.dsf.openehr.model.structure.ResultSet;

public class TestOpenEhrClientJersey
{
	public static void main(String... args) throws Exception
	{
		OpenEhrClient client = new OpenEhrClientJersey("http://localhost:8003/rest/openehr/v1", "username", "password",
				"truststore.pem", 2000, 10000, objectMapper());

		String query = "SELECT e FROM EHR e";
		ResultSet resultSet = client.query(query, null);
		JsonNodeRowElement result = (JsonNodeRowElement) resultSet.getRow(0).get(0);
		System.out.println(result.getValue());
	}

	private static ObjectMapper objectMapper()
	{
		return OpenEhrObjectMapperFactory.createObjectMapper();
	}
}

