# Troubleshooting

## Docker and Environment

- If `docker` or `docker compose` is unavailable, run commands from a shell that is integrated with Docker Desktop or another Docker runtime
- If Docker reports missing variables, make sure `.env` exists in the repository root
- If ports `3306`, `6379`, `5672`, or `15672` are busy, stop the conflicting process or change `docker-compose.yml`

## Flyway and Maven

- If Flyway reports a checksum mismatch, do not edit an already-applied migration; create a new `Vx__...sql` file instead
- If internal SNAPSHOT dependencies are missing, run `./mvnw -pl merchantops-api -am -DskipTests install` from the repository root
- If Maven says `No plugin found for prefix 'spring-boot'`, run the command with `-f merchantops-api/pom.xml`

## API Startup

- If port `8080` is in use, stop the current process or start on another port:

```bash
./mvnw -f merchantops-api/pom.xml spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

- If Swagger UI looks stale, confirm you are connected to the newly started process and inspect `/v3/api-docs` directly
