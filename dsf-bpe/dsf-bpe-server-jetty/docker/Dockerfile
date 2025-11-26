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

FROM debian:trixie-slim AS builder
WORKDIR /opt/bpe
COPY --chown=root:2202 ./ ./
RUN chown root:2202 ./ && \
    chmod 750 ./ ./api ./api/v1 ./api/v2 ./ca ./ca/client_ca_chains ./ca/server_root_cas ./conf ./lib ./lib_external ./process ./ui ./dsf_bpe_start.sh ./healthcheck.sh && \
    chmod 440 ./ca/client_ca_chains/*.crt ./ca/server_root_cas/*.crt ./dsf_bpe.jar ./lib/*.jar && \
    chmod 1775 ./log


FROM azul/zulu-openjdk:25-jre-headless
LABEL org.opencontainers.image.source=https://github.com/datasharingframework/dsf
LABEL org.opencontainers.image.description="DSF BPE Server"
LABEL org.opencontainers.image.licenses="Apache License, Version 2.0"

EXPOSE 8080

RUN --mount=type=cache,target=/var/cache/apt,sharing=locked \
    --mount=type=cache,target=/var/lib/apt,sharing=locked \
    apt update && apt dist-upgrade -y && apt install curl -y
RUN adduser --system --no-create-home --group --uid 2202 java

WORKDIR /opt/bpe
COPY --from=builder /opt/bpe ./

USER java
ENTRYPOINT ["./dsf_bpe_start.sh"]
ENV DEV_DSF_SERVER_API_HOST="0.0.0.0"
ENV DEV_DSF_SERVER_API_PORT="8080"
ENV DEV_DSF_SERVER_STATUS_PORT="10000"
ENV DEV_DSF_SERVER_CONTEXT_PATH="/bpe"
HEALTHCHECK --interval=10s --timeout=15s --start-period=10s --retries=5 CMD [ "./healthcheck.sh" ]