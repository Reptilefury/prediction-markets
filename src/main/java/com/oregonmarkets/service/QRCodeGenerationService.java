package com.oregonmarkets.service;

import com.google.cloud.storage.Storage;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class QRCodeGenerationService {

  private final Storage storage;  // Injected singleton Storage bean

  @Value("${spring.cloud.gcp.storage.project-id:${gcp.project-id}}")
  private String gcpProjectId;

  @Value("${spring.cloud.gcp.storage.bucket-name:prediction-markets-storage}")
  private String bucketName;

  @Value("${app.logodev.publishable-key:}")
  private String logoDevPublishableKey;

  // Removed unused secret key to avoid accidental exposure

  // Logo cache for instant access (populated on startup)
  private final java.util.concurrent.ConcurrentHashMap<String, BufferedImage> logoCache =
      new java.util.concurrent.ConcurrentHashMap<>();

  private static final int QR_CODE_SIZE = 512;
  private static final int LOGO_SIZE = 80;
  private static final Color QR_COLOR = new Color(0x1a1a1a);
  private static final Color BACKGROUND_COLOR = Color.WHITE;

  // String constants to avoid duplication
  private static final String WALLET_TYPE = "wallet";
  private static final String SOLANA_TYPE = "solana";
  private static final String BITCOIN_TYPE = "bitcoin";
  private static final String ETHEREUM_TYPE = "ethereum";
  private static final String POLYGON_TYPE = "polygon";
  private static final String UDA_TYPE = "uda";
  private static final String BASE_TYPE = "base";

  /**
   * Pre-cache all cryptocurrency logos on application startup This eliminates logo download latency
   * during QR code generation
   */
  @javax.annotation.PostConstruct
  public void initializeLogoCache() {
    log.info("[LOGO-CACHE] Initializing cryptocurrency logo cache on startup...");
    long startTime = System.currentTimeMillis();

    // List of all supported token types that have logos
    java.util.List<String> tokenTypes =
        java.util.Arrays.asList(ETHEREUM_TYPE, POLYGON_TYPE, BASE_TYPE, SOLANA_TYPE, BITCOIN_TYPE);

    int successCount = 0;
    int failureCount = 0;

    for (String tokenType : tokenTypes) {
      try {
        BufferedImage logo = downloadAndResizeTokenLogo(tokenType);
        if (logo != null) {
          logoCache.put(tokenType, logo);
          successCount++;
          log.info("[LOGO-CACHE] ✓ Cached logo for: {}", tokenType);
        } else {
          failureCount++;
          log.warn("[LOGO-CACHE] ✗ Failed to cache logo for: {}", tokenType);
        }
      } catch (Exception e) {
        failureCount++;
        log.warn("[LOGO-CACHE] ✗ Error caching logo for {}: {}", tokenType, e.getMessage());
      }
    }

    long duration = System.currentTimeMillis() - startTime;
    log.info(
        "[LOGO-CACHE] Logo cache initialized: {} succeeded, {} failed in {}ms",
        successCount,
        failureCount,
        duration);
  }

  /**
   * Generate QR codes for all deposit addresses and proxy wallet Returns a map with address type as
   * key and QR code URL as value
   */
  public Mono<Map<String, String>> generateAndUploadQRCodes(
      UUID userId,
      String proxyWalletAddress,
      String enclaveUdaAddress,
      Map<String, String> evmDepositAddresses,
      String solanaDepositAddress,
      Map<String, String> bitcoinDepositAddresses) {

    log.info("[QR-PARALLEL] Starting parallel QR code generation for user: {}", userId);
    long startTime = System.currentTimeMillis();

    // Create a list to hold all QR code generation tasks
    java.util.List<Mono<java.util.Map.Entry<String, String>>> qrCodeTasks =
        new java.util.ArrayList<>();

    // Task 1: Proxy wallet QR code
    if (proxyWalletAddress != null && !proxyWalletAddress.isBlank()) {
      qrCodeTasks.add(
          Mono.fromCallable(
                  () ->
                      java.util.Map.entry(
                          "proxyWalletQrCode",
                          generateAndUploadBrandedQRCode(
                              userId, "proxy_wallet", proxyWalletAddress, WALLET_TYPE)))
              .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic()));
    }

    // Task 2: Enclave UDA QR code
    if (enclaveUdaAddress != null && !enclaveUdaAddress.isBlank()) {
      qrCodeTasks.add(
          Mono.fromCallable(
                  () ->
                      java.util.Map.entry(
                          "enclaveUdaQrCode",
                          generateAndUploadBrandedQRCode(
                              userId, "enclave_uda", enclaveUdaAddress, UDA_TYPE)))
              .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic()));
    }

    // Task 3-N: EVM deposit addresses (parallel generation)
    if (evmDepositAddresses != null && !evmDepositAddresses.isEmpty()) {
      java.util.List<Mono<java.util.Map.Entry<String, String>>> evmTasks =
          new java.util.ArrayList<>();
      for (Map.Entry<String, String> entry : evmDepositAddresses.entrySet()) {
        String network = entry.getKey();
        String address = entry.getValue();
        evmTasks.add(
            Mono.fromCallable(
                    () ->
                        java.util.Map.entry(
                            "evm_" + network,
                            generateAndUploadBrandedQRCode(
                                userId, "evm_" + network, address, network)))
                .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic()));
      }

      // Combine all EVM QR codes into a single entry
      qrCodeTasks.add(
          reactor.core.publisher.Flux.merge(evmTasks)
              .collectMap(java.util.Map.Entry::getKey, java.util.Map.Entry::getValue)
              .map(
                  evmMap -> {
                    // Convert to format expected by frontend
                    Map<String, String> cleanedMap = new HashMap<>();
                    evmMap.forEach((key, value) -> cleanedMap.put(key.replace("evm_", ""), value));
                    return java.util.Map.entry("evmDepositQrCodes", cleanedMap.toString());
                  }));
    }

    // Task N+1: Solana deposit address
    if (solanaDepositAddress != null && !solanaDepositAddress.isBlank()) {
      qrCodeTasks.add(
          Mono.fromCallable(
                  () ->
                      java.util.Map.entry(
                          "solanaDepositQrCode",
                          generateAndUploadBrandedQRCode(
                              userId, "solana_deposit", solanaDepositAddress, SOLANA_TYPE)))
              .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic()));
    }

    // Task N+2-M: Bitcoin addresses (parallel generation)
    if (bitcoinDepositAddresses != null && !bitcoinDepositAddresses.isEmpty()) {
      java.util.List<Mono<java.util.Map.Entry<String, String>>> btcTasks =
          new java.util.ArrayList<>();
      for (Map.Entry<String, String> entry : bitcoinDepositAddresses.entrySet()) {
        String format = entry.getKey();
        String address = entry.getValue();
        btcTasks.add(
            Mono.fromCallable(
                    () ->
                        java.util.Map.entry(
                            "btc_" + format,
                            generateAndUploadBrandedQRCode(
                                userId, "btc_" + format, address, BITCOIN_TYPE)))
                .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic()));
      }

      // Combine all Bitcoin QR codes into a single entry
      qrCodeTasks.add(
          reactor.core.publisher.Flux.merge(btcTasks)
              .collectMap(java.util.Map.Entry::getKey, java.util.Map.Entry::getValue)
              .map(
                  btcMap -> {
                    // Convert to format expected by frontend
                    Map<String, String> cleanedMap = new HashMap<>();
                    btcMap.forEach((key, value) -> cleanedMap.put(key.replace("btc_", ""), value));
                    return java.util.Map.entry("bitcoinDepositQrCodes", cleanedMap.toString());
                  }));
    }

    // Execute all QR code generation tasks in parallel
    return reactor.core.publisher.Flux.merge(qrCodeTasks)
        .collectMap(java.util.Map.Entry::getKey, java.util.Map.Entry::getValue)
        .doOnSuccess(
            qrCodeUrls -> {
              long duration = System.currentTimeMillis() - startTime;
              log.info(
                  "[QR-PARALLEL] ✓ All {} QR codes generated in {}ms (parallel execution)",
                  qrCodeUrls.size(),
                  duration);
            })
        .doOnError(
            e ->
                log.error(
                    "[QR-PARALLEL] Failed to generate QR codes in parallel: {}", e.getMessage(), e))
        .onErrorResume(
            e -> {
              log.warn(
                  "[QR-PARALLEL] Falling back to empty result due to error: {}", e.getMessage());
              return Mono.just(new HashMap<>());
            });
  }

  private String generateAndUploadBrandedQRCode(
      UUID userId, String addressType, String addressValue, String tokenType) {
    // Input validation
    if (userId == null || addressType == null || addressValue == null || tokenType == null) {
      throw new IllegalArgumentException("All parameters must be non-null");
    }
    if (addressValue.trim().isEmpty()
        || addressType.trim().isEmpty()
        || tokenType.trim().isEmpty()) {
      throw new IllegalArgumentException("String parameters must not be empty");
    }

    try {
      // Generate branded QR code image with logo
      byte[] qrCodeBytes = generateBrandedQRCodeImage(addressValue, tokenType);

      // Upload to GCP Cloud Storage
      log.info(
          "Branded QR code generated successfully for address type {} token {}",
          addressType,
          tokenType);
      return uploadQRCodeToGCP(userId, addressType, qrCodeBytes);
    } catch (Exception e) {
      log.error(
          "Failed to generate/upload branded QR code for {} {}: {}",
          addressType,
          tokenType,
          e.getMessage(),
          e);
      // Fallback to simple QR code
      try {
        byte[] fallbackBytes = generateSimpleQRCodeImage(addressValue);
        return uploadQRCodeToGCP(userId, addressType, fallbackBytes);
      } catch (Exception fallbackError) {
        return "https://storage.googleapis.com/"
            + bucketName
            + "/qrcodes/"
            + userId
            + "/"
            + addressType
            + ".png";
      }
    }
  }

  private byte[] generateBrandedQRCodeImage(String text, String tokenType) throws RuntimeException {
    try {
      // Configure QR code with higher error correction for logo overlay
      Map<EncodeHintType, Object> hints = new java.util.EnumMap<>(EncodeHintType.class);
      hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
      hints.put(EncodeHintType.MARGIN, 2);

      QRCodeWriter qrCodeWriter = new QRCodeWriter();
      BitMatrix bitMatrix =
          qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE, hints);

      // Create buffered image with custom colors
      BufferedImage qrImage =
          new BufferedImage(QR_CODE_SIZE, QR_CODE_SIZE, BufferedImage.TYPE_INT_RGB);
      Graphics2D graphics = qrImage.createGraphics();
      graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      // Fill background
      graphics.setColor(BACKGROUND_COLOR);
      graphics.fillRect(0, 0, QR_CODE_SIZE, QR_CODE_SIZE);

      // Draw QR code
      graphics.setColor(QR_COLOR);
      for (int x = 0; x < bitMatrix.getWidth(); x++) {
        for (int y = 0; y < bitMatrix.getHeight(); y++) {
          if (bitMatrix.get(x, y)) {
            graphics.fillRect(x, y, 1, 1);
          }
        }
      }

      // Add token logo in center
      addTokenLogo(graphics, tokenType);

      graphics.dispose();

      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      ImageIO.write(qrImage, "PNG", outputStream);
      return outputStream.toByteArray();
    } catch (Exception e) {
      log.error("Error generating branded QR code image: {}", e.getMessage(), e);
      throw new RuntimeException("Failed to generate branded QR code image", e);
    }
  }

  private byte[] generateSimpleQRCodeImage(String text) throws RuntimeException {
    try {
      QRCodeWriter qrCodeWriter = new QRCodeWriter();
      BitMatrix bitMatrix =
          qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE);

      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
      return outputStream.toByteArray();
    } catch (Exception e) {
      log.error("Error generating simple QR code image: {}", e.getMessage(), e);
      throw new RuntimeException("Failed to generate simple QR code image", e);
    }
  }

  private void addTokenLogo(Graphics2D graphics, String tokenType) {
    try {
      BufferedImage logo = downloadTokenLogo(tokenType);
      if (logo != null) {
        int logoX = (QR_CODE_SIZE - LOGO_SIZE) / 2;
        int logoY = (QR_CODE_SIZE - LOGO_SIZE) / 2;

        // Add white background circle for logo
        graphics.setColor(Color.WHITE);
        graphics.fillOval(logoX - 8, logoY - 8, LOGO_SIZE + 16, LOGO_SIZE + 16);

        // Draw logo
        graphics.drawImage(logo, logoX, logoY, LOGO_SIZE, LOGO_SIZE, null);
      }
    } catch (Exception e) {
      log.warn("Failed to add token logo for {}: {}", tokenType, e.getMessage());
      // Fallback to simple colored circle
      addFallbackLogo(graphics, tokenType);
    }
  }

  /**
   * Get token logo from cache, or download if not cached Runtime method - checks cache first for
   * instant access
   */
  private BufferedImage downloadTokenLogo(String tokenType) {
    // Check cache first for instant access
    BufferedImage cachedLogo = logoCache.get(tokenType);
    if (cachedLogo != null) {
      log.debug("[LOGO-CACHE] ✓ Cache hit for: {}", tokenType);
      return cachedLogo;
    }

    log.debug("[LOGO-CACHE] Cache miss for: {}, downloading...", tokenType);

    // Cache miss - download and cache for future use
    BufferedImage downloadedLogo = downloadAndResizeTokenLogo(tokenType);
    if (downloadedLogo != null) {
      logoCache.put(tokenType, downloadedLogo);
      log.info("[LOGO-CACHE] ✓ Downloaded and cached logo for: {}", tokenType);
    }

    return downloadedLogo;
  }

  /**
   * Download and resize token logo from logo.dev Used for both cache initialization and runtime
   * downloads
   */
  private BufferedImage downloadAndResizeTokenLogo(String tokenType) {
    try {
      String logoUrl = getLogoDevUrl(tokenType);
      if (logoUrl == null) {
        return null;
      }

      // Validate URL is from trusted domain (logo.dev or img.logo.dev)
      if (!logoUrl.startsWith("https://logo.dev/")
          && !logoUrl.startsWith("https://img.logo.dev/")) {
        log.warn("Untrusted logo URL rejected: {}", logoUrl);
        return null;
      }

      java.net.HttpURLConnection connection =
          (java.net.HttpURLConnection) new URL(logoUrl).openConnection();

      // Security headers and timeouts
      connection.setRequestProperty("User-Agent", "PredictionMarkets/1.0");
      connection.setRequestProperty("Accept", "image/png,image/jpeg,image/webp,image/*");

      connection.setRequestMethod("GET");
      connection.setConnectTimeout(5000);
      connection.setReadTimeout(10000);
      connection.setInstanceFollowRedirects(true); // Follow redirects from CDN

      // Check response
      int responseCode = connection.getResponseCode();
      if (responseCode != 200) {
        log.warn("Logo download failed with status {}: {}", responseCode, logoUrl);
        return null;
      }

      // Validate content type
      String contentType = connection.getContentType();
      if (contentType == null || !contentType.startsWith("image/")) {
        log.warn("Invalid content type for logo: {}", contentType);
        return null;
      }

      // Check content length (max 5MB)
      int contentLength = connection.getContentLength();
      if (contentLength > 5 * 1024 * 1024) {
        log.warn("Logo file too large: {} bytes", contentLength);
        return null;
      }

      BufferedImage originalLogo;
      try (java.io.InputStream is = connection.getInputStream()) {
        originalLogo = ImageIO.read(is);
      }

      if (originalLogo == null) {
        log.warn("ImageIO.read returned null for logo: {}", tokenType);
        return null;
      }

      // Resize to fit QR code
      BufferedImage resizedLogo =
          new BufferedImage(LOGO_SIZE, LOGO_SIZE, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g2d = resizedLogo.createGraphics();
      g2d.setRenderingHint(
          RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2d.drawImage(originalLogo, 0, 0, LOGO_SIZE, LOGO_SIZE, null);
      g2d.dispose();

      return resizedLogo;
    } catch (Exception e) {
      log.warn("Failed to download logo for {}: {}", tokenType, e.getMessage());
    }
    return null;
  }

  private void addFallbackLogo(Graphics2D graphics, String tokenType) {
    try {
      int logoX = (QR_CODE_SIZE - LOGO_SIZE) / 2;
      int logoY = (QR_CODE_SIZE - LOGO_SIZE) / 2;

      // Create simple colored circle with token symbol
      Color tokenColor = getTokenColor(tokenType);
      graphics.setColor(tokenColor);
      graphics.fillOval(logoX, logoY, LOGO_SIZE, LOGO_SIZE);

      // Add token symbol
      graphics.setColor(Color.WHITE);
      graphics.setFont(new Font("Arial", Font.BOLD, 20));
      FontMetrics fm = graphics.getFontMetrics();
      String symbol = getTokenSymbol(tokenType);
      int textX = logoX + (LOGO_SIZE - fm.stringWidth(symbol)) / 2;
      int textY = logoY + (LOGO_SIZE + fm.getAscent()) / 2;
      graphics.drawString(symbol, textX, textY);
    } catch (Exception e) {
      log.warn("Failed to create fallback logo for {}: {}", tokenType, e.getMessage());
    }
  }

  private String getLogoDevUrl(String tokenType) {
    // Map token type to crypto symbol for logo.dev API
    String cryptoSymbol =
        switch (tokenType.toLowerCase()) {
          case ETHEREUM_TYPE -> "eth";
          case POLYGON_TYPE -> "matic";
          case "base" -> "base";
          case SOLANA_TYPE -> "sol";
          case BITCOIN_TYPE -> "btc";
          case "uda" -> null; // Custom token, use fallback
          case WALLET_TYPE -> null; // Generic wallet, use fallback
          default -> null;
        };

    if (cryptoSymbol != null) {
      // Use logo.dev crypto API format: https://img.logo.dev/crypto/{symbol}?token=PUBLISHABLE_KEY
      log.debug(
          "[LOGO-DEV] logoDevPublishableKey = '{}' (null={}, blank={})",
          logoDevPublishableKey,
          logoDevPublishableKey == null,
          logoDevPublishableKey != null && logoDevPublishableKey.isBlank());

      if (logoDevPublishableKey != null && !logoDevPublishableKey.isBlank()) {
        String url =
            String.format(
                "https://img.logo.dev/crypto/%s?token=%s", cryptoSymbol, logoDevPublishableKey);
        log.debug("[LOGO-DEV] Generated URL with token: {}", url);
        return url;
      } else {
        log.warn(
            "[LOGO-DEV] No publishable key configured, using URL without token (will get 401)");
        return String.format("https://img.logo.dev/crypto/%s", cryptoSymbol);
      }
    }
    return null;
  }

  private Color getTokenColor(String tokenType) {
    return switch (tokenType.toLowerCase()) {
      case ETHEREUM_TYPE -> new Color(0x627EEA);
      case POLYGON_TYPE -> new Color(0x8247E5);
      case "base" -> new Color(0x0052FF);
      case SOLANA_TYPE -> new Color(0x9945FF);
      case BITCOIN_TYPE -> new Color(0xF7931A);
      case "uda" -> new Color(0x00D4AA);
      case WALLET_TYPE -> new Color(0x6366F1);
      default -> new Color(0x6B7280);
    };
  }

  private String getTokenSymbol(String tokenType) {
    return switch (tokenType.toLowerCase()) {
      case ETHEREUM_TYPE -> "ETH";
      case POLYGON_TYPE -> "MATIC";
      case BASE_TYPE -> "BASE";
      case SOLANA_TYPE -> "SOL";
      case BITCOIN_TYPE -> "BTC";
      case UDA_TYPE -> "UDA";
      case WALLET_TYPE -> "💳";
      default -> "?";
    };
  }

  private String uploadQRCodeToGCP(UUID userId, String addressType, byte[] qrCodeBytes) {
    try {
      String blobName = String.format("qrcodes/%s/%s.png", userId, addressType);

      // Upload directly without checking bucket existence (bucket must exist)
      // BlobInfo for the new object
      com.google.cloud.storage.BlobInfo blobInfo =
          com.google.cloud.storage.BlobInfo.newBuilder(bucketName, blobName)
              .setContentType("image/png")
              .build();

      // Upload the QR code
      storage.create(blobInfo, qrCodeBytes);

      String uploadedUrl =
          String.format("https://storage.googleapis.com/%s/%s", bucketName, blobName);

      log.info("[QR-UPLOAD] ✓ QR code uploaded successfully: {}", uploadedUrl);
      return uploadedUrl;
    } catch (Exception e) {
      log.error("[QR-UPLOAD] ✗ Failed to upload QR code to GCP: {}", e.getMessage(), e);
      // Return a default URL even if upload fails
      return String.format(
          "https://storage.googleapis.com/%s/qrcodes/%s/%s.png", bucketName, userId, addressType);
    }
  }
}
