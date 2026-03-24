package dtu.fusion;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import java.util.function.Supplier;

/**
 * Abstract base for all Fusion repository beans.
 *
 * <p>
 * Centralises:
 * <ul>
 * <li>the circuit-breaker + retry execution wrapper</li>
 * <li>collection-parameter validation (limit / offset)</li>
 * <li>circuit-breaker state reporting for health indicators</li>
 * </ul>
 *
 * Concrete subclasses supply the service-specific HTTP calls, caches, and
 * mappers while calling {@link #validatePagination} and
 * {@link #executeWithResilience} rather than duplicating those patterns.
 */
public abstract class AbstractFusionRepository {
  private final CircuitBreaker circuitBreaker;
  private final Retry retry;
  private final int maxPaginationLimit;

  protected AbstractFusionRepository(CircuitBreaker circuitBreaker, Retry retry, int maxPaginationLimit) {
    this.circuitBreaker = circuitBreaker;
    this.retry = retry;
    this.maxPaginationLimit = maxPaginationLimit;
  }

  /**
   * Executes {@code supplier} guarded by this repository's circuit breaker and
   * retry policy.
   */
  protected <T> T executeWithResilience(Supplier<T> supplier) {
    return circuitBreaker.executeSupplier(() -> retry.executeSupplier(supplier));
  }

  /**
   * Validates the {@code limit} and {@code offset} parameters shared by all
   * collection endpoints.
   *
   * @throws IllegalArgumentException when {@code limit < 1},
   *                                  {@code limit > maxPaginationLimit}, or
   *                                  {@code offset < 0}
   */
  protected void validatePagination(Integer limit, Integer offset) {
    if (limit != null && limit < 1)
      throw new IllegalArgumentException("limit must be >= 1; got: " + limit);
    if (limit != null && limit > maxPaginationLimit)
      throw new IllegalArgumentException("limit must be <= " + maxPaginationLimit + "; got: " + limit);
    if (offset != null && offset < 0)
      throw new IllegalArgumentException("offset must be >= 0; got: " + offset);
  }

  /**
   * Clears any cached data held by this repository.
   *
   * <p>
   * Called by event handlers when the underlying data source signals a change.
   * The default implementation is a no-op for repositories that do not cache.
   * Subclasses that maintain a local cache must override this method to evict
   * all cached entries so that the next call re-fetches from the backend.
   */
  public void invalidateCache() {
  }

  /** Returns the current circuit-breaker state for health reporting. */
  public CircuitBreaker.State circuitBreakerState() {
    return circuitBreaker.getState();
  }

  /**
   * Convenience factory — creates a {@link CircuitBreaker} from the shared
   * {@link FusionProperties} circuit-breaker block.
   *
   * @param name unique circuit-breaker name (e.g. {@code "fusion-erp-projects"})
   */
  protected static CircuitBreaker createCircuitBreaker(String name, FusionProperties props) {
    return CircuitBreaker.of(name, FusionCircuitBreakerConfig.build(
        props.getCircuitBreaker().getSlidingWindowSize(),
        props.getCircuitBreaker().getFailureRateThreshold(),
        props.getCircuitBreaker().getWaitDurationOpenStateSeconds()));
  }

  /**
   * Convenience factory — creates a {@link Retry} from the shared
   * {@link FusionProperties} retry block.
   *
   * @param name unique retry name (e.g. {@code "fusion-erp-projects-retry"})
   */
  protected static Retry createRetry(String name, FusionProperties props) {
    return FusionRetryConfig.build(name,
        props.getRetry().getMaxAttempts(),
        props.getRetry().getWaitDurationMillis());
  }
}
