package dev.dsf.bpe.ui;

import org.thymeleaf.context.Context;

import jakarta.ws.rs.core.StreamingOutput;

public interface ThymeleafTemplateService
{
	/**
	 * @param context
	 *            not <code>null</code>
	 * @return
	 */
	StreamingOutput writeRootUi(Context context);
}
