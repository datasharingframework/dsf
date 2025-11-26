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
package dev.dsf.bpe.api.client.oidc;

import java.util.Optional;
import java.util.Set;

public interface Jwks
{
	public static interface JwksKey
	{
		String getKid();

		String getKty();

		String getAlg();

		String getCrv();

		String getUse();

		String getN();

		String getE();

		String getX();

		String getY();
	}

	Set<JwksKey> getKeys();

	Optional<JwksKey> getKey(String kid);
}
