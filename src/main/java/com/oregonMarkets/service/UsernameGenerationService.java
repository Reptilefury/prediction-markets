package com.oregonMarkets.service;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.stereotype.Service;

/**
 * Service for generating unique, thematic usernames and display names
 *
 * <p>Uses Datafaker library with UUID-based uniqueness
 *
 * <p>Examples: SilentTrader-a4b2, QuantumWizard-9f3e, OracleViper-2c1d
 */
@Service
@Slf4j
public class UsernameGenerationService {

  private final Faker faker = new Faker();

  /**
   * Generate a unique username with prediction markets theme
   *
   * @param userId User's UUID for uniqueness
   * @return Generated username (e.g., "DarkOracle-4a2b")
   */
  public String generateUsername(UUID userId) {
    // Use Datafaker's esports/gaming names for cool factor
    String baseName = faker.esports().player(); // e.g., "ShadowReaper", "DragonKnight"

    // Add unique suffix from user ID (first 4 chars)
    String uniqueSuffix = userId.toString().replace("-", "").substring(0, 4);

    String username = baseName + "-" + uniqueSuffix;
    log.debug("Generated username: {} for userId: {}", username, userId);
    return username;
  }

  /**
   * Generate a display name from username (removes unique suffix)
   *
   * @param username The generated username
   * @return Display name without suffix (e.g., "Dark Oracle")
   */
  public String generateDisplayName(String username) {
    // Remove the unique suffix (everything after last hyphen)
    int lastHyphen = username.lastIndexOf('-');
    if (lastHyphen > 0) {
      return username.substring(0, lastHyphen);
    }
    return username;
  }

  /**
   * Generate a display name directly from user ID
   *
   * @param userId User's UUID
   * @return Display name (e.g., "Shadow Reaper")
   */
  public String generateDisplayNameFromUserId(UUID userId) {
    String username = generateUsername(userId);
    return generateDisplayName(username);
  }

  /**
   * Generate an alternative prediction markets-themed username
   *
   * @param userId User's UUID for uniqueness
   * @return Themed username (e.g., "QuantumTrader-4a2b", "OracleSeer-9f3e")
   */
  public String generateMarketThemedUsername(UUID userId) {
    // Combine prediction market terms with cool adjectives
    String adjective =
        faker
            .options()
            .option(
                "Quantum", "Oracle", "Cyber", "Neon", "Dark", "Silent", "Epic", "Alpha", "Prime",
                "Nova");
    String noun =
        faker
            .options()
            .option(
                "Trader",
                "Seer",
                "Prophet",
                "Analyst",
                "Wizard",
                "Sage",
                "Strategist",
                "Visionary",
                "Maven",
                "Guru");

    String uniqueSuffix = userId.toString().replace("-", "").substring(0, 4);

    String username = adjective + noun + "-" + uniqueSuffix;
    log.debug("Generated market-themed username: {} for userId: {}", username, userId);
    return username;
  }

  /**
   * Apply username and display name to a User entity This is a helper to avoid code duplication
   * across registration services
   *
   * @param user User entity to update
   */
  public void applyUsernameAndDisplayName(com.oregonMarkets.domain.user.model.User user) {
    String username = generateMarketThemedUsername(user.getId());
    String displayName = generateDisplayName(username);
    user.setUsername(username);
    user.setDisplayName(displayName);
  }
}
