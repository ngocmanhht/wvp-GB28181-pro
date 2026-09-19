# syntax=docker/dockerfile:1
# ==============================================================================
# Multi-stage optimized Dockerfile for WVP-GB28181-PRO Master Control Plane
# ==============================================================================

# STAGE 1: Builder (Có sẵn Maven 3.9 & JDK 21)
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

COPY pom.xml /build/pom.xml
COPY src /build/src

# Biên dịch song song đa nhân (-T 1C) với BuildKit cache mount để tái sử dụng thư viện
RUN --mount=type=cache,target=/root/.m2 mvn clean package -Dmaven.test.skip=true -Dgit.commit.id.skip=true -T 1C

# ==============================================================================
# STAGE 2: Runtime Runner Image
# ==============================================================================
FROM ringcentral/jdk:21.0.9

EXPOSE 18978/tcp
EXPOSE 8116/tcp
EXPOSE 8116/udp

RUN mkdir -p /opt/wvp /opt/ylcx/wvp
WORKDIR /opt/wvp

COPY --from=builder /build/target/wvp-pro-*.jar /opt/wvp/wvp.jar
COPY ./docker/wvp/wvp /opt/wvp/
COPY ./docker/wvp/wvp /opt/ylcx/wvp/

ENTRYPOINT ["java", "-Xms512m", "-Xmx1024m", "-XX:+HeapDumpOnOutOfMemoryError", "-XX:HeapDumpPath=/opt/ylcx/", "-jar", "wvp.jar", "--spring.config.additional-location=file:/opt/ylcx/wvp/"]
