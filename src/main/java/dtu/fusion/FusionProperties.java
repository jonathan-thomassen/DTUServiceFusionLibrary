package dtu.fusion;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly-typed representation of all {@code fusion.*} configuration
 * properties shared across all Fusion repository beans.
 *
 * <p>
 * Defaults match the values in {@code application.properties}, so tests that
 * construct repositories directly can simply pass
 * {@code new FusionProperties()} without specifying any overrides.
 */
@ConfigurationProperties(prefix = "fusion")
public class FusionProperties
{
  private CircuitBreaker circuitBreaker = new CircuitBreaker();
  private Cache cache = new Cache();
  private VirtualThread virtualThread = new VirtualThread();
  private Pagination pagination = new Pagination();
  private Dff dff = new Dff();
  private RetrySettings retry = new RetrySettings();

  public CircuitBreaker getCircuitBreaker()
  {
    return circuitBreaker;
  }

  public void setCircuitBreaker(CircuitBreaker v)
  {
    this.circuitBreaker = v;
  }

  public Cache getCache()
  {
    return cache;
  }

  public void setCache(Cache v)
  {
    this.cache = v;
  }

  public VirtualThread getVirtualThread()
  {
    return virtualThread;
  }

  public void setVirtualThread(VirtualThread v)
  {
    this.virtualThread = v;
  }

  public Pagination getPagination()
  {
    return pagination;
  }

  public void setPagination(Pagination v)
  {
    this.pagination = v;
  }

  public Dff getDff()
  {
    return dff;
  }

  public void setDff(Dff v)
  {
    this.dff = v;
  }

  public RetrySettings getRetry()
  {
    return retry;
  }

  public void setRetry(RetrySettings v)
  {
    this.retry = v;
  }

  public static class CircuitBreaker
  {
    private int slidingWindowSize = 10;
    private float failureRateThreshold = 50.0f;
    private int waitDurationOpenStateSeconds = 30;

    public int getSlidingWindowSize()
    {
      return slidingWindowSize;
    }

    public void setSlidingWindowSize(int v)
    {
      this.slidingWindowSize = v;
    }

    public float getFailureRateThreshold()
    {
      return failureRateThreshold;
    }

    public void setFailureRateThreshold(float v)
    {
      this.failureRateThreshold = v;
    }

    public int getWaitDurationOpenStateSeconds()
    {
      return waitDurationOpenStateSeconds;
    }

    public void setWaitDurationOpenStateSeconds(int v)
    {
      this.waitDurationOpenStateSeconds = v;
    }
  }

  public static class Cache
  {
    private int resultTtlMinutes = 5;
    private int lookupTtlHours = 1;
    private int templateTtlHours = 1;

    public int getResultTtlMinutes()
    {
      return resultTtlMinutes;
    }

    public void setResultTtlMinutes(int v)
    {
      this.resultTtlMinutes = v;
    }

    public int getLookupTtlHours()
    {
      return lookupTtlHours;
    }

    public void setLookupTtlHours(int v)
    {
      this.lookupTtlHours = v;
    }

    public int getTemplateTtlHours()
    {
      return templateTtlHours;
    }

    public void setTemplateTtlHours(int v)
    {
      this.templateTtlHours = v;
    }
  }

  /**
   * Configuration for the shared virtual-thread executor used by all Fusion
   * repository beans.
   */
  public static class VirtualThread
  {
    /**
     * Maximum number of enrichment tasks that may run concurrently on the shared
     * virtual-thread executor. Limits GC pressure and Fusion HTTP connection pool
     * saturation under burst load. Defaults to 300.
     */
    private int maxConcurrency = 300;

    /**
     * Maximum time in seconds that a single per-item enrichment (fan-out of
     * parallel Fusion lookups) is allowed to take before it is cancelled and
     * partial data is returned instead. Defaults to 10 seconds.
     */
    private int enrichmentTimeoutSeconds = 10;

    public int getMaxConcurrency()
    {
      return maxConcurrency;
    }

    public void setMaxConcurrency(int v)
    {
      this.maxConcurrency = v;
    }

    public int getEnrichmentTimeoutSeconds()
    {
      return enrichmentTimeoutSeconds;
    }

    public void setEnrichmentTimeoutSeconds(int v)
    {
      this.enrichmentTimeoutSeconds = v;
    }
  }

  /**
   * Pagination guard-rails applied before forwarding requests to Fusion. Prevents
   * callers from requesting more records than Fusion can safely return in one
   * page (Oracle Fusion's hard cap is 500).
   */
  public static class Pagination
  {
    /**
     * Upper bound on the {@code limit} query parameter. Any request with a
     * {@code limit} above this value is rejected with HTTP 400. Defaults to 500
     * (Oracle Fusion's natural maximum page size).
     */
    private int maxLimit = 500;

    public int getMaxLimit()
    {
      return maxLimit;
    }

    public void setMaxLimit(int v)
    {
      this.maxLimit = v;
    }
  }

  /**
   * Attribute key names for Oracle Fusion Descriptive Flexfields (DFF). These are
   * the technical column names assigned in the Fusion DFF setup and must be
   * confirmed by running the discovery call in {@code sandbox/test.rest} (call 9:
   * expand=OrganizationDFF) against the actual Fusion instance before going to
   * production.
   */
  public static class Dff
  {
    /**
     * Attribute key for the DTU department number stored in
     * {@code OrganizationDFF}. Confirm via call 9 in sandbox/test.rest.
     */
    private String orgNumberAttributeKey = "dtuDepartmentNumber";

    /**
     * Attribute key for the organisation unit type (e.g. "Institut") stored in
     * {@code OrganizationDFF}. Confirm via call 9 in sandbox/test.rest.
     */
    private String orgTypeAttributeKey = "organizationType";

    public String getOrgNumberAttributeKey()
    {
      return orgNumberAttributeKey;
    }

    public void setOrgNumberAttributeKey(String v)
    {
      this.orgNumberAttributeKey = v;
    }

    public String getOrgTypeAttributeKey()
    {
      return orgTypeAttributeKey;
    }

    public void setOrgTypeAttributeKey(String v)
    {
      this.orgTypeAttributeKey = v;
    }
  }

  /**
   * Retry policy applied inside the circuit breaker for each Fusion HTTP call. A
   * single retry with a short fixed backoff absorbs transient network hiccups
   * without incrementing the circuit-breaker failure rate.
   */
  public static class RetrySettings
  {
    /**
     * Total number of attempts including the initial call. Defaults to 2 (one retry
     * on the first failure).
     */
    private int maxAttempts = 2;

    /**
     * Fixed wait duration in milliseconds between retry attempts. Defaults to 200
     * ms.
     */
    private long waitDurationMillis = 200L;

    public int getMaxAttempts()
    {
      return maxAttempts;
    }

    public void setMaxAttempts(int v)
    {
      this.maxAttempts = v;
    }

    public long getWaitDurationMillis()
    {
      return waitDurationMillis;
    }

    public void setWaitDurationMillis(long v)
    {
      this.waitDurationMillis = v;
    }
  }
}
