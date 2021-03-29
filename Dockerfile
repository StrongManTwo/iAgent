#FROM openjdk:8u275
FROM openjdk:11
MAINTAINER HanKeQi
VOLUME /tmp
ADD ./target/agent-zuul.jar app.jar
EXPOSE 9082
ENTRYPOINT ["java", "-jar", "app.jar"]
