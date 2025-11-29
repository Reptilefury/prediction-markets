package com.oregonMarkets.service;

import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
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

    @Value("${gcp.project-id:}")
    private String gcpProjectId;

    @Value("${gcp.storage.bucket:oregon-markets-qrcodes}")
    private String bucketName;

    private static final int QR_CODE_SIZE = 512;

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
                            generateAndUploadQRCode(userId, "proxy_wallet", proxyWalletAddress));
                }

                // Generate QR code for Enclave UDA
                if (enclaveUdaAddress != null && !enclaveUdaAddress.isBlank()) {
                    qrCodeUrls.put("enclaveUdaQrCode",
                            generateAndUploadQRCode(userId, "enclave_uda", enclaveUdaAddress));
                }

                // Generate QR codes for EVM deposit addresses
                if (evmDepositAddresses != null && !evmDepositAddresses.isEmpty()) {
                    Map<String, String> evmQrCodes = new HashMap<>();
                    for (Map.Entry<String, String> entry : evmDepositAddresses.entrySet()) {
                        String qrUrl = generateAndUploadQRCode(userId, "evm_" + entry.getKey(), entry.getValue());
                        evmQrCodes.put(entry.getKey(), qrUrl);
                    }
                    qrCodeUrls.put("evmDepositQrCodes", evmQrCodes.toString());
                }

                // Generate QR code for Solana deposit address
                if (solanaDepositAddress != null && !solanaDepositAddress.isBlank()) {
                    qrCodeUrls.put("solanaDepositQrCode",
                            generateAndUploadQRCode(userId, "solana_deposit", solanaDepositAddress));
                }

                // Generate QR codes for Bitcoin addresses
                if (bitcoinDepositAddresses != null && !bitcoinDepositAddresses.isEmpty()) {
                    Map<String, String> btcQrCodes = new HashMap<>();
                    for (Map.Entry<String, String> entry : bitcoinDepositAddresses.entrySet()) {
                        String qrUrl = generateAndUploadQRCode(userId, "btc_" + entry.getKey(), entry.getValue());
                        btcQrCodes.put(entry.getKey(), qrUrl);
                    }
                    qrCodeUrls.put("bitcoinDepositQrCodes", btcQrCodes.toString());
                }

                log.info("QR codes generated and uploaded for user: {}, total count: {}", userId, qrCodeUrls.size());
                return qrCodeUrls;
            } catch (Exception e) {
                log.error("Failed to generate/upload QR codes for user {}: {}", userId, e.getMessage(), e);
                throw e;
            }
        });
    }

    private String generateAndUploadQRCode(UUID userId, String addressType, String addressValue) {
        try {
            // Generate QR code image
            byte[] qrCodeBytes = generateQRCodeImage(addressValue);

            // Upload to GCP Cloud Storage
            String uploadUrl = uploadQRCodeToGCP(userId, addressType, qrCodeBytes);

            log.info("QR code generated for user {} address type {}", userId, addressType);
            return uploadUrl;
        } catch (Exception e) {
            log.error("Failed to generate/upload QR code for {} {}: {}", userId, addressType, e.getMessage(), e);
            // Return a placeholder URL on failure
            return "https://storage.googleapis.com/" + bucketName + "/qrcodes/" + userId + "/" + addressType + ".png";
        }
    }

    private byte[] generateQRCodeImage(String text) throws Exception {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("Error generating QR code image: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate QR code image", e);
        }
    }

    private String uploadQRCodeToGCP(UUID userId, String addressType, byte[] qrCodeBytes) {
        try {
            Storage storage = StorageOptions.getDefaultInstance().getService();
            Bucket bucket = storage.get(bucketName);

            if (bucket == null) {
                log.warn("GCP bucket '{}' not found, returning mock URL", bucketName);
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
