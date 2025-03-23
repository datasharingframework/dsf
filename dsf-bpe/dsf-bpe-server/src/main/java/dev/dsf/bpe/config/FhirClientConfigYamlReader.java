package dev.dsf.bpe.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import dev.dsf.bpe.api.config.FhirClientConfigs;

public interface FhirClientConfigYamlReader
{
	/**
	 * @param yaml
	 *            not <code>null</code>
	 * @return never <code>null</code>, empty List if given <b>yaml</b> is blank
	 * @throws IOException
	 *             if referenced resources can not be read or parsed
	 * @throws ConfigInvalidException
	 *             if the given <b>yaml</b> is not valid
	 */
	FhirClientConfigs readConfigs(String yaml) throws IOException, ConfigInvalidException;

	/**
	 * @param yaml
	 *            not <code>null</code>
	 * @return never <code>null</code>
	 * @throws IOException
	 *             if the given <b>yaml</b> can not be read or parse, or if referenced resources can not be read or
	 *             parsed
	 * @throws ConfigInvalidException
	 *             if the given <b>yaml</b> is not valid
	 */
	default FhirClientConfigs readConfigs(Path yaml) throws IOException, ConfigInvalidException
	{
		Objects.requireNonNull(yaml, "yaml");

		try (InputStream in = Files.newInputStream(yaml))
		{
			return readConfigs(in);
		}
	}

	/**
	 * @param yaml
	 *            not <code>null</code>
	 * @return never <code>null</code>
	 * @throws IOException
	 *             if the given <b>yaml</b> can not be read or parse, or if referenced resources can not be read or
	 *             parsed
	 * @throws ConfigInvalidException
	 *             if the given <b>yaml</b> is not valid
	 */
	default FhirClientConfigs readConfigs(InputStream yaml) throws IOException, ConfigInvalidException
	{
		Objects.requireNonNull(yaml, "yaml");

		try (InputStreamReader reader = new InputStreamReader(yaml))
		{
			return readConfigs(reader);
		}
	}

	/**
	 * @param yaml
	 *            not <code>null</code>
	 * @return never <code>null</code>
	 * @throws IOException
	 *             if the given <b>yaml</b> can not be read or parse, or if referenced resources can not be read or
	 *             parsed
	 * @throws ConfigInvalidException
	 *             if the given <b>yaml</b> is not valid
	 */
	FhirClientConfigs readConfigs(Reader yaml) throws IOException, ConfigInvalidException;
}
