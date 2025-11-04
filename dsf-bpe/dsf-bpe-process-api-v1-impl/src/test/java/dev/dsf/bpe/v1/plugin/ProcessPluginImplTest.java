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
package dev.dsf.bpe.v1.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.operaton.bpm.engine.delegate.BpmnError;
import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.instance.Process;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonProperties;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.bpe.api.plugin.BpmnFileAndModel;
import dev.dsf.bpe.api.plugin.ProcessPlugin;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.ProcessPluginApiImpl;
import dev.dsf.bpe.v1.ProcessPluginDefinition;
import dev.dsf.bpe.v1.activity.AbstractServiceDelegate;
import dev.dsf.bpe.v1.config.ProxyConfig;
import dev.dsf.bpe.v1.service.EndpointProvider;
import dev.dsf.bpe.v1.service.FhirWebserviceClientProviderImpl;
import dev.dsf.bpe.v1.service.MailService;
import dev.dsf.bpe.v1.service.OrganizationProvider;
import dev.dsf.bpe.v1.service.QuestionnaireResponseHelper;
import dev.dsf.bpe.v1.service.TaskHelper;
import dev.dsf.bpe.v1.variables.ObjectMapperFactory;
import dev.dsf.bpe.v1.variables.Variables;
import dev.dsf.fhir.authorization.process.ProcessAuthorizationHelper;
import dev.dsf.fhir.authorization.read.ReadAccessHelper;

public class ProcessPluginImplTest
{
	private static final class TestProcessPluginDefinition implements ProcessPluginDefinition
	{
		final Map<String, List<String>> fhirResources;
		final List<String> processModels;
		final String version;
		final List<Class<?>> springConfigurations;
		final LocalDate releaseDate;

		TestProcessPluginDefinition(Map<String, List<String>> fhirResources, List<String> processModels, String version,
				List<Class<?>> springConfigurations, LocalDate releaseDate)
		{
			this.fhirResources = fhirResources;
			this.processModels = processModels;
			this.version = version;
			this.springConfigurations = springConfigurations;
			this.releaseDate = releaseDate;
		}

		@Override
		public String getName()
		{
			return "test";
		}

		@Override
		public String getVersion()
		{
			return version;
		}

		@Override
		public LocalDate getReleaseDate()
		{
			return releaseDate;
		}

		@Override
		public List<Class<?>> getSpringConfigurations()
		{
			return springConfigurations;
		}

		@Override
		public List<String> getProcessModels()
		{
			return processModels;
		}

		@Override
		public Map<String, List<String>> getFhirResourcesByProcessId()
		{
			return fhirResources;
		}
	}

	@Configuration
	// Configuration may not be private, final
	public static class TestConfig
	{
		@Autowired
		private ProcessPluginApi processPluginApi;

		@Bean
		@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
		public TestService testService()
		{
			return new TestService(processPluginApi);
		}
	}

	private static final class TestService extends AbstractServiceDelegate
	{
		public TestService(ProcessPluginApi processPluginApi)
		{
			super(processPluginApi);
		}

		@Override
		protected void doExecute(DelegateExecution execution, Variables variables) throws BpmnError, Exception
		{
			// test: do nothing
		}
	}

	private final ProxyConfig proxyConfig = mock(ProxyConfig.class);
	private final EndpointProvider endpointProvider = mock(EndpointProvider.class);
	private final FhirContext fhirContext = FhirContext.forR4();
	private final FhirWebserviceClientProviderImpl fhirWebserviceClientProvider = mock(
			FhirWebserviceClientProviderImpl.class);
	private final MailService mailService = mock(MailService.class);
	private final ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper(fhirContext);
	private final OrganizationProvider organizationProvider = mock(OrganizationProvider.class);
	private final QuestionnaireResponseHelper questionnaireResponseHelper = mock(QuestionnaireResponseHelper.class);
	private final ProcessAuthorizationHelper processAuthorizationHelper = mock(ProcessAuthorizationHelper.class);
	private final ReadAccessHelper readAccessHelper = mock(ReadAccessHelper.class);
	private final TaskHelper taskHelper = mock(TaskHelper.class);

	private final ProcessPluginApi processPluginApi = new ProcessPluginApiImpl(proxyConfig, endpointProvider,
			fhirContext, fhirWebserviceClientProvider, mailService, objectMapper, organizationProvider,
			processAuthorizationHelper, questionnaireResponseHelper, readAccessHelper, taskHelper);
	private final ConfigurableEnvironment environment = new StandardEnvironment();

	private final AbstractApplicationContext apiApplicationContext;

	public ProcessPluginImplTest()
	{
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerSingleton("processPluginApiV1", processPluginApi);
		factory.registerSingleton("fhirContext", fhirContext);

		apiApplicationContext = new AnnotationConfigApplicationContext(factory);
		apiApplicationContext.refresh();
	}

	@Test
	public void testInitializeAndValidateResourcesAllNull() throws Exception
	{
		var definition = createPluginDefinition(null, null, null);
		ProcessPluginImpl plugin = createPlugin(definition, false);

		assertFalse(plugin.initializeAndValidateResources(null));
		try
		{
			plugin.getApplicationContext();
			fail("IllegalStateException expected");
		}
		catch (IllegalStateException e)
		{
		}

		try
		{
			plugin.getProcessModels();
			fail("IllegalStateException expected");
		}
		catch (IllegalStateException e)
		{
		}
		try
		{
			plugin.getFhirResources();
			fail("IllegalStateException expected");
		}
		catch (IllegalStateException e)
		{
		}
	}

	@Test
	public void testInitializeAndValidateResourcesEmptySpringConfigBpmnAndFhirResources() throws Exception
	{
		var definition = createPluginDefinition(List.of(), List.of(), Map.of());
		ProcessPluginImpl plugin = createPlugin(definition, false);

		assertFalse(plugin.initializeAndValidateResources(null));
		try
		{
			plugin.getApplicationContext();
			fail("IllegalStateException expected");
		}
		catch (IllegalStateException e)
		{
		}

		try
		{
			plugin.getProcessModels();
			fail("IllegalStateException expected");
		}
		catch (IllegalStateException e)
		{
		}
		try
		{
			plugin.getFhirResources();
			fail("IllegalStateException expected");
		}
		catch (IllegalStateException e)
		{
		}
	}

	@Test
	public void testInitializeAndValidateResourcesNotExistingModelAndFhirResources() throws Exception
	{
		var definition = createPluginDefinition(List.of(TestConfig.class), List.of("test-plugin/does_not_exist.bpmn"),
				Map.of("testorg_test", List.of("test-plugin/does_not_exist.xml")));
		ProcessPluginImpl plugin = createPlugin(definition, false);

		assertFalse(plugin.initializeAndValidateResources(null));
		try
		{
			plugin.getApplicationContext();
			fail("IllegalStateException expected");
		}
		catch (IllegalStateException e)
		{
		}

		try
		{
			plugin.getProcessModels();
			fail("IllegalStateException expected");
		}
		catch (IllegalStateException e)
		{
		}
		try
		{
			plugin.getFhirResources();
			fail("IllegalStateException expected");
		}
		catch (IllegalStateException e)
		{
		}
	}

	@Test
	public void testInitializeAndValidateResourcesNotExistingFhirResources() throws Exception
	{
		var definition = createPluginDefinition(List.of(TestConfig.class), List.of("test-plugin/test.bpmn"),
				Map.of("testorg_test", List.of("test-plugin/does_not_exist.xml")));
		ProcessPluginImpl plugin = createPlugin(definition, false);

		assertFalse(plugin.initializeAndValidateResources(null));
		assertNotNull(plugin.getApplicationContext());

		try
		{
			plugin.getProcessModels();
			fail("IllegalStateException expected");
		}
		catch (IllegalStateException e)
		{
		}
		try
		{
			plugin.getFhirResources();
			fail("IllegalStateException expected");
		}
		catch (IllegalStateException e)
		{
		}
	}

	@Test
	public void testInitializeAndValidateResources() throws Exception
	{
		var definition = createPluginDefinition(List.of(TestConfig.class), List.of("test-plugin/test.bpmn"),
				Map.of("testorg_test", List.of("test-plugin/ActivityDefinition_test.xml")));
		ProcessPluginImpl plugin = createPlugin(definition, false);

		assertTrue(plugin.initializeAndValidateResources("test.org"));
		assertNotNull(plugin.getApplicationContext());
		assertNotNull(plugin.getProcessModels());
		assertNotNull(plugin.getFhirResources());

		List<BpmnFileAndModel> models = plugin.getProcessModels();
		assertEquals(1, models.size());
		BpmnFileAndModel bpmnFileAndModel = models.get(0);
		BpmnModelInstance model = bpmnFileAndModel.model();
		assertNotNull(model);

		Collection<Process> processes = model.getModelElementsByType(Process.class);
		assertNotNull(processes);
		assertEquals(1, processes.size());
		Process process = processes.stream().findFirst().get();
		Collection<OperatonProperties> camundaPropertiesElements = process.getExtensionElements()
				.getChildElementsByType(OperatonProperties.class);
		assertNotNull(camundaPropertiesElements);
		assertEquals(1, camundaPropertiesElements.size());
		OperatonProperties camundaProperties = camundaPropertiesElements.stream().findFirst().get();
		Collection<OperatonProperty> camundaPropertyElements = camundaProperties.getOperatonProperties();
		assertNotNull(camundaPropertyElements);
		assertEquals(1, camundaPropertyElements.size());
		OperatonProperty property = camundaPropertyElements.stream().findFirst().get();
		assertEquals(ProcessPlugin.MODEL_ATTRIBUTE_PROCESS_API_VERSION, property.getOperatonName());
		assertEquals(ProcessPluginFactoryImpl.API_VERSION, Integer.parseInt(property.getOperatonValue()));
	}

	private ProcessPluginDefinition createPluginDefinition(List<Class<?>> springConfigurations,
			List<String> processModels, Map<String, List<String>> fhirResources)
	{
		return new TestProcessPluginDefinition(fhirResources, processModels, "1.0.0.0", springConfigurations,
				LocalDate.now());
	}

	private ProcessPluginImpl createPlugin(ProcessPluginDefinition processPluginDefinition, boolean draft)
	{
		return new ProcessPluginImpl(processPluginDefinition, ProcessPluginFactoryImpl.API_VERSION, draft,
				Paths.get("test.jar"), getClass().getClassLoader(), environment, apiApplicationContext,
				"https://localhost/fhir");
	}
}
