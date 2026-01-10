package com.oregonmarkets.config;

import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "security")
@Data
public class SecurityProperties {

  private List<String> publicPaths = List.of("/api/icons/**");
}
