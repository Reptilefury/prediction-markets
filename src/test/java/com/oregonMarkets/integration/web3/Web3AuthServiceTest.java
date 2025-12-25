package com.oregonmarkets.integration.web3;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class Web3AuthServiceTest {

  private Web3AuthService service;

  @BeforeEach
  void setUp() {
    service = new Web3AuthService();
  }

  @Test
  void verifySignature_ValidSignature() {
    String walletAddress = "0x742d35Cc6634C0532925a3b8D400E4C0532925a3";
    String message =
        "Sign this message to authenticate with Oregon Markets\n\nWallet: " + walletAddress;
    String signature = "0x" + "a".repeat(130);

    StepVerifier.create(service.verifySignature(walletAddress, message, signature))
        .expectNext(true)
        .verifyComplete();
  }

  @Test
  void verifySignature_InvalidWalletAddress() {
    String invalidAddress = "invalid-address";
    String message = "Sign this message to authenticate with Oregon Markets";
    String signature = "0x" + "a".repeat(130);

    StepVerifier.create(service.verifySignature(invalidAddress, message, signature))
        .expectNext(false)
        .verifyComplete();
  }

  @Test
  void verifySignature_InvalidMessage() {
    String walletAddress = "0x742d35Cc6634C0532925a3b8D400E4C0532925a3";
    String invalidMessage = "Invalid message";
    String signature = "0x" + "a".repeat(130);

    StepVerifier.create(service.verifySignature(walletAddress, invalidMessage, signature))
        .expectNext(false)
        .verifyComplete();
  }

  @Test
  void verifySignature_InvalidSignature() {
    String walletAddress = "0x742d35Cc6634C0532925a3b8D400E4C0532925a3";
    String message = "Sign this message to authenticate with Oregon Markets";
    String invalidSignature = "invalid";

    StepVerifier.create(service.verifySignature(walletAddress, message, invalidSignature))
        .expectNext(false)
        .verifyComplete();
  }

  @Test
  void generateAuthMessage() {
    String walletAddress = "0x742d35Cc6634C0532925a3b8D400E4C0532925a3";

    String message = service.generateAuthMessage(walletAddress);

    assertTrue(message.contains("Sign this message to authenticate with Oregon Markets"));
    assertTrue(message.contains(walletAddress));
    assertTrue(message.contains("Timestamp:"));
  }
}
