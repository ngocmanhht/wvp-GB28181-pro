# ==============================================================================
# Multi-stage Dockerfile for WVP Web Frontend & Nginx Proxy
# ==============================================================================

# STAGE 1: Frontend Builder (Node.js 18 with yarn)
FROM node:18-alpine AS builder

WORKDIR /build

# Webpack 4 crypto hashing requires OpenSSL legacy provider on Node 17+
ENV NODE_OPTIONS="--openssl-legacy-provider"

COPY ./web/package.json ./
RUN yarn config set registry https://registry.npmmirror.com && \
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
