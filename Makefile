ifeq ($(OS),Windows_NT)
    MVNW = mvn.cmd
else
    MVNW = ./mvnw
endif

build:
	$(MVNW) clean install -DskipTests=true

run:
	$(MVNW) spring-boot:run

openapi-generate:
	$(MVNW) org.openapitools:openapi-generator-maven-plugin:generate@openapi
