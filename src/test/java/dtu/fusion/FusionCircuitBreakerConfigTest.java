package dtu.fusion;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.lang.reflect.Constructor;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class FusionCircuitBreakerConfigTest
{
  @Test
  void buildNoArgReturnsNonNullConfig()
  {
    CircuitBreakerConfig config = FusionCircuitBreakerConfig.build();

    assertThat(config).isNotNull();
  }

  @Test
  void constructorIsPrivate() throws Exception
  {
    Constructor<FusionCircuitBreakerConfig> ctor = FusionCircuitBreakerConfig.class.getDeclaredConstructor();

    assertThat(ctor.canAccess(null)).isFalse();

    ctor.setAccessible(true);
    ctor.newInstance(); // covers the private constructor body
  }

  // -------------------------------------------------------------------------
  // Full state cycle: CLOSED → OPEN → HALF_OPEN → CLOSED
  // -------------------------------------------------------------------------

  @Test
  void circuitBreakerTransitionsClosedToOpenOnFailures()
  {
    // 2-call window, 100 % threshold → opens after 2 RestClientExceptions
    CircuitBreakerConfig config = FusionCircuitBreakerConfig.build(2, 100.0f, 1);
    CircuitBreaker cb = CircuitBreaker.of("test-closed-to-open", config);

    assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

    for (int i = 0; i < 2; i++)
    {
      try
      {
        cb.executeSupplier(() ->
        {
          throw new RestClientException("down");
        });
      } catch (Exception _)
      {
        // Expected: circuit breaker records the failure
      }
    }

    assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
  }

  @Test
  void circuitBreakerTransitionsOpenToHalfOpenManually()
  {
    CircuitBreakerConfig config = FusionCircuitBreakerConfig.build(2, 100.0f, 1);
    CircuitBreaker cb = CircuitBreaker.of("test-half-open", config);

    // Force open
    for (int i = 0; i < 2; i++)
    {
      try
      {
        cb.executeSupplier(() ->
        {
          throw new RestClientException("down");
        });
      } catch (Exception _)
      {
        // Expected: circuit breaker records the failure
      }
    }
    assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);

    cb.transitionToHalfOpenState();

    assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
  }

  @Test
  void circuitBreakerFullCycleClosedToOpenToHalfOpenToClosed()
  {
    // Use a 1-permitted-call HALF_OPEN window so a single success closes the
    // breaker
    CircuitBreakerConfig config = CircuitBreakerConfig.custom()
        .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED).slidingWindowSize(2)
        .failureRateThreshold(100.0f).waitDurationInOpenState(Duration.ofMillis(1))
        .permittedNumberOfCallsInHalfOpenState(1).recordExceptions(RestClientException.class).build();
    CircuitBreaker cb = CircuitBreaker.of("test-full-cycle", config);

    // CLOSED → OPEN
    for (int i = 0; i < 2; i++)
    {
      try
      {
        cb.executeSupplier(() ->
        {
          throw new RestClientException("down");
        });
      } catch (Exception _)
      {
        // Expected: circuit breaker records the failure
      }
    }
    assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);

    // OPEN → HALF_OPEN
    cb.transitionToHalfOpenState();
    assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);

    // HALF_OPEN → CLOSED (1 successful probe)
    assertThatCode(() -> cb.executeSupplier(() -> "ok")).doesNotThrowAnyException();
    assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
  }

  @Test
  void buildWithCustomSettingsProducesExpectedSlidingWindowSize()
  {
    CircuitBreakerConfig config = FusionCircuitBreakerConfig.build(5, 75.0f, 10);

    assertThat(config.getSlidingWindowSize()).isEqualTo(5);
    assertThat(config.getFailureRateThreshold()).isEqualTo(75.0f);
  }
}
