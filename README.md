# fusion-common

Shared infrastructure library for services that communicate with **Oracle Fusion REST APIs** (HCM, ERP, etc.).

Centralises resilient HTTP client construction, safe query building, virtual-thread fan-out enrichment, health indicator scaffolding, and common web error handling so consuming services don't have to reimplement them.

---

## Contents

- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [Packages](#packages)
  - [dtu.fusion](#dtufusion)
  - [dtu.health](#dtuhealth)
  - [dtu.services](#dtuservices)
  - [dtu.web](#dtuweb)
- [Usage](#usage)
  - [REST Client](#rest-client)
  - [Repository Pattern](#repository-pattern)
  - [Query Builder](#query-builder)
  - [Health Indicator](#health-indicator)
  - [Metrics](#metrics)
  - [Error Handling](#error-handling)

---

## Getting Started

Add the library to your Maven project:

```xml
<dependency>
    <groupId>dtu.services</groupId>
    <artifactId>fusion-common</artifactId>
    <version>1.0.0</version>
</dependency>
```

The library targets **Java 25** and requires **Spring Boot 4.x**.

---

## Configuration

All settings are exposed under the `fusion.*` prefix via `@ConfigurationProperties`. Add them to your `application.yaml`:

```yaml
fusion:
  circuit-breaker:
    sliding-window-size: 10          # calls in the sliding window
    failure-rate-threshold: 50.0     # % failures that open the breaker
    wait-duration-open-state-seconds: 30
  retry:
    max-attempts: 2                  # total attempts including the first call
    wait-duration-millis: 200        # fixed backoff between attempts (ms)
  cache:
    result-ttl-minutes: 5
    lookup-ttl-hours: 1
    template-ttl-hours: 1
  virtual-thread:
    max-concurrency: 300             # semaphore cap on parallel enrichment tasks
    enrichment-timeout-seconds: 10
  pagination:
    max-limit: 500                   # Oracle Fusion hard cap
  dff:
    org-number-attribute-key: dtuDepartmentNumber
    org-type-attribute-key: organizationType
```

All values shown are the defaults; only override what you need.

---

## Packages

### `dtu.fusion`

Core Fusion REST infrastructure.

| Class | Responsibility |
| --- | --- |
| `FusionProperties` | Spring `@ConfigurationProperties` bean holding all tunable settings (see [Configuration](#configuration)). |
| `FusionRestClientFactory` | Static factory for `RestClient`. Supports **Basic Auth** and **OAuth2 client-credentials** flows with pooled Apache `HttpClient5` transport. Sets default headers (`Accept: application/json`, `REST-Framework-Version: 9`, `Accept-Encoding: gzip`). |
| `AbstractFusionRepository` | Abstract base for Fusion data-access beans. Wraps calls with circuit breaker + retry via `executeWithResilience(Supplier<T>)`, validates pagination parameters, and exposes `circuitBreakerState()`. |
| `FusionCircuitBreakerConfig` | Static factory (`build(...)`) for a Resilience4j count-based `CircuitBreakerConfig`. Only `RestClientException` counts as a failure. |
| `FusionRetryConfig` | Static factory (`build(...)`) for a Resilience4j fixed-backoff `Retry`. Retries only on `RestClientException`. Retry is placed *inside* the circuit breaker so retried successes do not register as failures. |
| `FusionQueryBuilder` | Fluent, injection-safe builder for Oracle Fusion `q=` query parameters. Single-quotes are stripped from user input; `eqCode` validates against `\w+`. Returns `null` when no conditions have been added. |
| `FusionVirtualThreadConfig` | `@Configuration` bean that creates a `fusionVirtualThreadExecutor` — a semaphore-bounded virtual-thread `ExecutorService` that prevents GC pressure and HTTP pool saturation under burst enrichment load. |

### `dtu.health`

| Class | Responsibility |
| --- | --- |
| `AbstractFusionHealthIndicator` | Spring Boot `HealthIndicator` base. Subclasses implement `circuitBreakerStates()`. Translates: all CLOSED → UP; any HALF_OPEN → UNKNOWN; any OPEN → DOWN. Suitable for Kubernetes readiness probes. |

### `dtu.services`

| Class | Responsibility |
| --- | --- |
| `AbstractMetricsService<M>` | Generic base for per-service metrics controllers. Holds a `MetricsAggregator` and converts snapshots to the generated API model type. |

### `dtu.web`

| Class | Responsibility |
| --- | --- |
| `ResourceNotFoundHandler` | `@RestControllerAdvice` at `HIGHEST_PRECEDENCE`. Maps `NoResourceFoundException` → 404 empty body, `ResourceNotFoundException` → 404 `{"error":"..."}`, and `IllegalArgumentException` → 400 `{"error":"..."}`. Auto-registered via component scan. |

---

## Usage

### REST Client

```java
// Basic Auth
RestClient client = FusionRestClientFactory.build(
    "https://fusion.example.com",
    "username", "password",
    maxConnections, connectionTtlSeconds
);

// OAuth2 client-credentials
OAuth2AuthorizedClientManager manager =
    FusionRestClientFactory.buildClientCredentialsManager(
        clientRegistrationRepository, authorizedClientRepository
    );

RestClient client = FusionRestClientFactory.buildOAuth2(
    "https://fusion.example.com",
    "my-client-registration-id",
    manager,
    maxConnections, connectionTtlSeconds
);
```

### Repository Pattern

```java
@Repository
public class EmployeeRepository extends AbstractFusionRepository {

    private final RestClient restClient;

    public EmployeeRepository(FusionProperties props, RestClient fusionClient) {
        super(
            AbstractFusionRepository.createCircuitBreaker("employees", props),
            AbstractFusionRepository.createRetry("employees", props)
        );
        this.restClient = fusionClient;
    }

    public List<Employee> findAll() {
        return executeWithResilience(() ->
            restClient.get()
                .uri("/hcmRestApi/resources/11.13.18.05/workers")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {})
        );
    }
}
```

### Query Builder

```java
String query = new FusionQueryBuilder()
    .like("LastName", searchTerm)       // input is sanitised automatically
    .eq("ActiveFlag", "Y")
    .eqNum("DepartmentId", deptId)
    .build();

// Use the result as the `q` parameter (may be null if no conditions were added)
UriComponentsBuilder.fromPath("/workers")
    .queryParamIfPresent("q", Optional.ofNullable(query))
    .build();
```

### Health Indicator

```java
@Component
public class FusionHealthIndicator extends AbstractFusionHealthIndicator {

    private final EmployeeRepository employeeRepo;
    private final PositionRepository positionRepo;

    @Override
    protected Map<String, CircuitBreaker.State> circuitBreakerStates() {
        return Map.of(
            "employees", employeeRepo.circuitBreakerState(),
            "positions", positionRepo.circuitBreakerState()
        );
    }
}
```

Spring Boot Actuator will expose this at `/actuator/health`.

### Metrics

```java
@RestController
public class MetricsController extends AbstractMetricsService<MyMetricsModel> {

    public MetricsController(MetricsAggregator aggregator, ObjectMapper mapper) {
        super(aggregator, mapper, MyMetricsModel.class);
    }

    @GetMapping("/metrics")
    public ResponseEntity<MyMetricsModel> metrics() {
        return getMetrics();
    }
}
```

### Error Handling

`ResourceNotFoundHandler` is picked up automatically by Spring's component scan of the `dtu` package — no explicit registration is required.

Throw the appropriate exception from your service layer:

```java
throw new ResourceNotFoundException("Employee not found: " + id);  // → 404
throw new IllegalArgumentException("Invalid department id");        // → 400
```

---

## Building

```bash
mvn install
```

Tests are run with JaCoCo coverage. Reports are written to `target/site/jacoco/`.
