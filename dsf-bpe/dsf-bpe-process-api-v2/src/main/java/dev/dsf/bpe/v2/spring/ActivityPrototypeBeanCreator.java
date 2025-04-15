package dev.dsf.bpe.v2.spring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.dsf.bpe.v2.activity.Activity;

/**
 * Helper class to register {@link Activity}s as prototype beans. Must be configured as a <code>static</code>
 * {@link Bean} inside a {@link Configuration} class.
 * <p>
 * Usage:
 * <p>
 *
 * {@snippet id = "usage" lang = "java" :
 * &#64;Configuration
 * public class Config
 * {
 * 	&#64;Bean
 * 	public static ActivityPrototypeBeanCreator activityPrototypeBeanCreator()
 * 	{
 * 		return new ActivityPrototypeBeanCreator(SomeServiceTask.class, AnExecutionListener.class,
 * 				MyMessageIntermediateThrowEvent.class);
 * 	}
 * }
 * }
 */
public class ActivityPrototypeBeanCreator implements BeanDefinitionRegistryPostProcessor
{
	private final List<Class<? extends Activity>> activities = new ArrayList<>();

	@SafeVarargs
	public ActivityPrototypeBeanCreator(Class<? extends Activity>... activities)
	{
		this(Arrays.asList(activities));
	}

	public ActivityPrototypeBeanCreator(Collection<Class<? extends Activity>> activities)
	{
		if (activities != null)
			this.activities.addAll(activities);
	}

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException
	{
		activities.stream().forEach(a ->
		{
			BeanDefinition definition = createBeanDefinition(a);
			String beanName = toBeanName(a);
			registry.registerBeanDefinition(beanName, definition);
		});
	}

	private String toBeanName(Class<? extends Activity> a)
	{
		String simpleName = a.getSimpleName();
		return Character.toLowerCase(simpleName.charAt(0)) + (simpleName.length() > 1 ? simpleName.substring(1) : "");
	}

	private BeanDefinition createBeanDefinition(Class<? extends Activity> activity)
	{
		GenericBeanDefinition definition = new GenericBeanDefinition();
		definition.setBeanClass(activity);
		definition.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		return definition;
	}
}
