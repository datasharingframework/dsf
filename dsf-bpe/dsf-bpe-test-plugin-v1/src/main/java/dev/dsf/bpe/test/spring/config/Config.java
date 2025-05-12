package dev.dsf.bpe.test.spring.config;

import java.util.Objects;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import dev.dsf.bpe.test.service.ApiTest;
import dev.dsf.bpe.test.service.EndpointProviderTest;
import dev.dsf.bpe.test.service.EnvironmentVariableTest;
import dev.dsf.bpe.test.service.OrganizationProviderTest;
import dev.dsf.bpe.test.service.ProxyTest;
import dev.dsf.bpe.test.service.TestActivitySelector;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.documentation.ProcessDocumentation;

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

	@Autowired
	private ProcessPluginApi api;

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(envVariableMandatory, "envVariableMandatory");
	}

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
	public EnvironmentVariableTest environmentVariableTest()
	{
		return new EnvironmentVariableTest(api, envVariableMandatory, envVariableOptional, envVariableProxyUrl);
	}
}
