package dev.dsf.pseudonymization.psn;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;

import dev.dsf.pseudonymization.recordlinkage.MedicId;
import dev.dsf.pseudonymization.recordlinkage.TestMedicId;

public class PseudonyWithPaddingJsonTest
{
	private static final Logger logger = LoggerFactory.getLogger(PseudonyWithPaddingJsonTest.class);

	@Test
	public void testWriteRead() throws Exception
	{
		MedicId medicId1 = new TestMedicId("org1", "value1");
		MedicId medicId2 = new TestMedicId("org2", "value2");
		PseudonymWithPadding p = new PseudonymWithPadding("", Arrays.asList(medicId1, medicId2));

		ObjectMapper o = new ObjectMapper();
		o.registerSubtypes(new NamedType(TestMedicId.class, "TestMedicId"));

		String string = o.writeValueAsString(p);
		logger.debug(string);

		PseudonymWithPadding read = o.readValue(string, PseudonymWithPadding.class);
		assertNotNull(read);
	}
}
