# URL Shortener — Spring Boot

A production-grade URL shortener with an agentic workflow engine, built with Spring Boot 3 and SQLite.

## Features

- **Shorten URLs** — generates a 7-character base-62 code (62⁷ ≈ 3.5 trillion unique codes)
- **Custom aliases** — choose your own short code, with collision detection
- **Click analytics** — async click recording with referrer tracking
- **Rate limiting** — token-bucket limiter per client IP (configurable requests/min)
- **Idempotent create** — same URL returns the same code (200 dedup)
- **307 redirects** — Temporary Redirect prevents browsers from caching stale links
- **Agentic Workflow Engine** — plan, replan, fallback, and safe-stop multi-stage AI workflows
- **OpenAPI / Swagger UI** — live docs at `/docs`

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+

### Run

```bash
mvn spring-boot:run
```

App starts on **http://localhost:8081**

### Test

```bash
mvn test
```

### Build JAR

```bash
mvn package
java -jar target/url-shortener-0.0.1-SNAPSHOT.jar
```

## Configuration

All settings can be overridden with environment variables:

| Property | Env var | Default | Description |
|---|---|---|---|
| `shortener.db-path` | `SHORTENER_DB_PATH` | `shortener.db` | SQLite file path (`:memory:` for tests) |
| `shortener.base-url` | `SHORTENER_BASE_URL` | `http://localhost:8081` | Prefix used in generated short URLs |
| `shortener.code-length` | `SHORTENER_CODE_LENGTH` | `7` | Length of generated short codes |
| `shortener.create-rate-per-minute` | `SHORTENER_CREATE_RATE_PER_MINUTE` | `60` | Max link creations per IP per minute |

Example with custom DB path:

```bash
SHORTENER_DB_PATH=/data/links.db mvn spring-boot:run
```

## API Reference

### URL Shortener

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/links` | Create a short link |
| `POST` | `/api/shorten` | Alias for `/api/links` |
| `GET` | `/api/links/{code}` | Look up a link |
| `DELETE` | `/api/links/{code}` | Delete a link |
| `GET` | `/api/links/{code}/stats` | Click statistics |
| `GET` | `/{code}` | Redirect to original URL (307) |
| `GET` | `/health` | Health check |

#### Create a link

```bash
curl -X POST http://localhost:8081/api/links \
  -H "Content-Type: application/json" \
  -d '{"url": "https://example.com/my-long-path", "custom_alias": "my-link"}'
```

```json
{
  "code": "my-link",
  "short_url": "http://localhost:8081/my-link",
  "url": "https://example.com/my-long-path",
  "created_at": 1727143200.0,
  "is_custom": true
}
```

#### Get stats

```bash
curl http://localhost:8081/api/links/my-link/stats
```

```json
{
  "code": "my-link",
  "total_clicks": 42,
  "clicks_by_referrer": { "https://twitter.com": 30, "direct": 12 }
}
```

### Agentic Workflow Engine

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/agent/workflows` | Create a workflow |
| `GET` | `/api/agent/workflows/{id}` | Get workflow state |
| `POST` | `/api/agent/workflows/{id}/advance` | Advance to next stage |
| `POST` | `/api/agent/workflows/{id}/replan` | Trigger a replan |
| `POST` | `/api/agent/workflows/{id}/fallback` | Fallback a stage |
| `POST` | `/api/agent/workflows/{id}/safe-stop` | Governance safe-stop |
| `GET` | `/api/agent/metrics` | Reliability metrics |

Full OpenAPI spec: `docs/agentic-workflow-openapi.yaml`

## Project Structure

```
src/main/java/com/example/urlshortener/
├── UrlShortenerApplication.java      # Entry point (@SpringBootApplication, @EnableAsync)
├── config/
│   ├── AppConfig.java                # Beans: ownHost, TokenBucketLimiter
│   └── ShortenerProperties.java      # @ConfigurationProperties(prefix="shortener")
├── controller/
│   ├── ShortUrlController.java       # REST endpoints for URL shortening
│   └── AgenticWorkflowController.java# REST endpoints for agentic workflows
├── exception/
│   ├── ApiException.java             # 422 Unprocessable Entity
│   ├── DuplicateCodeException.java   # 409 Conflict
│   └── GlobalExceptionHandler.java   # @RestControllerAdvice
├── model/
│   ├── CreateLinkRequest.java        # Request DTO
│   └── Workflow*.java                # Workflow models
├── repository/
│   ├── Link.java                     # Record: code, url, createdAt, isCustom
│   └── LinkRepository.java           # SQLite via JdbcTemplate
└── service/
    ├── ShortUrlService.java          # Core shortening logic
    ├── AgenticWorkflowService.java   # Workflow orchestration
    ├── ClickRecorder.java            # @Async click recording
    ├── CodeGenerator.java            # SecureRandom base-62 generation
    ├── TokenBucketLimiter.java       # Per-IP rate limiting
    └── UrlValidator.java             # URL validation (http/https only)
```

## HTTP Status Codes

| Status | When |
|---|---|
| `201 Created` | New link created |
| `200 OK` | Duplicate URL returned (dedup) |
| `307 Temporary Redirect` | Redirect to original URL |
| `204 No Content` | Link deleted |
| `409 Conflict` | Custom alias already taken |
| `422 Unprocessable Entity` | Invalid URL or reserved alias |
| `429 Too Many Requests` | Rate limit exceeded |
| `404 Not Found` | Code not found |

## Reserved Aliases

The following codes cannot be used as custom aliases:
`api`, `docs`, `redoc`, `openapi.json`, `healthz`, `health`, `readyz`
