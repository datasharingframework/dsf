package dev.dsf.bpe.ui;

import org.thymeleaf.context.Context;

import jakarta.ws.rs.core.StreamingOutput;

public interface ThymeleafTemplateService
{
	record MainValues(String title, String heading, String htmlFragment, String username, boolean openid)
	{
	}

	/**
	 * @param context
	 *            not <code>null</code>
	 * @param mainValues
	 *            not <code>null</code>
	 * @return
	 */
	StreamingOutput write(Context context, MainValues mainValues);
}
