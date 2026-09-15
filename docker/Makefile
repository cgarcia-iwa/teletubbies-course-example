PROJECT_NAME ?= teletubbies-course-example

docker-build: ;docker compose --project-name $(PROJECT_NAME) --env-file ./local.env -f docker/docker-compose.yml up --build -d
docker-update: ;docker compose --project-name $(PROJECT_NAME) --env-file ./local.env -f docker/docker-compose.yml up --force-recreate --build -d
