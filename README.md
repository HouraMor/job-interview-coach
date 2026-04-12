# job-interview-coach

A lightweight AI-powered web application that helps users prepare for interviews based on their CV and a job description.

## Features

- Paste CV text and job description
- Generate tailored interview questions
- Includes technical and role-specific questions
- Submit your own answer to a selected question
- Receive AI-generated feedback on your answer
- Fallback dummy mode if no API key is configured

## Tech Stack

- Java 17
- Spring Boot
- Thymeleaf
- Maven
- Groq API
- Docker

## Run Locally

Make sure you have Java 17 installed.

Set your API key:

```bash
export GROQ_API_KEY=your_key_here
```
Start the application:
```
./mvnw spring-boot:run
```
Open in browser:
```
http://localhost:8080
```
---

## Run with Docker

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
docker run -p 8080:8080 -e GROQ_API_KEY=your_key_here job-interview-coach
```

Open in browser:
```
http://localhost:8080
```
---

## Future Improvements
- [ ] PDF upload for CVs

- [ ] Save interview sessions

- [ ] Better UI for question selection

- [ ] Score history and feedback dashboard

---

