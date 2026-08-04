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
package dev.dsf.bpe.v2.service.stream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

public class LimitedInputStreamTest
{
	@SuppressWarnings("resource")
	@Test(expected = NullPointerException.class)
	public void constructorRejectsNullInputStream()
	{
		new LimitedInputStream(null, 1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructorRejectsNegativeLimit()
	{
		new LimitedInputStream(new ByteArrayInputStream(new byte[0]), -1);
	}

	@Test
	public void initialRemainingBytesEqualsConfiguredLimit()
	{
		LimitedInputStream in = new LimitedInputStream(new ByteArrayInputStream(new byte[] { 1, 2, 3 }), 2);

		assertEquals(2, in.getRemainingBytes());
	}

	@Test
	public void readSingleBytesConsumesLimit() throws Exception
	{
		LimitedInputStream in = new LimitedInputStream(new ByteArrayInputStream(new byte[] { 10, 20, 30 }), 2);

		assertEquals(10, in.read());
		assertEquals(1, in.getRemainingBytes());

		assertEquals(20, in.read());
		assertEquals(0, in.getRemainingBytes());

		try
		{
			in.read();
			fail("Expected IOException");
		}
		catch (IOException e)
		{
			assertEquals("Stream limit of 2 bytes exceeded", e.getMessage());
		}
	}

	@Test
	public void readReturnsEndOfStreamWithoutConsumingRemainingLimit() throws Exception
	{
		LimitedInputStream in = new LimitedInputStream(new ByteArrayInputStream(new byte[] { 1 }), 5);

		assertEquals(1, in.read());
		assertEquals(4, in.getRemainingBytes());

		assertEquals(-1, in.read());
		assertEquals(4, in.getRemainingBytes());

		assertEquals(-1, in.read());
		assertEquals(4, in.getRemainingBytes());
	}

	@Test
	public void bulkReadRespectsConfiguredLimit() throws Exception
	{
		LimitedInputStream in = new LimitedInputStream(new ByteArrayInputStream(new byte[] { 1, 2, 3, 4, 5 }), 3);

		byte[] buffer = new byte[10];

		int count = in.read(buffer, 2, 5);

		assertEquals(3, count);
		assertArrayEquals(new byte[] { 0, 0, 1, 2, 3, 0, 0, 0, 0, 0 }, buffer);

		assertEquals(0, in.getRemainingBytes());

		try
		{
			in.read(buffer, 0, 1);
			fail("Expected IOException");
		}
		catch (IOException e)
		{
			assertEquals("Stream limit of 3 bytes exceeded", e.getMessage());
		}
	}

	@Test
	public void zeroLengthReadReturnsZeroWithoutCheckingLimit() throws Exception
	{
		LimitedInputStream in = new LimitedInputStream(new ByteArrayInputStream(new byte[] { 1 }), 0);

		byte[] buffer = new byte[4];

		assertEquals(0, in.read(buffer, 0, 0));
		assertEquals(0, in.getRemainingBytes());
	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void bulkReadValidatesRange() throws Exception
	{
		LimitedInputStream in = new LimitedInputStream(new ByteArrayInputStream(new byte[1]), 1);

		in.read(new byte[2], 1, 2);
	}

	@Test
	public void skipConsumesRemainingLimit() throws Exception
	{
		LimitedInputStream in = new LimitedInputStream(new ByteArrayInputStream(new byte[] { 1, 2, 3, 4 }), 3);

		assertEquals(2, in.skip(2));
		assertEquals(1, in.getRemainingBytes());

		assertEquals(1, in.skip(5));
		assertEquals(0, in.getRemainingBytes());

		try
		{
			in.skip(1);
			fail("Expected IOException");
		}
		catch (IOException e)
		{
			assertEquals("Stream limit of 3 bytes exceeded", e.getMessage());
		}
	}

	@Test
	public void skipWithNonPositiveValueReturnsZero() throws Exception
	{
		LimitedInputStream in = new LimitedInputStream(new ByteArrayInputStream(new byte[] { 1, 2, 3 }), 3);

		assertEquals(0, in.skip(0));
		assertEquals(0, in.skip(-5));
		assertEquals(3, in.getRemainingBytes());
	}

	@Test
	public void availableIsLimitedByRemainingBytes() throws Exception
	{
		LimitedInputStream in = new LimitedInputStream(new ByteArrayInputStream(new byte[] { 1, 2, 3, 4, 5 }), 3);

		assertEquals(3, in.available());

		in.read();

		assertEquals(2, in.available());
	}

	@Test
	public void markAndResetRestoreRemainingBytes() throws Exception
	{
		ByteArrayInputStream source = new ByteArrayInputStream(new byte[] { 1, 2, 3, 4 });

		LimitedInputStream in = new LimitedInputStream(source, 4);

		assertTrue(in.markSupported());

		in.read();
		assertEquals(3, in.getRemainingBytes());

		in.mark(10);

		in.read();
		in.read();

		assertEquals(1, in.getRemainingBytes());

		in.reset();

		assertEquals(3, in.getRemainingBytes());
		assertEquals(2, in.read());
		assertEquals(2, in.getRemainingBytes());
	}

	@Test
	public void markDoesNothingWhenUnderlyingStreamDoesNotSupportMark() throws Exception
	{
		InputStream source = new InputStream()
		{
			private final byte[] data = { 1, 2, 3 };
			private int index;

			@Override
			public int read()
			{
				return index < data.length ? data[index++] : -1;
			}

			@Override
			public boolean markSupported()
			{
				return false;
			}
		};

		@SuppressWarnings("resource")
		LimitedInputStream in = new LimitedInputStream(source, 3);

		assertFalse(in.markSupported());

		in.read();
		assertEquals(2, in.getRemainingBytes());

		in.mark(100);

		try
		{
			in.reset();
			fail("Expected IOException");
		}
		catch (IOException expected)
		{
			// Expected from FilterInputStream/InputStream because mark is unsupported.
		}
	}

	@Test
	public void exceptionMessageUsesSingularForOneByteLimit() throws Exception
	{
		LimitedInputStream in = new LimitedInputStream(new ByteArrayInputStream(new byte[] { 1 }), 1);

		assertEquals(1, in.read());

		try
		{
			in.read();
			fail("Expected IOException");
		}
		catch (IOException e)
		{
			assertEquals("Stream limit of 1 byte exceeded", e.getMessage());
		}
	}
}
