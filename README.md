# Quintkard

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=pateljheel_quintkard&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=pateljheel_quintkard)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=pateljheel_quintkard&metric=coverage)](https://sonarcloud.io/summary/new_code?id=pateljheel_quintkard)

Quintkard is a local-first workflow assistant for turning inbound messages into actionable cards. It combines a Spring Boot backend, a Next.js UI, database-backed message processing, AI-driven orchestration, agent tools, and hybrid card search.

The system is designed around user-level multi-tenancy: cards, messages, agents, orchestrator settings, and AI tool execution are intended to be scoped per user. At the moment, local usage and the main verified flow are still centered on the seeded `admin` user, so broader multi-user behavior should be treated as design intent rather than fully verified runtime coverage.

## Modules

- `quintkard-app`
  Spring Boot backend with PostgreSQL, security, message queue processing, orchestration, card management, embeddings, and agent/tool execution.
- `quintkard-ui`
  Next.js frontend for cards, messages, agents, orchestrator settings, and account management.

## Architecture

- [Backend architecture](./docs/backend-architecture.md)
  Detailed overview of the backend runtime flow, key design decisions, and extension points.

## Current MVPs

- Message ingestion and database-backed queue processing
- AI filtering and routing orchestration
- Configurable agent execution with bounded tool loops
- Card CRUD and status management
- AI card tools for create, update, search, and status changes
- Hybrid card search with full-text and embeddings
- Message search and filtering
- Agent and orchestrator configuration UI
- Authenticated browser-based app flow
- Structured logging, migrations, and test/coverage setup

## Roadmap

1. Redaction service:
   Add optional user-level redaction so sensitive message content can be removed or masked before being sent to AI services.
2. Better card UX:
   Improve the cards UI so actionable items stand out more clearly and are easier to triage quickly.
3. Message ingestion plugins:
   Add plugins for ingesting messages from Gmail and Slack.
4. Semantic linking:
   Add semantic linking across cards and messages to surface related work and context automatically.
5. Broader AI provider support:
   Add support and test coverage for additional AI providers beyond the current Google GenAI setup.
6. AI provider failure handling:
   Add more robust handling for AI provider failures, including retries.
7. Card update history:
   Append card changes as history instead of always replacing card details in place so change context is preserved over time.

## Local prerequisites

- Java 21
- Node.js
- PostgreSQL with a `quintkard` database
- Google Cloud auth for Gemini access

Default backend local DB settings are currently:

- database: `quintkard`
- username: `quintkard`
- password: `quintkard`

The backend also enables the PostgreSQL `vector` extension on connection startup.

## AI setup

The backend is currently configured to use Gemini through Vertex AI.

Suggested configuration by usage:

- Use `Gemini on Vertex AI` if you want to run against a Google Cloud project, use GCP credits, or keep both chat and embeddings on the same Google-managed setup.
- Use `Gemini Developer API / AI Studio key` if you want the simplest local API-key setup without Google Cloud ADC.
- Use `OpenAI` if you want to run `gpt-*` chat models. In the current app, chat can be multi-provider, while embeddings are still configured separately and should stay pinned to one provider.

### Option 1: Vertex AI with Application Default Credentials

Authenticate with Google Cloud locally:

```bash
gcloud auth application-default login
```

You should also make sure the configured Google Cloud project has Vertex AI access enabled.

### Option 2: Gemini Developer API / AI Studio key

The backend properties also include a commented-out AI Studio API key configuration.
If you want to use that path instead of Vertex AI:

1. Uncomment the `spring.ai.google.genai.api-key` properties in `quintkard-app/src/main/resources/application.properties`
2. Disable the active Vertex AI properties
3. Export an API key:

```bash
export GOOGLE_API_KEY=your_key_here
```

### Option 3: OpenAI API key

The backend also supports OpenAI chat models alongside Gemini.

1. Export an API key:

```bash
export OPENAI_API_KEY=your_key_here
```

2. Make sure the OpenAI-related model entries remain present in `quintkard-app/src/main/resources/application.properties`
3. Configure agents or orchestrator steps to use a supported `gpt-*` model

OpenAI is currently used for chat only. Embeddings should remain pinned to a single configured provider.

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
