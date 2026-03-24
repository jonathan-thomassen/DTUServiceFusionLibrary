package dtu.fusion;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.springframework.web.client.RestClientException;

/**
 * Factory for Resilience4j {@link Retry} instances used by all Fusion
 * repository beans.
 *
 * <p>
 * Retry is positioned <em>inside</em> the circuit breaker so that transient
 * failures which succeed on a subsequent attempt do not increment the
 * circuit-breaker failure rate.
 */
public final class FusionRetryConfig {
  private FusionRetryConfig() {
  }

  /**
   * Creates a {@link Retry} with a fixed-backoff, exception-scoped policy.
   *
   * @param name               unique name for this retry instance
   * @param maxAttempts        total number of attempts (including the first call)
   * @param waitDurationMillis fixed pause between attempts in milliseconds
   */
  public static Retry build(String name, int maxAttempts, long waitDurationMillis) {
    RetryConfig config = RetryConfig.custom().maxAttempts(maxAttempts)
        .intervalFunction(IntervalFunction.ofRandomized(waitDurationMillis))
        .retryOnException(RestClientException.class::isInstance)
        .build();
    return Retry.of(name, config);
  }
}
