FROM debian:trixie-slim AS builder
WORKDIR /opt/fhir
COPY --chown=root:2101 ./ ./
RUN chown root:2101 ./ && \
    chmod 750 ./ ./ca ./conf ./lib ./ui ./dsf_fhir_start.sh ./healthcheck.sh && \
    chmod 440 ./ca/client_cert_ca_chains.pem ./ca/server_cert_root_cas.pem ./conf/log4j2.xml ./conf/bundle.xml ./dsf_fhir.jar ./lib/*.jar && \
    chmod 1775 ./log


FROM azul/zulu-openjdk:17-jre-headless
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