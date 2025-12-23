package com.oregonMarkets.integration.crypto;

import com.oregonMarkets.common.exception.ExternalServiceException;
import com.oregonMarkets.common.response.ResponseCode;
import com.oregonMarkets.integration.crypto.dto.CryptoServiceApiResponse;
import com.oregonMarkets.integration.crypto.dto.SmartAccountResponse;
import com.oregonMarkets.integration.crypto.dto.WalletCreateResponseData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.function.Function;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CryptoServiceClientTest {

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private CryptoServiceClient cryptoServiceClient;

    private static final String CRYPTO_SERVICE_URL = "http://localhost:8080";
    private static final String TEST_WALLET_ADDRESS = "0x1234567890abcdef1234567890abcdef12345678";
    private static final String TEST_DID_TOKEN = "test-did-token";

    @BeforeEach
    void setUp() {
        cryptoServiceClient = new CryptoServiceClient(webClientBuilder);
        ReflectionTestUtils.setField(cryptoServiceClient, "cryptoServiceBaseUrl", CRYPTO_SERVICE_URL);

        // Setup default mocking chain
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.defaultHeader(anyString(), anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void createSmartAccount_Success_ShouldReturnWalletData() {
        // Given
        SmartAccountResponse smartAccountResponse = new SmartAccountResponse();
        smartAccountResponse.setSmartAccountAddress("0xabcd...5678");
        smartAccountResponse.setDeployed(true);
        smartAccountResponse.setChainId(80001);

        WalletCreateResponseData responseData = new WalletCreateResponseData();
        responseData.setSmartAccount(smartAccountResponse);

        CryptoServiceApiResponse<WalletCreateResponseData> apiResponse = new CryptoServiceApiResponse<>();
        apiResponse.setData(responseData);

        when(responseSpec.onStatus(any(Predicate.class), any(Function.class))).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class))).thenReturn(Mono.just(apiResponse));

        // When & Then
        StepVerifier.create(cryptoServiceClient.createSmartAccount(TEST_WALLET_ADDRESS, TEST_DID_TOKEN))
                .assertNext(result -> {
                    assertThat(result).isNotNull();
                    assertThat(result.getSmartAccount()).isNotNull();
                    assertThat(result.getSmartAccount().getSmartAccountAddress()).isEqualTo("0xabcd...5678");
                    assertThat(result.getSmartAccount().getDeployed()).isTrue();
                    assertThat(result.getSmartAccount().getChainId()).isEqualTo(80001);
                })
                .verifyComplete();

        // Verify builder was called correctly
        verify(webClientBuilder).baseUrl(CRYPTO_SERVICE_URL);
        verify(webClientBuilder).defaultHeader(eq(HttpHeaders.CONTENT_TYPE), anyString());
        verify(webClientBuilder).build();

        // Verify request was constructed correctly
        verify(webClient).post();
        verify(requestBodyUriSpec).uri("/wallets/create");
        verify(requestBodySpec).header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_DID_TOKEN);
        verify(requestBodySpec).bodyValue(any());
    }

    @Test
    void createSmartAccount_ClientError_ShouldThrowExternalServiceException() {
        // Given
        when(responseSpec.onStatus(any(Predicate.class), any(Function.class))).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.error(new org.springframework.web.reactive.function.client.WebClientResponseException(
                        400, "Bad Request", null, null, null
                )));

        // When & Then
        StepVerifier.create(cryptoServiceClient.createSmartAccount(TEST_WALLET_ADDRESS, TEST_DID_TOKEN))
                .expectErrorMatches(throwable ->
                        throwable instanceof ExternalServiceException &&
                                throwable.getMessage().contains("Failed to create smart account")
                )
                .verify();
    }

    @Test
    void createSmartAccount_ServerError_ShouldThrowExternalServiceException() {
        // Given
        when(responseSpec.onStatus(any(Predicate.class), any(Function.class))).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.error(new org.springframework.web.reactive.function.client.WebClientResponseException(
                        500, "Internal Server Error", null, null, null
                )));

        // When & Then
        StepVerifier.create(cryptoServiceClient.createSmartAccount(TEST_WALLET_ADDRESS, TEST_DID_TOKEN))
                .expectErrorMatches(throwable ->
                        throwable instanceof ExternalServiceException &&
                                throwable.getMessage().contains("Failed to create smart account")
                )
                .verify();
    }

    @Test
    void createSmartAccount_Timeout_ShouldThrowExternalServiceException() {
        // Given
        when(responseSpec.onStatus(any(Predicate.class), any(Function.class))).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.error(new java.util.concurrent.TimeoutException("Request timeout after 30s")));

        // When & Then
        StepVerifier.create(cryptoServiceClient.createSmartAccount(TEST_WALLET_ADDRESS, TEST_DID_TOKEN))
                .expectErrorMatches(throwable ->
                        throwable instanceof ExternalServiceException &&
                                throwable.getMessage().contains("Failed to create smart account")
                )
                .verify();
    }

    @Test
    void createSmartAccount_NetworkError_ShouldRetryAndMap() {
        // Given
        RuntimeException networkError = new RuntimeException("Network error");

        when(responseSpec.onStatus(any(Predicate.class), any(Function.class))).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.error(networkError));

        // When & Then
        StepVerifier.create(cryptoServiceClient.createSmartAccount(TEST_WALLET_ADDRESS, TEST_DID_TOKEN))
                .expectErrorMatches(throwable ->
                        throwable instanceof ExternalServiceException &&
                                ((ExternalServiceException) throwable).getResponseCode() == ResponseCode.EXTERNAL_SERVICE_ERROR &&
                                throwable.getMessage().contains("Failed to create smart account")
                )
                .verify();
    }

    @Test
    void createSmartAccount_ExternalServiceException_ShouldNotRetry() {
        // Given
        ExternalServiceException serviceException = new ExternalServiceException(
                ResponseCode.EXTERNAL_SERVICE_ERROR,
                "Crypto Service",
                "Service error"
        );

        when(responseSpec.onStatus(any(Predicate.class), any(Function.class))).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.error(serviceException));

        // When & Then
        StepVerifier.create(cryptoServiceClient.createSmartAccount(TEST_WALLET_ADDRESS, TEST_DID_TOKEN))
                .expectError(ExternalServiceException.class)
                .verify();
    }

    @Test
    void createSmartAccount_ShouldSetCorrectHeaders() {
        // Given
        SmartAccountResponse smartAccountResponse = new SmartAccountResponse();
        smartAccountResponse.setSmartAccountAddress("0xtest");

        WalletCreateResponseData responseData = new WalletCreateResponseData();
        responseData.setSmartAccount(smartAccountResponse);

        CryptoServiceApiResponse<WalletCreateResponseData> apiResponse = new CryptoServiceApiResponse<>();
        apiResponse.setData(responseData);

        when(responseSpec.onStatus(any(Predicate.class), any(Function.class))).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class))).thenReturn(Mono.just(apiResponse));

        // When
        cryptoServiceClient.createSmartAccount(TEST_WALLET_ADDRESS, TEST_DID_TOKEN).block();

        // Then
        ArgumentCaptor<String> headerNameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> headerValueCaptor = ArgumentCaptor.forClass(String.class);

        verify(requestBodySpec).header(headerNameCaptor.capture(), headerValueCaptor.capture());

        assertThat(headerNameCaptor.getValue()).isEqualTo(HttpHeaders.AUTHORIZATION);
        assertThat(headerValueCaptor.getValue()).isEqualTo("Bearer " + TEST_DID_TOKEN);
    }

    @Test
    void createSmartAccount_ShouldUseCorrectEndpoint() {
        // Given
        SmartAccountResponse smartAccountResponse = new SmartAccountResponse();
        WalletCreateResponseData responseData = new WalletCreateResponseData();
        responseData.setSmartAccount(smartAccountResponse);

        CryptoServiceApiResponse<WalletCreateResponseData> apiResponse = new CryptoServiceApiResponse<>();
        apiResponse.setData(responseData);

        when(responseSpec.onStatus(any(Predicate.class), any(Function.class))).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class))).thenReturn(Mono.just(apiResponse));

        // When
        cryptoServiceClient.createSmartAccount(TEST_WALLET_ADDRESS, TEST_DID_TOKEN).block();

        // Then
        verify(webClientBuilder).baseUrl(CRYPTO_SERVICE_URL);
        verify(requestBodyUriSpec).uri("/wallets/create");
    }
}
