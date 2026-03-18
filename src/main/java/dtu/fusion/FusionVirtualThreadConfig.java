package dtu.fusion;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Shared Spring configuration that provisions a single virtual-thread executor
 * for all Fusion repository parallel operations.
 *
 * <p>
 * Both the HCM and ERP services consume this bean by name
 * ({@code fusionVirtualThreadExecutor}) for fan-out per-item enrichment or
 * parallel list conversion. Declaring it here in {@code common} avoids
 * duplicating the same bean definition in each service's client config.
 *
 * <p>
 * The executor is wrapped with a {@link Semaphore} (sized by
 * {@code fusion.virtual-thread.max-concurrency}) to cap the number of
 * concurrently running enrichment tasks. Virtual threads are cheap to create
 * but not free — excessive concurrency creates GC pressure and can saturate the
 * shared Fusion HTTP connection pool.
 */
@Configuration
@EnableConfigurationProperties(FusionProperties.class)
public class FusionVirtualThreadConfig
{
  @Bean(destroyMethod = "shutdown")
  public ExecutorService fusionVirtualThreadExecutor(FusionProperties fusionProperties)
  {
    int maxConcurrency = fusionProperties.getVirtualThread().getMaxConcurrency();
    ExecutorService delegate = Executors.newVirtualThreadPerTaskExecutor();
    return new BoundedVirtualExecutor(delegate, maxConcurrency);
  }

  /**
   * {@link ExecutorService} decorator that uses a {@link Semaphore} to cap the
   * number of tasks executing concurrently on the underlying virtual-thread
   * executor. The semaphore is acquired before the task is submitted and released
   * inside the task, so callers never block longer than it takes for one permit
   * to become available.
   */
  static final class BoundedVirtualExecutor extends AbstractExecutorService
  {
    private final ExecutorService delegate;
    private final Semaphore semaphore;

    BoundedVirtualExecutor(ExecutorService delegate, int maxConcurrency)
    {
      this.delegate = delegate;
      this.semaphore = new Semaphore(maxConcurrency);
    }

    @Override
    public void execute(Runnable command)
    {
      semaphore.acquireUninterruptibly();
      delegate.execute(() ->
      {
        try
        {
          command.run();
        } finally
        {
          semaphore.release();
        }
      });
    }

    @Override
    public void shutdown()
    {
      delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow()
    {
      return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown()
    {
      return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated()
    {
      return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException
    {
      return delegate.awaitTermination(timeout, unit);
    }
  }
}
