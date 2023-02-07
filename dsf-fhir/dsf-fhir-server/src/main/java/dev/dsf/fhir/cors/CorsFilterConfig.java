package dev.dsf.fhir.cors;

public interface CorsFilterConfig
{
	boolean originAllowed(String origin);
}
