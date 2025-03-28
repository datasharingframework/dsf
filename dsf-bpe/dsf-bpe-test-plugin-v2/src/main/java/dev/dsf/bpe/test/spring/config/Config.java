package dev.dsf.bpe.test.spring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.dsf.bpe.test.listener.StartFieldInjectionTestListener;
import dev.dsf.bpe.test.listener.StartSendTaskTestListener;
import dev.dsf.bpe.test.message.ContinueSendTestSend;
import dev.dsf.bpe.test.message.SendTaskTest;
import dev.dsf.bpe.test.service.ApiTest;
import dev.dsf.bpe.test.service.ContinueSendTest;
import dev.dsf.bpe.test.service.ContinueSendTestEvaluate;
import dev.dsf.bpe.test.service.EndpointProviderTest;
import dev.dsf.bpe.test.service.ErrorBoundaryEventTestThrow;
import dev.dsf.bpe.test.service.ErrorBoundaryEventTestVerify;
import dev.dsf.bpe.test.service.ExceptionTest;
import dev.dsf.bpe.test.service.FhirClientProviderTest;
import dev.dsf.bpe.test.service.FieldInjectionTest;
import dev.dsf.bpe.test.service.JsonVariableTestGet;
import dev.dsf.bpe.test.service.JsonVariableTestSet;
import dev.dsf.bpe.test.service.MimetypeServiceTest;
import dev.dsf.bpe.test.service.OrganizationProviderTest;
import dev.dsf.bpe.test.service.ProxyTest;
import dev.dsf.bpe.test.service.TestActivitySelector;
import dev.dsf.bpe.v2.spring.ActivityPrototypeBeanCreator;

@Configuration
public class Config
{
	@Bean
	public ActivityPrototypeBeanCreator activityPrototypeBeanCreator()
	{
		return new ActivityPrototypeBeanCreator(TestActivitySelector.class, ProxyTest.class, ApiTest.class,
				OrganizationProviderTest.class, EndpointProviderTest.class, FhirClientProviderTest.class,
				StartSendTaskTestListener.class, SendTaskTest.class, StartFieldInjectionTestListener.class,
				FieldInjectionTest.class, ErrorBoundaryEventTestThrow.class, ErrorBoundaryEventTestVerify.class,
				ExceptionTest.class, ContinueSendTest.class, ContinueSendTestSend.class, ContinueSendTestEvaluate.class,
				JsonVariableTestSet.class, JsonVariableTestGet.class, MimetypeServiceTest.class);
	}
}
