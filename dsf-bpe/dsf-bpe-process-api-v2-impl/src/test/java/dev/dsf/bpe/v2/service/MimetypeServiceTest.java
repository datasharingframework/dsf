package dev.dsf.bpe.v2.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.bpe.v2.service.detector.CombinedDetectors;
import dev.dsf.bpe.v2.service.detector.NdJsonDetector;
import dev.dsf.bpe.v2.variables.JsonVariableSerializationTest;

public class MimetypeServiceTest
{
	private static final Logger logger = LoggerFactory.getLogger(JsonVariableSerializationTest.class);

	private final MimetypeService mimetypeService = new MimetypeServiceImpl(
			CombinedDetectors.withDefaultAndNdJson(NdJsonDetector.DEFAULT_LINES_TO_CHECK));

	@Test
	public void testCsv()
	{
		String content = """
				Apple;Banana;Cherry;Date;Elderberry;Fig;Grape;Honeydew;Indian Fig;Jackfruit
				Kiwi;Lemon;Mango;Nectarine;Orange;Papaya;Quince;Raspberry;Strawberry;Tomato
				Ugli Fruit;Vanilla Bean;Watermelon;Xigua;Yellow Passionfruit;Zucchini;Apricot;Blackberry;Cantaloupe;Dragonfruit
				Eggplant;Feijoa;Grapefruit;Hackberry;Ilama;Jujube;Kumquat;Lime;Mulberry;Naranjilla
				Olive;Peach;Queen Anne Cherry;Rambutan;Soursop;Tamarind;Uva;Voavanga;White Currant;Ziziphus
				""";

		test(content.getBytes(), "text/csv");
	}

	@Test
	public void testNdJson()
	{
		String content = """
				{"name": "Apple", "color": "Red", "type": "Pome"}
				{"name": "Banana", "color": "Yellow", "type": "Berry"}
				{"name": "Cherry", "color": "Red", "type": "Drupe"}
				{"name": "Mango", "color": "Orange", "type": "Drupe"}
				{"name": "Blueberry", "color": "Blue", "type": "Berry"}
				""";

		test(content.getBytes(), "application/x-ndjson");
	}

	@Test
	public void testBundle()
	{
		String content = """
					<Bundle xmlns="http://hl7.org/fhir">
					  <type value="collection"/>
					  <entry>
					    <resource>
					      <Patient>
					        <id value="example"/>
					        <name>
					          <use value="official"/>
					          <family value="Doe"/>
					          <given value="John"/>
					        </name>
					        <gender value="male"/>
					        <birthDate value="1990-01-01"/>
					      </Patient>
					    </resource>
					  </entry>
					</Bundle>
				""";

		test(content.getBytes(), "application/fhir+xml");
	}

	@Test
	public void testZip()
	{
		String csv = """
				Apple;Banana;Cherry;Date;Elderberry;Fig;Grape;Honeydew;Indian Fig;Jackfruit
				Kiwi;Lemon;Mango;Nectarine;Orange;Papaya;Quince;Raspberry;Strawberry;Tomato
				Ugli Fruit;Vanilla Bean;Watermelon;Xigua;Yellow Passionfruit;Zucchini;Apricot;Blackberry;Cantaloupe;Dragonfruit
				Eggplant;Feijoa;Grapefruit;Hackberry;Ilama;Jujube;Kumquat;Lime;Mulberry;Naranjilla
				Olive;Peach;Queen Anne Cherry;Rambutan;Soursop;Tamarind;Uva;Voavanga;White Currant;Ziziphus
				""";

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (ZipOutputStream file = new ZipOutputStream(out))
		{
			file.putNextEntry(new ZipEntry("fruits.csv"));
			file.write(csv.getBytes());
			file.closeEntry();
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}

		byte[] content = out.toByteArray();
		test(content, "application/zip");
	}

	private void test(byte[] content, String mimeType)
	{
		InputStream contentStream = new ByteArrayInputStream(content);

		String[] split = mimeType.split("/");
		String baseType = split[0];
		String subType = split[1];

		MimetypeService.ValidationResult validationResult = mimetypeService.validateWithResult(contentStream, mimeType);

		assertEquals(baseType, validationResult.detectedBaseType());
		assertEquals(subType, validationResult.detectedSubType());
		assertEquals(mimeType, validationResult.declared());
		assertEquals(mimeType, validationResult.detected());
		assertTrue(validationResult.mimetypesMatch());

		boolean booleanResult = mimetypeService.validateWithBoolean(contentStream, mimeType);
		assertTrue(booleanResult);

		try
		{
			mimetypeService.validateWithException(contentStream, mimeType);
		}
		catch (Exception e)
		{
			logger.info("Validation failed - {}", e.getMessage());
			fail();
		}
	}
}
