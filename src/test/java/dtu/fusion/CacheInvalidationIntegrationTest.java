package dtu.fusion;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Integration tests for the cache-invalidation contract:
 *
 * <pre>
 *   event → EventHandler → repository.invalidateCache() → next request re-fetches
 * </pre>
 *
 * <p>
 * These tests exercise the three collaborators together without requiring a
 * running Kafka broker or Spring context:
 * <ul>
 * <li>{@link CachingTestRepository} – a concrete
 * {@link AbstractFusionRepository}
 * that keeps a simple in-memory result and counts true backend calls.</li>
 * <li>{@link TestEventHandler} – a minimal event handler that mirrors the
 * {@code @DTUSubscriber} pattern used in production: it receives an event
 * and immediately calls {@code repository.invalidateCache()}.</li>
 * </ul>
 */
class CacheInvalidationIntegrationTest {
  // -------------------------------------------------------------------------
  // Test doubles
  // -------------------------------------------------------------------------

  /**
   * Concrete repository that caches a single result string and exposes a
   * backend-call counter so tests can assert that the cache was actually used
   * (or bypassed after invalidation).
   */
  private static final class CachingTestRepository extends AbstractFusionRepository {
    private final Supplier<String> backend;
    private final AtomicInteger backendCallCount = new AtomicInteger(0);

    /** Cached value; {@code null} means the cache is cold / invalidated. */
    private volatile String cachedResult = null;

    CachingTestRepository(CircuitBreaker circuitBreaker, Retry retry, Supplier<String> backend) {
      super(circuitBreaker, retry, 500);
      this.backend = backend;
    }

    /**
     * Returns the result – from cache when warm, from backend when cold.
     * The cache is consulted first, before the resilience wrapper, which
     * matches real repository implementations.
     */
    String fetch() {
      if (cachedResult != null)
        return cachedResult;

      return executeWithResilience(() -> {
        backendCallCount.incrementAndGet();
        cachedResult = backend.get();
        return cachedResult;
      });
    }

    @Override
    public void invalidateCache() {
      cachedResult = null;
    }

    int getBackendCallCount() {
      return backendCallCount.get();
    }
  }

  /**
   * Minimal event handler that mirrors the production {@code @DTUSubscriber}
   * pattern: receives an arbitrary event object and calls
   * {@link AbstractFusionRepository#invalidateCache()} on its repository.
   */
  private static final class TestEventHandler {
    private final CachingTestRepository repository;

    TestEventHandler(CachingTestRepository repository) {
      this.repository = repository;
    }

    /**
     * Simulates the method annotated with {@code @DTUSubscriber} in production
     * code.
     */
    void onDataChangedEvent() {
      repository.invalidateCache();
    }
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private static CachingTestRepository newRepo() {
    return newRepo(() -> "backend-result");
  }

  private static CachingTestRepository newRepo(Supplier<String> backend) {
    FusionProperties props = new FusionProperties();
    CircuitBreaker cb = AbstractFusionRepository.createCircuitBreaker("ci-test-cb", props);
    Retry retry = AbstractFusionRepository.createRetry("ci-test-retry", props);
    return new CachingTestRepository(cb, retry, backend);
  }

  // -------------------------------------------------------------------------
  // Cache baseline – verifies the cache doubles are working correctly
  // -------------------------------------------------------------------------

  @Test
  void repeatedFetchesAreServedFromCacheWithoutHittingBackend() {
    CachingTestRepository repo = newRepo();

    repo.fetch();
    repo.fetch();
    repo.fetch();

    assertThat(repo.getBackendCallCount()).isEqualTo(1);
  }

  @Test
  void firstFetchAlwaysHitsBackend() {
    CachingTestRepository repo = newRepo();

    repo.fetch();

    assertThat(repo.getBackendCallCount()).isEqualTo(1);
  }

  // -------------------------------------------------------------------------
  // Core flow: event → handler → invalidate → re-fetch
  // -------------------------------------------------------------------------

  @Test
  void eventHandlerInvalidationCausesReFetchOnNextRequest() {
    CachingTestRepository repo = newRepo();
    TestEventHandler handler = new TestEventHandler(repo);

    // 1. Warm the cache
    repo.fetch();
    assertThat(repo.getBackendCallCount()).isEqualTo(1);

    // 2. Repeated fetch – served from cache
    repo.fetch();
    assertThat(repo.getBackendCallCount()).isEqualTo(1);

    // 3. Event fires → handler invalidates cache
    handler.onDataChangedEvent();

    // 4. Next fetch must go back to the backend
    repo.fetch();
    assertThat(repo.getBackendCallCount()).isEqualTo(2);
  }

  @Test
  void postInvalidationResultReflectsUpdatedBackendData() {
    AtomicInteger seq = new AtomicInteger(0);
    CachingTestRepository repo = newRepo(() -> "v" + seq.incrementAndGet());
    TestEventHandler handler = new TestEventHandler(repo);

    // First fetch returns first backend value and caches it
    assertThat(repo.fetch()).isEqualTo("v1");
    // Cache is warm – still returns stale value
    assertThat(repo.fetch()).isEqualTo("v1");

    // Event fires – cache cleared
    handler.onDataChangedEvent();

    // Next fetch returns the fresh backend value
    assertThat(repo.fetch()).isEqualTo("v2");
  }

  @Test
  void cacheRemainsWarmUntilEventFires() {
    CachingTestRepository repo = newRepo();
    TestEventHandler handler = new TestEventHandler(repo);

    repo.fetch(); // warm cache

    // Many fetches without an event – only one backend call total
    for (int i = 0; i < 10; i++)
      repo.fetch();

    assertThat(repo.getBackendCallCount()).isEqualTo(1);

    // After the event, the very next fetch is a backend call
    handler.onDataChangedEvent();
    repo.fetch();

    assertThat(repo.getBackendCallCount()).isEqualTo(2);
  }

  @Test
  void multipleSequentialEventsEachRequireOnlyOneReFetch() {
    CachingTestRepository repo = newRepo();
    TestEventHandler handler = new TestEventHandler(repo);

    repo.fetch(); // warm cache

    // Two back-to-back events before the next read
    handler.onDataChangedEvent();
    handler.onDataChangedEvent();

    // One re-fetch re-warms the cache
    repo.fetch();
    assertThat(repo.getBackendCallCount()).isEqualTo(2);

    // Subsequent fetches are served from the freshly warmed cache
    repo.fetch();
    repo.fetch();
    assertThat(repo.getBackendCallCount()).isEqualTo(2);
  }

  @Test
  void invalidateCacheOnColdRepositoryDoesNotThrow() {
    CachingTestRepository repo = newRepo();
    TestEventHandler handler = new TestEventHandler(repo);

    // No fetch has happened yet – cache is already cold
    assertThatCode(handler::onDataChangedEvent)
        .doesNotThrowAnyException();
  }

  @Test
  void fetchAfterInvalidateOnColdRepositoryHitsBackend() {
    CachingTestRepository repo = newRepo();
    TestEventHandler handler = new TestEventHandler(repo);

    // Invalidate before any fetch
    handler.onDataChangedEvent();

    repo.fetch();

    assertThat(repo.getBackendCallCount()).isEqualTo(1);
  }

  // -------------------------------------------------------------------------
  // invalidateCache() contract on the base class
  // -------------------------------------------------------------------------

  @Test
  void defaultInvalidateCacheIsANoOpForRepositoriesWithoutCache() {
    // A repository that does not override invalidateCache() should not throw
    AbstractFusionRepository bare = new AbstractFusionRepository(
        AbstractFusionRepository.createCircuitBreaker("ci-bare-cb", new FusionProperties()),
        AbstractFusionRepository.createRetry("ci-bare-retry", new FusionProperties()),
        500) {
    };

    assertThatCode(bare::invalidateCache).doesNotThrowAnyException();
  }
}
