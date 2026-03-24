package dtu.health;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractFusionHealthIndicatorTest {
  private static AbstractFusionHealthIndicator indicatorWith(Map<String, CircuitBreaker.State> states) {
    return new AbstractFusionHealthIndicator() {
      @Override
      protected Map<String, CircuitBreaker.State> circuitBreakerStates() {
        return states;
      }
    };
  }

  // -------------------------------------------------------------------------
  // UP — all breakers CLOSED
  // -------------------------------------------------------------------------

  @Test
  void healthIsUpWhenAllBreakersAreClosed() {
    Map<String, CircuitBreaker.State> states = new LinkedHashMap<>();
    states.put("erp", CircuitBreaker.State.CLOSED);
    states.put("hcm", CircuitBreaker.State.CLOSED);

    Health health = indicatorWith(states).health();

    assertThat(health.getStatus()).isEqualTo(Status.UP);
  }

  @Test
  void healthDetailsContainAllBreakerNamesAndStates() {
    Map<String, CircuitBreaker.State> states = new LinkedHashMap<>();
    states.put("erp", CircuitBreaker.State.CLOSED);
    states.put("hcm", CircuitBreaker.State.CLOSED);

    Health health = indicatorWith(states).health();

    assertThat(health.getDetails()).containsEntry("erp", "CLOSED");
    assertThat(health.getDetails()).containsEntry("hcm", "CLOSED");
  }

  // -------------------------------------------------------------------------
  // DOWN — at least one breaker OPEN
  // -------------------------------------------------------------------------

  @Test
  void healthIsDownWhenAtLeastOneBreakerIsOpen() {
    Map<String, CircuitBreaker.State> states = new LinkedHashMap<>();
    states.put("erp", CircuitBreaker.State.OPEN);
    states.put("hcm", CircuitBreaker.State.CLOSED);

    Health health = indicatorWith(states).health();

    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
  }

  @Test
  void healthDownDetailsContainOpenBreakerState() {
    Map<String, CircuitBreaker.State> states = new LinkedHashMap<>();
    states.put("erp", CircuitBreaker.State.OPEN);

    Health health = indicatorWith(states).health();

    assertThat(health.getDetails()).containsEntry("erp", "OPEN");
  }

  // -------------------------------------------------------------------------
  // UNKNOWN — at least one breaker HALF_OPEN (and none OPEN)
  // -------------------------------------------------------------------------

  @Test
  void healthIsUnknownWhenAtLeastOneBreakerIsHalfOpen() {
    Map<String, CircuitBreaker.State> states = new LinkedHashMap<>();
    states.put("erp", CircuitBreaker.State.HALF_OPEN);
    states.put("hcm", CircuitBreaker.State.CLOSED);

    Health health = indicatorWith(states).health();

    assertThat(health.getStatus()).isEqualTo(Status.UNKNOWN);
  }

  // -------------------------------------------------------------------------
  // OPEN takes precedence over HALF_OPEN → DOWN
  // -------------------------------------------------------------------------

  @Test
  void openTakesPrecedenceOverHalfOpen() {
    Map<String, CircuitBreaker.State> states = new LinkedHashMap<>();
    states.put("erp", CircuitBreaker.State.OPEN);
    states.put("hcm", CircuitBreaker.State.HALF_OPEN);

    Health health = indicatorWith(states).health();

    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
  }
}
