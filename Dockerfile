# ===== STAGE 1: BUILD =====
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# Copia Maven Wrapper e POM primeiro (melhor cache)
COPY mvnw .
COPY .mvn/ .mvn/
COPY pom.xml .

RUN chmod +x mvnw

# Baixa dependências para cache
RUN ./mvnw dependency:go-offline -B

# Copia código-fonte
COPY src ./src

# Build do projeto (gera o JAR)
RUN ./mvnw clean package -DskipTests

# ===== STAGE 2: RUNTIME =====
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copia o JAR gerado no stage de build
COPY --from=build /app/target/questionario-0.0.1-SNAPSHOT.jar app.jar

# Render define a variável $PORT. Vamos mandar o Spring subir nessa porta.
ENV JAVA_OPTS=""

# (Opcional) só informativo; Render não depende do EXPOSE, mas não atrapalha
EXPOSE 8080

# Comando de inicialização
CMD ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar app.jar"]
