# ============================================================
# STAGE 1 — Builder: compila el JAR con Maven y JDK 21
# ============================================================
FROM maven:3.9-eclipse-temurin-21 AS builder

# Carpeta de trabajo dentro del contenedor
WORKDIR /build

# Copiamos primero solo el pom.xml para aprovechar la caché de capas
# de Docker: si no cambia el pom, no se vuelven a descargar dependencias
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Ahora copiamos el código fuente y compilamos
COPY src ./src
RUN mvn clean package -DskipTests -B

# ============================================================
# STAGE 2 — Runtime: imagen ligera con solo JRE para ejecutar
# ============================================================
FROM eclipse-temurin:21-jre AS runtime

# Carpeta donde vivirá la app
WORKDIR /app

# Copiamos SOLO el JAR resultante de la stage anterior
COPY --from=builder /build/target/*.jar app.jar

# Puerto en el que escucha Spring Boot
EXPOSE 8080

# Comando que arranca la app cuando el contenedor se inicia
ENTRYPOINT ["java", "-jar", "app.jar"]