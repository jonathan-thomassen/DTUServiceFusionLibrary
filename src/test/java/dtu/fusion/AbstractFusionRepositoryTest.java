package dtu.fusion;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AbstractFusionRepositoryTest {
  // Minimal concrete subclass for testing protected APIs
  private static final class TestRepository extends AbstractFusionRepository {
    TestRepository(CircuitBreaker circuitBreaker, Retry retry, int maxPaginationLimit) {
      super(circuitBreaker, retry, maxPaginationLimit);
    }

    <T> T callExecuteWithResilience(Supplier<T> supplier) {
      return executeWithResilience(supplier);
    }

    void callValidatePagination(Integer limit, Integer offset) {
      validatePagination(limit, offset);
    }
  }

  private static FusionProperties defaultProps() {
    return new FusionProperties();
  }

  private static TestRepository defaultRepo() {
    FusionProperties props = defaultProps();
    CircuitBreaker cb = AbstractFusionRepository.createCircuitBreaker("test-cb", props);
    Retry retry = AbstractFusionRepository.createRetry("test-retry", props);
    return new TestRepository(cb, retry, props.getPagination().getMaxLimit());
  }

  // -------------------------------------------------------------------------
  // createCircuitBreaker
  // -------------------------------------------------------------------------

  @Test
  void createCircuitBreakerReturnsClosedBreaker() {
    CircuitBreaker cb = AbstractFusionRepository.createCircuitBreaker("repo-test-cb", defaultProps());

    assertThat(cb).isNotNull();
    assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
  }

  // -------------------------------------------------------------------------
  // createRetry
  // -------------------------------------------------------------------------

  @Test
  void createRetryReturnsNonNullRetry() {
    Retry retry = AbstractFusionRepository.createRetry("repo-test-retry", defaultProps());

    assertThat(retry).isNotNull();
  }

  // -------------------------------------------------------------------------
  // circuitBreakerState
  // -------------------------------------------------------------------------

  @Test
  void circuitBreakerStateReturnsClosedInitially() {
    assertThat(defaultRepo().circuitBreakerState()).isEqualTo(CircuitBreaker.State.CLOSED);
  }

  // -------------------------------------------------------------------------
  // executeWithResilience
  // -------------------------------------------------------------------------

  @Test
  void executeWithResilienceReturnsSupplierResult() {
    String result = defaultRepo().callExecuteWithResilience(() -> "hello");

    assertThat(result).isEqualTo("hello");
  }

  @Test
  void executeWithResiliencePropagatesRestClientException() {
    TestRepository repo = defaultRepo();

    assertThatThrownBy(() -> repo.callExecuteWithResilience(() -> {
      throw new RestClientException("down");
    })).isInstanceOf(RestClientException.class);
  }

  // -------------------------------------------------------------------------
  // validatePagination – null / valid inputs (no exception)
  // -------------------------------------------------------------------------

  @Test
  void validatePaginationAcceptsNullLimitAndOffset() {
    assertThatCode(() -> defaultRepo().callValidatePagination(null, null)).doesNotThrowAnyException();
  }

  @Test
  void validatePaginationAcceptsNullLimit() {
    assertThatCode(() -> defaultRepo().callValidatePagination(null, 0)).doesNotThrowAnyException();
  }

  @Test
  void validatePaginationAcceptsNullOffset() {
    assertThatCode(() -> defaultRepo().callValidatePagination(1, null)).doesNotThrowAnyException();
  }

  @Test
  void validatePaginationAcceptsValidLimitAndOffset() {
    assertThatCode(() -> defaultRepo().callValidatePagination(10, 5)).doesNotThrowAnyException();
  }

  @Test
  void validatePaginationAcceptsMaxLimitAndZeroOffset() {
    assertThatCode(() -> defaultRepo().callValidatePagination(500, 0)).doesNotThrowAnyException();
  }

  // -------------------------------------------------------------------------
  // validatePagination – invalid limit
  // -------------------------------------------------------------------------

  @Test
  void validatePaginationThrowsWhenLimitIsZero() {
    TestRepository repo = defaultRepo();
    assertThatThrownBy(() -> repo.callValidatePagination(0, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("limit must be >= 1");
  }

  @Test
  void validatePaginationThrowsWhenLimitIsNegative() {
    TestRepository repo = defaultRepo();
    assertThatThrownBy(() -> repo.callValidatePagination(-5, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("limit must be >= 1");
  }

  @Test
  void validatePaginationThrowsWhenLimitExceedsMax() {
    TestRepository repo = defaultRepo();
    assertThatThrownBy(() -> repo.callValidatePagination(501, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("limit must be <=");
  }

  // -------------------------------------------------------------------------
  // validatePagination – invalid offset
  // -------------------------------------------------------------------------

  @Test
  void validatePaginationThrowsWhenOffsetIsNegative() {
    TestRepository repo = defaultRepo();
    assertThatThrownBy(() -> repo.callValidatePagination(10, -1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("offset must be >= 0");
  }
}
