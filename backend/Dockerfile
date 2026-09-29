# syntax=docker/dockerfile:1

# ---- Compilación: JDK completo y Maven (con caché de dependencias entre builds) ----
FROM eclipse-temurin:25-jdk AS compilacion
WORKDIR /codigo

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:go-offline

COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q package -DskipTests \
    && java -Djarmode=tools -jar target/workflow-*.jar extract --layers --launcher --destination /extraido

# ---- Ejecución: solo el JRE y la aplicación, sin privilegios de root ----
FROM eclipse-temurin:25-jre
RUN useradd --system --uid 1001 --no-create-home workflow \
    && mkdir -p /datos/jwt && chown workflow /datos/jwt
WORKDIR /app

# Capas de menos a más cambiantes: al tocar el código solo se reconstruye la última
COPY --from=compilacion /extraido/dependencies/ ./
COPY --from=compilacion /extraido/spring-boot-loader/ ./
COPY --from=compilacion /extraido/snapshot-dependencies/ ./
COPY --from=compilacion /extraido/application/ ./

USER workflow
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "org.springframework.boot.loader.launch.JarLauncher"]
