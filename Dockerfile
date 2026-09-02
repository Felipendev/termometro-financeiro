# syntax=docker/dockerfile:1

# ---------------------------------------------------------------- 1) front-end
FROM node:20-alpine AS frontend
WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
# Back-end e front-end no mesmo serviço/origem: chamadas de API relativas ao caminho onde as
# rotas @RestController ficam (ver WebConfig) — nada de host fixo, funciona em qualquer domínio.
ENV VITE_API_BASE_URL=/termometro/api
RUN npm run build

# ---------------------------------------------------------------- 2) back-end
FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /app
COPY pom.xml ./
RUN mvn -q -B dependency:go-offline
COPY src ./src
# Vai pra classpath:/static — é daqui que o Spring Boot serve o front-end na raiz do domínio.
COPY --from=frontend /app/frontend/dist ./src/main/resources/static
RUN mvn -q -B package -DskipTests

# ---------------------------------------------------------------- 3) runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=backend /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
