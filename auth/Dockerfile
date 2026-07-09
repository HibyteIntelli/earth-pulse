FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw package -DskipTests -B


FROM eclipse-temurin:25-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring && chown spring:spring /app
USER spring:spring

COPY --from=build /app/target/*.jar app.jar
COPY --chown=spring:spring data/rockyou.txt /app/rockyou.txt

EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
