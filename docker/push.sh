#!/bin/bash
set -e

version=2.7.4

if [ -z "$DOCKER_REGISTRY" ]; then
    echo "ERROR: DOCKER_REGISTRY environment variable is not set."
    echo "Usage: DOCKER_REGISTRY=your-registry.example.com ./push.sh"
    exit 1
fi

services=("media" "mysql" "redis" "wvp" "nginx")

for svc in "${services[@]}"; do
    echo "Pushing ${DOCKER_REGISTRY}/ylcx-${svc}:${version} ..."
    docker push "${DOCKER_REGISTRY}/ylcx-${svc}:${version}"
    docker push "${DOCKER_REGISTRY}/ylcx-${svc}:latest"
done

echo "All images pushed successfully to ${DOCKER_REGISTRY}!"