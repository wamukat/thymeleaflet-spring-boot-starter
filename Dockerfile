FROM maven:3.9.8-eclipse-temurin-17 AS build

WORKDIR /app
COPY . .

RUN ./mvnw -f pom.xml -DskipTests install
RUN ./mvnw -f sample/pom.xml -DskipTests package

FROM eclipse-temurin:17-jre

WORKDIR /app
ENV PORT=6006
ENV SPRING_PROFILES_ACTIVE=koyeb
EXPOSE 6006

COPY --from=build /app/sample/target/thymeleaflet-sample-*.jar /app/app.jar

CMD ["java", "-jar", "/app/app.jar"]
