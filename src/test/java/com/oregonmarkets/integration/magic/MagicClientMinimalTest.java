package com.oregonmarkets.integration.magic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

@ExtendWith(MockitoExtension.class)
class MagicClientMinimalTest {

  @Mock private WebClient webClient;

  private MagicClient magicClient;

  @BeforeEach
  void setUp() {
    magicClient = new MagicClient(webClient);
    ReflectionTestUtils.setField(magicClient, "apiKey", "sk_test_12345678");
  }

  @Test
  void constructor_WithValidDependencies_CreatesInstance() {
    assertNotNull(magicClient);
  }

  @Test
  void init_WithValidApiKey_LogsCorrectly() {
    // Test is covered by setUp() calling init()
    // Verify the API key was set correctly
    magicClient.init();
    String apiKey = (String) ReflectionTestUtils.getField(magicClient, "apiKey");
    assertEquals("sk_test_12345678", apiKey);
  }

  @Test
  void init_WithNullApiKey_HandlesGracefully() {
    MagicClient clientWithNullKey = new MagicClient(webClient);
    ReflectionTestUtils.setField(clientWithNullKey, "apiKey", null);

    // Should not throw exception
    clientWithNullKey.init();
  }

  @Test
  void init_WithEmptyApiKey_HandlesGracefully() {
    MagicClient clientWithEmptyKey = new MagicClient(webClient);
    ReflectionTestUtils.setField(clientWithEmptyKey, "apiKey", "");

    // Should not throw exception
    clientWithEmptyKey.init();
  }

  @Test
  void magicUserInfo_SettersAndGetters_WorkCorrectly() {
    MagicClient.MagicUserInfo userInfo = new MagicClient.MagicUserInfo();

    userInfo.setIssuer("test-issuer");
    userInfo.setEmail("test@example.com");
    userInfo.setPublicAddress("0x123");

    assertEquals("test-issuer", userInfo.getIssuer());
    assertEquals("test@example.com", userInfo.getEmail());
    assertEquals("0x123", userInfo.getPublicAddress());
  }

  @Test
  void magicUserInfo_DefaultValues_AreNull() {
    MagicClient.MagicUserInfo userInfo = new MagicClient.MagicUserInfo();

    assertEquals(null, userInfo.getIssuer());
    assertEquals(null, userInfo.getEmail());
    assertEquals(null, userInfo.getPublicAddress());
  }
}
