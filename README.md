# Quintkard

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=pateljheel_quintkard&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=pateljheel_quintkard)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=pateljheel_quintkard&metric=coverage)](https://sonarcloud.io/summary/new_code?id=pateljheel_quintkard)

Quintkard is a local-first workflow assistant for turning inbound messages into actionable cards. It combines a Spring Boot backend, a Next.js UI, database-backed message processing, AI-driven orchestration, agent tools, and hybrid card search.

## Modules

- `quintkard-app`
  Spring Boot backend with PostgreSQL, security, message queue processing, orchestration, card management, embeddings, and agent/tool execution.
- `quintkard-ui`
  Next.js frontend for cards, messages, agents, orchestrator settings, and account management.

## Main capabilities

- Ingest and queue messages for processing
- Route messages through an orchestrator and agent loop
- Create, update, search, and manage cards
- Hybrid card search using full-text search plus embeddings
- User-scoped agent tools for card workflows and utility tasks
- Structured logging with MDC correlation fields

## Roadmap

1. Redaction service:
   Add optional user-level redaction so sensitive message content can be removed or masked before being sent to AI services.
2. Better card UX:
   Improve the cards UI so actionable items stand out more clearly and are easier to triage quickly.
3. Message ingestion plugins:
   Add plugins for ingesting messages from Gmail and Slack.
4. Stronger card filtering:
   Expand the cards UI with richer filtering and narrowing options.
5. Semantic linking:
   Add semantic linking across cards and messages to surface related work and context automatically.
6. Broader AI provider support:
   Add support and test coverage for additional AI providers beyond the current Google GenAI setup.

## Local prerequisites

- Java 21
- Node.js
- PostgreSQL with a `quintkard` database
- Google Cloud ADC or equivalent Vertex AI auth for AI model access

Default backend local DB settings are currently:

- database: `quintkard`
- username: `quintkard`
- password: `quintkard`

The backend also enables the PostgreSQL `vector` extension on connection startup.

## Run locally

### Local services

Start PostgreSQL with the provided Docker Compose file:

```bash
cd local
docker compose up -d
```

This starts:

- PostgreSQL with pgvector on `localhost:5432`
- database `quintkard`
- username `quintkard`
- password `quintkard`

### Backend

```bash
cd quintkard-app
./gradlew bootRun
```

The API runs on `http://localhost:8080`.

### Frontend

```bash
cd quintkard-ui
npm install
npm run dev
```

The UI runs on `http://localhost:3000`.

## Test and coverage

Backend tests:

```bash
cd quintkard-app
./gradlew test
```

Generate a fresh coverage report:

```bash
cd quintkard-app
./gradlew test jacocoTestReport --rerun-tasks
```

Coverage report:

- `quintkard-app/build/reports/jacoco/test/html/index.html`

## Notes

- The backend is configured for Google GenAI chat and embeddings through Vertex AI.
- Message processing is database-backed and enabled by default.
- The UI talks to the backend on `http://localhost:8080`.
