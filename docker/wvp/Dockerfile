# syntax=docker/dockerfile:1
# ==============================================================================
# Multi-stage optimized Dockerfile for WVP-GB28181-PRO Master Control Plane
# Tối ưu hóa: Layer Caching + Pre-installed Maven + BuildKit Cache Mount + Multi-thread
# ==============================================================================

# STAGE 1: Builder (Sử dụng image có sẵn Maven 3.9 & JDK 21 để không tốn thời gian apt-get)
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Tối ưu 1: Copy riêng pom.xml trước để cache toàn bộ thư viện dependencies
COPY pom.xml /build/pom.xml

# Tối ưu 2: Tải dependencies với BuildKit cache mount để tái sử dụng giữa các lần build
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -B || true

# Tối ưu 3: Copy mã nguồn và biên dịch song song đa nhân (-T 1C)
COPY src /build/src
RUN --mount=type=cache,target=/root/.m2 mvn package -Dmaven.test.skip=true -T 1C

# ==============================================================================
# STAGE 2: Runtime Runner Image (Nhẹ và khởi động tức thì)
# ==============================================================================
FROM ringcentral/jdk:21.0.9

EXPOSE 18978/tcp
EXPOSE 8116/tcp
EXPOSE 8116/udp

RUN mkdir -p /opt/wvp /opt/ylcx/wvp
WORKDIR /opt/wvp

# Copy file jar đã biên dịch từ Stage 1
COPY --from=builder /build/target/wvp-pro-*.jar /opt/wvp/wvp.jar
COPY ./docker/wvp/wvp /opt/wvp/
COPY ./docker/wvp/wvp /opt/ylcx/wvp/

ENTRYPOINT ["java", "-Xms512m", "-Xmx1024m", "-XX:+HeapDumpOnOutOfMemoryError", "-XX:HeapDumpPath=/opt/ylcx/", "-jar", "wvp.jar", "--spring.config.additional-location=file:/opt/ylcx/wvp/"]
