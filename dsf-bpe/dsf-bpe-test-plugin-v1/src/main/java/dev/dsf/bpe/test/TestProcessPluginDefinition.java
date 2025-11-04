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
package dev.dsf.bpe.test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import dev.dsf.bpe.test.spring.config.Config;
import dev.dsf.bpe.v1.ProcessPluginDefinition;

public class TestProcessPluginDefinition implements ProcessPluginDefinition
{
	public static final String VERSION = "1.0.0.0";
	public static final LocalDate RELEASE_DATE = LocalDate.of(2025, 3, 25);

	@Override
	public String getName()
	{
		return "dsf-process-test";
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
		var sTest = "fhir/StructureDefinition/dsf-task-test.xml";
		var vTest = "fhir/ValueSet/dsf-test.xml";

		return Map.of("dsfdev_test", List.of(aTest, cTest, sTest, vTest));
	}
}
