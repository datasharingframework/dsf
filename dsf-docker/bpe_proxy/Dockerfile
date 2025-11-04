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

FROM httpd:2.4-alpine
LABEL org.opencontainers.image.source=https://github.com/datasharingframework/dsf
LABEL org.opencontainers.image.description="DSF BPE Reverse Proxy"
LABEL org.opencontainers.image.licenses="Apache License, Version 2.0"

RUN --mount=type=cache,target=/etc/apk/cache \
    apk update && apk upgrade && apk -q add libcap && \
    setcap 'cap_net_bind_service=+ep' /usr/local/apache2/bin/httpd && \
    addgroup -g 4202 apache && adduser -S -H -u 4202 -G apache apache

WORKDIR /usr/local/apache2
COPY ./ ./

RUN chmod 750 ./ca/ ./ca/client_ca_chains ./ca/client_issuing_cas ./start.sh && \
    chmod 440 ./ca/client_ca_chains/*.crt ./ca/client_issuing_cas/*.crt && \
    chmod 644 ./conf/httpd.conf ./conf/extra/host.conf ./conf/extra/host-ssl.conf ./conf/extra/httpd-ssl.conf && \
    chown -hR apache:apache /usr/local/apache2/

# setting non existing default values, see host-ssl.conf IfFile tests
ENV SSL_CERTIFICATE_CHAIN_FILE="/does/not/exist"

# file with trusted client certificate issuing CAs, modifies the "Acceptable client certificate CA names" send to the client, uses all from SSL_CA_CERTIFICATE_FILE or SSL_CA_CERTIFICATE_PATH if not set or empty, not used by default, overrides SSL_CA_DN_REQUEST_PATH if not empty
ENV SSL_CA_DN_REQUEST_FILE=""

# file with trusted full CA chains for validating client certificates, not used by default, overrides SSL_CA_CERTIFICATE_PATH if not empty
ENV SSL_CA_CERTIFICATE_FILE=""

# folder with trusted client certificate issuing CAs, modifies the "Acceptable client certificate CA names" send to the client, uses all from SSL_CA_CERTIFICATE_FILE or SSL_CA_CERTIFICATE_PATH if not set or empty
ENV SSL_CA_DN_REQUEST_PATH="ca/client_issuing_cas"

# folder with trusted full CA chains for validating client certificates
ENV SSL_CA_CERTIFICATE_PATH="ca/client_ca_chains"

# setting default value - client certificate required, use 'optional' when using OIDC
ENV SSL_VERIFY_CLIENT="require"

# expected client certificate subject DN country (C) values
ENV SSL_EXPECTED_CLIENT_S_DN_C_VALUES="'DE'"

# expected client certificate issuer DN common-name (CN) values
ENV SSL_EXPECTED_CLIENT_I_DN_CN_VALUES="'GEANT TLS ECC 1', 'HARICA OV TLS ECC', 'GEANT TLS RSA 1', 'HARICA OV TLS RSA', 'GEANT S/MIME ECC 1', 'HARICA Client Authentication ECC', 'HARICA S/MIME ECC', 'GEANT S/MIME RSA 1', 'HARICA Client Authentication RSA', 'HARICA S/MIME RSA', 'DFN-Verein Global Issuing CA', 'Fraunhofer User CA - G02', 'D-TRUST SSL Class 3 CA 1 2009', 'Sectigo RSA Organization Validation Secure Server CA', 'GEANT OV RSA CA 4', 'GEANT Personal CA 4', 'GEANT eScience Personal CA 4', 'Sectigo ECC Organization Validation Secure Server CA', 'GEANT OV ECC CA 4', 'GEANT Personal ECC CA 4', 'GEANT eScience Personal ECC CA 4', 'D-TRUST Limited Basic CA 1-2 2019', 'D-TRUST Limited Basic CA 1-3 2019'"

# timeout (seconds) for reverse proxy to app server http connection, time the proxy waits for a reply
ENV PROXY_PASS_TIMEOUT_HTTP=60

# timeout (seconds) for reverse proxy to app server ws connection, time the proxy waits for a reply
ENV PROXY_PASS_TIMEOUT_WS=60

# connection timeout (seconds) for reverse proxy to app server http connection, time the proxy waits for a connection to be established
ENV PROXY_PASS_CONNECTION_TIMEOUT_HTTP=30

# connection timeout (seconds) for reverse proxy to app server ws connection, time the proxy waits for a connection to be established
ENV PROXY_PASS_CONNECTION_TIMEOUT_WS=30

# server context path: / character at start, no / character at end
ENV SERVER_CONTEXT_PATH="/bpe"

USER apache
ENTRYPOINT [ "sh", "./start.sh" ]
HEALTHCHECK --interval=10s --timeout=15s --start-period=10s --retries=5 CMD nc -zv localhost 80 && nc -zv localhost 443 || exit 1