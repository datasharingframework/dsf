package dev.dsf.fhir.client;

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
