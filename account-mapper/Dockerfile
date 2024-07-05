FROM eclipse-temurin:17-jdk-jammy as build
WORKDIR /workspace/quickbase/account-mapper
COPY . ./
RUN ./mvnw clean install -DskipTests
ENTRYPOINT ["java", "-Dspring.data.mongodb.uri=mongodb://mongodb:27017/name_of_your_db", "-jar","target/account-mapper-1.0.0.0.jar"]