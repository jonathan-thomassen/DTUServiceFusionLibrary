package dtu.fusion;

import io.github.resilience4j.retry.Retry;
import java.lang.reflect.Constructor;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FusionRetryConfigTest
{
  @Test
  void buildReturnsNonNullRetry()
  {
    Retry retry = FusionRetryConfig.build("test", 2, 100L);

    assertThat(retry).isNotNull();
  }

  @Test
  void constructorIsPrivate() throws Exception
  {
    Constructor<FusionRetryConfig> ctor = FusionRetryConfig.class.getDeclaredConstructor();

    assertThat(ctor.canAccess(null)).isFalse();

    ctor.setAccessible(true);
    ctor.newInstance(); // covers the private constructor body
  }

  @Test
  void retryRetriesOnRestClientException()
  {
    // maxAttempts=2 means 1 retry → total 2 calls
    Retry retry = FusionRetryConfig.build("test-retry", 2, 0L);
    int[] callCount = { 0 };

    assertThatThrownBy(() -> retry.executeSupplier(() ->
    {
      callCount[0]++;
      throw new RestClientException("down");
    })).isInstanceOf(RestClientException.class);

    assertThat(callCount[0]).isEqualTo(2);
  }

  @Test
  void retryDoesNotRetryOnNonRestClientException()
  {
    // Should not retry non-RestClientException failures
    Retry retry = FusionRetryConfig.build("test-no-retry", 3, 0L);
    int[] callCount = { 0 };

    assertThatThrownBy(() -> retry.executeSupplier(() ->
    {
      callCount[0]++;
      throw new IllegalStateException("unexpected");
    })).isInstanceOf(IllegalStateException.class);

    assertThat(callCount[0]).isEqualTo(1);
  }

  @Test
  void retrySucceedsWithinAttemptLimit()
  {
    // Fails first attempt, succeeds second → result returned without exception
    Retry retry = FusionRetryConfig.build("test-success", 3, 0L);
    int[] callCount = { 0 };

    String result = retry.executeSupplier(() ->
    {
      callCount[0]++;
      if (callCount[0] < 2)
        throw new RestClientException("transient");
      return "ok";
    });

    assertThat(result).isEqualTo("ok");
    assertThat(callCount[0]).isEqualTo(2);
  }
}
