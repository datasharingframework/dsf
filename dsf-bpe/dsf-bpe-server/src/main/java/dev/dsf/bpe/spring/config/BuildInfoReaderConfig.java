package dev.dsf.bpe.spring.config;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import dev.dsf.tools.build.BuildInfoReader;
import dev.dsf.tools.build.BuildInfoReaderImpl;

@Configuration
public class BuildInfoReaderConfig
{
	@Bean
	public BuildInfoReader buildInfoReader()
	{
		return new BuildInfoReaderImpl();
	}

	@EventListener({ ContextRefreshedEvent.class })
	public void onContextRefreshedEvent(ContextRefreshedEvent event) throws IOException
	{
		buildInfoReader().logSystemDefaultTimezone();
		buildInfoReader().logBuildInfo();
	}
}
