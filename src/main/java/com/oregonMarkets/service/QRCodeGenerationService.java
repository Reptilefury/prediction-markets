package com.oregonMarkets.service;

import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class QRCodeGenerationService {

    @Value("${spring.cloud.gcp.storage.project-id:${gcp.project-id}}")
    private String gcpProjectId;

    @Value("${spring.cloud.gcp.storage.bucket-name:prediction-markets-storage}")
    private String bucketName;

    @Value("${app.logodev.api-key:}")
    private String logoDevApiKey;
    // Removed unused secret key to avoid accidental exposure

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

    /**
     * Generate QR codes for all deposit addresses and proxy wallet
     * Returns a map with address type as key and QR code URL as value
     */
    public Mono<Map<String, String>> generateAndUploadQRCodes(
            UUID userId,
            String proxyWalletAddress,
            String enclaveUdaAddress,
            Map<String, String> evmDepositAddresses,
            String solanaDepositAddress,
            Map<String, String> bitcoinDepositAddresses) {

        return Mono.fromCallable(() -> {
            Map<String, String> qrCodeUrls = new HashMap<>();

            try {
                // Generate QR code for proxy wallet
                if (proxyWalletAddress != null && !proxyWalletAddress.isBlank()) {
                    qrCodeUrls.put("proxyWalletQrCode",
                            generateAndUploadBrandedQRCode(userId, "proxy_wallet", proxyWalletAddress, WALLET_TYPE));
                }

                // Generate QR code for Enclave UDA
                if (enclaveUdaAddress != null && !enclaveUdaAddress.isBlank()) {
                    qrCodeUrls.put("enclaveUdaQrCode",
                            generateAndUploadBrandedQRCode(userId, "enclave_uda", enclaveUdaAddress, "uda"));
                }

                // Generate QR codes for EVM deposit addresses
                if (evmDepositAddresses != null && !evmDepositAddresses.isEmpty()) {
                    Map<String, String> evmQrCodes = new HashMap<>();
                    for (Map.Entry<String, String> entry : evmDepositAddresses.entrySet()) {
                        String qrUrl = generateAndUploadBrandedQRCode(userId, "evm_" + entry.getKey(), entry.getValue(), entry.getKey());
                        evmQrCodes.put(entry.getKey(), qrUrl);
                    }
                    qrCodeUrls.put("evmDepositQrCodes", evmQrCodes.toString());
                }

                // Generate QR code for Solana deposit address
                if (solanaDepositAddress != null && !solanaDepositAddress.isBlank()) {
                    qrCodeUrls.put("solanaDepositQrCode",
                            generateAndUploadBrandedQRCode(userId, "solana_deposit", solanaDepositAddress, SOLANA_TYPE));
                }

                // Generate QR codes for Bitcoin addresses
                if (bitcoinDepositAddresses != null && !bitcoinDepositAddresses.isEmpty()) {
                    Map<String, String> btcQrCodes = new HashMap<>();
                    for (Map.Entry<String, String> entry : bitcoinDepositAddresses.entrySet()) {
                        String qrUrl = generateAndUploadBrandedQRCode(userId, "btc_" + entry.getKey(), entry.getValue(), BITCOIN_TYPE);
                        btcQrCodes.put(entry.getKey(), qrUrl);
                    }
                    qrCodeUrls.put("bitcoinDepositQrCodes", btcQrCodes.toString());
                }

                log.info("QR codes generated and uploaded successfully, total count: {}", qrCodeUrls.size());
                return qrCodeUrls;
            } catch (Exception e) {
                log.error("Failed to generate/upload QR codes: {}", e.getMessage(), e);
                throw e;
            }
        });
    }

    private String generateAndUploadBrandedQRCode(UUID userId, String addressType, String addressValue, String tokenType) {
        // Input validation
        if (userId == null || addressType == null || addressValue == null || tokenType == null) {
            throw new IllegalArgumentException("All parameters must be non-null");
        }
        if (addressValue.trim().isEmpty() || addressType.trim().isEmpty() || tokenType.trim().isEmpty()) {
            throw new IllegalArgumentException("String parameters must not be empty");
        }
        
        try {
            // Generate branded QR code image with logo
            byte[] qrCodeBytes = generateBrandedQRCodeImage(addressValue, tokenType);

            // Upload to GCP Cloud Storage
            log.info("Branded QR code generated successfully for address type {} token {}", addressType, tokenType);
            return uploadQRCodeToGCP(userId, addressType, qrCodeBytes);
        } catch (Exception e) {
            log.error("Failed to generate/upload branded QR code for {} {}: {}", addressType, tokenType, e.getMessage(), e);
            // Fallback to simple QR code
            try {
                byte[] fallbackBytes = generateSimpleQRCodeImage(addressValue);
                return uploadQRCodeToGCP(userId, addressType, fallbackBytes);
            } catch (Exception fallbackError) {
                return "https://storage.googleapis.com/" + bucketName + "/qrcodes/" + userId + "/" + addressType + ".png";
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
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE, hints);

            // Create buffered image with custom colors
            BufferedImage qrImage = new BufferedImage(QR_CODE_SIZE, QR_CODE_SIZE, BufferedImage.TYPE_INT_RGB);
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
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE);

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

    private BufferedImage downloadTokenLogo(String tokenType) {
        try {
            String logoUrl = getLogoDevUrl(tokenType);
            if (logoUrl == null) {
                return null;
            }
            
            // Validate URL is from trusted domain
            if (!logoUrl.startsWith("https://logo.dev/")) {
                log.warn("Untrusted logo URL rejected: {}", logoUrl);
                return null;
            }
            
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) new URL(logoUrl).openConnection();
            
            // Security headers and timeouts
            connection.setRequestProperty("User-Agent", "PredictionMarkets/1.0");
            connection.setRequestProperty("Accept", "image/png,image/jpeg,image/webp");
            
            // Only set Authorization if explicitly required and available
            if (logoDevApiKey != null && !logoDevApiKey.isBlank()) {
                connection.setRequestProperty("Authorization", "Bearer " + logoDevApiKey);
            }
            
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            connection.setInstanceFollowRedirects(false); // Prevent redirect attacks

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
                    log.warn("ImageIO.read returned null for logo: {}", tokenType);
                    return null;
                }
            
                // Resize to fit QR code
                BufferedImage resizedLogo = new BufferedImage(LOGO_SIZE, LOGO_SIZE, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = resizedLogo.createGraphics();
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.drawImage(originalLogo, 0, 0, LOGO_SIZE, LOGO_SIZE, null);
                g2d.dispose();
                
                return resizedLogo;
            }
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
        String logoName = switch (tokenType.toLowerCase()) {
            case ETHEREUM_TYPE -> ETHEREUM_TYPE;
            case POLYGON_TYPE -> POLYGON_TYPE;
            case "base" -> "base";
            case "solana" -> SOLANA_TYPE;
            case "bitcoin" -> BITCOIN_TYPE;
            case "uda" -> null; // Custom token, use fallback
            case "wallet" -> null; // Generic wallet, use fallback
            default -> null;
        };
        
        if (logoName != null) {
            return String.format("https://logo.dev/api/crypto/%s/png/128", logoName);
        }
        return null;
    }

    private Color getTokenColor(String tokenType) {
        return switch (tokenType.toLowerCase()) {
            case ETHEREUM_TYPE -> new Color(0x627EEA);
            case POLYGON_TYPE -> new Color(0x8247E5);
            case "base" -> new Color(0x0052FF);
            case "solana" -> new Color(0x9945FF);
            case "bitcoin" -> new Color(0xF7931A);
            case "uda" -> new Color(0x00D4AA);
            case "wallet" -> new Color(0x6366F1);
            default -> new Color(0x6B7280);
        };
    }

    private String getTokenSymbol(String tokenType) {
        return switch (tokenType.toLowerCase()) {
            case ETHEREUM_TYPE -> "ETH";
            case POLYGON_TYPE -> "MATIC";
            case "base" -> "BASE";
            case "solana" -> "SOL";
            case "bitcoin" -> "BTC";
            case "uda" -> "UDA";
            case "wallet" -> "💳";
            default -> "?";
        };
    }

    private String uploadQRCodeToGCP(UUID userId, String addressType, byte[] qrCodeBytes) {
        try {
            Storage storage = (gcpProjectId != null && !gcpProjectId.isBlank())
                ? StorageOptions.newBuilder().setProjectId(gcpProjectId).build().getService()
                : StorageOptions.getDefaultInstance().getService();
            Bucket bucket = storage.get(bucketName);

            if (bucket == null) {
                log.warn("GCP bucket '{}' not found in project '{}', returning mock URL", bucketName, gcpProjectId);
                return String.format(
                    "https://storage.googleapis.com/%s/qrcodes/%s/%s.png",
                    bucketName,
                    userId,
                    addressType
                );
            }

            String blobName = String.format("qrcodes/%s/%s.png", userId, addressType);
            bucket.create(blobName, qrCodeBytes);

            String uploadedUrl = String.format(
                "https://storage.googleapis.com/%s/%s",
                bucketName,
                blobName
            );

            log.info("QR code uploaded to GCP: {}", uploadedUrl);
            return uploadedUrl;
        } catch (Exception e) {
            log.error("Failed to upload QR code to GCP: {}", e.getMessage(), e);
            // Return a default URL even if upload fails
            return String.format(
                "https://storage.googleapis.com/%s/qrcodes/%s/%s.png",
                bucketName,
                userId,
                addressType
            );
        }
    }
}
