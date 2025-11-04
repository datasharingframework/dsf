/*
 * Copyright 2018-2025 Heilbronn University of Applied Sciences
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.dsf.common.docker.secrets;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;

public class DockerSecretsPropertySourceFactory
{
	private static final Logger logger = LoggerFactory.getLogger(DockerSecretsPropertySourceFactory.class);

	private final Map<String, String> secretFilesByFinalPropertyName = new HashMap<>();
	private final ConfigurableEnvironment environment;

	public DockerSecretsPropertySourceFactory(ConfigurableEnvironment environment)
	{
		Stream<String> passwordProperties = environment.getPropertySources().stream()
				.filter(s -> s instanceof EnumerablePropertySource).map(s -> (EnumerablePropertySource<?>) s)
				.flatMap(s -> List.of(s.getPropertyNames()).stream()).filter(Objects::nonNull)
				.filter(p -> p.toLowerCase().endsWith(".password.file") || p.toLowerCase().endsWith("_password_file")
						|| p.toLowerCase().endsWith(".secret.file") || p.toLowerCase().endsWith("_secret_file"));

		passwordProperties.forEach(property ->
		{
			String fileName = environment.getProperty(property, String.class, null);
			secretFilesByFinalPropertyName
					.put(property.toLowerCase().replace('_', '.').substring(0, property.length() - 5), fileName);
		});

		this.environment = environment;
	}

	public PropertiesPropertySource readDockerSecretsAndAddPropertiesToEnvironment()
	{
		MutablePropertySources sources = environment.getPropertySources();
		PropertiesPropertySource propertiesFromDockerSecrets = getPropertiesFromDockerSecrets();
		sources.addFirst(propertiesFromDockerSecrets);
		return propertiesFromDockerSecrets;
	}

	private PropertiesPropertySource getPropertiesFromDockerSecrets()
	{
		Properties properties = new Properties();

		secretFilesByFinalPropertyName.forEach((property, secretsFile) ->
		{
			String readSecretsFileValue = readSecretsFile(property, secretsFile);
			if (readSecretsFileValue != null)
				properties.put(property, readSecretsFileValue);
		});

		return new PropertiesPropertySource("docker-secrets", properties);
	}

	private String readSecretsFile(String property, String secretsFile)
	{
		if (secretsFile == null)
		{
			logger.debug("Secrets file for property {} not defined", property);
			return null;
		}

		Path secretsFilePath = Paths.get(secretsFile);

		if (!Files.isReadable(secretsFilePath))
		{
			logger.warn("Secrets file at {} for property {} not readable", secretsFilePath.toString(), property);
			return null;
		}

		try
		{
			List<String> secretLines = Files.readAllLines(secretsFilePath, StandardCharsets.UTF_8);

			if (secretLines.isEmpty())
			{
				logger.warn("Secrets file at {} for property {} is empty", secretsFilePath.toString(), property);
				return null;
			}

			if (secretLines.size() > 1)
				logger.warn("Secrets file at {} for property {} contains multiple lines, using only the first line",
						secretsFilePath.toString(), property);

			return secretLines.get(0);
		}
		catch (IOException e)
		{
			logger.warn("Error while reading secrets file {} for property {}: {}", secretsFilePath.toString(), property,
					e.getMessage());
			throw new RuntimeException(e);
		}
	}
}
