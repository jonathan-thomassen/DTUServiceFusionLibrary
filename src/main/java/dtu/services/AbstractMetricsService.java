package dtu.services;

import tools.jackson.databind.ObjectMapper;
import dtu.services.library.metrics.MetricsAggregator;
import org.springframework.http.ResponseEntity;

/**
 * Abstract base for the generated-per-service {@code MetricsService}
 * controller.
 *
 * <p>
 * Holds the shared implementation so each service's {@code MetricsService} only
 * needs to declare its {@code MetricsApi} contract and delegate to
 * {@link #getMetrics()}.
 *
 * @param <M> the generated per-service {@code Metrics} API model type
 */
public abstract class AbstractMetricsService<M>
{
  private final MetricsAggregator metrics;
  private final ObjectMapper mapper;
  private final Class<M> metricsType;

  protected AbstractMetricsService(MetricsAggregator metrics, ObjectMapper mapper, Class<M> metricsType)
  {
    this.metrics = metrics;
    this.mapper = mapper;
    this.metricsType = metricsType;
  }

  protected ResponseEntity<M> getMetrics()
  {
    M stats = mapper.convertValue(metrics.getSnapshot(), metricsType);
    return ResponseEntity.ok(stats);
  }
}
