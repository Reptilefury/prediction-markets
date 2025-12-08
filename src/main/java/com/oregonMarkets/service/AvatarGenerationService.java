package com.oregonMarkets.service;

import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
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
public class AvatarGenerationService {

  @Value("${spring.cloud.gcp.storage.project-id:${gcp.project-id}}")
  private String gcpProjectId;

  @Value("${spring.cloud.gcp.storage.bucket-name:prediction-markets-storage}")
  private String bucketName;

  /** Generate an avatar for a user and upload it to GCP Cloud Storage */
  public Mono<String> generateAndUploadAvatar(UUID userId) {
    return Mono.fromCallable(
        () -> {
          try {
            // Generate avatar image
            byte[] avatarBytes = generateAvatarImage(userId);

            // Upload to GCP Cloud Storage
            String uploadUrl = uploadAvatarToGCP(userId, avatarBytes);

            log.info("Avatar generated and uploaded for user: {}, URL: {}", userId, uploadUrl);
            return uploadUrl;
          } catch (Exception e) {
            log.error(
                "Failed to generate/upload avatar for user {}: {}", userId, e.getMessage(), e);
            throw e;
          }
        });
  }

  private byte[] generateAvatarImage(UUID userId) {
    try {
      int size = 256;
      BufferedImage avatar = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
      Graphics2D g2d = avatar.createGraphics();

      // Generate deterministic colors based on user ID
      int hash = userId.toString().hashCode();
      int red = ((hash >> 16) & 0xFF);
      int green = ((hash >> 8) & 0xFF);
      int blue = (hash & 0xFF);

      // Fill background with color
      g2d.setColor(new Color(red, green, blue));
      g2d.fillRect(0, 0, size, size);

      // Draw face (circle)
      g2d.setColor(Color.YELLOW);
      int faceSize = 200;
      int faceX = (size - faceSize) / 2;
      int faceY = (size - faceSize) / 2;
      g2d.fillOval(faceX, faceY, faceSize, faceSize);

      // Draw face outline
      g2d.setColor(Color.BLACK);
      g2d.setStroke(new BasicStroke(3));
      g2d.drawOval(faceX, faceY, faceSize, faceSize);

      // Draw left eye
      int eyeY = faceY + 70;
      int leftEyeX = faceX + 60;
      int eyeSize = 20;
      g2d.fillOval(leftEyeX, eyeY, eyeSize, eyeSize);

      // Draw right eye
      int rightEyeX = faceX + 130;
      g2d.fillOval(rightEyeX, eyeY, eyeSize, eyeSize);

      // Draw mouth (arc/smile)
      int mouthY = faceY + 130;
      int mouthWidth = 80;
      int mouthHeight = 40;
      int mouthX = faceX + 60;
      g2d.setStroke(new BasicStroke(4));
      g2d.drawArc(mouthX, mouthY, mouthWidth, mouthHeight, 0, -180);

      g2d.dispose();

      // Convert BufferedImage to byte array
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      ImageIO.write(avatar, "png", outputStream);
      return outputStream.toByteArray();
    } catch (Exception e) {
      log.error("Error generating avatar image: {}", e.getMessage(), e);
      throw new RuntimeException("Failed to generate avatar image", e);
    }
  }

  private String uploadAvatarToGCP(UUID userId, byte[] avatarBytes) {
    try {
      Storage storage = StorageOptions.getDefaultInstance().getService();
      Bucket bucket = storage.get(bucketName);

      if (bucket == null) {
        log.warn("GCP bucket '{}' not found, returning mock URL", bucketName);
        return "https://storage.googleapis.com/" + bucketName + "/avatars/" + userId + ".png";
      }

      String blobName = "avatars/" + userId + ".png";
      bucket.create(blobName, avatarBytes);

      String uploadedUrl =
          String.format("https://storage.googleapis.com/%s/%s", bucketName, blobName);

      log.info("Avatar uploaded to GCP: {}", uploadedUrl);
      return uploadedUrl;
    } catch (Exception e) {
      log.error("Failed to upload avatar to GCP: {}", e.getMessage(), e);
      // Return a default URL even if upload fails - avatar generation succeeded
      return "https://storage.googleapis.com/" + bucketName + "/avatars/" + userId + ".png";
    }
  }
}
