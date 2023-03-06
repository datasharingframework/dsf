FROM debian:buster-slim AS builder
RUN adduser --system --no-create-home --group --uid 2101 java
WORKDIR /opt/fhir
COPY --chown=root:java ./ ./
RUN chown root:java ./ && \
    chmod 750 ./ ./conf ./lib ./dsf_fhir_start.sh && \
	chmod 440 ./conf/log4j2.xml ./dsf_fhir.jar ./dsf_status_client.jar ./lib/*.jar && \
	chmod 1775 ./log


FROM azul/zulu-openjdk:17-jre-headless
LABEL maintainer="hauke.hund@hs-heilbronn.de"

EXPOSE 8080

RUN adduser --system --no-create-home --group --uid 2101 java && apt update && \
    apt dist-upgrade -y

WORKDIR /opt/fhir
COPY --from=builder /opt/fhir ./

USER java
ENTRYPOINT ["./dsf_fhir_start.sh"]
ENV JETTY_HOST="0.0.0.0"
ENV JETTY_PORT="8080"
ENV JETTY_STATUS_PORT="10000"
ENV JETTY_CONTEXT_PATH="/fhir"
HEALTHCHECK --interval=10s --timeout=15s --start-period=10s --retries=5 CMD [ "java", "-cp", "dsf_status_client.jar", "dev.dsf.common.status.client.StatusClient" ]