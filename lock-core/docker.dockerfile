FROM openjdk:8-jre-slim
COPY ./target/lock-core-0.0.1-SNAPSHOT.jar /tmp
WORKDIR /tmp
EXPOSE 8080
ENTRYPOINT ["java","-jar", "-Dfile.encoding=UTF-8", "lock-core-0.0.1-SNAPSHOT.jar"]