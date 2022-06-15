FROM openjdk:8-jre-slim
COPY ./target/lock-eureka-0.0.1-SNAPSHOT.jar /tmp
WORKDIR /tmp
EXPOSE 8761
EXPOSE 8765
ENTRYPOINT ["java","-jar", "-Dfile.encoding=UTF-8", "lock-eureka-0.0.1-SNAPSHOT.jar"]