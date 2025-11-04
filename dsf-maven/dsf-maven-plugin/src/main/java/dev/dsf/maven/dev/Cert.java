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
package dev.dsf.maven.dev;

import java.io.File;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.function.BiFunction;

import de.hsheilbronn.mi.utils.crypto.ca.CertificateAuthority;
import de.hsheilbronn.mi.utils.crypto.ca.CertificationRequest;
import dev.dsf.maven.dev.CertificateGenerator.CertificationRequestConfig;

public class Cert
{
	public static enum Type
	{
		CLIENT, SERVER, CLIENT_SERVER
	}

	private String cn;
	private String email;
	private List<String> sans;
	private Type type;
	private List<File> targets;

	public String getCn()
	{
		return cn;
	}

	public String getEmail()
	{
		return email;
	}

	public List<String> getSans()
	{
		return sans;
	}

	public Type getType()
	{
		return type;
	}

	public List<File> getTargets()
	{
		return targets;
	}

	@Override
	public String toString()
	{
		return "Cert [" + (cn != null ? "cn=" + cn + ", " : "") + (email != null ? "email=" + email + ", " : "")
				+ (sans != null ? "sans=" + sans + ", " : "") + (type != null ? "type=" + type + ", " : "")
				+ (targets != null ? "targets=" + targets : "") + "]";
	}

	public CertificationRequestConfig toCertificationRequestConfig()
	{
		BiFunction<CertificateAuthority, CertificationRequest, X509Certificate> signer = switch (type)
		{
			case CLIENT -> CertificateAuthority::signClientCertificate;
			case SERVER -> CertificateAuthority::signServerCertificate;
			case CLIENT_SERVER -> CertificateAuthority::signClientServerCertificate;

			default -> throw new IllegalArgumentException("Unexpected value: " + type);
		};

		return new CertificationRequestConfig(signer, cn, email, sans == null ? List.of() : sans);
	}
}
