# Usar la imagen de construcción UBI Quarkus con soporte para GraalVM y JDK 17
FROM quay.io/quarkus/ubi-quarkus-mandrel-builder-image:jdk-17 AS builder

# Establecer el directorio de trabajo
WORKDIR /work

# Copiar los archivos de configuración de Maven
COPY pom.xml ./
COPY src ./src

# Realizar la compilación en modo nativo
RUN mvn clean install -Pnative -Dquarkus.native.container-build=true -DskipTests

# Usar una imagen base de UBI minimal para la ejecución
FROM registry.access.redhat.com/ubi8/ubi-minimal:8.5

# Establecer las variables de entorno para Quarkus
ENV LANG='en_US.UTF-8' \
    LANGUAGE='en_US:en' \
    LC_ALL='en_US.UTF-8' \
    QUARKUS_PROFILE=prod

# Copiar el ejecutable nativo desde la imagen de construcción
COPY --from=builder /work/target/*-runner /application

# Exponer el puerto que usará Cloud Run
EXPOSE 8080

# Configurar el comando de entrada
CMD ["./application"]
