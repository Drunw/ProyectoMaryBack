# Usamos una imagen base para Quarkus con OpenJDK
FROM quay.io/quarkus/ubi-quarkus-mandrel-builder-image:17-java17 as build

# Copiamos el proyecto y lo construimos
COPY . /project
WORKDIR /project
RUN ./mvnw clean package -DskipTests

# Imagen base para la ejecución
FROM quay.io/quarkus/quarkus-distroless-image:1.0

# Copiamos el archivo JAR desde la imagen de construcción
COPY --from=build /project/target/*-runner.jar /application.jar

# Exponemos el puerto para las peticiones
EXPOSE 8080

# Comando para ejecutar la aplicación
CMD ["java", "-jar", "/application.jar"]
