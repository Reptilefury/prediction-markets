package com.oregonMarkets.domain.user.service;

import com.oregonMarkets.domain.user.dto.request.UserRegistrationRequest;
import com.oregonMarkets.domain.user.dto.response.UserRegistrationResponse;
import com.oregonMarkets.integration.magic.MagicDIDValidator;
import jakarta.validation.Valid;
import reactor.core.publisher.Mono;

/** Interface for user registration service Handles user onboarding via Magic.link authentication */
public interface IUserRegistrationService {

  /**
   * Register a new user with Magic.link authentication
   *
   * @param request User registration request containing email and profile data
   * @param magicUser Validated Magic user information from DID token
   * @param didToken The DID token for authentication
   * @return UserRegistrationResponse with user details and wallet addresses
   */
  Mono<UserRegistrationResponse> registerUser(
      @Valid UserRegistrationRequest request,
      MagicDIDValidator.MagicUserInfo magicUser,
      String didToken);

  /**
   * Get user profile by Magic user information
   *
   * @param magicUser Validated Magic user information from DID token
   * @return UserRegistrationResponse with current user profile data
   */
  Mono<UserRegistrationResponse> getUserProfile(MagicDIDValidator.MagicUserInfo magicUser);
}
