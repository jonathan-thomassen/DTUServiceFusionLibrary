package dtu.health;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.util.Map;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

/**
 * Abstract base for Fusion service health indicators.
 *
 * <p>
 * Concrete subclasses supply a {@link #circuitBreakerStates()} map of endpoint
 * name → circuit-breaker state; this class turns that map into a {@link Health}
 * value suitable for a Kubernetes readiness probe.
 *
 * <p>
 * Status mapping:
 * <ul>
 * <li>{@code UP} — all circuit breakers are CLOSED</li>
 * <li>{@code UNKNOWN} — at least one breaker is HALF_OPEN (probe in
 * flight)</li>
 * <li>{@code DOWN} — at least one breaker is OPEN (Fusion unreachable)</li>
 * </ul>
 */
public abstract class AbstractFusionHealthIndicator implements HealthIndicator
{
  /**
   * Returns a name → state map describing every circuit breaker that contributes
   * to the overall health status. Use a {@link java.util.LinkedHashMap} to
   * preserve a deterministic detail order in the JSON response.
   */
  protected abstract Map<String, CircuitBreaker.State> circuitBreakerStates();

  @Override
  public final Health health()
  {
    Map<String, CircuitBreaker.State> states = circuitBreakerStates();
    Health.Builder builder = toBuilder(states);
    states.forEach((name, state) -> builder.withDetail(name, state.name()));
    return builder.build();
  }

  private static Health.Builder toBuilder(Map<String, CircuitBreaker.State> states)
  {
    if (states.values().stream().anyMatch(s -> s == CircuitBreaker.State.OPEN))
      return Health.down();
    if (states.values().stream().anyMatch(s -> s == CircuitBreaker.State.HALF_OPEN))
      return Health.unknown();
    return Health.up();
  }
}
