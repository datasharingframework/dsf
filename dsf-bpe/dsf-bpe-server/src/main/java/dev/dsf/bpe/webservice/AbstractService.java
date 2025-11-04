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
package dev.dsf.bpe.webservice;

import java.security.Principal;
import java.util.Objects;
import java.util.function.Consumer;

import org.springframework.beans.factory.InitializingBean;
import org.thymeleaf.context.Context;

import dev.dsf.bpe.ui.ThymeleafTemplateService;
import dev.dsf.bpe.ui.ThymeleafTemplateService.MainValues;
import dev.dsf.common.auth.conf.Identity;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.StreamingOutput;

public abstract class AbstractService implements InitializingBean
{
	@jakarta.ws.rs.core.Context
	private volatile SecurityContext securityContext;

	private final ThymeleafTemplateService templateService;
	private final String htmlFragment;

	public AbstractService(ThymeleafTemplateService templateService, String htmlFragment)
	{
		this.templateService = templateService;
		this.htmlFragment = htmlFragment;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(templateService, "templateService");
		Objects.requireNonNull(htmlFragment, "htmlFragment");
	}

	protected StreamingOutput write(String title, String heading, Consumer<Context> setValues)
	{
		Context context = new Context();

		setValues.accept(context);

		Principal principal = securityContext.getUserPrincipal();
		MainValues mainValues = new MainValues(title, heading, htmlFragment,
				principal instanceof Identity i ? i.getDisplayName() : null,
				"OPENID".equals(securityContext.getAuthenticationScheme()));

		return templateService.write(context, mainValues);
	}
}
