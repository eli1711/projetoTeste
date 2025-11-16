FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# Copia primeiro os arquivos do Maven wrapper e POM (para cache eficiente)
COPY mvnw .
COPY .mvn/ .mvn/
COPY pom.xml .

# Dá permissão e baixa dependências
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

# Copia o código fonte
COPY src ./src

# Build do projeto
RUN ./mvnw clean package -DskipTests

# Imagem final (usa JRE para imagem menor)
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copia o JAR com o nome correto baseado no seu pom.xml
COPY --from=build /app/target/questionario-0.0.1-SNAPSHOT.jar app.jar

# Expõe a porta
EXPOSE 8080

# Comando para executar
ENTRYPOINT ["java", "-jar", "app.jar"]