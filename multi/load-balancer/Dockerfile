FROM alpine/java:21-jre
LABEL authors="rlaredo"
ARG JAR_FILE
COPY ${JAR_FILE} app.jar
ENV TZ=America/La_Paz
ENV LB_PORT=1915
EXPOSE 1915
ENTRYPOINT ["java","-Duser.language=es","-Duser.region=BO","-Duser.country=BO","-Duser.timezone=America/La_Paz","-jar","/app.jar"]
