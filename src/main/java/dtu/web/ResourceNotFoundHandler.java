package dtu.web;

import dtu.services.library.errors.ResourceNotFoundException;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Overrides the library-level ErrorHandler for missing static resources.
 *
 * <p>
 * Spring Boot 3+ throws {@link NoResourceFoundException} when a static resource
 * (e.g. /favicon.ico, /swagger-ui/) is not found. The shared
 * dtu-service-library ErrorHandler does not handle this exception, causing it
 * to be reported as an unexpected 500. This advice intercepts it first and
 * returns a plain 404 instead.
 *
 * <p>
 * Registered in {@code fusion-common} so all Fusion services get the fix
 * automatically via Spring Boot's component scan of the {@code dtu} package.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ResourceNotFoundHandler
{
  @ExceptionHandler(NoResourceFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public void handleNoResourceFound()
  {
    // Return 404 with an empty body — no logging needed for missing statics.
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<Map<String, String>> handleResourceNotFound(ResourceNotFoundException ex)
  {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex)
  {
    return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
  }
}
