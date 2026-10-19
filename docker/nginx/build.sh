#!/bin/bash
set -e

version=2.7.4

if [ -f "./dist/static/js/config.js" ]; then
    rm -f ./dist/static/js/config.js
    cp ./config.js ./dist/static/js/
fi

docker build -t polaris-nginx:${version} .

if [ -n "$DOCKER_REGISTRY" ]; then
    docker tag polaris-nginx:${version} "${DOCKER_REGISTRY}/polaris-nginx:${version}"
    docker tag polaris-nginx:${version} "${DOCKER_REGISTRY}/polaris-nginx:latest"
fi