package dev.dsf.bpe.test.service;

import static dev.dsf.bpe.test.PluginTestExecutor.expectNotNull;
import static dev.dsf.bpe.test.PluginTestExecutor.expectSame;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import dev.dsf.bpe.test.AbstractTest;
import dev.dsf.bpe.test.PluginTest;
import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.ServiceTask;
import dev.dsf.bpe.v2.error.ErrorBoundaryEvent;
import dev.dsf.bpe.v2.service.CompressionService;
import dev.dsf.bpe.v2.variables.Variables;

public class CompressionServiceTest extends AbstractTest implements ServiceTask
{
	@FunctionalInterface
	private static interface Converter
	{
		InputStream convert(InputStream i) throws IOException;
	}

	private static final byte[] TEST_DATA_1 = "Hello compression World!".getBytes(StandardCharsets.UTF_8);

	@Override
	public void execute(ProcessPluginApi api, Variables variables) throws ErrorBoundaryEvent, Exception
	{
		executeTests(api, variables, api.getCompressionService());
	}

	@PluginTest
	public void testGzip(CompressionService compressionService) throws Exception
	{
		test(compressionService::toGzip, 44, compressionService::fromGzip, TEST_DATA_1);
	}

	@PluginTest
	public void testBzip2(CompressionService compressionService) throws Exception
	{
		test(compressionService::toBzip2, 63, compressionService::fromBzip2, TEST_DATA_1);
	}

	@PluginTest
	public void testLzma2(CompressionService compressionService) throws Exception
	{
		test(compressionService::toLzma2, 80, compressionService::fromLzma2, TEST_DATA_1);
	}

	private void test(Converter compressor, int compressedSize, Converter decompressor, byte[] testData)
			throws IOException
	{
		byte[] compressed;
		try (InputStream in = compressor.convert(new ByteArrayInputStream(testData)))
		{
			compressed = in.readAllBytes();
		}

		expectNotNull(compressed);
		expectSame(compressedSize, compressed.length);

		byte[] uncompressed;
		try (InputStream in = decompressor.convert(new ByteArrayInputStream(compressed)))
		{
			uncompressed = in.readAllBytes();
		}

		expectNotNull(uncompressed);
		expectSame(testData.length, uncompressed.length);
		expectSame(testData, uncompressed);
	}
}
