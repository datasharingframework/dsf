package dev.dsf.bpe.ui;

import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

import org.springframework.beans.factory.InitializingBean;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import dev.dsf.common.ui.theme.Theme;
import jakarta.ws.rs.core.StreamingOutput;

public class ThymeleafTemplateServiceImpl implements ThymeleafTemplateService, InitializingBean
{
	private final String serverBaseUrl;
	private final Theme theme;
	private final boolean cacheEnabled;

	private final TemplateEngine templateEngine = new TemplateEngine();

	/**
	 * @param serverBaseUrl
	 *            not <code>null</code>
	 * @param theme
	 *            may be <code>null</code>
	 * @param cacheEnabled
	 */
	public ThymeleafTemplateServiceImpl(String serverBaseUrl, Theme theme, boolean cacheEnabled)
	{
		this.serverBaseUrl = serverBaseUrl;
		this.theme = theme;
		this.cacheEnabled = cacheEnabled;

		ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
		resolver.setTemplateMode(TemplateMode.HTML);
		resolver.setPrefix("/template/");
		resolver.setSuffix(".html");
		resolver.setCacheable(cacheEnabled);

		templateEngine.setTemplateResolver(resolver);
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(serverBaseUrl, "serverBaseUrl");
	}

	private String getServerBaseUrlPathWithLeadingSlash()
	{
		try
		{
			return new URL(serverBaseUrl).getPath();
		}
		catch (MalformedURLException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public StreamingOutput write(Context context, MainValues mainValues)
	{
		Objects.requireNonNull(context, "context");
		Objects.requireNonNull(mainValues, "mainValues");

		context.setVariable("title", mainValues.title());
		context.setVariable("heading", mainValues.heading());
		context.setVariable("htmlFragment", mainValues.htmlFragment());

		context.setVariable("username", mainValues.username());
		context.setVariable("openid", mainValues.openid());

		context.setVariable("basePath", getServerBaseUrlPathWithLeadingSlash());
		context.setVariable("theme", theme == null ? null : theme.toString());

		context.setVariable("bpmnProd", cacheEnabled);

		return output -> templateEngine.process("main", context, new OutputStreamWriter(output));
	}
}
