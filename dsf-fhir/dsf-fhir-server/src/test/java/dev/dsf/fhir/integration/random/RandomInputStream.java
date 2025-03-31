package dev.dsf.fhir.integration.random;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

public class RandomInputStream extends InputStream
{
	public static RandomInputStream zeros(int length)
	{
		return new RandomInputStream(length, null);
	}

	public static RandomInputStream rand(int length)
	{
		return new RandomInputStream(length, new Random());
	}

	public static final int ONE_GIBIBYTE = (int) Math.pow(2, 30);
	public static final int FIVE_HUNDRED_MEBIBYTE = (int) (Math.pow(2, 20) * 500);

	private final Random random;
	private final int length;
	private int position = 0;

	private RandomInputStream(int length, Random random)
	{
		if (length < 0)
			throw new IllegalArgumentException("length < 0");

		this.length = length;
		this.random = random;
	}

	@Override
	public int read() throws IOException
	{
		if (position >= length)
			return -1;

		position++;

		if (random != null)
			return random.nextInt(256);
		else
			return 0;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException
	{
		if (position >= length)
			return -1;

		int bytesToRead = Math.min(len, length - position);

		byte[] data = new byte[bytesToRead];

		if (random != null)
			random.nextBytes(data);

		System.arraycopy(data, 0, b, off, bytesToRead);

		position += bytesToRead;
		return bytesToRead;
	}

	@Override
	public long skip(long n) throws IOException
	{
		if (n > Integer.MAX_VALUE)
			n = Integer.MAX_VALUE;

		int bytesSkipped = Math.min((int) n, length - position);
		position += bytesSkipped;
		return bytesSkipped;
	}

	@Override
	public int available() throws IOException
	{
		return length - position;
	}
}
