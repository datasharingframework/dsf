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
package dev.dsf.maven.dev;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;

import dev.dsf.maven.exception.RuntimeIOException;

public class TemplateHandler
{
	private static final Logger logger = LoggerFactory.getLogger(TemplateHandler.class);

	private final AtomicReference<Environment> environmentReference = new AtomicReference<>();

	private final Path projectBasedir;
	private final CertificateGenerator generator;
	private final Charset charset;

	public TemplateHandler(Path projectBasedir, CertificateGenerator generator, String encoding)
	{
		this.projectBasedir = Objects.requireNonNull(projectBasedir, "projectBasedir");
		this.generator = Objects.requireNonNull(generator, "generator");
		this.charset = encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
	}

	private Environment getEnvironment()
	{
		Environment environment = environmentReference.get();
		if (environment != null)
			return environment;

		Properties properties = new Properties();
		generator.getCertificateThumbprintsByCommonNameAsHex()
				.forEach((cn, thumbprint) -> properties.put(cn + ".thumbprint", thumbprint));

		StandardEnvironment e = new StandardEnvironment();
		e.getPropertySources().addFirst(new PropertiesPropertySource("thumbprints", properties));

		if (environmentReference.compareAndSet(null, e))
			return e;
		else
			return environmentReference.get();
	}

	public void applyTemplates(List<Template> templates)
	{
		if (templates == null)
			return;

		templates.stream().filter(Objects::nonNull).forEach(template ->
		{
			Path sourcePath = template.getSource().toPath();
			Path targetPath = template.getTarget().toPath();

			applyTemplate(sourcePath, targetPath);
		});
	}

	private void applyTemplate(Path sourcePath, Path targetPath)
	{
		try
		{
			logger.info("Reading template source from {}", projectBasedir.relativize(sourcePath));
			String source = new String(Files.readAllBytes(sourcePath), charset);

			String target = getEnvironment().resolvePlaceholders(source);

			logger.info("Writing applied template to {}", projectBasedir.relativize(targetPath));
			Files.write(targetPath, target.getBytes(charset));
		}
		catch (IOException e)
		{
			throw new RuntimeIOException(e);
		}
	}
}
