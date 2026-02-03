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
package dev.dsf.common.auth.conf;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import javax.security.auth.x500.X500Principal;

import org.apache.commons.codec.binary.Hex;

public record X509CertificateWrapper(X509Certificate certificate, String thumbprint, String subjectDn)
{
	public X509CertificateWrapper(X509Certificate certificate)
	{
		this(certificate, getThumbprint(certificate), getSubjectDn(certificate));
	}

	private static String getThumbprint(X509Certificate certificate)
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

	private static String getSubjectDn(X509Certificate certificate)
	{
		return certificate.getSubjectX500Principal().getName(X500Principal.RFC1779);
	}
}
