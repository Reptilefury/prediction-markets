package com.oregonmarkets.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.oregonmarkets.domain.user.model.User;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UsernameGenerationServiceTest {

  private UsernameGenerationService service;

  @BeforeEach
  void setUp() {
    service = new UsernameGenerationService();
  }

  @Test
  void generateUsername_ShouldGenerateUsernameWithUniqueSuffix() {
    UUID userId = UUID.randomUUID();

    String username = service.generateUsername(userId);

    assertThat(username).isNotNull();
    assertThat(username).contains("-");
    String[] parts = username.split("-");
    assertThat(parts).hasSize(2);
    assertThat(parts[1]).hasSize(4); // 4-character unique suffix
  }

  @Test
  void generateUsername_ShouldGenerateDifferentUsernamesForDifferentUsers() {
    UUID userId1 = UUID.randomUUID();
    UUID userId2 = UUID.randomUUID();

    String username1 = service.generateUsername(userId1);
    String username2 = service.generateUsername(userId2);

    assertThat(username1).isNotEqualTo(username2);
  }

  @Test
  void generateDisplayName_ShouldRemoveUniqueSuffix() {
    String username = "ShadowReaper-4a2b";

    String displayName = service.generateDisplayName(username);

    assertThat(displayName).isEqualTo("ShadowReaper");
  }

  @Test
  void generateDisplayName_ShouldReturnOriginalIfNoHyphen() {
    String username = "NoHyphenUsername";

    String displayName = service.generateDisplayName(username);

    assertThat(displayName).isEqualTo("NoHyphenUsername");
  }

  @Test
  void generateDisplayNameFromUserId_ShouldGenerateDisplayNameWithoutSuffix() {
    UUID userId = UUID.randomUUID();

    String displayName = service.generateDisplayNameFromUserId(userId);

    assertThat(displayName).isNotNull();
    assertThat(displayName).doesNotContain("-");
  }

  @Test
  void generateMarketThemedUsername_ShouldContainMarketTheme() {
    UUID userId = UUID.randomUUID();

    String username = service.generateMarketThemedUsername(userId);

    assertThat(username).isNotNull();
    assertThat(username).contains("-");

    String[] parts = username.split("-");
    assertThat(parts).hasSize(2);
    assertThat(parts[1]).hasSize(4); // 4-character unique suffix

    // Base name should be one of the market-themed combinations
    String baseName = parts[0];
    assertThat(baseName)
        .matches(
            "(Quantum|Oracle|Cyber|Neon|Dark|Silent|Epic|Alpha|Prime|Nova)"
                + "(Trader|Seer|Prophet|Analyst|Wizard|Sage|Strategist|Visionary|Maven|Guru)");
  }

  @Test
  void generateMarketThemedUsername_ShouldBeConsistentForSameUserId() {
    UUID userId = UUID.randomUUID();

    String username1 = service.generateMarketThemedUsername(userId);
    String username2 = service.generateMarketThemedUsername(userId);

    // The suffix should be the same (based on userId)
    String[] parts1 = username1.split("-");
    String[] parts2 = username2.split("-");
    assertThat(parts1[1]).isEqualTo(parts2[1]);
  }

  @Test
  void applyUsernameAndDisplayName_ShouldSetBothFields() {
    UUID userId = UUID.randomUUID();
    User user = User.builder().id(userId).build();

    service.applyUsernameAndDisplayName(user);

    assertThat(user.getUsername()).isNotNull();
    assertThat(user.getDisplayName()).isNotNull();
    assertThat(user.getUsername()).contains("-");
    assertThat(user.getDisplayName()).doesNotContain("-");
  }

  @Test
  void applyUsernameAndDisplayName_ShouldGenerateMarketThemedUsername() {
    UUID userId = UUID.randomUUID();
    User user = User.builder().id(userId).build();

    service.applyUsernameAndDisplayName(user);

    assertThat(user.getUsername())
        .matches(
            "(Quantum|Oracle|Cyber|Neon|Dark|Silent|Epic|Alpha|Prime|Nova)"
                + "(Trader|Seer|Prophet|Analyst|Wizard|Sage|Strategist|Visionary|Maven|Guru)-[a-f0-9]{4}");
  }

  @Test
  void applyUsernameAndDisplayName_DisplayNameShouldBeUsernameWithoutSuffix() {
    UUID userId = UUID.randomUUID();
    User user = User.builder().id(userId).build();

    service.applyUsernameAndDisplayName(user);

    String expectedDisplayName =
        user.getUsername().substring(0, user.getUsername().lastIndexOf('-'));
    assertThat(user.getDisplayName()).isEqualTo(expectedDisplayName);
  }

  @Test
  void generateUsername_ShouldHandleUUIDWithoutHyphens() {
    UUID userId = UUID.fromString("12345678-1234-1234-1234-123456789abc");

    String username = service.generateUsername(userId);

    String[] parts = username.split("-");
    assertThat(parts[1]).isEqualTo("1234"); // First 4 chars of UUID without hyphens
  }

  @Test
  void generateMarketThemedUsername_ShouldHandleUUIDWithoutHyphens() {
    UUID userId = UUID.fromString("12345678-1234-1234-1234-123456789abc");

    String username = service.generateMarketThemedUsername(userId);

    String[] parts = username.split("-");
    assertThat(parts[1]).isEqualTo("1234"); // First 4 chars of UUID without hyphens
  }
}
