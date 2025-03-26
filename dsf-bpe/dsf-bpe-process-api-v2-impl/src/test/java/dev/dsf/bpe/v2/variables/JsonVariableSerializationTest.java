package dev.dsf.bpe.v2.variables;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.fhir.context.FhirContext;

public class JsonVariableSerializationTest
{
	private static final Logger logger = LoggerFactory.getLogger(JsonVariableSerializationTest.class);

	public static record TestPojo(@JsonProperty("test1") String test1, @JsonProperty("test2") String test2)
	{
		@JsonCreator
		public TestPojo(@JsonProperty("test1") String test1, @JsonProperty("test2") String test2)
		{
			this.test1 = test1;
			this.test2 = test2;
		}
	}

	@Test
	public void testReadWrite() throws Exception
	{
		final String testValue1 = "v1";
		final String testValue2 = "v2";

		ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper(FhirContext.forR4());

		JsonVariable createdVariable = new JsonVariable(new TestPojo(testValue1, testValue2));

		String json = objectMapper.writeValueAsString(createdVariable);

		logger.debug("json: {}", json);
		assertNotNull(json);

		JsonVariable readVariable = objectMapper.readValue(json, JsonVariable.class);

		assertNotNull(readVariable);
		assertNotNull(readVariable.getValue());
		assertTrue(readVariable.getValue() instanceof TestPojo);

		TestPojo readPojo = readVariable.getValue();
		assertEquals(testValue1, readPojo.test1());
		assertEquals(testValue2, readPojo.test2());
	}
}
