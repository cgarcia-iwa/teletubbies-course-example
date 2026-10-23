PROJECT_NAME ?= teletubbies-course-example

docker-up: ;docker compose --project-name $(PROJECT_NAME) --env-file ./local.env -f docker/docker-compose.yml up --build -d
docker-down-api: ;docker compose --project-name $(PROJECT_NAME) --env-file ./local.env -f docker/docker-compose.yml rm --stop --force api
docker-update: ;docker compose --project-name $(PROJECT_NAME) --env-file ./local.env -f docker/docker-compose.yml up --force-recreate --build -d
