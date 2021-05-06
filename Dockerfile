#FROM openjdk:8u275
#FROM acrpaasbaubase0001.azurecr.cn/openjdk:11
FROM openjdk:11
MAINTAINER HanKeQi
VOLUME /tmp
ADD ./target/agent-zuul.jar app.jar
EXPOSE 9082
ENTRYPOINT ["java", "-jar", "app.jar"]
