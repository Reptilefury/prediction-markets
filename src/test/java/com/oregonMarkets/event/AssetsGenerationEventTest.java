package com.oregonMarkets.event;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AssetsGenerationEventTest {

    @Test
    void builder_CreatesEvent() {
        UUID userId = UUID.randomUUID();
        Map<String, Object> depositAddresses = Map.of("ethereum", "0xeth");
        
        AssetsGenerationEvent event = AssetsGenerationEvent.builder()
                .userId(userId)
                .email("test@example.com")
                .proxyWalletAddress("0xproxy")
                .enclaveUdaAddress("0xenclave")
                .magicWalletAddress("0xmagic")
                .depositAddresses(depositAddresses)
                .timestamp(Instant.now())
                .build();
        
        assertEquals(userId, event.getUserId());
        assertEquals("test@example.com", event.getEmail());
        assertEquals("0xproxy", event.getProxyWalletAddress());
        assertEquals("0xenclave", event.getEnclaveUdaAddress());
        assertEquals("0xmagic", event.getMagicWalletAddress());
        assertEquals(depositAddresses, event.getDepositAddresses());
        assertNotNull(event.getTimestamp());
    }
}
