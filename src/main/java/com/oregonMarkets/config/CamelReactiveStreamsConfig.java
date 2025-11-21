package com.oregonMarkets.config;

/**
 * Note: We intentionally do not expose CamelReactiveStreamsService as a Spring bean.
 * Creating it as a Spring-managed bean can participate in a startup cycle when
 * Camel routes initialize. Instead, services that need it should obtain it
 * directly from the CamelContext using CamelReactiveStreams.get(camelContext).
 */
public final class CamelReactiveStreamsConfig { /* no Spring beans here by design */ }
