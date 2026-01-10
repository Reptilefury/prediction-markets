package com.oregonmarkets.security;

import com.oregonmarkets.common.exception.MagicAuthException;
import com.oregonmarkets.config.SecurityProperties;
import com.oregonmarkets.dto.ErrorType;
import com.oregonmarkets.integration.magic.MagicDIDValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MagicTokenFilterTest {

    @Mock
    private MagicDIDValidator magicValidator;
    
    @Mock
    private ErrorResponseBuilder errorResponseBuilder;
    
    @Mock
    private SecurityProperties securityProperties;
    
    @Mock
    private ServerWebExchange exchange;
    
    @Mock
    private WebFilterChain chain;
    
    @Mock
    private ServerHttpRequest request;
    
    @Mock
    private ServerHttpResponse response;
    
    @Mock
    private HttpHeaders headers;
    
    private MagicTokenFilter filter;

    @BeforeEach
    void setUp() {
        when(securityProperties.getPublicPaths()).thenReturn(List.of("/api/icons/**"));
        filter = new MagicTokenFilter(magicValidator, errorResponseBuilder, securityProperties);
        when(exchange.getRequest()).thenReturn(request);
        when(request.getMethod()).thenReturn(HttpMethod.GET);
    }

    @Test
    void shouldSkipValidationForNonApiPaths() {
        when(request.getPath()).thenReturn(mock(org.springframework.http.server.RequestPath.class));
        when(request.getPath().value()).thenReturn("/health");
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
        verifyNoInteractions(magicValidator);
    }

    @Test
    void shouldValidateApiPaths() {
        when(request.getPath()).thenReturn(mock(org.springframework.http.server.RequestPath.class));
        when(request.getPath().value()).thenReturn("/api/test");
        when(request.getHeaders()).thenReturn(headers);
        when(headers.getFirst("Authorization")).thenReturn("Bearer valid-token");
        when(chain.filter(exchange)).thenReturn(Mono.empty());
        
        MagicDIDValidator.MagicUserInfo userInfo = mock(MagicDIDValidator.MagicUserInfo.class);
        when(userInfo.getEmail()).thenReturn("test@example.com");
        when(magicValidator.validateDIDToken("valid-token")).thenReturn(Mono.just(userInfo));

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(magicValidator).validateDIDToken("valid-token");
        verify(chain).filter(exchange);
    }

    @Test
    void shouldSkipAdminPaths() {
        when(request.getPath()).thenReturn(mock(org.springframework.http.server.RequestPath.class));
        when(request.getPath().value()).thenReturn("/api/admin/permissions");
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
        verifyNoInteractions(magicValidator);
    }
}
