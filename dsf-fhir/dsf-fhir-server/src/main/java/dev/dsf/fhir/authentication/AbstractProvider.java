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
package dev.dsf.fhir.authentication;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.codec.binary.Hex;
import org.hl7.fhir.r4.model.Identifier;
import org.springframework.beans.factory.InitializingBean;

import dev.dsf.fhir.help.ExceptionHandler;

public abstract class AbstractProvider implements InitializingBean
{
	protected final ExceptionHandler exceptionHandler;

	public AbstractProvider(ExceptionHandler exceptionHandler)
	{
		this.exceptionHandler = exceptionHandler;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(exceptionHandler, "exceptionHandler");
	}

	protected final Optional<String> getIdentifierValue(List<Identifier> identifiers, String system)
	{
		return identifiers.stream().filter(Identifier::hasSystem).filter(Identifier::hasValue)
				.filter(i -> system.equals(i.getSystem())).map(Identifier::getValue).findFirst();
	}

	protected final String getThumbprint(X509Certificate certificate)
	{
		try
		{
			byte[] digest = MessageDigest.getInstance("SHA-512").digest(certificate.getEncoded());
			return Hex.encodeHexString(digest);
		}
		catch (CertificateEncodingException | NoSuchAlgorithmException e)
		{
			throw new RuntimeException(e);
		}
	}
}
