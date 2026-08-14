Can be in the current directory:
Use `docker compose up -d` to run directly.
Use `docker compose up -d --build --force-recreate` to force rebuild the images of all services and delete the old containers and run again

 `.env` is used to configure environment variables. After configuring here, other configurations will be automatically linked.

 `build.sh` is used to build an image with the date as the tag and push it to the specified container registry (you can use `Git Bash` to run under Windows)


The functions of other files are not yet clear.