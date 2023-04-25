package dev.dsf.bpe.v1.documentation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import dev.dsf.bpe.v1.ProcessPluginDefinition;

/**
 * Annotation for documenting DSF process plugin properties. Add this annotation in addition to {@link Value} to fields
 * of your spring {@link Configuration} class in order to take advantage of the "dsf-tools-documentation-generator"
 * maven plugin to generate a markdown file.
 * <p>
 * Example:
 *
 * <pre>
 * &#64;ProcessDocumentation(description = "Set to `true` to enable a special function", processNames = "testorg_process")
 * &#64;Value("${org.test.process.special:false}")
 * private boolean specialFunction;
 * </pre>
 *
 * @see ProcessPluginDefinition#getSpringConfigurations()
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ProcessDocumentation
{
	/**
	 * @return <code>true</code> if this property is required for processes listed in
	 *         {@link ProcessDocumentation#processNames}
	 */
	boolean required() default false;

	/**
	 * @return an empty array if all processes use this property or an array of length {@literal >= 1} containing only
	 *         specific processes that use this property, but not all
	 */
	String[] processNames() default {};

	/**
	 * @return description helping to configure this property
	 */
	String description();

	/**
	 * @return example value helping to configure this property
	 */
	String example() default "";

	/**
	 * @return recommendation helping to configure this property
	 */
	String recommendation() default "";
}
