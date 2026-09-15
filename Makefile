include docker/Makefile

ifeq ($(OS),Windows_NT)
    MVNW = mvn.cmd
    LOADENV = powershell -NoProfile -ExecutionPolicy Bypass -File scripts\load-env.ps1 &&
else
    MVNW = ./mvnw
    LOADENV = set -a && . ./local.env && set +a &&
endif

build:
	$(MVNW) clean install -DskipTests=true

run:
	$(LOADENV) $(MVNW) spring-boot:run

openapi-generate:
	$(MVNW) org.openapitools:openapi-generator-maven-plugin:generate@openapi
