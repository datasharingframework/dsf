package dev.dsf.maven.bundle;

import java.nio.file.Path;

import org.hl7.fhir.r4.model.Resource;

public interface BundleEntryPutReader
{
	void read(Class<? extends Resource> resource, Path resourceFile, Path putFile);
}
