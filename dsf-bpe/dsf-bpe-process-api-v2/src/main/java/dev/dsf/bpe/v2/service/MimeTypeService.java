package dev.dsf.bpe.v2.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface MimeTypeService
{
	Logger logger = LoggerFactory.getLogger(MimeTypeService.class);

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

		public boolean mimeTypesMatch()
		{
			return declared().equals(detected());
		}
	}

	/**
	 * Detects the MIME type of the provided byte array and validates if the detected MIME type equals the declared MIME
	 * type. Returns a {@link ValidationResult} containing both the declared and detected MIME types. This result can be
	 * used to drive custom logic based on whether the detected type matches the declared type.
	 *
	 * @param stream
	 *            input stream of which the MIME type should be detected
	 * @param declared
	 *            the declared MIME type of the data, e.g. <code>"application/pdf"</code>
	 * @return {@link ValidationResult} containing the declared and detected MIME types.
	 */
	ValidationResult validateWithResult(InputStream stream, String declared);

	/**
	 * Detects the MIME type of the provided byte array and validates if the detected MIME type equals the declared MIME
	 * type. Returns a {@link ValidationResult} containing both the declared and detected MIME types. This result can be
	 * used to drive custom logic based on whether the detected type matches the declared type.
	 *
	 * @param data
	 *            byte array of which the MIME type should be detected
	 * @param declared
	 *            the declared MIME type of the data, e.g. <code>"application/pdf"</code>
	 * @return {@link ValidationResult} containing the declared and detected MIME types.
	 */
	default ValidationResult validateWithResult(byte[] data, String declared)
	{
		return validateWithResult(new ByteArrayInputStream(data), declared);
	}

	/**
	 * Detects the MIME type of the provided byte array and validates if the detected MIME type equals the declared MIME
	 * type. Returns <code>true</code> if the full MIME type matches, <code>false</code> otherwise.
	 *
	 * @param stream
	 *            input stream of which the MIME type should be detected
	 * @param declared
	 *            the declared MIME type of the data, e.g. <code>"application/pdf"</code>
	 * @return <code>true</code> if the full MIME type matches, <code>false</code> otherwise
	 */
	default boolean validateWithBoolean(InputStream stream, String declared)
	{
		return validateWithResult(stream, declared).mimeTypesMatch();
	}

	/**
	 * Detects the MIME type of the provided byte array and validates if the detected MIME type equals the declared MIME
	 * type. Returns <code>true</code> if the full MIME type matches, <code>false</code> otherwise.
	 *
	 * @param data
	 *            byte array of which the MIME type should be detected
	 * @param declared
	 *            the declared MIME type of the data, e.g. <code>"application/pdf"</code>
	 * @return <code>true</code> if the full MIME type matches, <code>false</code> otherwise
	 */
	default boolean validateWithBoolean(byte[] data, String declared)
	{
		return validateWithResult(new ByteArrayInputStream(data), declared).mimeTypesMatch();
	}

	/**
	 * Detects the MIME type of the provided input stream and validates if the detected MIME type equals the declared
	 * MIME type. Logs a warning if the full MIME types do not match, throws a {@link RuntimeException} if the base MIME
	 * types do not match.
	 *
	 * @param stream
	 *            input stream of which the MIME type should be detected
	 * @param declared
	 *            the declared MIME type of the data, e.g. <code>"application/pdf"</code>
	 * @throws RuntimeException
	 *             if the detected and the declared base MIME type do not match
	 */
	default void validateWithException(InputStream stream, String declared)
	{
		ValidationResult result = validateWithResult(stream, declared);

		if (!result.mimeTypesMatch())
			logger.warn("Declared full MIME type {} does not match detected full MIME type {}", result.declared(),
					result.detected());

		if (!result.declaredBaseType().equals(result.detectedBaseType()))
		{
			throw new RuntimeException("Declared base MIME type of '" + result.declared()
					+ "' does not match detected base MIME type of '" + result.detected() + "'");
		}
	}

	/**
	 * Detects the MIME type of the provided byte array and validates if the detected MIME type equals the declared MIME
	 * type. Logs a warning if the full MIME types do not match, throws a {@link RuntimeException} if the base MIME
	 * types do not match.
	 *
	 * @param data
	 *            byte array of which the MIME type should be detected
	 * @param declared
	 *            the declared MIME type of the data, e.g. <code>"application/pdf"</code>
	 * @throws RuntimeException
	 *             if the detected and the declared base MIME type do not match
	 */
	default void validateWithException(byte[] data, String declared)
	{
		validateWithException(new ByteArrayInputStream(data), declared);
	}
}
