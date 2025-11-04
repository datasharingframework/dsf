package dev.dsf.bpe.spring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.dsf.common.buildinfo.BuildInfoReader;
import dev.dsf.common.buildinfo.BuildInfoReaderImpl;

@Configuration
public class BuildInfoReaderConfig
{
	@Bean
	public BuildInfoReader buildInfoReader()
	{
		return new BuildInfoReaderImpl();
	}
}
