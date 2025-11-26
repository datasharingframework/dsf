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
package dev.dsf.bpe.v2.client.dsf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BinaryInputStream extends InputStream
{
	public record Range(long size, long start, long end)
	{
	}

	private final InputStream delegate;
	private final long contentLength;
	private final Range range;

	public BinaryInputStream(InputStream delegate, long contentLength, Range range)
	{
		this.delegate = delegate;
		this.contentLength = contentLength;
		this.range = range;
	}

	/**
	 * @return {@link Long#MIN_VALUE} if Content-Length header missing or value not a number
	 */
	public long getContentLength()
	{
		return contentLength;
	}

	/**
	 * @return if partial result <code>not null</code>, otherwise <code>null</code>
	 */
	public Range getRange()
	{
		return range;
	}

	@Override
	public int read() throws IOException
	{
		return delegate.read();
	}

	@Override
	public int read(byte[] b) throws IOException
	{
		return delegate.read(b);
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException
	{
		return delegate.read(b, off, len);
	}

	@Override
	public byte[] readAllBytes() throws IOException
	{
		return delegate.readAllBytes();
	}

	@Override
	public byte[] readNBytes(int len) throws IOException
	{
		return delegate.readNBytes(len);
	}

	@Override
	public int readNBytes(byte[] b, int off, int len) throws IOException
	{
		return delegate.readNBytes(b, off, len);
	}

	@Override
	public long skip(long n) throws IOException
	{
		return delegate.skip(n);
	}

	@Override
	public void skipNBytes(long n) throws IOException
	{
		delegate.skipNBytes(n);
	}

	@Override
	public int available() throws IOException
	{
		return delegate.available();
	}

	@Override
	public void close() throws IOException
	{
		delegate.close();
	}

	@Override
	public void mark(int readlimit)
	{
		delegate.mark(readlimit);
	}

	@Override
	public void reset() throws IOException
	{
		delegate.reset();
	}

	@Override
	public boolean markSupported()
	{
		return delegate.markSupported();
	}

	@Override
	public long transferTo(OutputStream out) throws IOException
	{
		return delegate.transferTo(out);
	}
}
