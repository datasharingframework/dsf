package dev.dsf.fhir.spring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.dsf.common.build.BuildInfoReader;
import dev.dsf.common.build.BuildInfoReaderImpl;

@Configuration
public class BuildInfoReaderConfig
{
	@Bean
	public BuildInfoReader buildInfoReader()
	{
		return new BuildInfoReaderImpl();
	}
}
