# ==============================================================================
# Multi-stage Dockerfile for WVP Web Frontend & Nginx Proxy
# ==============================================================================

# STAGE 1: Frontend Builder (Node.js 20 with yarn & BuildKit cache)
FROM node:20-alpine AS builder

WORKDIR /build

COPY ./web/package.json ./
RUN --mount=type=cache,target=/usr/local/share/.cache/yarn \
    yarn install --network-timeout 600000 --ignore-engines

COPY ./web /build
RUN yarn build:prod

# STAGE 2: Lightweight Nginx Runtime
FROM nginx:alpine

ARG TZ=Asia/Ho_Chi_Minh
ENV TZ=${TZ}

COPY --from=builder /src/main/resources/static /opt/dist
COPY ./docker/nginx/config.js /opt/dist/static/js/config.js

CMD ["nginx", "-g", "daemon off;"]
