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
package dev.dsf.bpe.v2.service;

import org.hl7.fhir.r4.model.Resource;

/**
 * Logs data to the <code>dsf-data-logger</code> if enabled via the environment variables
 * <code>DEV_DSF_LOG_DATA_FILE_ENABLED: true</code>, <code>DEV_DSF_LOG_DATA_CONSOLE_OUT_ENABLED: true</code> or
 * <code>DEV_DSF_LOG_DATA_CONSOLE_ERR_ENABLED: true</code> (properties: <code>dev.dsf.log.data.file.enabled=true</code>,
 * <code>dev.dsf.log.data.console.out.enabled=true</code> or <code>dev.dsf.log.data.console.err.enabled=true</code>)
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
