package dev.dsf.bpe.v2.service;

import org.hl7.fhir.r4.model.Resource;

/**
 * Logs data to the <code>log/bpe-data.log</code> file if enabled via environment variable
 * (<code>DEV_DSF_LOG_DATA: true</code>) or property (<code>dev.dsf.log.data=true</code>)
 */
public interface DataLogger
{
	/**
	 * If data logging is enabled, logs message and object with debug level. The FHIR resource is serialized as json.
	 * Does nothing if the given <b>message</b> is <code>null</code>.
	 *
	 * @param message
	 *            not <code>null</code>
	 * @param resource
	 *            may be <code>null</code>
	 * @see #isEnabled()
	 */
	void log(String message, Resource resource);

	/**
	 * If data logging is enabled, logs message and object with debug level. The object is serialized by calling
	 * {@link String#valueOf(Object)}. Does nothing if the given <b>message</b> is <code>null</code>.
	 *
	 * @param message
	 *            not <code>null</code>
	 * @param object
	 *            may be <code>null</code>
	 * @see #isEnabled()
	 */
	void log(String message, Object object);

	/**
	 * @return <code>true</code> if data logging is enabled
	 */
	boolean isEnabled();
}
