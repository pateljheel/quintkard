# Backend Architecture

## Purpose

This document describes the architecture of the `quintkard-app` backend:

- the main runtime flow
- important design decisions and why they exist
- the primary extension points for future work

The backend is a Spring Boot application that combines:

- user-scoped CRUD APIs
- a database-backed asynchronous message-processing pipeline
- AI orchestration and agent execution
- user-scoped AI tools
- hybrid search over cards

The goal of the design is to keep the write path simple for user-facing APIs while isolating AI-driven processing behind explicit orchestration and agent execution layers.

## High-Level View

At a high level, the backend has four major responsibilities:

1. Serve authenticated user APIs for cards, messages, agents, orchestrator config, and account data.
2. Persist incoming messages and process them asynchronously through a queue-backed pipeline.
3. Use orchestration prompts to decide whether a message should be processed and which agents should run.
4. Execute AI agents with a bounded tool loop against user-scoped tools.

## Main Packages

The main backend modules are:

- `agent`
  Agent configuration, persistence, and user-facing agent APIs.

- `agentexecution`
  Runtime execution of a selected agent, including loop control and tool-call execution.

- `agenttool`
  User-scoped and public AI tools exposed to agents.

- `aimodel`
  AI provider abstraction, model selection, memory integration, and Spring AI integration.

- `card`
  Card domain model, CRUD APIs, embeddings (chunking/indexing), and hybrid card search.

- `config`
  Security, queue, admin bootstrap, CSRF, and other application wiring.

- `db`
  Database-related helpers and migration entry points.

- `embedding`
  Embedding provider integration and embedding service.

- `logging`
  Structured application logging and MDC context propagation.

- `message`
  Message ingestion, message CRUD APIs, and message search.

- `messagepipeline`
  Database-backed queue polling and message processing entry point.

- `orchestrator`
  Persistent orchestrator configuration per user.

- `orchestratorexecution`
  Filtering, routing, and agent dispatch execution logic.

- `redaction`
  Redaction-related abstractions for future sensitive-data handling.

- `user`
  User persistence, security integration, and account APIs.

## Request and Processing Flow

### 1. User-facing API flow

For normal CRUD operations, the flow is conventional:

- controller accepts authenticated request
- service enforces domain rules and user ownership
- repository persists or fetches data

Examples:

- [`CardController.java`](../quintkard-app/src/main/java/io/quintkard/quintkardapp/card/CardController.java)
- [`MessageController.java`](../quintkard-app/src/main/java/io/quintkard/quintkardapp/message/MessageController.java)
- [`AgentController.java`](../quintkard-app/src/main/java/io/quintkard/quintkardapp/agent/AgentController.java)
- [`UserController.java`](../quintkard-app/src/main/java/io/quintkard/quintkardapp/user/UserController.java)

Important decision point:

- User-facing APIs are user-scoped at the service/repository layer, not just by controller assumptions.
- This keeps authorization tied to data access rules, not only request routing.

### 2. Message ingestion flow

Message ingestion is intentionally split from message processing.

Flow:

- [`MessageIngestionController.java`](../quintkard-app/src/main/java/io/quintkard/quintkardapp/message/MessageIngestionController.java)
- [`MessageIngestionServiceImpl.java`](../quintkard-app/src/main/java/io/quintkard/quintkardapp/message/MessageIngestionServiceImpl.java)
- persisted `Message` starts with `PENDING` status
- background queue later claims and processes it

Important decision point:

- Ingestion is synchronous and lightweight.
- Orchestration and agent execution are asynchronous.

Why:

- UI and upstream integrations should not block on AI execution.
- Failed processing can be retried or marked failed without losing the original message.
- Can be later decoupled into separate services or microservices if needed without affecting the ingestion API.

### 3. Queue-backed message processing flow

Asynchronous processing starts in:

- [`DatabaseBackedMessageQueueService.java`](../quintkard-app/src/main/java/io/quintkard/quintkardapp/messagepipeline/DatabaseBackedMessageQueueService.java)

Flow:

1. scheduled poll checks whether `PENDING` messages exist
2. worker claims a batch with `FOR UPDATE SKIP LOCKED`
3. claimed messages are marked `PROCESSING`
4. each message is processed by the configured `MessageProcessor`
5. final status is updated to `SUCCESS` or `FAILED`

Important decision point:

- Queue claiming is global, not user-scoped.

Why:

- This component is queue infrastructure, not a user-facing query path.
- Its job is to process any pending work in the system safely across worker threads.
- Later the queue workers could be partitioned by tenant or user if needed.

Related repository methods are therefore intentionally not user-scoped:

- `claimMessageIdsByStatus(...)`
- `existsByStatus(...)`
- `findAllByIdIn(...)`

User scope is reintroduced when a claimed message is processed because the message itself carries its owning user.

### 4. Orchestration flow

The default message processor is:

- [`DefaultMessageProcessor.java`](../quintkard-app/src/main/java/io/quintkard/quintkardapp/messagepipeline/DefaultMessageProcessor.java)

It loads the user’s orchestrator config and delegates to:

- [`OrchestratorExecutionServiceImpl.java`](../quintkard-app/src/main/java/io/quintkard/quintkardapp/orchestratorexecution/OrchestratorExecutionServiceImpl.java)

Orchestration is a two-step AI decision:

1. filtering
   Decide whether the message deserves downstream processing.

2. routing
   Decide which active agent IDs should handle the message.

Important decision point:

- Filtering and routing are separate AI calls.

Why:

- It keeps the decision model small and easier to reason about.
- It avoids pushing all selection logic into one prompt.
- It makes logs and failure modes much clearer.

If filtering rejects the message, routing is skipped.

### 5. Agent dispatch flow

If routing selects one or more agent IDs, orchestration delegates to:

- [`AgentDispatchServiceImpl.java`](../quintkard-app/src/main/java/io/quintkard/quintkardapp/orchestratorexecution/AgentDispatchServiceImpl.java)

Current behavior:

- selected agents are executed sequentially
- results are collected in a map keyed by agent ID

Important decision point:

- Dispatch is sequential, not parallel.

Why:

- each agent can access user-scoped tools and data, so to prevent race conditions and complexity around concurrent tool execution, agents are run one after another for now.

Extension point:

- This service is the right place to introduce parallel agent execution later if the design evolves in that direction.

### 6. Agent execution flow

Agent runtime starts in:

- [`AgentExecutionServiceImpl.java`](../quintkard-app/src/main/java/io/quintkard/quintkardapp/agentexecution/AgentExecutionServiceImpl.java)

This service:

- normalizes the request
- resolves default tool scope
- applies runtime loop limits from configuration
- delegates to the loop executor

Actual iterative execution happens in:

- [`AgentLoopExecutorImpl.java`](../quintkard-app/src/main/java/io/quintkard/quintkardapp/agentexecution/AgentLoopExecutorImpl.java)

Loop shape:

1. build initial prompt from agent prompt + message context
2. call the AI chat service
3. inspect tool calls
4. execute allowed tools
5. feed tool results back as a tool message
6. continue until:
   - final response
   - max iterations reached
   - max tool calls reached

Important decision points:

- internal tool execution in provider SDK is disabled
- tool execution is handled inside the application
- tool failures are returned as tool results instead of crashing the whole loop

Why:

- user scope must be enforced by application code
- logging and observability need to stay inside the app
- provider-specific tool execution would reduce control over user/tenant boundaries
- recoverable tool failures should still let the model continue reasoning

## AI Model Layer

The AI abstraction is intentionally narrow.

Primary entry points:

- [`AiChatService.java`](../quintkard-app/src/main/java/io/quintkard/quintkardapp/aimodel/AiChatService.java)
- [`SpringAiChatService.java`](../quintkard-app/src/main/java/io/quintkard/quintkardapp/aimodel/SpringAiChatService.java)

Responsibilities of the AI model layer:

- choose the provider/model from the configured catalog
- build provider-specific options
- integrate memory
- translate between internal message/tool models and Spring AI types

### Multi-provider design

Provider registry:

- [`DefaultAiChatModelRegistry.java`](../quintkard-app/src/main/java/io/quintkard/quintkardapp/aimodel/DefaultAiChatModelRegistry.java)

Provider-specific options:

- [`DefaultAiChatOptionsFactory.java`](../quintkard-app/src/main/java/io/quintkard/quintkardapp/aimodel/DefaultAiChatOptionsFactory.java)

Provider beans:

- [`AiModelConfiguration.java`](../quintkard-app/src/main/java/io/quintkard/quintkardapp/aimodel/AiModelConfiguration.java)

Important decision point:

- model name is resolved through an internal catalog, not by hardwiring provider logic across the application.

Why:

- allows OpenAI and Google models to coexist cleanly
- keeps provider mapping centralized
- avoids scattered `if model starts with ...` logic

Extension point:

- add a new provider by:
  - extending the model catalog
  - providing provider beans
  - teaching the options factory how to build provider-specific options

### Memory behavior

Memory integration:

- [`SpringAiMemoryService.java`](../quintkard-app/src/main/java/io/quintkard/quintkardapp/aimodel/SpringAiMemoryService.java)

Current behavior:

- chat memory is in-memory via Spring AI `ChatMemoryRepository`
- orchestration and agent execution use a generated memory scope
- memory is explicitly cleared when orchestration completes

Important decision point:

- memory is ephemeral per processing run

Why:

- current pipeline processes messages as bounded units of work
- keeping memory after processing would create unbounded retention and hidden coupling
- explicit cleanup avoids stale cross-run context

Extension point:

- replace in-memory repository with durable storage if long-lived conversational memory becomes a product requirement

## Tool Layer

AI tools are application-owned functions exposed to the model.

Main interfaces:

- [`AiTool.java`](../quintkard-app/src/main/java/io/quintkard/quintkardapp/agenttool/AiTool.java)
- [`AiToolRegistry.java`](../quintkard-app/src/main/java/io/quintkard/quintkardapp/agenttool/AiToolRegistry.java)
- [`AiToolScopeResolver.java`](../quintkard-app/src/main/java/io/quintkard/quintkardapp/agenttool/AiToolScopeResolver.java)

Current scope resolution:

- [`DefaultAiToolScopeResolver.java`](../quintkard-app/src/main/java/io/quintkard/quintkardapp/agenttool/DefaultAiToolScopeResolver.java)

Current behavior:

- only tools named in the allowed scope are available in a given agent run
- user ID is passed into tool execution requests

Important decision point:

- tool access is capability-based, not globally open to every agent run

Why:

- narrows blast radius
- makes agent behavior more predictable
- supports per-agent specialization

Current tool categories:

- card tools
  - create
  - update
  - change status
  - get card
  - hybrid search

- utility/public tools
  - current date
  - current time
  - timezone conversion

Extension point:

- add a new tool by implementing `AiTool`, registering it as a Spring bean, and allowing it through tool scope resolution

## Card Domain and Search

The `card` module owns structured work items generated from messages or direct user actions.

Core classes:

- [`CardServiceImpl.java`](../quintkard-app/src/main/java/io/quintkard/quintkardapp/card/CardServiceImpl.java)
- [`CardRepository.java`](../quintkard-app/src/main/java/io/quintkard/quintkardapp/card/CardRepository.java)
- [`CardSearchRepositoryImpl.java`](../quintkard-app/src/main/java/io/quintkard/quintkardapp/card/CardSearchRepositoryImpl.java)

### Filtered listing

Normal filtered card listing uses:

- `CardFilter`
- `CardSpecifications`
- JPA specification execution

Important decision point:

- structured filtering uses Specifications
- hybrid search uses a custom repository

Why:

- Specifications are good for optional relational filters
- hybrid ranking logic is too specialized for JPA abstraction

### Hybrid search

Hybrid search combines:

- semantic ranking from vector embeddings
- PostgreSQL full-text search ranking

Important decision point:

- hybrid search is implemented as explicit SQL, not ORM-generated query logic

Why:

- reciprocal-rank fusion and pgvector operations are database-native concerns
- keeping this logic in SQL is clearer and more controllable than forcing it through JPA

Security note:

- dynamic query shape is assembled from trusted SQL fragments only
- user values are bound as JDBC parameters
- this avoids SQL injection while still supporting optional filters

### Embeddings

Embedding/indexing classes:

- [`CardEmbeddingServiceImpl.java`](../quintkard-app/src/main/java/io/quintkard/quintkardapp/card/CardEmbeddingServiceImpl.java)
- [`EmbeddingConfiguration.java`](../quintkard-app/src/main/java/io/quintkard/quintkardapp/embedding/EmbeddingConfiguration.java)
- [`SpringAiEmbeddingService.java`](../quintkard-app/src/main/java/io/quintkard/quintkardapp/embedding/SpringAiEmbeddingService.java)

Cards are reindexed on create/update, which keeps semantic retrieval aligned with current content.

Extension points:

- different chunking strategies
- different embedding models
- different vector store strategy if PostgreSQL/pgvector stops fitting

## Message Domain and Search

The `message` module owns:

- raw ingested messages
- message metadata
- filtered listing
- message search

As with cards:

- structured listing uses `MessageFilter` + `MessageSpecifications`
- full-text search uses [`MessageSearchRepositoryImpl.java`](../quintkard-app/src/main/java/io/quintkard/quintkardapp/message/MessageSearchRepositoryImpl.java)

Important decision point:

- message storage is the source of truth for ingestion
- downstream orchestration output is recorded into message details rather than replacing the original payload

Why:

- preserves auditability
- allows reprocessing with new prompts or models later

## Persistence and Schema Management

Persistence uses:

- Spring Data JPA for core entity CRUD
- custom repositories for advanced SQL paths
- Flyway migrations for schema and indexing

Important decision point:

- schema evolution is owned by Flyway migrations, not by runtime auto-DDL

Why:

- repeatable, explicit schema changes
- safer production rollout
- easier CI verification

Extension points:

- new tables and indexes via new Flyway migrations
- additional projections or custom repositories for database-specific workloads

## Security

Security wiring lives in:

- [`SecurityConfig.java`](../quintkard-app/src/main/java/io/quintkard/quintkardapp/config/SecurityConfig.java)
- [`QuintkardUserDetailsService.java`](../quintkard-app/src/main/java/io/quintkard/quintkardapp/user/QuintkardUserDetailsService.java)

Current model:

- HTTP Basic authentication
- browser CSRF protection enabled
- explicit CSRF bootstrap endpoint for the SPA
- CORS configured through application properties

Important decision point:

- CSRF is enabled because the UI is a browser client performing mutating requests

Why:

- browser-based authenticated APIs should not rely only on authentication
- mutating requests need anti-forgery protection

Extension points:

- move to stronger auth/session/token models later
- tighten authorization beyond authenticated-user-only as roles and tenancy needs evolve

## Logging and Observability

Logging infrastructure lives in:

- `logging` package
- MDC/LogContext propagation

Important decision point:

- log context carries `userId`, `messageId`, `runId`, `agentId`, and `toolName`

Why:

- AI workflows are multi-step and difficult to debug without correlated logs

This is especially important across:

- queue processing
- orchestration
- agent execution
- tool calls

## Important Architectural Decisions

These are the most important design choices in the current backend:

### 1. Asynchronous message processing

- ingestion is decoupled from orchestration
- improves responsiveness and failure isolation
- later can be decoupled into separate service and scaled independently if needed

### 2. Separate orchestration from agent execution

- orchestrator decides if and where to route
- agents perform domain actions
- keeps decision-making distinct from execution

### 3. Application-owned tool execution

- tools run inside backend code, not inside provider-managed execution
- preserves user scope, logging, and control

### 4. Specifications for structured filters, SQL for hybrid search

- clean split between relational filtering and ranking-heavy retrieval

### 5. Ephemeral memory per processing run

- reduces hidden state and simplifies reasoning

### 6. Multi-provider AI model abstraction

- provider choice is centralized and configurable
- avoids locking business logic to one provider

## Primary Extension Points

The cleanest extension points in the current design are:

### Add a new AI provider

Touch:

- model catalog properties
- model registry
- options factory
- provider bean configuration

### Add a new agent tool

Touch:

- `agenttool` package
- tool registry
- agent configs/tool scopes

### Add a new orchestration step

Likely touch:

- `OrchestratorExecutionService`
- `DefaultMessageProcessor`
- orchestration result schema/details payload

### Add persistent memory

Touch:

- `AiMemoryService`
- Spring AI `ChatMemoryRepository`
- lifecycle policy around cleanup

### Add new card/message filters

Touch:

- filter record
- specifications
- controller request parameters
- UI query params if needed

### Add richer graph/linking behavior between cards and messages

Likely touch:

- card domain model
- new relational/link tables
- orchestration or tool logic that creates and updates links
- UI query/read models

### Parallel agent execution

Touch:

- `AgentDispatchServiceImpl`
- logging context propagation
- result aggregation semantics

## Known Tradeoffs

The current architecture deliberately accepts a few tradeoffs:

- in-memory AI memory is simple but not durable
- sequential agent dispatch is simpler but slower than parallel fan-out
- prompt-driven orchestration is flexible but can be nondeterministic
- queue processing is global rather than partitioned by tenant/user
- hybrid search is PostgreSQL-specific and intentionally database-aware

These are acceptable for the current stage because they keep the system understandable while preserving good extension paths.

## Scalability Concerns Not Yet Verified

The current backend design is functionally structured for growth, but several scalability characteristics have not been verified with load or production-like concurrency testing yet.

These are the main areas to watch:

### 1. Global queue polling and batch claiming

Current behavior:

- one database-backed queue polls for any `PENDING` messages
- claiming uses `FOR UPDATE SKIP LOCKED`
- work is drained in batches by the queue worker

Potential concerns:

- queue hot spots if message volume grows significantly
- database contention on the `messages` table under heavy concurrent claiming
- uneven fairness across users because claiming is global, not partitioned
- high-volume users may dominate queue throughput

What is not yet verified:

- behavior with many workers across multiple application instances (cannot be tested until the queue is decoupled from the main app)
- throughput under sustained ingestion spikes
- recovery characteristics under repeated failure/retry storms

### 2. Sequential agent dispatch

Current behavior:

- routed agents are executed sequentially for a message

Potential concerns:

- a single long-running agent delays all later agents for the same message
- end-to-end latency increases linearly with number of routed agents
- queue throughput may degrade when many messages route to multiple agents

What is not yet verified:

- whether sequential dispatch remains acceptable with higher routing fan-out
- whether parallel execution would materially improve throughput without harming determinism

### 3. Agent tool loop cost

Current behavior:

- each agent run may iterate multiple times
- each iteration may trigger multiple tool calls
- each tool call may trigger database work, embeddings, or provider API calls

Potential concerns:

- multiplicative cost growth under long prompts and multi-step reasoning
- expensive tool-heavy runs reducing total queue capacity
- higher variance in processing time across messages

What is not yet verified:

- practical upper bounds for message throughput under realistic AI latency
- safe production values for iteration and tool-call limits at scale

### 4. In-memory chat memory repository

Current behavior:

- chat memory is stored in an in-memory Spring AI repository
- orchestration clears memory after a processing run

Potential concerns:

- memory pressure if cleanup fails or usage patterns change
- no sharing across application instances
- no durability or replay of conversational state

What is not yet verified:

- memory behavior under long-lived runs or large concurrent processing counts
- operational impact if future product features require longer retention

### 5. PostgreSQL-backed hybrid search

Current behavior:

- card hybrid search uses PostgreSQL full-text search plus pgvector ranking
- message search uses PostgreSQL full-text search

Potential concerns:

- ranking queries may become expensive as row counts and embedding counts grow
- pgvector distance scans can become a bottleneck if indexing strategy is insufficient
- search latency may vary substantially by query shape and corpus size

What is not yet verified:

- query latency at larger card/message volumes
- behavior with significantly larger embedding tables
- when dedicated search infrastructure would become justified

### 6. Embedding reindex cost on card mutations

Current behavior:

- card create/update triggers embedding reindexing

Potential concerns:

- card write throughput may degrade if embedding generation becomes slow
- backpressure may build if many cards are updated in bursts
- synchronous reindex coupling can make write latency sensitive to embedding provider performance

What is not yet verified:

- write-path impact under high card mutation volume
- whether embedding work should eventually move to its own async pipeline

### 7. Multi-user isolation under load

Current behavior:

- the design is user-scoped at the domain and tool-execution level
- the main verified runtime path still centers on the seeded `admin` user

Potential concerns:

- fairness and isolation characteristics have not been exercised with many active users
- queue, search, and orchestration behavior may surface different bottlenecks in real multi-user traffic

What is not yet verified:

- concurrency behavior across many active users
- whether user-level quotas, prioritization, or partitioning are needed

### 8. AI provider dependency and latency concentration

Current behavior:

- orchestration and agent execution depend on external AI providers

Potential concerns:

- provider latency directly impacts queue drain rate
- provider outages or rate limits can reduce or stall processing throughput
- long-tail AI response times can amplify worker occupancy

What is not yet verified:

- throughput under real provider latency variance
- resilience under provider throttling or intermittent failures
- whether additional circuit breaking, work shedding, or retry shaping is needed

### 9. Logging volume

Current behavior:

- the application logs orchestration, routing, agent execution, and tool call activity in detail

Potential concerns:

- log volume may become large under heavy queue throughput
- tool argument/result logging may increase storage cost and I/O pressure
- verbose logs can become operationally expensive before application compute becomes the main bottleneck

What is not yet verified:

- production log volume at sustained message-processing rates
- whether some log categories should become sampled or level-gated

### Recommended next validation steps

The highest-value scalability validation work would be:

1. queue throughput and worker-concurrency testing
2. hybrid search latency testing at larger dataset sizes
3. multi-user concurrency testing beyond the seeded admin flow
4. AI-provider latency and failure injection testing
5. measurement of log volume and storage cost under sustained load

## Recommended Reading Order in Code

For someone new to the backend, the best reading order is:

1. [`DatabaseBackedMessageQueueService.java`](../quintkard-app/src/main/java/io/quintkard/quintkardapp/messagepipeline/DatabaseBackedMessageQueueService.java)
2. [`DefaultMessageProcessor.java`](../quintkard-app/src/main/java/io/quintkard/quintkardapp/messagepipeline/DefaultMessageProcessor.java)
3. [`OrchestratorExecutionServiceImpl.java`](../quintkard-app/src/main/java/io/quintkard/quintkardapp/orchestratorexecution/OrchestratorExecutionServiceImpl.java)
4. [`AgentDispatchServiceImpl.java`](../quintkard-app/src/main/java/io/quintkard/quintkardapp/orchestratorexecution/AgentDispatchServiceImpl.java)
5. [`AgentExecutionServiceImpl.java`](../quintkard-app/src/main/java/io/quintkard/quintkardapp/agentexecution/AgentExecutionServiceImpl.java)
6. [`AgentLoopExecutorImpl.java`](../quintkard-app/src/main/java/io/quintkard/quintkardapp/agentexecution/AgentLoopExecutorImpl.java)
7. [`SpringAiChatService.java`](../quintkard-app/src/main/java/io/quintkard/quintkardapp/aimodel/SpringAiChatService.java)
8. [`CardServiceImpl.java`](../quintkard-app/src/main/java/io/quintkard/quintkardapp/card/CardServiceImpl.java)
9. [`CardSearchRepositoryImpl.java`](../quintkard-app/src/main/java/io/quintkard/quintkardapp/card/CardSearchRepositoryImpl.java)
10. [`MessageServiceImpl.java`](../quintkard-app/src/main/java/io/quintkard/quintkardapp/message/MessageServiceImpl.java)

That path mirrors the actual runtime behavior of the most important backend flow.
