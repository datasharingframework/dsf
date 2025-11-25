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
package dev.dsf.bpe.v2.error;

import java.util.Objects;

public class ErrorBoundaryEvent extends RuntimeException
{
	private static final long serialVersionUID = 3161271266680097207L;

	private final String errorCode;
	private final String errorMessage;

	/**
	 * @param errorCode
	 *            not <code>null</code>, not empty
	 * @param errorMessage
	 *            not <code>null</code>, not empty
	 */
	public ErrorBoundaryEvent(String errorCode, String errorMessage)
	{
		this.errorCode = Objects.requireNonNull(errorCode, "errorCode");
		this.errorMessage = Objects.requireNonNull(errorMessage, "errorMessage");

		if (errorCode.isEmpty())
			throw new IllegalArgumentException("errorCode empty");
		if (errorMessage.isEmpty())
			throw new IllegalArgumentException("errorMessage empty");
	}

	public String getErrorCode()
	{
		return errorCode;
	}

	public String getErrorMessage()
	{
		return errorMessage;
	}

	@Override
	public String getMessage()
	{
		return getErrorCode() + " - " + getErrorMessage();
	}
}
