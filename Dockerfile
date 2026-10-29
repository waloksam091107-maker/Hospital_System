FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY pom.xml ./
RUN mvn -B -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

FROM tomcat:9.0-jdk17-temurin-jammy

RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /workspace/target/ustbhospital.war /usr/local/tomcat/webapps/ustbhospital.war
COPY scripts/oceanbase/wait-oceanbase.sh /usr/local/bin/wait-oceanbase.sh
RUN chmod 755 /usr/local/bin/wait-oceanbase.sh

ENV TZ=Asia/Shanghai \
    OCEANBASE_HOST=oceanbase \
    OCEANBASE_PORT=2881 \
    OCEANBASE_WAIT_SECONDS=360 \
    HOSPITAL_UPLOAD_DIR=/var/lib/hospital/uploads

RUN mkdir -p /var/lib/hospital/uploads/avatars
HEALTHCHECK --interval=15s --timeout=6s --start-period=60s --retries=5 CMD curl --fail --silent --max-time 5 http://127.0.0.1:8080/ustbhospital/health/ready || exit 1

EXPOSE 8080
ENTRYPOINT ["/usr/local/bin/wait-oceanbase.sh"]
CMD ["catalina.sh", "run"]
