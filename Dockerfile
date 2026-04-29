FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY merchantops-domain/pom.xml merchantops-domain/pom.xml
COPY merchantops-infra/pom.xml merchantops-infra/pom.xml
COPY merchantops-api/pom.xml merchantops-api/pom.xml

RUN chmod +x mvnw
RUN ./mvnw --batch-mode -pl merchantops-api -am -DskipTests dependency:go-offline

COPY merchantops-domain/src merchantops-domain/src
COPY merchantops-infra/src merchantops-infra/src
COPY merchantops-api/src merchantops-api/src

RUN ./mvnw --batch-mode -pl merchantops-api -am -DskipTests package \
    && cp merchantops-api/target/merchantops-api-*.jar /workspace/merchantops-api.jar

FROM eclipse-temurin:21-jre-jammy AS runtime

RUN groupadd --system merchantops \
    && useradd --system --gid merchantops --create-home --home-dir /app merchantops

WORKDIR /app

COPY --from=build /workspace/merchantops-api.jar /app/app.jar

EXPOSE 8080

USER merchantops

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
