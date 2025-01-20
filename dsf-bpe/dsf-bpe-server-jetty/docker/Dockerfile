FROM debian:buster-slim AS builder
RUN adduser --system --no-create-home --group --uid 2202 java
WORKDIR /opt/bpe
COPY --chown=root:java ./ ./
RUN chown root:java ./ && \
    chmod 750 ./ ./ca ./conf ./lib ./lib_external ./process ./ui ./dsf_bpe_start.sh ./healthcheck.sh && \
	chmod 440 ./ca/ClientCertCaChains.pem ./ca/ServerCertRootCAs.pem ./conf/log4j2.xml ./dsf_bpe.jar ./lib/*.jar && \
	chmod 1775 ./log


FROM azul/zulu-openjdk:17-jre-headless
LABEL org.opencontainers.image.source=https://github.com/datasharingframework/dsf
LABEL org.opencontainers.image.description="DSF BPE Server"
LABEL org.opencontainers.image.licenses="Apache License, Version 2.0"

EXPOSE 8080

RUN adduser --system --no-create-home --group --uid 2202 java && \
    apt update && apt dist-upgrade -y && apt install curl -y

WORKDIR /opt/bpe
COPY --from=builder /opt/bpe ./

USER java
ENTRYPOINT ["./dsf_bpe_start.sh"]
ENV DEV_DSF_SERVER_API_HOST="0.0.0.0"
ENV DEV_DSF_SERVER_API_PORT="8080"
ENV DEV_DSF_SERVER_STATUS_PORT="10000"
ENV DEV_DSF_SERVER_CONTEXT_PATH="/bpe"
HEALTHCHECK --interval=10s --timeout=15s --start-period=10s --retries=5 CMD [ "./healthcheck.sh" ]