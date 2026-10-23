FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN apt-get update \
	&& apt-get install -y --no-install-recommends python3 python3-venv \
	&& rm -rf /var/lib/apt/lists/* \
	&& groupadd --system appgroup \
	&& useradd --system --gid appgroup --home-dir /app appuser
COPY analytics/requirements.txt /app/analytics/requirements.txt
RUN python3 -m venv /opt/etl-venv \
	&& /opt/etl-venv/bin/pip install --no-cache-dir -r /app/analytics/requirements.txt
COPY --from=build /src/target/*.jar app.jar
COPY analytics /app/analytics
RUN mkdir -p /app/analytics/output-real \
	&& chown -R appuser:appgroup /app/analytics/output-real
ENV ANALYTICS_ETL_PYTHON=/opt/etl-venv/bin/python
USER appuser
ENTRYPOINT ["java","-jar","app.jar"]
