package dev.dsf.bpe.v2.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface MimetypeService
{
	Logger logger = LoggerFactory.getLogger(MimetypeService.class);

	record ValidationResult(String declaredBaseType, String declaredSubType, String detectedBaseType,
			String detectedSubType)
	{
		public String declared()
		{
			return declaredBaseType + "/" + declaredSubType;
		}

		public String detected()
		{
			return detectedBaseType + "/" + detectedSubType;
		}

		public boolean mimetypesMatch()
		{
			return declared().equals(detected());
		}
	}

	/**
	 * Detects the mimetype of the provided byte array and validates if the detected mimetype equals the declared
	 * mimetype. Returns a {@link ValidationResult} containing both the declared and detected MIME types. This result
	 * can be used to drive custom logic based on whether the detected type matches the declared type.
	 *
	 * @param stream
	 *            input stream of which the mimetype should be detected
	 * @param declared
	 *            the declared mimetype of the data
	 * @return {@link ValidationResult} containing the declared and detected mimetypes.
	 */
	ValidationResult validateWithResult(InputStream stream, String declared);

	/**
	 * Detects the mimetype of the provided byte array and validates if the detected mimetype equals the declared
	 * mimetype. Returns a {@link ValidationResult} containing both the declared and detected MIME types. This result
	 * can be used to drive custom logic based on whether the detected type matches the declared type.
	 *
	 * @param data
	 *            byte array of which the mimetype should be detected
	 * @param declared
	 *            the declared mimetype of the data
	 * @return {@link ValidationResult} containing the declared and detected mimetypes.
	 */
	default ValidationResult validateWithResult(byte[] data, String declared)
	{
		return validateWithResult(new ByteArrayInputStream(data), declared);
	}

	/**
	 * Detects the mimetype of the provided byte array and validates if the detected mimetype equals the declared
	 * mimetype. Returns <code>true</code> if the full mimetype matches, <code>false</code> otherwise.
	 *
	 * @param stream
	 *            input stream of which the mimetype should be detected
	 * @param declared
	 *            the declared mimetype of the data
	 * @return <code>true</code> if the full mimetype matches, <code>false</code> otherwise
	 */
	default boolean validateWithBoolean(InputStream stream, String declared)
	{
		return validateWithResult(stream, declared).mimetypesMatch();
	}

	/**
	 * Detects the mimetype of the provided byte array and validates if the detected mimetype equals the declared
	 * mimetype. Returns <code>true</code> if the full mimetype matches, <code>false</code> otherwise.
	 *
	 * @param data
	 *            byte array of which the mimetype should be detected
	 * @param declared
	 *            the declared mimetype of the data
	 * @return <code>true</code> if the full mimetype matches, <code>false</code> otherwise
	 */
	default boolean validateWithBoolean(byte[] data, String declared)
	{
		return validateWithResult(new ByteArrayInputStream(data), declared).mimetypesMatch();
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
	default void validateWithException(InputStream stream, String declared)
	{
		ValidationResult result = validateWithResult(stream, declared);

		if (!result.mimetypesMatch())
			logger.warn("Declared full mimetype {} does not match detected full mimetype {}", result.declared(),
					result.detected());

		if (!result.declaredBaseType().equals(result.detectedBaseType()))
		{
			throw new RuntimeException("Declared base mimetype of '" + result.declared()
					+ "' does not match detected base mimetype of '" + result.detected() + "'");
		}
	}

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
	default void validateWithException(byte[] data, String declared)
	{
		validateWithException(new ByteArrayInputStream(data), declared);
	}
}
