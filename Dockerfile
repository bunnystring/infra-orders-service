# Usa una imagen base de Java 17
FROM eclipse-temurin:17-jre

# Crea un directorio para la app
WORKDIR /app

# Copia el jar compilado al contenedor
COPY target/*.jar app.jar

# Expone el puerto que utiliza el gateway
EXPOSE 8080

# Comando para arrancar el servidor
CMD ["java", "-jar", "app.jar"]