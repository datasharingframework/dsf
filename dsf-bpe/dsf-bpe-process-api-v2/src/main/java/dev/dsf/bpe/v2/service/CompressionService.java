package dev.dsf.bpe.v2.service;

import java.io.IOException;
import java.io.InputStream;

public interface CompressionService
{
	/**
	 * @param in
	 *            stream to compress, not <code>null</code>
	 * @return gzip compressed stream
	 */
	InputStream toGzip(InputStream in) throws IOException;

	/**
	 * Uses blockSize 9.
	 *
	 * @param in
	 *            stream to compress, not <code>null</code>
	 * @return bzip2 compressed stream
	 * @throws IOException
	 *             if the stream content is malformed or an I/O error occurs
	 * @see #toBzip2(InputStream, int)
	 */
	InputStream toBzip2(InputStream in) throws IOException;

	/**
	 * @param in
	 *            stream to compress, not <code>null</code>
	 * @param blockSize
	 *            1-9 (100k units)
	 * @return bzip2 compressed stream
	 * @throws IOException
	 *             if the stream content is malformed or an I/O error occurs
	 */
	InputStream toBzip2(InputStream in, int blockSize) throws IOException;

	/**
	 * Uses preset 6.
	 *
	 * @param in
	 *            stream to compress, not <code>null</code>
	 * @return lzma2 compressed stream
	 * @see #toLzma2(InputStream, int)
	 */
	InputStream toLzma2(InputStream in) throws IOException;

	/**
	 * <i>From XZ Java Library:</i>
	 * <p>
	 * The presets 0-3 are fast presets with medium compression. The presets 4-6 are fairly slow presets with high
	 * compression. The default preset is 6.
	 * <p>
	 * The presets 7-9 are like the preset 6 but use bigger dictionaries and have higher compressor and decompressor
	 * memory requirements. Unless the uncompressed size of the file exceeds 8&nbsp;MiB, 16&nbsp;MiB, or 32&nbsp;MiB, it
	 * is waste of memory to use the presets 7, 8, or 9, respectively.
	 *
	 * @param in
	 *            stream to compress, not <code>null</code>
	 * @param preset
	 *            0-9
	 * @return lzma2 compressed stream
	 */
	InputStream toLzma2(InputStream in, int preset) throws IOException;

	/**
	 * @param in
	 *            gzip compressed stream, not <code>null</code>
	 * @return uncompressed stream
	 */
	InputStream fromGzip(InputStream in) throws IOException;

	/**
	 * @param in
	 *            bzip2 compressed stream, not <code>null</code>
	 * @return uncompressed stream
	 */
	InputStream fromBzip2(InputStream in) throws IOException;

	/**
	 * @param in
	 *            lzma2 compressed stream, not <code>null</code>
	 * @return uncompressed stream
	 * @throws IOException
	 */
	InputStream fromLzma2(InputStream in) throws IOException;
}
