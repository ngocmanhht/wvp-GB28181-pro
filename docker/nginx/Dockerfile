FROM ubuntu:24.04 AS builder


RUN apt-get update && \
    apt-get install -y nodejs npm && \
    npm install -g yarn --registry=https://registry.npmmirror.com && \
    rm -rf /var/lib/apt/lists/*

COPY ./web /build
WORKDIR /build

RUN yarn config set registry https://registry.npmmirror.com
RUN yarn install --network-timeout 600000 --ignore-engines
RUN yarn build:prod

WORKDIR /src/main/resources
RUN ls

WORKDIR /src/main/resources/static
RUN ls

FROM nginx:alpine

ARG TZ=Asia/Shanghai


COPY --from=builder /src/main/resources/static /opt/dist

CMD ["nginx","-g","daemon off;"]

