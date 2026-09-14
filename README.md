# Agentic Software Engineering URL Shortener

This project is a runnable prototype that demonstrates an agentic software engineering workflow around a URL shortener service. It is designed to show requirement interpretation, task decomposition, orchestration, implementation, validation, documentation, and controlled human oversight.

## Working prototype status

This project is runnable end-to-end:

- The service starts successfully with Spring Boot
- The health endpoint responds
- The short-link API creates and redirects URLs
- Analytics and workflow endpoints respond with structured data
- Automated tests cover the main happy path and workflow behavior

## Architecture overview

### Components

1. Controller layer
   - ShortUrlController handles URL shortener endpoints
   - AgenticWorkflowController exposes the orchestration workflow endpoints
2. Service layer
   - ShortUrlService owns link creation, redirect tracking, analytics, and validation
   - AgenticWorkflowService models the SDLC workflow as a dependency graph with approval gates
3. Model layer
   - ShortUrl, VisitEvent, LinkStats, WorkflowExecution, WorkflowStage, WorkflowMetrics
4. Exception layer
   - ApiException and GlobalExceptionHandler enforce validation and safe failures
5. Persistence/runtime
   - In-memory store is used for the prototype, suitable for demo and validation

### Orchestration model

The workflow is not a raw linear chain. It is represented as a dependency-graph with explicit stage sequencing and approval boundaries.

Workflow stages:

1. requirement-analysis
2. task-decomposition
3. implementation
4. testing
5. documentation
6. release-readiness

The release-readiness stage requires human approval before progression. This demonstrates controlled autonomy with governance and human-in-the-loop oversight.

### End-to-end integration flow
<img width="8192" height="2047" alt="API Consumer Release-2026-09-14-040352" src="https://github.com/user-attachments/assets/b527430b-404a-4672-a5c2-3ffd60d39fdb" />

### Control flow

- A user creates a workflow with a requirement and a scope value
- The requirement is normalized into a clearer engineering problem
- Stage dependencies are checked before any stage can progress
- High-impact approval gates require explicit approval
- Metrics are exposed to track reliability indicators such as success rate, retry count, rollback count, MTTR, and latency

### Production-grade design notes

- Input validation is enforced on workflow requests and approval payloads using Jakarta Bean Validation.
- The workflow graph is stateful and governance-aware: each stage records entry/exit gates, sync groups, path types, and decision lineage.
- Recovery behavior includes bounded retries, safe-stop, rollback, and fallback pathways. These are surfaced through explicit API endpoints and audit trail entries.
- Short-link traffic includes basic reliability controls (rate limiting and cache-aware lookup) to keep the system stable under load.
- The API contract is documented in `docs/agentic-workflow-openapi.yaml` for clearer change control and maintainability.

### Key decisions

- Use in-memory storage for a demo-friendly prototype without introducing infrastructure complexity
- Keep workflow stage semantics explicit and inspectable for auditability
- Separate API and workflow concerns to demonstrate both product execution and engineering lifecycle orchestration
- Use approval gates to enforce safe-stop and human oversight for release decisions

## Scenario coverage

### 1) Greenfield scenario

Example requirement:

- "Build a URL shortener service from scratch with validation, redirect logic, and analytics."

Decomposition:

- requirement-analysis
- task-decomposition
- implementation
- testing
- documentation
- release-readiness

Orchestration:

- Workflow is created through POST /api/agent/workflows with scope=greenfield
- Stages are tracked and advanced with /advance
- Release-readiness requires approval

Validation:

- Health endpoint returns success
- Create link returns 201 and redirect works
- Test suite validates happy path and request handling

### 2) Brownfield scenario

Example requirement:

- "Enhance an existing URL shortener with analytics and safety checks while preserving compatibility."

Decomposition:

- Understand impact on current API contracts and modules
- Identify modules impacted by URL validation, redirect logic, and analytics
- Add controlled rollout and approval gate before final release

Orchestration:

- Scope is set to brownfield in the workflow request
- Dependency ordering ensures implementation before testing and release approval

Validation:

- API contract remains consistent for existing endpoints
- Workflow approval state is explicit and auditable
- Metrics are available to review reliability post-change

### 3) Ambiguous scenario

Example requirement:

- "Make a short-link service, but the exact targets, constraints, and acceptance criteria are not fully specified."

Decomposition:

- Normalize the requirement into a concrete engineering problem
- Record the assumptions and constraints explicitly
- Sequence the implementation only after clarifying needed behavior

Orchestration:

- Workflow request is created with a requirement that contains ambiguity signals
- Normalization logs the scope and objective before execution
- Approval gates force review on high-impact decisions

Validation:

- Risk and trade-offs are documented in the workflow decision log
- Testing is used to lock in the expected behavior after assumptions are defined

## Setup instructions

### Prerequisites

- Java 17
- Maven
- Local environment with port 8081 free

### Run the project

```bash
cd C:\Code-workspace\spring-url-shortener
mvn spring-boot:run
```

### Health check

```bash
curl http://localhost:8081/health
```

Expected response:

```json
{"service":"url-shortener","status":"ok"}
```

### Create a short link

```bash
curl -X POST http://localhost:8081/api/links \
  -H "Content-Type: application/json" \
  -d '{"url":"https://example.com/docs","alias":"docs-demo"}'
```

### Create a workflow

```bash
curl -X POST http://localhost:8081/api/agent/workflows \
  -H "Content-Type: application/json" \
  -d '{"requirement":"Build a URL shortener service with governance and automated validation.","scope":"greenfield"}'
```

### Approve final stage

```bash
curl -X POST http://localhost:8081/api/agent/workflows/{workflowId}/approve \
  -H "Content-Type: application/json" \
  -d '{"stageId":"release-readiness","approved":true,"approver":"human-reviewer","rationale":"Release criteria satisfied."}'
```

## Testing approach

The project uses Spring Boot integration tests with MockMvc and JUnit 5.

Covered checks:

- health endpoint responds
- short-link creation works
- redirect behavior returns the target URL
- click tracking increments metrics
- workflow creation and approval gates behave as expected

Command:

```bash
cd C:\Code-workspace\spring-url-shortener
mvn test
```

## Limitations and trade-offs

- In-memory storage is intentionally simple and not production-grade persistence
- No distributed locking or concurrency hardening is implemented yet
- No database migration, authentication, or rate-limiter is included beyond the prototype-level design
- Workflow metrics are representative rather than production observability-grade telemetry
- The orchestration layer is a lightweight model for demonstration, not a full enterprise orchestration engine

## Summary

This prototype demonstrates the required agentic engineering pattern: requirement understanding, decomposition, orchestration, validation, and governance, packaged around a runnable URL shortener implementation.
