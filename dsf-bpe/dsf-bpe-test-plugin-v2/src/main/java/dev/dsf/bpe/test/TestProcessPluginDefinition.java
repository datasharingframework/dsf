package dev.dsf.bpe.test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import dev.dsf.bpe.test.spring.config.Config;
import dev.dsf.bpe.v2.ProcessPluginDefinition;

public class TestProcessPluginDefinition implements ProcessPluginDefinition
{
	public static final String NAME = "dsf-process-test";
	public static final String VERSION = "2.0.0.0";
	public static final LocalDate RELEASE_DATE = LocalDate.of(2025, 9, 1);

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getVersion()
	{
		return VERSION;
	}

	@Override
	public LocalDate getReleaseDate()
	{
		return RELEASE_DATE;
	}

	@Override
	public List<String> getProcessModels()
	{
		return List.of("bpe/test.bpmn");
	}

	@Override
	public List<Class<?>> getSpringConfigurations()
	{
		return List.of(Config.class);
	}

	@Override
	public Map<String, List<String>> getFhirResourcesByProcessId()
	{
		var aTest = "fhir/ActivityDefinition/dsf-test.xml";
		var cTest = "fhir/CodeSystem/dsf-test.xml";
		var qTest = "fhir/Questionnaire/test.xml";
		var sTest = "fhir/StructureDefinition/dsf-task-test.xml";
		var sContinueSentTest = "fhir/StructureDefinition/dsf-task-continue-send-test.xml";
		var vTest = "fhir/ValueSet/dsf-test.xml";

		return Map.of("dsfdev_test", List.of(aTest, cTest, qTest, sContinueSentTest, sTest, vTest));
	}

	@Override
	public String getTitle()
	{
		return "API v2 Test Process Plugin";
	}

	@Override
	public String getPublisher()
	{
		return "Heilbronn University of Applied Sciences";
	}

	@Override
	public String getPublisherEmail()
	{
		return "dsf-gecko@hs-heilbronn.de";
	}

	@Override
	public License getLicense()
	{
		return License.Apache2;
	}
}
