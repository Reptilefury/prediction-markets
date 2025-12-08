package com.oregonMarkets.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.blnk")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlnkProperties {
  private String apiUrl;
  private String ledgerId;

  @Override
  public String toString() {
    return "BlnkProperties{apiUrl='" + apiUrl + "', ledgerId='" + ledgerId + "'}";
  }
}
