package dev.dsf.bpe.spring.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

@Configuration
public class PostStartupConfig
{
	private static final Logger logger = LoggerFactory.getLogger(PostStartupConfig.class);

	@Autowired
	private PluginConfig pluginConfig;

	@Autowired
	private WebsocketConfig fhirConfig;

	@Autowired
	private CamundaConfig camundaConfig;

	@EventListener({ ContextRefreshedEvent.class })
	public void onContextRefreshedEvent(ContextRefreshedEvent event)
	{
		logger.info("Deploying process plugins ...");
		pluginConfig.processPluginManager().loadAndDeployPlugins();
		logger.info("Deploying process plugins [Done]");

		logger.info("Starting process engine ...");
		camundaConfig.processEngineConfiguration().getJobExecutor().start();
		logger.info("Starting process engine [Done]");

		fhirConfig.fhirConnectorTask().connect();
		fhirConfig.fhirConnectorQuestionnaireResponse().connect();
		// websocket connect is an async operation
	}
}
