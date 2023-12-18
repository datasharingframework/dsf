package dev.dsf.bpe.spring.config;

import java.util.Locale;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.core.StreamReadConstraints;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.i18n.HapiLocalizer;

@Configuration
public class FhirConfig
{
	@Bean
	public FhirContext fhirContext()
	{
		// TODO remove workaround after upgrading to HAPI 6.8+, see https://github.com/hapifhir/hapi-fhir/issues/5205
		StreamReadConstraints.overrideDefaultStreamReadConstraints(
				StreamReadConstraints.builder().maxStringLength(Integer.MAX_VALUE).build());

		FhirContext context = FhirContext.forR4();
		HapiLocalizer localizer = new HapiLocalizer()
		{
			@Override
			public Locale getLocale()
			{
				return Locale.ROOT;
			}
		};
		context.setLocalizer(localizer);
		return context;
	}
}
