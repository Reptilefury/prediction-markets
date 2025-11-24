/**
 * Magic SDK wrapper for GraalVM
 * Provides token validation using Magic's official Node SDK
 */

const { Magic } = require('@magic-sdk/admin');

// Initialize Magic admin instance
// API key is passed from Java
let magicInstance = null;

/**
 * Initialize Magic SDK with API key
 * Called once when validator starts up
 */
function initializeMagic(apiKey) {
  try {
    magicInstance = new Magic(apiKey);
    console.log('[Magic] Initialized successfully');
    return { success: true, message: 'Magic SDK initialized' };
  } catch (error) {
    console.error('[Magic] Initialization failed:', error.message);
    return { success: false, error: error.message };
  }
}

/**
 * Validate DID token and extract user metadata
 * Returns user info: email, issuer, publicAddress, phone
 */
async function validateDIDToken(didToken) {
  if (!magicInstance) {
    throw new Error('Magic SDK not initialized. Call initializeMagic first.');
  }

  try {
    // Magic Node SDK validates token internally
    // getMetadataByToken checks if token is valid and extracts claims
    const metadata = await magicInstance.users.getMetadataByToken(didToken);

    return {
      success: true,
      data: {
        email: metadata.email || null,
        publicAddress: metadata.publicAddress || null,
        issuer: metadata.issuer || 'https://api.magic.link',
        phone: metadata.phone || null,
        isTwoFactorEnabled: metadata.isTwoFactorEnabled || false
      }
    };
  } catch (error) {
    console.error('[Magic] Token validation failed:', error.message);
    return {
      success: false,
      error: error.message || 'Token validation failed'
    };
  }
}

/**
 * Validate JWT token from external OIDC provider (Auth0, Google, etc.)
 * For future use with Identity Providers
 */
async function validateJWTToken(jwtToken, issuer) {
  try {
    // Placeholder for future JWT validation
    // Can be extended to validate JWTs from configured identity providers
    console.log('[Magic] JWT validation requested for issuer:', issuer);
    return {
      success: false,
      error: 'JWT validation not yet implemented'
    };
  } catch (error) {
    return {
      success: false,
      error: error.message
    };
  }
}

// Export functions to be called from Java
globalThis.initializeMagic = initializeMagic;
globalThis.validateDIDToken = validateDIDToken;
globalThis.validateJWTToken = validateJWTToken;
