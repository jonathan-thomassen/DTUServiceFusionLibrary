package dtu.fusion;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FusionPropertiesTest {
  // -------------------------------------------------------------------------
  // FusionProperties setters
  // -------------------------------------------------------------------------

  @Test
  void setCircuitBreakerReplacesValue() {
    FusionProperties props = new FusionProperties();
    FusionProperties.CircuitBreaker cb = new FusionProperties.CircuitBreaker();
    props.setCircuitBreaker(cb);
    assertThat(props.getCircuitBreaker()).isSameAs(cb);
  }

  @Test
  void setCacheReplacesValue() {
    FusionProperties props = new FusionProperties();
    FusionProperties.Cache cache = new FusionProperties.Cache();
    props.setCache(cache);
    assertThat(props.getCache()).isSameAs(cache);
  }

  // -------------------------------------------------------------------------
  // CircuitBreaker setters
  // -------------------------------------------------------------------------

  @Test
  void circuitBreakerSetSlidingWindowSizeStoresValue() {
    FusionProperties.CircuitBreaker cb = new FusionProperties.CircuitBreaker();
    cb.setSlidingWindowSize(20);
    assertThat(cb.getSlidingWindowSize()).isEqualTo(20);
  }

  @Test
  void circuitBreakerSetFailureRateThresholdStoresValue() {
    FusionProperties.CircuitBreaker cb = new FusionProperties.CircuitBreaker();
    cb.setFailureRateThreshold(75.0f);
    assertThat(cb.getFailureRateThreshold()).isEqualTo(75.0f);
  }

  @Test
  void circuitBreakerSetWaitDurationOpenStateSecondsStoresValue() {
    FusionProperties.CircuitBreaker cb = new FusionProperties.CircuitBreaker();
    cb.setWaitDurationOpenStateSeconds(60);
    assertThat(cb.getWaitDurationOpenStateSeconds()).isEqualTo(60);
  }

  // -------------------------------------------------------------------------
  // Cache setters
  // -------------------------------------------------------------------------

  @Test
  void cacheSetResultTtlMinutesStoresValue() {
    FusionProperties.Cache cache = new FusionProperties.Cache();
    cache.setResultTtlMinutes(10);
    assertThat(cache.getResultTtlMinutes()).isEqualTo(10);
  }

  @Test
  void cacheSetLookupTtlHoursStoresValue() {
    FusionProperties.Cache cache = new FusionProperties.Cache();
    cache.setLookupTtlHours(2);
    assertThat(cache.getLookupTtlHours()).isEqualTo(2);
  }

  @Test
  void cacheSetTemplateTtlHoursStoresValue() {
    FusionProperties.Cache cache = new FusionProperties.Cache();
    cache.setTemplateTtlHours(3);
    assertThat(cache.getTemplateTtlHours()).isEqualTo(3);
  }

  // -------------------------------------------------------------------------
  // Pagination setters
  // -------------------------------------------------------------------------

  @Test
  void setPaginationReplacesValue() {
    FusionProperties props = new FusionProperties();
    FusionProperties.Pagination pagination = new FusionProperties.Pagination();
    props.setPagination(pagination);
    assertThat(props.getPagination()).isSameAs(pagination);
  }

  @Test
  void paginationDefaultMaxLimitIs500() {
    assertThat(new FusionProperties().getPagination().getMaxLimit()).isEqualTo(500);
  }

  @Test
  void paginationSetMaxLimitStoresValue() {
    FusionProperties.Pagination pagination = new FusionProperties.Pagination();
    pagination.setMaxLimit(100);
    assertThat(pagination.getMaxLimit()).isEqualTo(100);
  }

  // -------------------------------------------------------------------------
  // Dff setters
  // -------------------------------------------------------------------------

  @Test
  void setDffReplacesValue() {
    FusionProperties props = new FusionProperties();
    FusionProperties.Dff dff = new FusionProperties.Dff();
    props.setDff(dff);
    assertThat(props.getDff()).isSameAs(dff);
  }

  @Test
  void dffDefaultOrgNumberAttributeKeyIsDtuDepartmentNumber() {
    assertThat(new FusionProperties().getDff().getOrgNumberAttributeKey()).isEqualTo("dtuDepartmentNumber");
  }

  @Test
  void dffDefaultOrgTypeAttributeKeyIsOrganizationType() {
    assertThat(new FusionProperties().getDff().getOrgTypeAttributeKey()).isEqualTo("organizationType");
  }

  @Test
  void dffSetOrgNumberAttributeKeyStoresValue() {
    FusionProperties.Dff dff = new FusionProperties.Dff();
    dff.setOrgNumberAttributeKey("DFF_OrgNum");
    assertThat(dff.getOrgNumberAttributeKey()).isEqualTo("DFF_OrgNum");
  }

  @Test
  void dffSetOrgTypeAttributeKeyStoresValue() {
    FusionProperties.Dff dff = new FusionProperties.Dff();
    dff.setOrgTypeAttributeKey("DFF_OrgType");
    assertThat(dff.getOrgTypeAttributeKey()).isEqualTo("DFF_OrgType");
  }

  // -------------------------------------------------------------------------
  // RetrySettings setters
  // -------------------------------------------------------------------------

  @Test
  void setRetryReplacesValue() {
    FusionProperties props = new FusionProperties();
    FusionProperties.RetrySettings retry = new FusionProperties.RetrySettings();
    props.setRetry(retry);
    assertThat(props.getRetry()).isSameAs(retry);
  }

  @Test
  void retryDefaultMaxAttemptsIs2() {
    assertThat(new FusionProperties().getRetry().getMaxAttempts()).isEqualTo(2);
  }

  @Test
  void retryDefaultWaitDurationMillisIs200() {
    assertThat(new FusionProperties().getRetry().getWaitDurationMillis()).isEqualTo(200L);
  }

  @Test
  void retrySetMaxAttemptsStoresValue() {
    FusionProperties.RetrySettings retry = new FusionProperties.RetrySettings();
    retry.setMaxAttempts(5);
    assertThat(retry.getMaxAttempts()).isEqualTo(5);
  }

  @Test
  void retrySetWaitDurationMillisStoresValue() {
    FusionProperties.RetrySettings retry = new FusionProperties.RetrySettings();
    retry.setWaitDurationMillis(500L);
    assertThat(retry.getWaitDurationMillis()).isEqualTo(500L);
  }

  // -------------------------------------------------------------------------
  // VirtualThread setters
  // -------------------------------------------------------------------------

  @Test
  void setVirtualThreadReplacesValue() {
    FusionProperties props = new FusionProperties();
    FusionProperties.VirtualThread vt = new FusionProperties.VirtualThread();
    props.setVirtualThread(vt);
    assertThat(props.getVirtualThread()).isSameAs(vt);
  }

  @Test
  void virtualThreadDefaultMaxConcurrencyIs300() {
    assertThat(new FusionProperties().getVirtualThread().getMaxConcurrency()).isEqualTo(300);
  }

  @Test
  void virtualThreadDefaultEnrichmentTimeoutSecondsIs10() {
    assertThat(new FusionProperties().getVirtualThread().getEnrichmentTimeoutSeconds()).isEqualTo(10);
  }

  @Test
  void virtualThreadSetMaxConcurrencyStoresValue() {
    FusionProperties.VirtualThread vt = new FusionProperties.VirtualThread();
    vt.setMaxConcurrency(50);
    assertThat(vt.getMaxConcurrency()).isEqualTo(50);
  }

  @Test
  void virtualThreadSetEnrichmentTimeoutSecondsStoresValue() {
    FusionProperties.VirtualThread vt = new FusionProperties.VirtualThread();
    vt.setEnrichmentTimeoutSeconds(30);
    assertThat(vt.getEnrichmentTimeoutSeconds()).isEqualTo(30);
  }
}
