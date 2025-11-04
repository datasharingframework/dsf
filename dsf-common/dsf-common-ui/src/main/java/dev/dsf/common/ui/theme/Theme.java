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
package dev.dsf.common.ui.theme;

public enum Theme
{
	DEV, TEST, PROD;

	public static Theme fromString(String s)
	{
		if (s == null || s.isBlank())
			return null;
		else
		{
			return switch (s.toLowerCase())
			{
				case "dev" -> DEV;
				case "test" -> TEST;
				case "prod" -> PROD;
				default -> null;
			};
		}
	}

	@Override
	public String toString()
	{
		return name().toLowerCase();
	}
}