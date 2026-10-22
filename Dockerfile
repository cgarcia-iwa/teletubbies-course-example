# Imagen base interna de IWA (Java 25 sobre distroless). Su entrypoint ya ejecuta
# "java -jar", por eso el CMD de abajo solo lleva la ruta del jar.
FROM iwaconsolti/iwa-distroless-java-25-base:1.0.28

# Alternativa sin la imagen de IWA: una imagen pública de Java (p. ej. Temurin).
# Cambia el FROM de arriba por este y usa ENTRYPOINT en lugar de CMD,
# porque esta imagen no trae "java -jar" como entrypoint:
#
#   FROM eclipse-temurin:25-jre
#   ...
#   ENTRYPOINT ["java", "-jar", "/app/teletubbies-api.jar"]

WORKDIR /app

COPY target/teletubbies-api.jar /app/teletubbies-api.jar

EXPOSE 8080
CMD ["/app/teletubbies-api.jar"]
