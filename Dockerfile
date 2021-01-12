FROM openjdk:8u262
MAINTAINER HanKeQi
VOLUME /tmp
ADD ./target/agent-zuul.jar agent-zuul.jar
EXPOSE 9082
ENTRYPOINT ["java", "-jar", "agent-zuul.jar"]