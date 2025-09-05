package dev.dsf.bpe.v2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.bpe.v2.config.ProxyConfig;
import dev.dsf.bpe.v2.service.CompressionService;
import dev.dsf.bpe.v2.service.CryptoService;
import dev.dsf.bpe.v2.service.DataLogger;
import dev.dsf.bpe.v2.service.DsfClientProvider;
import dev.dsf.bpe.v2.service.EndpointProvider;
import dev.dsf.bpe.v2.service.FhirClientConfigProvider;
import dev.dsf.bpe.v2.service.FhirClientProvider;
import dev.dsf.bpe.v2.service.MailService;
import dev.dsf.bpe.v2.service.MimeTypeService;
import dev.dsf.bpe.v2.service.OidcClientProvider;
import dev.dsf.bpe.v2.service.OrganizationProvider;
import dev.dsf.bpe.v2.service.QuestionnaireResponseHelper;
import dev.dsf.bpe.v2.service.ReadAccessHelper;
import dev.dsf.bpe.v2.service.TargetProvider;
import dev.dsf.bpe.v2.service.TaskHelper;
import dev.dsf.bpe.v2.service.process.ProcessAuthorizationHelper;
import dev.dsf.bpe.v2.variables.Variables;

/**
 * Gives access to services available to process plugins. This api and all services excepted {@link Variables} can be
 * injected using {@link Autowired} into spring {@link Configuration} classes.
 *
 * @see ProcessPluginDefinition#getSpringConfigurations()
 */
public interface ProcessPluginApi
{
	ProcessPluginDefinition getProcessPluginDefinition();

	ProxyConfig getProxyConfig();

	EndpointProvider getEndpointProvider();

	FhirContext getFhirContext();

	DsfClientProvider getDsfClientProvider();

	FhirClientProvider getFhirClientProvider();

	FhirClientConfigProvider getFhirClientConfigProvider();

	OidcClientProvider getOidcClientProvider();

	MailService getMailService();

	MimeTypeService getMimeTypeService();

	ObjectMapper getObjectMapper();

	OrganizationProvider getOrganizationProvider();

	ProcessAuthorizationHelper getProcessAuthorizationHelper();

	QuestionnaireResponseHelper getQuestionnaireResponseHelper();

	ReadAccessHelper getReadAccessHelper();

	TaskHelper getTaskHelper();

	CompressionService getCompressionService();

	CryptoService getCryptoService();

	TargetProvider getTargetProvider();

	DataLogger getDataLogger();
}
