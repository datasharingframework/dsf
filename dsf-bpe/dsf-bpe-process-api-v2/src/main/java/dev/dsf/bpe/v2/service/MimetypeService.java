package dev.dsf.bpe.v2.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public interface MimetypeService
{
	/**
	 * Detects the mimetype of the provided byte array and validates if the detected mimetype equals the declared
	 * mimetype. Logs a warning if the full mimetypes do not match, throws a {@link RuntimeException} if the base
	 * mimetypes do not match.
	 *
	 * @param data
	 *            byte array of which the mimetype should be detected
	 * @param declared
	 *            the declared mimetype of the data
	 * @throws RuntimeException
	 *             if the detected and the declared base mimetype do not match
	 */
	default void validate(byte[] data, String declared)
	{
		validate(new ByteArrayInputStream(data), declared);
	}

	/**
	 * Detects the mimetype of the provided input stream and validates if the detected mimetype equals the declared
	 * mimetype. Logs a warning if the full mimetypes do not match, throws a {@link RuntimeException} if the base
	 * mimetypes do not match.
	 *
	 * @param stream
	 *            input stream of which the mimetype should be detected
	 * @param declared
	 *            the declared mimetype of the data
	 * @throws RuntimeException
	 *             if the detected and the declared base mimetype do not match
	 */
	void validate(InputStream stream, String declared);
}
