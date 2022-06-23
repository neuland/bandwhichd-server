FROM eclipse-temurin:17.0.3_7-jre-alpine as build
RUN set -euxo pipefail; \
    apk update; \
    apk add --no-cache \
        bash \
        ca-certificates \
        ;
ADD https://github.com/sbt/sbt/releases/download/v1.6.2/sbt-1.6.2.tgz /tmp/sbt-1.6.2.tgz
RUN set -euxo pipefail; \
    tar --extract --gzip --file /tmp/sbt-1.6.2.tgz --directory /opt; \
    rm -rf /tmp/sbt-1.6.2.tgz; \
    chown -hR root:root /opt/sbt
WORKDIR /opt/sbt
RUN set -euxo pipefail; \
    /opt/sbt/bin/sbt sbtVersion
COPY --chown=root:root . /tmp/bandwhichd-server/
WORKDIR /tmp/bandwhichd-server
RUN /opt/sbt/bin/sbt assembly

FROM eclipse-temurin:17.0.3_7-jre-alpine
LABEL org.opencontainers.image.authors="neuland Open Source Maintainers <opensource@neuland-bfi.de>"
LABEL org.opencontainers.image.url="https://github.com/neuland/bandwhichd-server"
LABEL org.opencontainers.image.documentation="https://github.com/neuland/bandwhichd-server"
LABEL org.opencontainers.image.source="https://github.com/neuland/bandwhichd-server"
LABEL org.opencontainers.image.vendor="neuland – Büro für Informatik GmbH"
LABEL org.opencontainers.image.licenses="Apache-2.0"
LABEL org.opencontainers.image.title="bandwhichd-server"
LABEL org.opencontainers.image.description="bandwhichd server collecting measurements and calculating statistics"
LABEL org.opencontainers.image.version="0.5.1"
USER guest
ENTRYPOINT ["/opt/java/openjdk/bin/java"]
CMD ["-jar", "/opt/bandwhichd-server.jar"]
EXPOSE 8080
HEALTHCHECK --interval=5s --timeout=1s --start-period=2s --retries=2 \
    CMD wget --spider http://localhost:8080/v1/health || exit 1
COPY --from=build --chown=root:root /tmp/bandwhichd-server/target/scala-3.1.2/bandwhichd-server-assembly-0.5.1.jar /opt/bandwhichd-server.jar