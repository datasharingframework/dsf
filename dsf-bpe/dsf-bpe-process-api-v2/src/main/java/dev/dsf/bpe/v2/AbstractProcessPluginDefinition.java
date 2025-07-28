package dev.dsf.bpe.v2;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements {@link #getName()}, {@link #getVersion()} and {@link #getReleaseDate()} based on properties defined in a
 * {@value #PROPERTIES_FILE} file. The UTF-8 encoded file needs to contain property entries for {@value #NAME_PROPERTY},
 * {@value #VERSION_PROPERTY} (suffixes like <code>-SNAPSHOT</code> will be removed from the value, regex:
 * <code>-.*$</code>) and {@value #RELEASE_DATE_PROPERTY} (with the value formated as a ISO-8601 timestamp, see
 * {@link ZonedDateTime#parse(CharSequence)}).
 * <p>
 * Using maven the file should be located at <code>src/main/resources/plugin.properties</code> with the following
 * content:
 * {@snippet id = "plugin.properties" :
 * release-date=${project.build.outputTimestamp}
 * version=${project.version}
 * name=${project.artifactId}
 * }
 * <p>
 * The maven pom.xml file needs to define the <code>project.build.outputTimestamp</code> property (also needed for
 * reproducible builds) and enable resource filtering for the <code>plugin.properties</code> file:
 * {@snippet id = "pom.xml" :
 * <project>
 *   <properties>
 *     <project.build.outputTimestamp>2025-07-22T16:45:00Z</project.build.outputTimestamp>
 *     <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
 *   </properties>
 *   <build>
 *     <resources>
 *       <resource>
 *         <directory>src/main/resources</directory>
 *         <filtering>false</filtering>
 *         <excludes>
 *           <exclude>plugin.properties</exclude>
 *         </excludes>
 *       </resource>
 *       <resource>
 *         <directory>src/main/resources</directory>
 *         <filtering>true</filtering>
 *         <includes>
 *           <include>plugin.properties</include>
 *         </includes>
 *       </resource>
 *     </resources>
 *   </build>
 * </project>
 * }
 */
public abstract class AbstractProcessPluginDefinition implements ProcessPluginDefinition
{
	private static final Logger logger = LoggerFactory.getLogger(AbstractProcessPluginDefinition.class);

	private static final String PROPERTIES_FILE = "plugin.properties";

	private static final String NAME_PROPERTY = "name";
	private static final String VERSION_PROPERTY = "version";
	private static final String RELEASE_DATE_PROPERTY = "release-date";

	private final String name;
	private final String version;
	private final LocalDate releaseDate;

	public AbstractProcessPluginDefinition()
	{
		InputStream in = AbstractProcessPluginDefinition.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE);
		if (in == null)
		{
			logger.warn("{} file not found in root folder", PROPERTIES_FILE);
			throw new RuntimeException(PROPERTIES_FILE + " file not found");
		}

		try (in; Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
		{
			Properties properties = new Properties();
			properties.load(reader);

			name = getPropertyAndCheckNotNullNotEmpty(properties, NAME_PROPERTY);
			version = getPropertyAndCheckNotNullNotEmpty(properties, VERSION_PROPERTY).replaceFirst("-.*$", "");

			try
			{
				releaseDate = ZonedDateTime.parse(getPropertyAndCheckNotNullNotEmpty(properties, RELEASE_DATE_PROPERTY))
						.toLocalDate();
			}
			catch (DateTimeParseException e)
			{
				logger.warn("Property {} defined in {} file not parsable as ISO-8601 timestamp: {} - {}",
						RELEASE_DATE_PROPERTY, PROPERTIES_FILE, e.getClass().getName(), e.getMessage());
				throw e;
			}
		}
		catch (IOException e)
		{
			logger.warn("Unable to read {} file: {} - {}", PROPERTIES_FILE, e.getClass().getName(), e.getMessage());
			throw new RuntimeException(e);
		}
	}

	private String getPropertyAndCheckNotNullNotEmpty(Properties properties, String key)
	{
		String value = properties.getProperty(key);

		if (value == null)
		{
			logger.warn("Property {} not defined in {} file", key, PROPERTIES_FILE);
			throw new RuntimeException("Property " + key + " not defined");
		}
		else if (value.isBlank())
		{
			logger.warn("Property {} defined in {} file is blank", key, PROPERTIES_FILE);
			throw new RuntimeException("Property " + key + " is blank");
		}

		return value;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public String getVersion()
	{
		return version;
	}

	@Override
	public LocalDate getReleaseDate()
	{
		return releaseDate;
	}
}
