# Multi-stage: JDK para compilar, JRE para rodar (imagem final menor).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Baixa as dependências primeiro para aproveitar o cache do Docker
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/connecta.jar app.jar
EXPOSE 7000
ENTRYPOINT ["java", "-jar", "app.jar"]
