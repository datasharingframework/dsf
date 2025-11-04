/*
 * Copyright 2018-2025 Heilbronn University of Applied Sciences
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.dsf.bpe.v2.service;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

public class CompressionServiceTest
{
	@FunctionalInterface
	private static interface Converter
	{
		InputStream convert(InputStream i) throws IOException;
	}

	private static final byte[] TEST_DATA_1 = "Hello compression World!".getBytes(StandardCharsets.UTF_8);
	private static final byte[] TEST_DATA_2 = new byte[1_000_000];
	private static final byte[] TEST_DATA_3 = """
			Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nulla a mauris ligula. Maecenas finibus magna mi, at placerat neque sollicitudin et. Nunc iaculis eros sagittis, rutrum lectus quis, eleifend nunc. Interdum et malesuada fames ac ante ipsum primis in faucibus. Aenean quam risus, blandit in mauris eu, imperdiet tristique neque. Curabitur varius nisl risus, mattis dapibus est vulputate quis. Mauris cursus vel justo id consectetur. Vivamus mollis orci sit amet erat sagittis sodales.
			Maecenas interdum erat et ipsum fermentum lobortis. Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Proin et ipsum velit. Donec efficitur commodo ipsum, in condimentum dui facilisis non. Donec urna ipsum, tempus non finibus quis, condimentum at mi. Aliquam at porta ante. Integer luctus nec tellus in ornare. Quisque vel elit posuere, eleifend turpis eget, molestie nulla. Sed volutpat porttitor aliquet. Quisque eu mauris eget nisl porta cursus non sed nisl.
			Mauris mi nisi, pellentesque malesuada magna interdum, pretium feugiat erat. Mauris et velit sed erat efficitur pulvinar a in dui. Mauris id sem purus. In eget efficitur elit. Mauris a eros sodales, interdum libero at, rutrum sapien. Ut vitae nibh at augue mollis hendrerit a ut odio. Phasellus quis diam id est interdum facilisis. Nullam vitae turpis nisl. Pellentesque eu venenatis diam, ac mattis nisl. Maecenas libero dui, volutpat id eros ut, pulvinar fringilla ligula. Nam maximus orci a lectus suscipit ornare. Praesent vehicula, eros ut scelerisque faucibus, sem sapien pellentesque enim, nec suscipit risus lorem eget odio. Vivamus condimentum urna eget urna feugiat ornare. Donec vitae tempor sem. Duis scelerisque ipsum ac nunc elementum maximus id sed turpis.
			Proin finibus sapien lacus, eu iaculis neque tempus sed. Nam aliquam odio quis vestibulum sodales. Nam vehicula dictum libero id venenatis. Duis vulputate ligula in risus ultricies tincidunt. Nulla dignissim augue enim, eget interdum sapien interdum at. Nulla enim ligula, facilisis finibus elit euismod, tempor aliquet ante. Vivamus malesuada quis nisi vitae varius. Maecenas id velit convallis, vehicula nisl et, volutpat dui.
			Pellentesque et dignissim arcu. Cras eleifend egestas tellus, sit amet pellentesque quam vestibulum in. Duis id hendrerit tortor, eget dapibus sapien. Nulla massa tellus, sollicitudin sit amet mauris vitae, iaculis commodo sem. Fusce gravida eros in lorem imperdiet vestibulum. Nullam quis dolor nec est dictum euismod. Aliquam id tempus quam. Phasellus sed fringilla dui, aliquet semper nisi.
			"""
			.getBytes(StandardCharsets.UTF_8);

	private CompressionService compressionService = new CompressionServiceImpl();

	@Test
	public void testGzip() throws Exception
	{
		test(compressionService::toGzip, 44, compressionService::fromGzip, TEST_DATA_1);
		test(compressionService::toGzip, 1003, compressionService::fromGzip, TEST_DATA_2);
		test(compressionService::toGzip, 1120, compressionService::fromGzip, TEST_DATA_3);
	}

	@Test
	public void testBzip2() throws Exception
	{
		test(compressionService::toBzip2, 63, compressionService::fromBzip2, TEST_DATA_1);
		test(compressionService::toBzip2, 48, compressionService::fromBzip2, TEST_DATA_2);
		test(compressionService::toBzip2, 1078, compressionService::fromBzip2, TEST_DATA_3);
	}

	@Test
	public void testLzma2() throws Exception
	{
		test(compressionService::toLzma2, 80, compressionService::fromLzma2, TEST_DATA_1);
		test(compressionService::toLzma2, 276, compressionService::fromLzma2, TEST_DATA_2);
		test(compressionService::toLzma2, 1228, compressionService::fromLzma2, TEST_DATA_3);
	}

	private void test(Converter compressor, int compressedSize, Converter decompressor, byte[] testData)
			throws IOException
	{
		byte[] compressed;
		try (InputStream in = compressor.convert(new ByteArrayInputStream(testData)))
		{
			compressed = in.readAllBytes();
		}

		assertNotNull(compressed);
		assertEquals(compressedSize, compressed.length);

		byte[] uncompressed;
		try (InputStream in = decompressor.convert(new ByteArrayInputStream(compressed)))
		{
			uncompressed = in.readAllBytes();
		}

		assertNotNull(uncompressed);
		assertEquals(testData.length, uncompressed.length);
		assertArrayEquals(testData, uncompressed);
	}
}
