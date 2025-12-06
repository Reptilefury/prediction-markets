package com.oregonMarkets.common.exception;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.oregonMarkets.common.response.ApiResponse;
import com.oregonMarkets.common.response.ResponseCode;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.support.WebExchangeBindException;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

  private GlobalExceptionHandler handler;

  @BeforeEach
  void setUp() {
    handler = new GlobalExceptionHandler();
  }

  @Test
  void handleBusinessException() {
    BusinessException ex = new BusinessException(ResponseCode.VALIDATION_ERROR, "Test message");

    ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(ex);

    assertEquals(ResponseCode.VALIDATION_ERROR.getHttpStatus(), response.getStatusCode());
    assertEquals(ResponseCode.VALIDATION_ERROR.getCode(), response.getBody().getError().getCode());
    assertEquals("Test message", response.getBody().getError().getMessage());
    assertNotNull(response.getBody().getError().getTraceId());
  }

  @Test
  void handleUserAlreadyExists() {
    UserAlreadyExistsException ex = new UserAlreadyExistsException("User exists");

    ResponseEntity<ApiResponse<Void>> response = handler.handleUserAlreadyExists(ex);

    assertEquals(ResponseCode.DUPLICATE_USER.getHttpStatus(), response.getStatusCode());
    assertEquals(ResponseCode.DUPLICATE_USER.getCode(), response.getBody().getError().getCode());
    assertEquals(
        "User with email 'User exists' already exists", response.getBody().getError().getMessage());
  }

  @Test
  void handleMagicAuth() {
    MagicAuthException ex = new MagicAuthException("Magic auth failed");

    ResponseEntity<ApiResponse<Void>> response = handler.handleMagicAuth(ex);

    assertEquals(ResponseCode.MAGIC_AUTH_FAILED.getHttpStatus(), response.getStatusCode());
    assertEquals(ResponseCode.MAGIC_AUTH_FAILED.getCode(), response.getBody().getError().getCode());
  }

  @Test
  void handleEnclaveApi() {
    EnclaveApiException ex = new EnclaveApiException("Enclave error");

    ResponseEntity<ApiResponse<Void>> response = handler.handleEnclaveApi(ex);

    assertEquals(ResponseCode.ENCLAVE_API_ERROR.getHttpStatus(), response.getStatusCode());
    assertEquals(ResponseCode.ENCLAVE_API_ERROR.getCode(), response.getBody().getError().getCode());
  }

  @Test
  void handleBlnkApi() {
    BlnkApiException ex = new BlnkApiException("Blnk error");

    ResponseEntity<ApiResponse<Void>> response = handler.handleBlnkApi(ex);

    assertEquals(ResponseCode.BLNK_API_ERROR.getHttpStatus(), response.getStatusCode());
    assertEquals(ResponseCode.BLNK_API_ERROR.getCode(), response.getBody().getError().getCode());
  }

  @Test
  void handleWeb3Auth() {
    Web3AuthException ex = new Web3AuthException("Web3 error");

    ResponseEntity<ApiResponse<Void>> response = handler.handleWeb3Auth(ex);

    assertEquals(ResponseCode.WEB3_AUTH_FAILED.getHttpStatus(), response.getStatusCode());
    assertEquals(ResponseCode.WEB3_AUTH_FAILED.getCode(), response.getBody().getError().getCode());
  }

  @Test
  void handleKeycloakAuth() {
    KeycloakAuthException ex = new KeycloakAuthException("Keycloak error");

    ResponseEntity<ApiResponse<Void>> response = handler.handleKeycloakAuth(ex);

    assertEquals(ResponseCode.KEYCLOAK_AUTH_FAILED.getHttpStatus(), response.getStatusCode());
    assertEquals(
        ResponseCode.KEYCLOAK_AUTH_FAILED.getCode(), response.getBody().getError().getCode());
  }

  @Test
  void handleBlockchain() {
    BlockchainException ex = new BlockchainException("Blockchain error");

    ResponseEntity<ApiResponse<Void>> response = handler.handleBlockchain(ex);

    assertEquals(ResponseCode.BLOCKCHAIN_ERROR.getHttpStatus(), response.getStatusCode());
    assertEquals(ResponseCode.BLOCKCHAIN_ERROR.getCode(), response.getBody().getError().getCode());
  }

  @Test
  void handleExternalServiceException() {
    ExternalServiceException ex =
        new ExternalServiceException(
            ResponseCode.EXTERNAL_SERVICE_ERROR, "TestService", "Service error");

    ResponseEntity<ApiResponse<Void>> response = handler.handleExternalServiceException(ex);

    assertEquals(ResponseCode.EXTERNAL_SERVICE_ERROR.getHttpStatus(), response.getStatusCode());
    assertEquals(
        ResponseCode.EXTERNAL_SERVICE_ERROR.getCode(), response.getBody().getError().getCode());
  }

  @Test
  void handleValidationException() {
    MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
    BindingResult bindingResult = mock(BindingResult.class);
    FieldError fieldError = new FieldError("object", "field", "error message");

    when(ex.getBindingResult()).thenReturn(bindingResult);
    when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));

    ResponseEntity<ApiResponse<Void>> response = handler.handleValidationException(ex);

    assertEquals(ResponseCode.VALIDATION_ERROR.getHttpStatus(), response.getStatusCode());
  }

  @Test
  void handleWebExchangeValidation() {
    WebExchangeBindException ex = mock(WebExchangeBindException.class);
    BindingResult bindingResult = mock(BindingResult.class);
    FieldError fieldError = new FieldError("object", "field", "error message");

    when(ex.getBindingResult()).thenReturn(bindingResult);
    when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

    ResponseEntity<ApiResponse<Void>> response = handler.handleWebExchangeValidation(ex);

    assertEquals(ResponseCode.VALIDATION_ERROR.getHttpStatus(), response.getStatusCode());
    assertEquals("field: error message", response.getBody().getError().getMessage());
  }

  @Test
  void handleAuthenticationException_BadCredentials() {
    BadCredentialsException ex = new BadCredentialsException("Bad credentials");

    ResponseEntity<ApiResponse<Void>> response = handler.handleAuthenticationException(ex);

    assertEquals(ResponseCode.INVALID_CREDENTIALS.getHttpStatus(), response.getStatusCode());
    assertEquals(
        ResponseCode.INVALID_CREDENTIALS.getCode(), response.getBody().getError().getCode());
  }

  @Test
  void handleAuthenticationException_Generic() {
    AuthenticationException ex = new AuthenticationException("Auth failed") {};

    ResponseEntity<ApiResponse<Void>> response = handler.handleAuthenticationException(ex);

    assertEquals(ResponseCode.UNAUTHORIZED.getHttpStatus(), response.getStatusCode());
    assertEquals(ResponseCode.UNAUTHORIZED.getCode(), response.getBody().getError().getCode());
  }

  @Test
  void handleAccessDeniedException() {
    AccessDeniedException ex = new AccessDeniedException("Access denied");

    ResponseEntity<ApiResponse<Void>> response = handler.handleAccessDeniedException(ex);

    assertEquals(ResponseCode.FORBIDDEN.getHttpStatus(), response.getStatusCode());
    assertEquals(ResponseCode.FORBIDDEN.getCode(), response.getBody().getError().getCode());
  }

  @Test
  void handleDuplicateKeyException_Email() {
    DuplicateKeyException ex = new DuplicateKeyException("Duplicate key error");

    ResponseEntity<ApiResponse<Void>> response = handler.handleDuplicateKeyException(ex);

    assertEquals(ResponseCode.DUPLICATE_USER.getHttpStatus(), response.getStatusCode());
    assertEquals("User with this email already exists", response.getBody().getError().getMessage());
  }

  @Test
  void handleDuplicateKeyException_MagicId() {
    DuplicateKeyException ex = new DuplicateKeyException("users_magic_user_id_key violation");

    ResponseEntity<ApiResponse<Void>> response = handler.handleDuplicateKeyException(ex);

    assertEquals(ResponseCode.DUPLICATE_USER.getHttpStatus(), response.getStatusCode());
    assertEquals(
        "User with this Magic ID already exists", response.getBody().getError().getMessage());
  }

  @Test
  void handleDataAccessException() {
    DataAccessException ex = new DataAccessException("DB error") {};

    ResponseEntity<ApiResponse<Void>> response = handler.handleDataAccessException(ex);

    assertEquals(ResponseCode.DATABASE_ERROR.getHttpStatus(), response.getStatusCode());
    assertEquals(ResponseCode.DATABASE_ERROR.getCode(), response.getBody().getError().getCode());
  }

  @Test
  void handleGeneric() {
    Exception ex = new RuntimeException("Unexpected error");

    ResponseEntity<ApiResponse<Void>> response = handler.handleGeneric(ex);

    assertEquals(ResponseCode.INTERNAL_SERVER_ERROR.getHttpStatus(), response.getStatusCode());
    assertEquals(
        ResponseCode.INTERNAL_SERVER_ERROR.getCode(), response.getBody().getError().getCode());
    assertTrue(response.getBody().getError().getMessage().contains("unexpected error"));
  }
}
