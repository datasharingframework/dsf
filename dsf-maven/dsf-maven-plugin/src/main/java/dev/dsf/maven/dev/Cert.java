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
	private List<String> sans;
	private Type type;
	private List<File> targets;

	public String getCn()
	{
		return cn;
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
		return "Cert [" + (cn != null ? "cn=" + cn + ", " : "") + (sans != null ? "sans=" + sans + ", " : "")
				+ (type != null ? "type=" + type + ", " : "") + (targets != null ? "targets=" + targets : "") + "]";
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

		return new CertificationRequestConfig(signer, cn, sans == null ? List.of() : sans);
	}
}
