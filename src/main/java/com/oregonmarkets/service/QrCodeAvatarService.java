package com.oregonmarkets.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Service for generating QR codes and avatar URLs */
@Service
@Slf4j
public class QrCodeAvatarService {

  private final ObjectMapper objectMapper = new ObjectMapper();

  /** Generate avatar URL using DiceBear API */
  public String generateAvatarUrl(String seed) {
    if (seed == null || seed.isEmpty()) {
      seed = "default";
    }
    return String.format("https://api.dicebear.com/7.x/avataaars/svg?seed=%s", seed);
  }

  /** Generate QR code URL for wallet address using QR Server API */
  public String generateWalletQrCode(String address, String label) {
    if (address == null || address.isEmpty()) {
      return null;
    }
    String data = String.format("%s:%s", label, address);
    return String.format(
        "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=%s",
        java.net.URLEncoder.encode(data, java.nio.charset.StandardCharsets.UTF_8));
  }

  /** Generate QR code URL for UDA address */
  public String generateUdaQrCode(String udaAddress) {
    return generateWalletQrCode(udaAddress, "UDA");
  }

  /** Generate QR code URL for proxy wallet */
  public String generateProxyWalletQrCode(String proxyWalletAddress) {
    return generateWalletQrCode(proxyWalletAddress, "Proxy");
  }

  /** Generate Solana deposit QR code from deposit addresses JSON */
  public String generateSolanaDepositQrCode(String depositAddressesJson) {
    try {
      if (depositAddressesJson != null) {
        JsonNode root = objectMapper.readTree(depositAddressesJson);
        JsonNode solanaNode = root.get("solana_deposit_address");
        if (solanaNode != null && solanaNode.has("address")) {
          String solanaAddress = solanaNode.get("address").asText();
          return generateWalletQrCode(solanaAddress, "Solana");
        }
      }
    } catch (Exception e) {
      log.warn("Failed to parse Solana address from deposit addresses: {}", e.getMessage());
    }
    return null;
  }

  /** Generate Bitcoin deposit QR codes JSON from deposit addresses */
  public String generateBitcoinDepositQrCodes(String depositAddressesJson) {
    try {
      if (depositAddressesJson != null) {
        JsonNode root = objectMapper.readTree(depositAddressesJson);
        JsonNode bitcoinNode = root.get("bitcoin_deposit_address");
        if (bitcoinNode != null) {
          String legacy =
              bitcoinNode.has("legacy_address")
                  ? generateWalletQrCode(bitcoinNode.get("legacy_address").asText(), "BTC-Legacy")
                  : null;
          String segwit =
              bitcoinNode.has("segwit_address")
                  ? generateWalletQrCode(bitcoinNode.get("segwit_address").asText(), "BTC-Segwit")
                  : null;
          String nativeSegwit =
              bitcoinNode.has("native_segwit_address")
                  ? generateWalletQrCode(
                      bitcoinNode.get("native_segwit_address").asText(), "BTC-Native")
                  : null;
          String taproot =
              bitcoinNode.has("taproot_address")
                  ? generateWalletQrCode(bitcoinNode.get("taproot_address").asText(), "BTC-Taproot")
                  : null;

          return String.format(
              "{\"legacy\":\"%s\",\"segwit\":\"%s\",\"native_segwit\":\"%s\",\"taproot\":\"%s\"}",
              legacy, segwit, nativeSegwit, taproot);
        }
      }
    } catch (Exception e) {
      log.warn("Failed to generate Bitcoin QR codes: {}", e.getMessage());
    }
    return null;
  }

  /** Generate deposit QR codes JSON for multiple addresses */
  public String generateDepositQrCodesJson(Object depositAddresses) {
    // For now, return a simple JSON structure
    // In production, you'd parse the depositAddresses and generate QR codes for each
    try {
      if (depositAddresses != null) {
        return String.format(
            "{\"generated_at\":\"%s\",\"qr_codes\":\"available\"}",
            java.time.Instant.now().toString());
      }
    } catch (Exception e) {
      log.warn("Failed to generate deposit QR codes: {}", e.getMessage());
    }
    return null;
  }
}
