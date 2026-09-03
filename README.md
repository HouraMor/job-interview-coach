# job-interview-coach

A lightweight AI-powered web application that helps users prepare for interviews based on their CV and a job description.

## Features

- Provide both the CV and job description as pasted text or text-based PDF uploads (scanned PDFs requiring OCR are not supported)
- Generate tailored interview questions
- Includes technical and role-specific questions
- Submit your own answer to a selected question
- Receive AI-generated feedback on your answer
- Review persisted sessions, questions, answers, and feedback in a read-only history
- Fallback dummy mode if no API key is configured

## Security

User-provided CVs, job descriptions, interview questions, and candidate answers are treated as untrusted LLM input. Prompts separate trusted instructions from delimited data and escape delimiter characters to reduce prompt-injection risk. This hardening does not guarantee complete protection against prompt injection.

## Tech Stack

- Java 17
- Spring Boot
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

Generated questions are saved as a new `InterviewSession` with ordered `InterviewQuestion` records. Each submitted answer and its AI feedback are saved as an `AnswerAttempt` linked to the selected question. A read-only session overview and detail view are available at `/history`; editing and deleting sessions are not implemented.

## Run Locally

Make sure you have Java 17 installed.

Set your API key:

```bash
export GROQ_API_KEY=your_key_here
```

Start the application:

```bash
./mvnw spring-boot:run
```

Open in browser:

```text
http://localhost:8080
```
---

## Run with Docker

The Compose file starts PostgreSQL only. A separately run application container must receive `DB_*` settings for a PostgreSQL host that is reachable from inside that container; its default `DB_HOST=localhost` will not reach the Compose service.

Build the JAR:
```
./mvnw clean package
```

Build the Docker image:
```
docker build -t job-interview-coach .
```
Run the container:

```
docker run -p 8080:8080 -e GROQ_API_KEY=your_key_here -e DB_HOST=reachable_postgres_host job-interview-coach
```

Open in browser:
```
http://localhost:8080
```
---

## Future Improvements
- [x] Add a read-only interview session history

- [ ] Delete sessions and add retention controls

- [ ] Better UI for question selection

- [ ] Score history and feedback dashboard

---
