FROM httpd:2.4-alpine
LABEL org.opencontainers.image.source=https://github.com/datasharingframework/dsf
LABEL org.opencontainers.image.description="DSF BPE Reverse Proxy"
LABEL org.opencontainers.image.licenses="Apache License, Version 2.0"

WORKDIR /usr/local/apache2
COPY ./ ./
RUN chown daemon:daemon ./ca/ && \
	chmod 750 ./ca/ ./start.sh && \
	chmod 440 ./ca/ClientCertCaChains.pem ./ca/ClientCertIssuingCAs.pem && \
    chmod 644 ./conf/httpd.conf ./conf/extra/host.conf ./conf/extra/host-ssl.conf ./conf/extra/httpd-ssl.conf && \
    apk update && apk upgrade && rm -vrf /var/cache/apk/*

# setting non existing default values, see host-ssl.conf IfFile tests
ENV SSL_CERTIFICATE_CHAIN_FILE="/does/not/exist"

# trusted client certificate issuing CAs, modifies the "Acceptable client certificate CA names" send to the client, uses all from SSLCACertificateFile if set to a non existing file e.g. /does/not/exist 
ENV SSL_CA_DN_REQUEST_FILE="ca/ClientCertIssuingCAs.pem"

# trusted full CA chains for validating client certificates
ENV SSL_CA_CERTIFICATE_FILE="ca/ClientCertCaChains.pem"

# setting default value - client certificate required, use 'optional' when using OIDC 
ENV SSL_VERIFY_CLIENT="require"

# expected client certificate subject DN country (C) values
ENV SSL_EXPECTED_CLIENT_S_DN_C_VALUES="'DE'"

# expected client certificate issuer DN common-name (CN) values
ENV SSL_EXPECTED_CLIENT_I_DN_CN_VALUES="'GEANT TLS ECC 1', 'HARICA OV TLS ECC', 'GEANT TLS RSA 1', 'HARICA OV TLS RSA', 'GEANT S/MIME ECC 1', 'HARICA S/MIME ECC', 'GEANT S/MIME RSA 1', 'HARICA S/MIME RSA', 'DFN-Verein Global Issuing CA', 'Fraunhofer User CA - G02', 'D-TRUST SSL Class 3 CA 1 2009', 'Sectigo RSA Organization Validation Secure Server CA', 'GEANT OV RSA CA 4', 'GEANT Personal CA 4', 'GEANT eScience Personal CA 4', 'Sectigo ECC Organization Validation Secure Server CA', 'GEANT OV ECC CA 4', 'GEANT Personal ECC CA 4', 'GEANT eScience Personal ECC CA 4', 'D-TRUST Limited Basic CA 1-2 2019', 'D-TRUST Limited Basic CA 1-3 2019'"

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

ENTRYPOINT [ "sh", "./start.sh" ]