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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * An InputStream wrapper that exposes at most a configured number of bytes from the underlying stream.
 *
 * <p>
 * Unlike a regular end-of-stream condition, exhausting the configured limit is treated as a resource limit violation.
 * Once the configured limit has been exhausted, every subsequent call to {@link #read()} or {@link #skip(long)} throws
 * {@link IOException}, regardless of whether the underlying stream has reached end-of-stream.
 * </p>
 *
 * <p>
 * If the underlying stream supports mark/reset, this stream supports it as well. The remaining byte limit is restored
 * when {@link #reset()} is called.
 * </p>
 *
 * This class is not safe for concurrent use by multiple threads.
 */
public final class LimitedInputStream extends FilterInputStream
{
	private final long maxBytes;

	private long remainingBytes;
	private long markedRemaining;

	/**
	 * Creates a stream that allows reading at most {@code maxBytes} bytes.
	 *
	 * @param in
	 *            the underlying input stream
	 * @param maxBytes
	 *            the maximum number of bytes that may be read
	 * @throws NullPointerException
	 *             if {@code in} is null
	 * @throws IllegalArgumentException
	 *             if {@code maxBytes} is negative
	 */
	public LimitedInputStream(InputStream in, long maxBytes)
	{
		super(Objects.requireNonNull(in, "in"));

		if (maxBytes < 0)
			throw new IllegalArgumentException("maxBytes must be >= 0");

		this.maxBytes = maxBytes;

		this.remainingBytes = maxBytes;
		this.markedRemaining = maxBytes;
	}

	/**
	 * Returns the number of bytes that may still be consumed.
	 *
	 * @return remaining byte allowance
	 */
	public long getRemainingBytes()
	{
		return remainingBytes;
	}

	@Override
	public int read() throws IOException
	{
		ensureRemaining();

		int b = super.read();
		if (b != -1)
			remainingBytes--;

		return b;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException
	{
		Objects.checkFromIndexSize(off, len, b.length);

		if (len == 0)
			return 0;

		ensureRemaining();

		int allowed = (int) Math.min(len, remainingBytes);
		int count = super.read(b, off, allowed);

		if (count > 0)
			remainingBytes -= count;

		return count;
	}

	/**
	 * Skipping bytes beyond the configured limit is treated as an attempt to consume data beyond the allowed resource
	 * boundary.
	 *
	 * @throws IOException
	 *             if the limit has already been exhausted
	 */
	@Override
	public long skip(long n) throws IOException
	{
		if (n <= 0)
			return 0;

		ensureRemaining();

		long skipped = super.skip(Math.min(n, remainingBytes));
		remainingBytes -= skipped;

		return skipped;
	}

	/**
	 * Returns the smaller of the underlying available bytes and the remaining byte allowance. This value represents
	 * bytes available within this wrapper's limit, not necessarily bytes immediately readable.
	 */
	@Override
	public int available() throws IOException
	{
		return (int) Math.min(super.available(), remainingBytes);
	}

	@Override
	public synchronized void mark(int readlimit)
	{
		if (markSupported())
		{
			super.mark(readlimit);
			markedRemaining = remainingBytes;
		}
	}

	@Override
	public synchronized void reset() throws IOException
	{
		super.reset();
		remainingBytes = markedRemaining;
	}

	private void ensureRemaining() throws IOException
	{
		if (remainingBytes == 0)
			throw new IOException("Stream limit of " + maxBytes + " byte" + (maxBytes != 1 ? "s" : "") + " exceeded");
	}
}