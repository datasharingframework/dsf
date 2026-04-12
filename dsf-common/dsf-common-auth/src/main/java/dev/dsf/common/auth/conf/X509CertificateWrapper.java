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
import java.time.Instant;

import javax.security.auth.x500.X500Principal;

import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;

public class X509CertificateWrapper
{
	private final X509Certificate certificate;
	private final String thumbprint;
	private final String subjectDn;

	private final Instant expiration;

	public X509CertificateWrapper(X509Certificate certificate)
	{
		this.certificate = certificate;
		this.thumbprint = toThumbprint(certificate);
		this.subjectDn = toSubjectDn(certificate);

		this.expiration = certificate == null ? null : certificate.getNotAfter().toInstant();
	}

	private static String toThumbprint(X509Certificate certificate)
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

	private static String toSubjectDn(X509Certificate certificate)
	{
		return certificate.getSubjectX500Principal().getName(X500Principal.RFC1779);
	}

	public X509Certificate getCertificate()
	{
		return certificate;
	}

	public String getThumbprint()
	{
		return thumbprint;
	}

	public String getSubjectDn()
	{
		return subjectDn;
	}

	public JcaX509CertificateHolder toJcaX509CertificateHolder()
	{
		try
		{
			return new JcaX509CertificateHolder(certificate);
		}
		catch (CertificateEncodingException e)
		{
			throw new RuntimeException(e);
		}
	}

	public boolean isNotExpired()
	{
		return expiration != null && Instant.now().isBefore(expiration);
	}
}
