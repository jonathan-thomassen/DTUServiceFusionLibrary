package dtu.fusion;

/**
 * Fluent builder for Oracle Fusion REST query strings (the {@code q=}
 * parameter).
 *
 * <p>
 * Handles {@code AND}-joining of multiple predicates, single-quote sanitisation
 * for injection defence, and {@code \\w+} validation for short enum-like code
 * fields.
 *
 * <p>
 * Usage example:
 * 
 * <pre>{@code
 * String q = new FusionQueryBuilder().raw(rawQueryParam).like("Name", name).eq("LocationCode", locationCode)
 *     .eqCode("status", "Status", status).literal(activeOnly ? "Status='A'" : null).build();
 * }</pre>
 */
public final class FusionQueryBuilder
{
  private static final String AND = " AND ";
  private final StringBuilder sb = new StringBuilder();

  /**
   * Appends a raw, pre-formed query fragment (passed through unchanged). Use for
   * the caller-supplied {@code query} passthrough parameter.
   */
  public FusionQueryBuilder raw(String fragment)
  {
    if (fragment != null && !fragment.isBlank())
      sb.append(fragment);
    return this;
  }

  /**
   * Appends {@code field LIKE '%value%'}, stripping single-quotes from
   * {@code value}.
   */
  public FusionQueryBuilder like(String field, String value)
  {
    if (value != null && !value.isBlank())
      and(field + " LIKE '%" + strip(value) + "%'");
    return this;
  }

  /**
   * Appends {@code field='value'}, stripping single-quotes from {@code value}.
   * Use for fields where arbitrary printable characters are valid (e.g. project
   * numbers).
   */
  public FusionQueryBuilder eq(String field, String value)
  {
    if (value != null && !value.isBlank())
      and(field + "='" + strip(value) + "'");
    return this;
  }

  /**
   * Appends {@code field=value} for a numeric (non-quoted) equality filter. Using
   * a primitive {@code long} makes interpolation safe: no injection possible.
   */
  public FusionQueryBuilder eqNum(String field, long value)
  {
    and(field + "=" + value);
    return this;
  }

  /**
   * Appends {@code field='value'} after validating that {@code value} matches
   * {@code \\w+}. Use for short enum-like code fields (e.g. status codes,
   * classification codes).
   *
   * @param paramName human-readable parameter name used in the exception message
   * @throws IllegalArgumentException if {@code value} contains non-word
   *                                  characters
   */
  public FusionQueryBuilder eqCode(String paramName, String field, String value)
  {
    if (value != null && !value.isBlank())
    {
      if (!value.matches("\\w+"))
        throw new IllegalArgumentException("Invalid value for filter field '" + paramName + "': " + value);
      and(field + "='" + value + "'");
    }
    return this;
  }

  /**
   * Appends a hard-coded literal condition (no user input — no sanitisation
   * applied). Passing {@code null} or blank is a no-op.
   */
  public FusionQueryBuilder literal(String condition)
  {
    if (condition != null && !condition.isBlank())
      and(condition);
    return this;
  }

  /**
   * Returns the built query string, or {@code null} when no conditions were
   * added.
   */
  public String build()
  {
    return sb.isEmpty() ? null : sb.toString();
  }

  private void and(String condition)
  {
    if (!sb.isEmpty())
      sb.append(AND);
    sb.append(condition);
  }

  private static String strip(String value)
  {
    return value.replace("'", "");
  }
}
