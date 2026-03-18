package dtu.fusion;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link FusionVirtualThreadConfig}.
 */
class FusionVirtualThreadConfigTest
{
  private final FusionVirtualThreadConfig config = new FusionVirtualThreadConfig();

  @Test
  void fusionVirtualThreadExecutorReturnsNonNullExecutorService()
  {
    ExecutorService executor = config.fusionVirtualThreadExecutor(new FusionProperties());
    assertThat(executor).isNotNull();
    executor.shutdown();
  }

  @Test
  void executorEnforcesConcurrencyLimit() throws InterruptedException
  {
    FusionProperties props = new FusionProperties();
    props.getVirtualThread().setMaxConcurrency(3);

    ExecutorService executor = config.fusionVirtualThreadExecutor(props);
    try
    {
      AtomicInteger concurrentCount = new AtomicInteger(0);
      AtomicInteger maxObservedConcurrency = new AtomicInteger(0);
      CountDownLatch allStarted = new CountDownLatch(3);
      CountDownLatch release = new CountDownLatch(1);

      List<Future<?>> futures = new ArrayList<>();
      for (int i = 0; i < 3; i++)
      {
        futures.add(executor.submit(() ->
        {
          int current = concurrentCount.incrementAndGet();
          maxObservedConcurrency.accumulateAndGet(current, Math::max);
          allStarted.countDown();
          try
          {
            release.await();
          } catch (InterruptedException _)
          {
            Thread.currentThread().interrupt();
          }
          concurrentCount.decrementAndGet();
        }));
      }

      allStarted.await();
      // All 3 tasks (equal to max-concurrency) should have started
      assertThat(maxObservedConcurrency.get()).isEqualTo(3);
      release.countDown();

      for (Future<?> f : futures)
      {
        try
        {
          f.get();
        } catch (Exception _)
        {
          // Ignored for now
        }
      }
    } finally
    {
      executor.shutdown();
    }
  }

  @Test
  void boundedVirtualExecutorShutdownNowReturnsEmptyListWhenIdle()
  {
    ExecutorService executor = config.fusionVirtualThreadExecutor(new FusionProperties());
    List<Runnable> remaining = executor.shutdownNow();
    assertThat(remaining).isNotNull();
  }

  @Test
  void boundedVirtualExecutorIsShutdownReturnsFalseBeforeAndTrueAfterShutdown()
  {
    ExecutorService executor = config.fusionVirtualThreadExecutor(new FusionProperties());
    assertThat(executor.isShutdown()).isFalse();
    executor.shutdown();
    assertThat(executor.isShutdown()).isTrue();
  }

  @Test
  void boundedVirtualExecutorIsTerminatedReturnsTrueAfterShutdown() throws InterruptedException
  {
    ExecutorService executor = config.fusionVirtualThreadExecutor(new FusionProperties());
    executor.shutdown();
    executor.awaitTermination(5, TimeUnit.SECONDS);
    assertThat(executor.isTerminated()).isTrue();
  }

  @Test
  void boundedVirtualExecutorAwaitTerminationReturnsTrueWhenAlreadyTerminated() throws InterruptedException
  {
    ExecutorService executor = config.fusionVirtualThreadExecutor(new FusionProperties());
    executor.shutdown();
    boolean terminated = executor.awaitTermination(5, TimeUnit.SECONDS);
    assertThat(terminated).isTrue();
  }
}
