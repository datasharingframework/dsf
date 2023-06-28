FROM debian:buster-slim AS builder
RUN adduser --system --no-create-home --group --uid 2101 java
WORKDIR /opt/fhir
COPY --chown=root:java ./ ./
RUN chown root:java ./ && \
    chmod 750 ./ ./conf ./lib ./dsf_fhir_start.sh && \
	chmod 440 ./conf/log4j2.xml ./conf/bundle.xml ./dsf_fhir.jar ./dsf_status_client.jar ./lib/*.jar && \
	chmod 1775 ./log


FROM azul/zulu-openjdk:17-jre-headless
LABEL org.opencontainers.image.source=https://github.com/datasharingframework/dsf
LABEL org.opencontainers.image.description="DSF FHIR Server"
LABEL org.opencontainers.image.licenses="Apache License, Version 2.0"

EXPOSE 8080

RUN adduser --system --no-create-home --group --uid 2101 java && apt update && \
    apt dist-upgrade -y

WORKDIR /opt/fhir
COPY --from=builder /opt/fhir ./

USER java
ENTRYPOINT ["./dsf_fhir_start.sh"]
ENV DEV_DSF_SERVER_API_HOST="0.0.0.0"
ENV DEV_DSF_SERVER_API_PORT="8080"
ENV DEV_DSF_SERVER_STATUS_PORT="10000"
ENV DEV_DSF_SERVER_CONTEXT_PATH="/fhir"
HEALTHCHECK --interval=10s --timeout=15s --start-period=10s --retries=5 CMD [ "java", "-cp", "dsf_status_client.jar", "dev.dsf.common.status.client.StatusClient" ]