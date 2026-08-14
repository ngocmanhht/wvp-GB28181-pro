#!/bin/bash

# Get the current date as label (format：YYYYMMDD）
date_tag=$(date +%Y%m%d)

# Switch to the directory one level above the directory where the script is located as the working directory
cd "$(dirname "$0")/.." || {
    echo "Error: Unable to switch to parent directory"
    exit 1
}
echo "Working directory has been changed to：$(pwd)"

# Check private repository environment variables
if [ -z "$DOCKER_REGISTRY" ]; then
    echo "DOCKER_REGISTRY environment variable not set"
    read -p "Please enter the private Docker registration library address (please leave it blank if you do not want to push it)）: " input_registry
    docker_registry="$input_registry"
else
    docker_registry="$DOCKER_REGISTRY"
fi

# Define the image to be built and the corresponding Dockerfile path (relative to the current working directory）
images=(
    "wvp-service:docker/wvp/Dockerfile"
    "wvp-nginx:docker/nginx/Dockerfile"
)

# Function to build the image
build_image() {
    local image_name="$1"
    local dockerfile_path="$2"
    
    # Check if Dockerfile exists
    if [ ! -f "$dockerfile_path" ]; then
        echo "Error: not foundDockerfile - \"$dockerfile_path\"，Skip build"
        return 1
    fi
    
    # Build image
    local full_image_name="${image_name}:${date_tag}"
    echo
    echo "=============================================="
    echo "Start building the image：${full_image_name}"
    echo "Dockerfilepath：${dockerfile_path}"
    
    docker build -t "${full_image_name}" -f "${dockerfile_path}" .
    if [ $? -ne 0 ]; then
        echo "mirror${full_image_name}Build failed"
        return 1
    fi
    
    # Push the image (if the warehouse address is set）
    if [ -n "$docker_registry" ]; then
        local registry_image="${docker_registry}/${full_image_name}"
        echo "Tag the image：${registry_image}"
        docker tag "${full_image_name}" "${registry_image}"
        
        echo "Push the image to the registry"
        docker push "${registry_image}"
        if [ $? -eq 0 ]; then
            echo "mirror${registry_image}Push successful"
        else
            echo "mirror${registry_image}Push failed"
        fi
    else
        echo "The registration library address is not provided and the push is not performed."
    fi
    echo "=============================================="
    echo
}

# Build all images in a loop
for item in "${images[@]}"; do
    IFS=':' read -r image_name dockerfile_path <<< "$item"
    build_image "$image_name" "$dockerfile_path"
done

echo "All image processing completed"
exit 0
