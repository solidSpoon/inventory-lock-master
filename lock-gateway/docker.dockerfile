FROM openjdk:8-jre-slim
COPY ./target/lock-gateway-0.0.1-SNAPSHOT.jar /tmp
WORKDIR /tmp
EXPOSE 10002
ENTRYPOINT ["java","-jar", "-Dfile.encoding=UTF-8", "lock-gateway-0.0.1-SNAPSHOT.jar"]