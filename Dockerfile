# Usar una imagen base que contenga Java y Maven
FROM registry.access.redhat.com/ubi8/openjdk-17:1.16 AS builder

# Directorio de trabajo en la imagen
WORKDIR /app

# Copiar el pom.xml para descargar dependencias
COPY pom.xml .

# Descargar dependencias
RUN mvn dependency:go-offline

# Copiar el resto del código fuente
COPY src ./src

# Compilar la aplicación
RUN mvn package

# Imagen base para ejecutar la aplicación
FROM  registry.access.redhat.com/ubi8/openjdk-17:1.16

# Directorio de trabajo en la imagen
WORKDIR /app

# Copiar el archivo JAR construido desde la etapa anterior
COPY --from=builder /app/target/*.jar app.jar

# Puerto expuesto por la aplicación
EXPOSE 8080

# Comando para ejecutar la aplicación al iniciar el contenedor
CMD ["java", "-jar", "app.jar"]