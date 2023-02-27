package dev.dsf.openehr.client.stub;

import java.util.function.BiFunction;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.dsf.openehr.client.OpenEhrClient;
import dev.dsf.openehr.client.OpenEhrClientFactory;
import dev.dsf.openehr.json.OpenEhrObjectMapperFactory;

public class OpenEhrClientStubFactory implements OpenEhrClientFactory
{
	@Override
	public OpenEhrClient createClient(BiFunction<String, String, String> propertyResolver)
	{
		ObjectMapper openEhrObjectMapper = OpenEhrObjectMapperFactory.createObjectMapper();
		return new OpenEhrClientStub(openEhrObjectMapper);
	}
}
