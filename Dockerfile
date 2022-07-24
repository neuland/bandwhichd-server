FROM sbtscala/scala-sbt:eclipse-temurin-17.0.3_1.7.1_3.1.3 as build
WORKDIR /tmp/bandwhichd-server
COPY --chown=sbtuser:sbtuser . ./
RUN sbt assembly

FROM eclipse-temurin:17.0.3_7-jre-alpine
LABEL org.opencontainers.image.authors="neuland Open Source Maintainers <opensource@neuland-bfi.de>"
LABEL org.opencontainers.image.url="https://github.com/neuland/bandwhichd-server"
LABEL org.opencontainers.image.documentation="https://github.com/neuland/bandwhichd-server"
LABEL org.opencontainers.image.source="https://github.com/neuland/bandwhichd-server"
LABEL org.opencontainers.image.vendor="neuland – Büro für Informatik GmbH"
LABEL org.opencontainers.image.licenses="Apache-2.0"
LABEL org.opencontainers.image.title="bandwhichd-server"
LABEL org.opencontainers.image.description="bandwhichd server collecting measurements and calculating statistics"
LABEL org.opencontainers.image.version="0.6.0-rc7"
USER guest
ENTRYPOINT ["/opt/java/openjdk/bin/java"]
CMD ["-jar", "/opt/bandwhichd-server.jar"]
EXPOSE 8080
STOPSIGNAL SIGTERM
COPY --from=build --chown=root:root /tmp/bandwhichd-server/target/scala-3.1.3/bandwhichd-server-assembly-0.6.0-rc7.jar /opt/bandwhichd-server.jar