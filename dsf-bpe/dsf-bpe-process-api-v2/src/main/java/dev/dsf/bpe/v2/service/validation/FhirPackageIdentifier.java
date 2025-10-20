package dev.dsf.bpe.v2.service.validation;

public record FhirPackageIdentifier(String name, String version)
{
	@Override
	public String toString()
	{
		return name + "|" + version;
	}
}