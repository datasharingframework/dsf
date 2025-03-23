package dev.dsf.bpe.test.spring.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import dev.dsf.bpe.test.service.ApiTest;
import dev.dsf.bpe.test.service.EndpointProviderTest;
import dev.dsf.bpe.test.service.FhirClientProviderTest;
import dev.dsf.bpe.test.service.OrganizationProviderTest;
import dev.dsf.bpe.test.service.ProxyTest;
import dev.dsf.bpe.test.service.TestActivitySelector;
import dev.dsf.bpe.v2.ProcessPluginApi;

@Configuration
public class Config
{
	@Autowired
	private ProcessPluginApi api;

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public TestActivitySelector testActivitySelector()
	{
		return new TestActivitySelector(api);
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public ProxyTest proxyTest()
	{
		return new ProxyTest(api);
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public ApiTest apiTest()
	{
		return new ApiTest(api);
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public OrganizationProviderTest organizationProviderTest()
	{
		return new OrganizationProviderTest(api);
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public EndpointProviderTest endpointProviderTest()
	{
		return new EndpointProviderTest(api);
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public FhirClientProviderTest fhirClientProviderTest()
	{
		return new FhirClientProviderTest(api);
	}
}
