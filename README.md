# job-interview-coach

A lightweight AI-powered web application that helps users prepare for interviews based on their CV and a job description.

## Features

- Provide both the CV and job description as pasted text or text-based PDF uploads (scanned PDFs requiring OCR are not supported)
- Generate tailored interview questions
- Includes technical and role-specific questions
- Submit your own answer to a selected question
- Receive AI-generated feedback on your answer
- Review persisted sessions, questions, answers, and feedback in an authenticated history, and delete complete sessions
- Expose a minimal public health endpoint for deployment platforms

## Security

User-provided CVs, job descriptions, interview questions, and candidate answers are treated as untrusted LLM input. Prompts separate trusted instructions from delimited data and escape delimiter characters to reduce prompt-injection risk. This hardening does not guarantee complete protection against prompt injection.

The `/history` pages require the single owner account configured with `APP_ADMIN_USERNAME` and `APP_ADMIN_PASSWORD`. The password is BCrypt-encoded in memory when the application starts. This is intentionally a simple portfolio authentication model, not a multi-user account or per-user ownership system.

The interview workflow is public and submitted CVs, job descriptions, answers, and AI feedback are persisted. The single owner can view all submitted data. This design is suitable for a controlled portfolio demo, but it is not a private multi-user data system and should not be treated as one.

## Tech Stack

- Java 17
- Spring Boot
- Spring Boot Actuator
- Spring Security
- Spring Data JPA
- PostgreSQL
- Flyway
- Thymeleaf
- Maven
- Groq API
- Docker

## Local PostgreSQL Setup

Start PostgreSQL with Docker Compose:

```bash
docker compose up -d postgres
```

The application reads its database connection from these environment variables. The defaults match the local Compose service:

| Variable | Local default |
| --- | --- |
| `DB_HOST` | `localhost` |
| `DB_PORT` | `5432` |
| `DB_NAME` | `interview_coach` |
| `DB_USER` | `interview_coach` |
| `DB_PASSWORD` | `interview_coach` |

These values are local development defaults only. Override them with environment variables for other environments.

Stop PostgreSQL when finished:

```bash
docker compose down
```

The named Docker volume is retained, so local database data persists across restarts. This Compose file starts PostgreSQL only; run the Spring Boot application from the host as described below.

## Persistence Domain

```text
InterviewSession
  -> InterviewQuestion
       -> AnswerAttempt
```

Generated questions are saved as a new `InterviewSession` with ordered `InterviewQuestion` records. Each submitted answer and its AI feedback are saved as an `AnswerAttempt` linked to the selected question. The authenticated session overview and detail view are available at `/history`. Deleting a session permanently cascades to its questions and answer attempts; editing and recovery are not implemented.

## Run Locally

Make sure you have Java 17 installed. Set the required owner credentials:

```bash
export APP_ADMIN_USERNAME=admin
export APP_ADMIN_PASSWORD='choose-a-strong-local-password'
```

The application intentionally has no default owner password, so it fails to start if either value is missing. For a public deployment, provide these values through the platform's secret-management mechanism and use HTTPS.

Set your API key:

```bash
export GROQ_API_KEY=your_key_here
```

A working Groq API key is required for question generation and feedback. If configuration or the provider fails, the application shows a service-unavailable error and does not persist substitute questions or feedback.

Start the application:

```bash
./mvnw spring-boot:run
```

Open in browser:

```text
http://localhost:8080
```

## Deployment Preparation

This repository contains configuration and a container image suitable for deployment testing, but it does not deploy the application or create any cloud resources.

### Production profile

Enable the production profile with:

```bash
export SPRING_PROFILES_ACTIVE=prod
```

The `prod` profile requires the Groq key and core database settings to be supplied externally. It keeps Flyway enabled, uses PostgreSQL, disables SQL/debug output, and prevents internal error details or stack traces from being exposed by the default error response. Flyway applies pending migrations to the configured database when the application starts.

### Environment variables

| Variable | Required in `prod` | Default | Purpose |
| --- | --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | Set to `prod` | none | Enables the production-specific safeguards |
| `GROQ_API_KEY` | yes | empty outside `prod`; none in `prod` | Groq API authentication |
| `APP_ADMIN_USERNAME` | yes | none | Single history-owner username |
| `APP_ADMIN_PASSWORD` | yes | none | Single history-owner password |
| `DB_HOST` | yes | `localhost` outside `prod`; none in `prod` | PostgreSQL host |
| `DB_PORT` | yes | `5432` outside `prod`; none in `prod` | PostgreSQL port |
| `DB_NAME` | yes | `interview_coach` outside `prod`; none in `prod` | PostgreSQL database |
| `DB_USER` | yes | `interview_coach` outside `prod`; none in `prod` | PostgreSQL user |
| `DB_PASSWORD` | yes | `interview_coach` outside `prod`; none in `prod` | PostgreSQL password |
| `DB_JDBC_PARAMETERS` | no | empty | Optional JDBC suffix, including the leading `?`, such as `?sslmode=require` when required by a provider |
| `PORT` | no | `8080` | HTTP server port |
| `RATE_LIMIT_REQUESTS` | no | `10` | Combined AI requests allowed per client and window |
| `RATE_LIMIT_WINDOW_SECONDS` | no | `60` | Rate-limit window length in seconds |
| `MAX_CV_CHARACTERS` | no | `20000` | Maximum final CV text length |
| `MAX_JOB_CHARACTERS` | no | `20000` | Maximum final job-description length |
| `MAX_ANSWER_CHARACTERS` | no | `10000` | Maximum candidate-answer length |
| `MAX_PDF_BYTES` | no | `5242880` | Maximum size of each uploaded PDF, in bytes |
| `MAX_MULTIPART_REQUEST_BYTES` | no | `11534336` | Maximum complete multipart request size, in bytes |

Use the deployment platform's secret-management mechanism for credentials and API keys. Do not put real values in source control or image layers. `MAX_PDF_BYTES` and `MAX_MULTIPART_REQUEST_BYTES` must be numeric byte counts.

### Health endpoint

The only Actuator endpoint exposed over HTTP is the public health check:

```text
GET /actuator/health
```

Health-component details are not exposed. A deployment platform can check it with:

```bash
curl http://localhost:8080/actuator/health
```

### Input and abuse limits

The application validates pasted text, extracted PDF text, uploaded file sizes, and candidate answers before calling Groq. Oversized multipart requests are also rejected by Spring before normal controller processing.

`POST /questions` and `POST /feedback` share a fixed-window rate limit based on the direct client address. The limiter is intentionally small and in memory:

- each application instance keeps independent counters
- counters reset whenever that instance restarts
- clients behind the same proxy or NAT may share a limit
- forwarding headers are not blindly trusted
- it is basic cost/abuse protection, not distributed rate limiting or DDoS protection

## Test the Production-like Docker Image Locally

The Dockerfile uses a Maven build stage and a smaller Java runtime stage. The final container runs as a non-root user and contains only the application JAR and runtime image.

Start the existing PostgreSQL Compose service:

```bash
docker compose up -d postgres
```

Build the application image:

```bash
docker build -t job-interview-coach .
```

Run it with the production profile. `host.docker.internal` lets the application container reach PostgreSQL through the port published by Compose; `host-gateway` provides the mapping on Docker Engine installations that need it.

```bash
docker run --rm -d \
  --name job-interview-coach \
  --add-host=host.docker.internal:host-gateway \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e PORT=8080 \
  -e GROQ_API_KEY=replace-with-your-key \
  -e APP_ADMIN_USERNAME=admin \
  -e APP_ADMIN_PASSWORD='replace-with-a-strong-password' \
  -e DB_HOST=host.docker.internal \
  -e DB_PORT=5432 \
  -e DB_NAME=interview_coach \
  -e DB_USER=interview_coach \
  -e DB_PASSWORD=interview_coach \
  job-interview-coach
```

Inspect startup, including Flyway migration output, and check health:

```bash
docker logs job-interview-coach
curl http://localhost:8080/actuator/health
```

Then open `http://localhost:8080`. Stop and remove the application container and stop PostgreSQL with:

```bash
docker stop job-interview-coach
docker compose down
```

For a real deployment, use the managed PostgreSQL hostname rather than `host.docker.internal`, add `DB_JDBC_PARAMETERS` only when required by the provider, terminate traffic with HTTPS, and store secrets in the platform's secret manager.

## Future Improvements

- [x] Add authenticated interview session history

- [x] Delete interview sessions and their saved questions and attempts

- [ ] Add retention controls

- [ ] Add multi-user accounts and per-user data ownership

- [ ] Better UI for question selection

- [ ] Score history and feedback dashboard

---
