package com.oregonMarkets.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Small helper component that reads Pub/Sub-related properties and returns
 * trimmed values to avoid issues caused by accidental whitespace in env vars.
 */
@Component
public class GcpPubSubProperties {

    private final String projectId;
    private final String topicUniversalDepositWallet;
    private final String subscriptionUniversalDepositWallet;

    public GcpPubSubProperties(
            @Value("${gcp.project-id:}") String projectId,
            @Value("${gcp.pubsub.topics.universal-deposit-wallet:universal-deposit-wallet}") String topicUniversalDepositWallet,
            @Value("${gcp.pubsub.subscriptions.universal-deposit-wallet:universal-deposit-wallet-subscription}") String subscriptionUniversalDepositWallet) {
        this.projectId = projectId;
        this.topicUniversalDepositWallet = topicUniversalDepositWallet;
        this.subscriptionUniversalDepositWallet = subscriptionUniversalDepositWallet;
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    public String getProjectId() {
        return trim(projectId);
    }

    public String getTopicUniversalDepositWallet() {
        return trim(topicUniversalDepositWallet);
    }

    public String getSubscriptionUniversalDepositWallet() {
        return trim(subscriptionUniversalDepositWallet);
    }
}
