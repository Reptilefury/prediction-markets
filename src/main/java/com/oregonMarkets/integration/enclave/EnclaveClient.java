package com.oregonMarkets.integration.enclave;

import com.oregonMarkets.common.exception.EnclaveApiException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class EnclaveClient {

  private final WebClient webClient;

  @Value("${app.enclave.api-key:}")
  private String apiKey;

  @Value("${app.base-url:http://localhost:8080}")
  private String baseUrl;

  public EnclaveClient(@Qualifier("enclaveWebClient") WebClient webClient) {
    this.webClient = webClient;
  }

  public Mono<EnclaveUDAResponse> createUDA(
      String userId, String email, String walletAddress, String destinationTokenAddress) {
    log.info(
        "[ENCLAVE UDA] Starting UDA creation flow for user: {}, email: {}, wallet: {}",
        userId,
        email,
        walletAddress);

    if (userId == null || userId.isEmpty()) {
      log.error("[ENCLAVE UDA] Validation failed: User ID is required");
      return Mono.error(new EnclaveApiException("User ID is required for UDA creation"));
    }
    if (email == null || email.isEmpty()) {
      log.error("[ENCLAVE UDA] Validation failed: Email is required");
      return Mono.error(new EnclaveApiException("Email is required for UDA creation"));
    }
    if (walletAddress == null || walletAddress.isEmpty()) {
      log.error("[ENCLAVE UDA] Validation failed: Wallet address is required");
      return Mono.error(new EnclaveApiException("Wallet address is required for UDA creation"));
    }
    if (destinationTokenAddress == null || destinationTokenAddress.isEmpty()) {
      log.error("[ENCLAVE UDA] Validation failed: Destination token address is required");
      return Mono.error(
          new EnclaveApiException("Destination token address is required for UDA creation"));
    }

    // Using Base (chainId: 8453) as per Enclave API documentation
    int destinationChainId = 8453; // Base chain

    Map<String, Object> requestBody =
        Map.of(
            "userId", userId,
            "destinationChainId", destinationChainId,
            "destinationAddress", walletAddress,
            "destinationTokenAddress", destinationTokenAddress);

    log.info("[ENCLAVE UDA] Request body prepared:");
    log.info("[ENCLAVE UDA]   - userId: {}", userId);
    log.info("[ENCLAVE UDA]   - destinationChainId: {}", destinationChainId);
    log.info("[ENCLAVE UDA]   - destinationAddress: {}", walletAddress);
    log.info("[ENCLAVE UDA]   - destinationTokenAddress: {}", destinationTokenAddress);

    if (apiKey == null || apiKey.isEmpty()) {
      log.error(
          "[ENCLAVE UDA] CRITICAL ERROR: Enclave API key is not configured. Please set app.enclave.api-key in application configuration");
      return Mono.error(new EnclaveApiException("Enclave API key is not configured"));
    }

    log.info("[ENCLAVE UDA] Sending POST request to /unified-deposit-address/create endpoint");

    return webClient
        .post()
        .uri("/unified-deposit-address/create")
        .header("Authorization", apiKey)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(requestBody)
        .retrieve()
        .bodyToMono(String.class)
        .doOnNext(
            body -> {
              log.info("[ENCLAVE UDA] Raw Response Body: {}", body);
            })
        .flatMap(
            body -> {
              try {
                com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
                // Enclave API returns wrapped response: {"success": true, "data": {...}}
                com.fasterxml.jackson.databind.JsonNode rootNode = mapper.readTree(body);
                com.fasterxml.jackson.databind.JsonNode dataNode = rootNode.get("data");

                if (dataNode == null) {
                  throw new EnclaveApiException("Missing 'data' field in Enclave API response");
                }

                EnclaveUDAResponse udaResponse =
                    mapper.treeToValue(dataNode, EnclaveUDAResponse.class);
                log.info("[ENCLAVE UDA] ✓ Successfully created Enclave UDA for user: {}", userId);
                log.info(
                    "[ENCLAVE UDA] Response details - udaAddress: {}, destinationAddress: {}, status: {}, userId: {}",
                    udaResponse.getUdaAddress(),
                    udaResponse.getDestinationAddress(),
                    udaResponse.getStatus(),
                    udaResponse.getUserId());
                if (udaResponse.getDepositAddresses() != null) {
                  log.debug("[ENCLAVE UDA] Deposit addresses configured");
                }
                return reactor.core.publisher.Mono.just(udaResponse);
              } catch (Exception e) {
                log.error("[ENCLAVE UDA] Failed to parse response: {}", e.getMessage());
                return reactor.core.publisher.Mono.error(e);
              }
            })
        .doOnError(
            error -> {
              log.error(
                  "[ENCLAVE UDA] ✗ Failed to create UDA for user: {} - Error: {}",
                  userId,
                  error.getMessage());
              if (error
                  instanceof
                  org.springframework.web.reactive.function.client.WebClientResponseException) {
                org.springframework.web.reactive.function.client.WebClientResponseException ex =
                    (org.springframework.web.reactive.function.client.WebClientResponseException)
                        error;
                log.error("[ENCLAVE UDA] HTTP Status: {}", ex.getStatusCode());
                log.error("[ENCLAVE UDA] Response Body: {}", ex.getResponseBodyAsString());
              }
              log.debug("[ENCLAVE UDA] Full error details:", error);
            })
        .onErrorMap(error -> new EnclaveApiException("Failed to create UDA", error));
  }

  @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
  public static class EnclaveUDAResponse {
    private String userId;
    private String enclaveId;
    private String orgId;
    private int destinationChainId;
    private String destinationAddress;
    private String destinationTokenAddress;
    private Map<String, Object> depositAddresses;
    private String status;
    private long createdAt;
    private long updatedAt;

    // Additional fields from actual API response
    @com.fasterxml.jackson.annotation.JsonProperty("bridgeSettings")
    private Map<String, Object> bridgeSettings;

    @com.fasterxml.jackson.annotation.JsonProperty("totalDeposits")
    private Map<String, Object> totalDeposits;

    @com.fasterxml.jackson.annotation.JsonProperty("_id")
    private String id;

    @com.fasterxml.jackson.annotation.JsonProperty("__v")
    private int version;

    // Legacy field mapping for backwards compatibility
    // UDA Address is actually the Enclave ID (_id), NOT the destinationAddress
    // destinationAddress is the proxy wallet address passed in the request
    public String getUdaAddress() {
      return id; // Return the Enclave-assigned ID as the UDA address
    }

    public void setUdaAddress(String udaAddress) {
      this.id = udaAddress;
    }

    public String getTag() {
      return orgId;
    }

    public void setTag(String tag) {
      this.orgId = tag;
    }

    public Map<String, Object> getChainAddresses() {
      return depositAddresses;
    }

    public void setChainAddresses(Map<String, Object> chainAddresses) {
      this.depositAddresses = chainAddresses;
    }

    // Getters and setters for new fields
    public String getUserId() {
      return userId;
    }

    public void setUserId(String userId) {
      this.userId = userId;
    }

    public String getEnclaveId() {
      return enclaveId;
    }

    public void setEnclaveId(String enclaveId) {
      this.enclaveId = enclaveId;
    }

    public String getOrgId() {
      return orgId;
    }

    public void setOrgId(String orgId) {
      this.orgId = orgId;
    }

    public int getDestinationChainId() {
      return destinationChainId;
    }

    public void setDestinationChainId(int destinationChainId) {
      this.destinationChainId = destinationChainId;
    }

    public String getDestinationAddress() {
      return destinationAddress;
    }

    public void setDestinationAddress(String destinationAddress) {
      this.destinationAddress = destinationAddress;
    }

    public String getDestinationTokenAddress() {
      return destinationTokenAddress;
    }

    public void setDestinationTokenAddress(String destinationTokenAddress) {
      this.destinationTokenAddress = destinationTokenAddress;
    }

    public Map<String, Object> getDepositAddresses() {
      return depositAddresses;
    }

    public void setDepositAddresses(Map<String, Object> depositAddresses) {
      this.depositAddresses = depositAddresses;
    }

    public String getStatus() {
      return status;
    }

    public void setStatus(String status) {
      this.status = status;
    }

    public long getCreatedAt() {
      return createdAt;
    }

    public void setCreatedAt(long createdAt) {
      this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
      return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
      this.updatedAt = updatedAt;
    }

    public Map<String, Object> getBridgeSettings() {
      return bridgeSettings;
    }

    public void setBridgeSettings(Map<String, Object> bridgeSettings) {
      this.bridgeSettings = bridgeSettings;
    }

    public Map<String, Object> getTotalDeposits() {
      return totalDeposits;
    }

    public void setTotalDeposits(Map<String, Object> totalDeposits) {
      this.totalDeposits = totalDeposits;
    }

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public int getVersion() {
      return version;
    }

    public void setVersion(int version) {
      this.version = version;
    }
  }
}
