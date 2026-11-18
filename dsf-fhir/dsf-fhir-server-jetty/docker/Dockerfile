#
# Copyright 2018-2025 Heilbronn University of Applied Sciences
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

FROM docker.io/library/debian:trixie-slim@sha256:a72b4433de1cf7d6bae0c4cee74d0547f58601a643b3375f070c51600d63b6b2 AS builder
WORKDIR /opt/fhir
COPY --chown=root:2101 ./ ./
RUN chown root:2101 ./ && \
    chmod 750 ./ ./ca ./ca/client_ca_chains ./ca/server_root_cas ./conf ./lib ./ui ./dsf_fhir_start.sh ./healthcheck.sh && \
    chmod 440 ./ca/client_ca_chains/*.crt ./ca/server_root_cas/*.crt ./conf/bundle.xml ./dsf_fhir.jar ./lib/*.jar && \
    chmod 1775 ./log


FROM docker.io/library/azul-zulu:26-jre-headless@sha256:003bd0eccb8c26f57ceeefd6de72a297df3ab3fbfcb7006f06d5ef79dbb96bbc
LABEL org.opencontainers.image.source=https://github.com/datasharingframework/dsf
LABEL org.opencontainers.image.description="DSF FHIR Server"
LABEL org.opencontainers.image.licenses="Apache License, Version 2.0"

EXPOSE 8080

RUN --mount=type=cache,target=/var/cache/apt,sharing=locked \
    --mount=type=cache,target=/var/lib/apt,sharing=locked \
    apt update && apt dist-upgrade -y && apt install curl -y
RUN adduser --system --no-create-home --group --uid 2101 java

WORKDIR /opt/fhir
COPY --from=builder /opt/fhir ./

USER java
ENTRYPOINT ["./dsf_fhir_start.sh"]
ENV DEV_DSF_SERVER_API_HOST="0.0.0.0"
ENV DEV_DSF_SERVER_API_PORT="8080"
ENV DEV_DSF_SERVER_STATUS_PORT="10000"
ENV DEV_DSF_SERVER_CONTEXT_PATH="/fhir"
HEALTHCHECK --interval=10s --timeout=15s --start-period=10s --retries=5 CMD [ "./healthcheck.sh" ]