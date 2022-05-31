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
RUN set -euxo pipefail; \
    /opt/sbt/bin/sbt assembly

FROM eclipse-temurin:17.0.3_7-jre-alpine
COPY --from=build --chown=root:root /tmp/bandwhichd-server/target/scala-3.1.2/bandwhichd-server-assembly-0.1.0.jar /opt/bandwhichd-server.jar
USER guest
ENTRYPOINT java -jar /opt/bandwhichd-server.jar