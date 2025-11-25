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
package dev.dsf.bpe.test.spring.config;

import java.util.Objects;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import dev.dsf.bpe.test.autowire.DemoService;
import dev.dsf.bpe.test.deployment.ProcessPluginDeploymentListenerTestImpl;
import dev.dsf.bpe.test.fhir.FhirResourceModifierImpl;
import dev.dsf.bpe.test.listener.StartFieldInjectionTestListener;
import dev.dsf.bpe.test.listener.StartSendTaskTestListener;
import dev.dsf.bpe.test.message.ContinueSendTestSend;
import dev.dsf.bpe.test.message.SendTaskTest;
import dev.dsf.bpe.test.service.ApiTest;
import dev.dsf.bpe.test.service.AutowireTest;
import dev.dsf.bpe.test.service.CompressionServiceTest;
import dev.dsf.bpe.test.service.ContinueSendTest;
import dev.dsf.bpe.test.service.ContinueSendTestEvaluate;
import dev.dsf.bpe.test.service.CryptoServiceTest;
import dev.dsf.bpe.test.service.DataLoggerTest;
import dev.dsf.bpe.test.service.DsfClientTest;
import dev.dsf.bpe.test.service.EndpointProviderTest;
import dev.dsf.bpe.test.service.EnvironmentVariableTest;
import dev.dsf.bpe.test.service.ErrorBoundaryEventTestThrow;
import dev.dsf.bpe.test.service.ErrorBoundaryEventTestVerify;
import dev.dsf.bpe.test.service.ExceptionTest;
import dev.dsf.bpe.test.service.FhirBinaryVariableTestGet;
import dev.dsf.bpe.test.service.FhirBinaryVariableTestSet;
import dev.dsf.bpe.test.service.FhirClientConfigProviderTest;
import dev.dsf.bpe.test.service.FhirClientProviderTest;
import dev.dsf.bpe.test.service.FieldInjectionTest;
import dev.dsf.bpe.test.service.JsonVariableTestGet;
import dev.dsf.bpe.test.service.JsonVariableTestSet;
import dev.dsf.bpe.test.service.MimeTypeServiceTest;
import dev.dsf.bpe.test.service.OrganizationProviderTest;
import dev.dsf.bpe.test.service.ProxyTest;
import dev.dsf.bpe.test.service.QuestionnaireTestAnswer;
import dev.dsf.bpe.test.service.QuestionnaireTestAnswerCheck;
import dev.dsf.bpe.test.service.QuestionnaireTestSetIdentifies;
import dev.dsf.bpe.test.service.TargetProviderTest;
import dev.dsf.bpe.test.service.TestActivitySelector;
import dev.dsf.bpe.v2.ProcessPluginDeploymentListener;
import dev.dsf.bpe.v2.documentation.ProcessDocumentation;
import dev.dsf.bpe.v2.fhir.FhirResourceModifier;
import dev.dsf.bpe.v2.spring.ActivityPrototypeBeanCreator;

@Configuration
public class Config implements InitializingBean
{
	@ProcessDocumentation(description = "Mandatory property", example = "foo", required = true)
	@Value("${dev.dsf.bpe.test.env.mandatory:#{null}}")
	private String envVariableMandatory;

	@ProcessDocumentation(description = "Property with default value", recommendation = "Override default value if necessary")
	@Value("${dev.dsf.bpe.test.env.optional:default-value}")
	private String envVariableOptional;

	@Value("${dev.dsf.proxy.url}")
	private String envVariableProxyUrl;

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(envVariableMandatory, "envVariableMandatory");
	}

	@Bean
	public static ActivityPrototypeBeanCreator activityPrototypeBeanCreator()
	{
		return new ActivityPrototypeBeanCreator(TestActivitySelector.class, ProxyTest.class, ApiTest.class,
				OrganizationProviderTest.class, EndpointProviderTest.class, FhirClientProviderTest.class,
				FhirClientConfigProviderTest.class, StartSendTaskTestListener.class, SendTaskTest.class,
				StartFieldInjectionTestListener.class, FieldInjectionTest.class, ErrorBoundaryEventTestThrow.class,
				ErrorBoundaryEventTestVerify.class, ExceptionTest.class, CompressionServiceTest.class,
				ContinueSendTest.class, ContinueSendTestSend.class, ContinueSendTestEvaluate.class,
				JsonVariableTestSet.class, JsonVariableTestGet.class, CryptoServiceTest.class,
				MimeTypeServiceTest.class, FhirBinaryVariableTestSet.class, FhirBinaryVariableTestGet.class,
				DsfClientTest.class, TargetProviderTest.class, DataLoggerTest.class, AutowireTest.class,
				QuestionnaireTestAnswer.class, QuestionnaireTestAnswerCheck.class,
				QuestionnaireTestSetIdentifies.class);
	}

	@Bean
	public FhirResourceModifier fhirResourceModifier()
	{
		return new FhirResourceModifierImpl();
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public EnvironmentVariableTest environmentVariableTest()
	{
		return new EnvironmentVariableTest(envVariableMandatory, envVariableOptional, envVariableProxyUrl);
	}

	@Bean
	public ProcessPluginDeploymentListener processPluginDeploymentListener()
	{
		return new ProcessPluginDeploymentListenerTestImpl();
	}

	@Bean
	public DemoService demoService()
	{
		return new DemoService();
	}
}
