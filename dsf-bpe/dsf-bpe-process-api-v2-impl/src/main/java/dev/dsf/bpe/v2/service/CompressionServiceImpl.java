package dev.dsf.bpe.v2.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZInputStream;
import org.tukaani.xz.XZOutputStream;

public class CompressionServiceImpl implements CompressionService
{
	private static final int BUFFER_SIZE = 8192;

	@FunctionalInterface
	private static interface Compressor
	{
		OutputStream withCompression(OutputStream o) throws IOException;
	}

	@Override
	public InputStream toGzip(InputStream in) throws IOException
	{
		Objects.requireNonNull(in, "in");

		return to(in, GZIPOutputStream::new);
	}

	@Override
	public InputStream toBzip2(InputStream in) throws IOException
	{
		Objects.requireNonNull(in, "in");

		return toBzip2(in, 9);
	}

	@Override
	public InputStream toBzip2(InputStream in, int blockSize) throws IOException
	{
		Objects.requireNonNull(in, "in");
		if (blockSize < 1 || blockSize > 9)
			throw new IllegalArgumentException("blockSize < 1 or > 9");

		return to(in, out -> new BZip2CompressorOutputStream(out, blockSize));
	}

	@Override
	public InputStream toLzma2(InputStream in) throws IOException
	{
		Objects.requireNonNull(in, "in");

		return toLzma2(in, 6);
	}

	@Override
	public InputStream toLzma2(InputStream in, int preset) throws IOException
	{
		Objects.requireNonNull(in, "in");
		if (preset < 0 || preset > 9)
			throw new IllegalArgumentException("preset < 0 or > 9");

		return to(in, out -> new XZOutputStream(out, new LZMA2Options(preset)));
	}

	private InputStream to(InputStream in, Compressor compressor) throws IOException
	{
		PipedOutputStream pipedOut = new PipedOutputStream();
		PipedInputStream pipedIn = new PipedInputStream(pipedOut, BUFFER_SIZE);

		List<IOException> capturedExceptions = new ArrayList<>();
		Thread worker = new Thread(() ->
		{
			try (in; OutputStream out = compressor.withCompression(pipedOut))
			{
				byte[] buffer = new byte[BUFFER_SIZE];
				int bytesRead;
				while ((bytesRead = in.read(buffer)) != -1)
					out.write(buffer, 0, bytesRead);
			}
			catch (IOException e)
			{
				capturedExceptions.add(e);
			}
		});
		worker.setDaemon(true);
		worker.start();

		if (!capturedExceptions.isEmpty())
		{
			IOException e = capturedExceptions.removeFirst();
			capturedExceptions.stream().forEach(e::addSuppressed);
			throw e;
		}

		return pipedIn;
	}

	@Override
	public InputStream fromGzip(InputStream in) throws IOException
	{
		return new GZIPInputStream(in);
	}

	@Override
	public InputStream fromBzip2(InputStream in) throws IOException
	{
		Objects.requireNonNull(in, "in");

		return new BZip2CompressorInputStream(in);
	}

	@Override
	public InputStream fromLzma2(InputStream in) throws IOException
	{
		return new XZInputStream(in);
	}
}
