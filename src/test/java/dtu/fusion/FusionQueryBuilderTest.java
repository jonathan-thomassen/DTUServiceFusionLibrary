package dtu.fusion;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FusionQueryBuilderTest {
  // -------------------------------------------------------------------------
  // build()
  // -------------------------------------------------------------------------

  @Test
  void buildReturnsNullWhenNoConditionsAdded() {
    assertThat(new FusionQueryBuilder().build()).isNull();
  }

  // -------------------------------------------------------------------------
  // raw()
  // -------------------------------------------------------------------------

  @Test
  void rawAppendsFragment() {
    String result = new FusionQueryBuilder().raw("ProjectNumber='P-001'").build();
    assertThat(result).isEqualTo("ProjectNumber='P-001'");
  }

  @Test
  void rawIgnoresNull() {
    assertThat(new FusionQueryBuilder().raw(null).build()).isNull();
  }

  @Test
  void rawIgnoresBlank() {
    assertThat(new FusionQueryBuilder().raw("   ").build()).isNull();
  }

  // -------------------------------------------------------------------------
  // like()
  // -------------------------------------------------------------------------

  @Test
  void likeAppendsPattern() {
    String result = new FusionQueryBuilder().like("Name", "fusion").build();
    assertThat(result).isEqualTo("Name LIKE '%fusion%'");
  }

  @Test
  void likeEscapesSingleQuotes() {
    String result = new FusionQueryBuilder().like("Name", "o'brien").build();
    assertThat(result).isEqualTo("Name LIKE '%o''brien%'");
  }

  @Test
  void likeIgnoresNull() {
    assertThat(new FusionQueryBuilder().like("Name", null).build()).isNull();
  }

  @Test
  void likeIgnoresBlank() {
    assertThat(new FusionQueryBuilder().like("Name", "  ").build()).isNull();
  }

  // -------------------------------------------------------------------------
  // eq()
  // -------------------------------------------------------------------------

  @Test
  void eqAppendsCondition() {
    String result = new FusionQueryBuilder().eq("ProjectNumber", "P-001").build();
    assertThat(result).isEqualTo("ProjectNumber='P-001'");
  }

  @Test
  void eqEscapesSingleQuotes() {
    String result = new FusionQueryBuilder().eq("Description", "it's done").build();
    assertThat(result).isEqualTo("Description='it''s done'");
  }

  @Test
  void eqIgnoresNull() {
    assertThat(new FusionQueryBuilder().eq("ProjectNumber", null).build()).isNull();
  }

  @Test
  void eqIgnoresBlank() {
    assertThat(new FusionQueryBuilder().eq("ProjectNumber", "").build()).isNull();
  }

  // -------------------------------------------------------------------------
  // eqNum()
  // -------------------------------------------------------------------------

  @Test
  void eqNumAppendsUnquotedNumber() {
    String result = new FusionQueryBuilder().eqNum("OrganizationId", 42L).build();
    assertThat(result).isEqualTo("OrganizationId=42");
  }

  @Test
  void eqNumZeroAppendsCondition() {
    String result = new FusionQueryBuilder().eqNum("OrganizationId", 0L).build();
    assertThat(result).isEqualTo("OrganizationId=0");
  }

  @Test
  void eqNumIsAndJoinedWithOtherConditions() {
    String result = new FusionQueryBuilder().eqNum("OrganizationId", 7L).eq("Name", "DTU").build();
    assertThat(result).isEqualTo("OrganizationId=7 AND Name='DTU'");
  }

  // -------------------------------------------------------------------------
  // eqCode()
  // -------------------------------------------------------------------------

  @Test
  void eqCodeAppendsConditionForWordCharacters() {
    String result = new FusionQueryBuilder().eqCode("status", "Status", "ACTIVE").build();
    assertThat(result).isEqualTo("Status='ACTIVE'");
  }

  @Test
  void eqCodeAcceptsAlphanumericAndUnderscore() {
    String result = new FusionQueryBuilder().eqCode("type", "ProjectType", "ORA_INTERNAL_2").build();
    assertThat(result).isEqualTo("ProjectType='ORA_INTERNAL_2'");
  }

  @Test
  void eqCodeThrowsForNonWordCharacters() {
    var builder = new FusionQueryBuilder();
    assertThatThrownBy(() -> builder.eqCode("status", "Status", "AC-TIVE")).isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("status");
  }

  @Test
  void eqCodeThrowDoesNotCorruptBuilderState() {
    // Fragments appended before the failing eqCode call must not appear in any
    // subsequent build() when the exception is caught – the checkpoint rollback
    // must leave sb exactly as it was before eqCode() was entered.
    FusionQueryBuilder builder = new FusionQueryBuilder();
    builder.eq("Name", "test");
    try {
      builder.eqCode("status", "Status", "bad!value");
    } catch (IllegalArgumentException _) {
      // Expected — the test verifies post-throw builder state, not the exception
      // itself.
    }
    // Builder should reflect only the eq() call; no partial eqCode fragment.
    assertThat(builder.build()).isEqualTo("Name='test'");
  }

  @Test
  void eqCodeThrowsForValueContainingSingleQuote() {
    var builder = new FusionQueryBuilder();
    assertThatThrownBy(() -> builder.eqCode("status", "Status", "A'B")).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void eqCodeIgnoresNull() {
    assertThat(new FusionQueryBuilder().eqCode("status", "Status", null).build()).isNull();
  }

  @Test
  void eqCodeIgnoresBlank() {
    assertThat(new FusionQueryBuilder().eqCode("status", "Status", "  ").build()).isNull();
  }

  // -------------------------------------------------------------------------
  // literal()
  // -------------------------------------------------------------------------

  @Test
  void literalAppendsConditionVerbatim() {
    String result = new FusionQueryBuilder().literal("Status='A'").build();
    assertThat(result).isEqualTo("Status='A'");
  }

  @Test
  void literalIgnoresNull() {
    assertThat(new FusionQueryBuilder().literal(null).build()).isNull();
  }

  @Test
  void literalIgnoresBlank() {
    assertThat(new FusionQueryBuilder().literal("").build()).isNull();
  }

  // -------------------------------------------------------------------------
  // AND joining
  // -------------------------------------------------------------------------

  @Test
  void multipleConditionsAreJoinedWithAnd() {
    String result = new FusionQueryBuilder().like("Name", "fusion").eq("ProjectNumber", "P-001").build();
    assertThat(result).isEqualTo("Name LIKE '%fusion%' AND ProjectNumber='P-001'");
  }

  @Test
  void rawAndLikeAreJoinedWithAnd() {
    String result = new FusionQueryBuilder().raw("OrganizationId=123").like("Name", "test").build();
    assertThat(result).isEqualTo("OrganizationId=123 AND Name LIKE '%test%'");
  }

  @Test
  void allMethodsChainedProduceCorrectQuery() {
    String result = new FusionQueryBuilder().raw("OrganizationId=42").like("Name", "fusion").eq("ProjectNumber", "P-1")
        .eqCode("status", "Status", "ACTIVE").literal("BurdenScheduleFlag='Y'").build();
    assertThat(result).isEqualTo(
        "OrganizationId=42 AND Name LIKE '%fusion%' AND ProjectNumber='P-1' AND Status='ACTIVE' AND BurdenScheduleFlag='Y'");
  }

  @Test
  void nullConditionsDoNotAddSpuriousAndJoins() {
    String result = new FusionQueryBuilder().like("Name", "test").eq("Code", null) // skipped
        .eqCode("st", "Status", null) // skipped
        .literal(null) // skipped
        .build();
    assertThat(result).isEqualTo("Name LIKE '%test%'");
  }
}
