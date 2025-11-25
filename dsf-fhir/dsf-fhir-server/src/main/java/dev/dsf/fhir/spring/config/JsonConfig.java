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
package dev.dsf.fhir.spring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

@Configuration
public class JsonConfig
{
	@Bean
	public ObjectMapper objectMapper()
	{
		JsonMapper jsonMapper = JsonMapper.builder().disable(MapperFeature.DEFAULT_VIEW_INCLUSION)
				.defaultPropertyInclusion(JsonInclude.Value.construct(Include.NON_NULL, Include.NON_NULL))
				.defaultPropertyInclusion(JsonInclude.Value.construct(Include.NON_EMPTY, Include.NON_EMPTY))
				.disable(Feature.AUTO_CLOSE_TARGET).build();

		return jsonMapper;
	}
}
