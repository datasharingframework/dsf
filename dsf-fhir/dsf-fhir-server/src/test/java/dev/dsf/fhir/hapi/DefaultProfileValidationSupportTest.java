package dev.dsf.fhir.hapi;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.context.support.IValidationSupport;

public class DefaultProfileValidationSupportTest
{
	@Test
	public void testFetchAllBugInHapiWorkaround() throws Exception
	{
		IValidationSupport support = new DefaultProfileValidationSupport(FhirContext.forR4());
		assertNotNull(support.fetchAllStructureDefinitions());
		assertNotNull(support.fetchAllConformanceResources());
	}
}
