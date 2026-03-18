package dtu.fusion;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.time.Duration;
import org.springframework.web.client.RestClientException;

/**
 * Shared circuit-breaker configuration for all Fusion repository calls.
 *
 * <p>
 * A 10-request sliding window opens when ≥ 50 % of calls fail, and stays open
 * for 30 seconds before allowing a single probe request through. Only
 * {@link RestClientException} (and its subclasses) count as failures, so
 * business-logic exceptions passed through the circuit breaker do not affect
 * the failure rate.
 */
public final class FusionCircuitBreakerConfig
{
  private FusionCircuitBreakerConfig()
  {
  }

  /**
   * Creates a {@link CircuitBreakerConfig} from explicit settings, allowing
   * operators to tune the thresholds via externalized properties.
   *
   * @param slidingWindowSize            number of calls in the sliding window
   * @param failureRateThreshold         percentage (0–100) that opens the breaker
   * @param waitDurationOpenStateSeconds seconds to stay open before allowing a
   *                                     probe
   */
  public static CircuitBreakerConfig build(int slidingWindowSize, float failureRateThreshold,
      int waitDurationOpenStateSeconds)
  {
    return CircuitBreakerConfig.custom().slidingWindowSize(slidingWindowSize).failureRateThreshold(failureRateThreshold)
        .waitDurationInOpenState(Duration.ofSeconds(waitDurationOpenStateSeconds))
        .recordExceptions(RestClientException.class).build();
  }

  /** Convenience overload using the default production settings. */
  public static CircuitBreakerConfig build()
  {
    return build(10, 50.0f, 30);
  }
}
