# Gateway-Level Authentication Implementation (Apache APISIX)

## Overview

This document describes the complete gateway-level authentication architecture for Oregon Markets using Apache APISIX. The gateway must replicate the Magic DID token validation and username sanitization performed at the service layer before calling Keycloak.

## Key Architectural Principle

**NO DATABASE LOOKUPS AT GATEWAY**: As per explicit requirement, the gateway does not look up the database. All validation is stateless and based on cryptographic verification.

## Service-Layer Implementation Reference

Before implementing at gateway level, understand what the service layer does:

### Service-Layer Files
- `KeycloakAdminClient.java:61-77` - **Username sanitization**: `username.replace("=", "")`
- `KeycloakAdminClient.java:79-106` - **Keycloak user creation with sanitized username**
- `MagicDIDValidator.java:58-175` - **Complete Magic DID token validation**
  - Base64 decoding
  - Token format validation `[proof, claim]`
  - Claim expiration and not-before checks
  - ECDSA signature recovery using web3j
  - Issuer verification

## Gateway Implementation Strategy

The gateway authentication consists of three Lua plugins executed in sequence:

```
Request → magic-did-auth → keycloak-session → request-enrichment → Service
```

### 1. Magic DID Auth Plugin (`magic-did-auth.lua`)

**Purpose**: Validate Magic DID token and extract sanitized username

**Responsibilities**:
1. Extract DID token from `Authorization: Bearer <token>` header
2. Base64 decode token
3. Parse `[proof, claim]` structure
4. Validate claim temporal constraints (iat, ext, nbf)
5. Perform ECDSA signature recovery to get authoritative Ethereum address
6. Verify signature matches issuer (`did:ethr:0x...`)
7. Extract and sanitize username from `sub` field (remove `=`)
8. Store in request context for next plugin

**Critical Implementation Details**:

```lua
-- Username Sanitization (MUST match service layer)
local sanitized_username = username:gsub("=", "")

-- ECDSA Signature Recovery
-- Recover Ethereum address from personal_sign signature
-- Verify: recovered_address == issuer_address
-- DO NOT use unverified `add` claim field from token

-- Expiration Validation
-- Check: now < exp_time (ext field)
-- Check: now >= not_before (nbf field)
-- Reject expired tokens BEFORE calling Keycloak
```

**Error Handling**:
- Malformed token → 401 Unauthorized
- Invalid signature → 401 Unauthorized
- Expired token → 401 Unauthorized
- Invalid issuer format → 401 Unauthorized

**Output Context Variables**:
```lua
ngx.ctx.magic_user_id = sanitized_username        -- Magic user ID (sub field, sanitized)
ngx.ctx.magic_did_token = token                    -- Original DID token
ngx.ctx.magic_issuer = issuer                      -- did:ethr:0x... format
ngx.ctx.magic_signature_valid = true               -- Indicates signature verified
```

### 2. Keycloak Session Plugin (`keycloak-session.lua`)

**Purpose**: Manage Keycloak sessions with token versioning and caching

**Key Features**:
- Cache Keycloak tokens keyed by magic_user_id + token_hash
- Detect session changes by comparing DID token hashes
- Implement token versioning to avoid redundant Keycloak calls
- Handle Keycloak token expiration gracefully

**Token Versioning Logic**:

```
Scenario 1: Same DID Token
- Hash = SHA256(DID_token)
- If hash matches cached hash:
  → Use cached Keycloak token (no Keycloak call)
  → Set request header: X-Keycloak-Token: <cached_token>

Scenario 2: Different DID Token (Same User)
- Hash != cached hash → Session changed
- Call Keycloak reset-password with:
  - username: sanitized_magic_user_id
  - password: new_did_token
- Generate new Keycloak token
- Cache with new hash
- Set request header: X-Keycloak-Token: <new_token>

Scenario 3: No Cached Session
- First request from user
- Call Keycloak reset-password
- Generate and cache token
```

**Redis Cache Structure**:
```
Key: session:{magic_user_id}:{token_hash}
Value: {
  keycloak_token: "...",
  refresh_token: "...",
  token_hash: "...",
  issued_at: 1700000000,
  expires_at: 1700003600,
  version: 1
}
```

**Token Expiration Handling**:
```lua
if now > cached_token_expiry then
  if did_token_expired(ctx.magic_did_token) then
    -- Both tokens expired, reject request
    return 401
  else
    -- Keycloak token expired, DID token still valid
    -- Use refresh_token to get new Keycloak token
    local new_token = keycloak_refresh_token(refresh_token)
    -- Update cache
    cache_token(new_token)
  end
end
```

**Critical Synchronization**:
- Keycloak token TTL must be ≤ DID token TTL
- Both expiration times tracked and validated
- Refresh mechanism prevents orphaned sessions

### 3. Request Enrichment Plugin (`request-enrichment.lua`)

**Purpose**: Add authentication context to requests for service layer

**Headers Added**:
```
X-Magic-User-ID: <sanitized_magic_user_id>
X-Magic-DID-Token: <original_did_token>
X-Magic-Issuer: <did:ethr:0x...>
X-Keycloak-Token: <keycloak_token>
X-Keycloak-Refresh-Token: <refresh_token>
X-Auth-Signature-Valid: true
X-Token-Version: <version>
```

**Service Layer Integration**:
- Spring controller receives headers instead of doing its own validation
- Can trust DID token validity and username sanitization already done
- Headers provide audit trail

## Implementation Checklist

### magic-did-auth.lua
- [ ] Extract token from Authorization header
- [ ] Base64 decode token
- [ ] Parse [proof, claim] array structure
- [ ] Validate claim has required fields (iss, sub, ext, nbf)
- [ ] Check iat timestamp (issued at)
- [ ] Check nbf timestamp (not before)
- [ ] Check ext timestamp (expiration) - MUST REJECT IF EXPIRED
- [ ] Validate issuer format matches `did:ethr:0x[0-9a-f]+`
- [ ] Extract issuer address from `did:ethr:` prefix
- [ ] Implement ECDSA signature recovery
  - [ ] Normalize proof hex string
  - [ ] Extract v, r, s from 65-byte signature
  - [ ] Use Sign.signedPrefixedMessageToKey equivalent
  - [ ] Recover public key and derive address
- [ ] Compare recovered address with issuer address (case-insensitive)
- [ ] Extract sub field (Magic user ID)
- [ ] Sanitize username: `sub:gsub("=", "")`
- [ ] Store in ngx.ctx for next plugin
- [ ] Return 401 with discreet error messages on validation failures

### keycloak-session.lua
- [ ] Initialize Redis connection
- [ ] Extract sanitized_username and did_token from context
- [ ] Calculate SHA256 hash of DID token
- [ ] Check Redis cache for existing session
- [ ] Implement cache hit logic (same token hash)
  - [ ] Validate cached token not expired
  - [ ] Return cached Keycloak token
- [ ] Implement cache miss logic (new token or first session)
  - [ ] Call Keycloak reset-password endpoint
  - [ ] Extract new Keycloak token from response
  - [ ] Parse token expiration time
  - [ ] Cache token with metadata
- [ ] Implement session change logic (different token hash)
  - [ ] Call Keycloak reset-password with new DID token
  - [ ] Update cache with new token and hash
- [ ] Store Keycloak token in ngx.ctx
- [ ] Handle Keycloak API errors gracefully
- [ ] Log token operations (no sensitive data)

### request-enrichment.lua
- [ ] Add X-Magic-User-ID header
- [ ] Add X-Magic-DID-Token header
- [ ] Add X-Magic-Issuer header
- [ ] Add X-Keycloak-Token header
- [ ] Add X-Auth-Signature-Valid header
- [ ] Set routing headers for backend service

## Route Configuration

Routes protected by gateway auth (only client endpoints):

```yaml
# Protected endpoints (require auth plugins)
- path: /api/auth/register
  skip_auth: true  # Public, no plugin chain

- path: /api/markets/*
  plugins: [magic-did-auth, keycloak-session, request-enrichment]

- path: /api/orders/*
  plugins: [magic-did-auth, keycloak-session, request-enrichment]

- path: /api/account/*
  plugins: [magic-did-auth, keycloak-session, request-enrichment]

- path: /api/deposits/*
  plugins: [magic-did-auth, keycloak-session, request-enrichment]

- path: /api/withdrawals/*
  plugins: [magic-did-auth, keycloak-session, request-enrichment]

# Admin endpoints (no auth required at gateway)
- path: /api/admin/*
  skip_auth: true

# Health checks (no auth required)
- path: /health
  skip_auth: true

- path: /metrics
  skip_auth: true
```

## Rate Limiting Strategy

### At Gateway Level

Rate limits BEFORE authentication validation (to prevent DOS):

```lua
-- Per IP address (global rate limit)
local ip = ngx.var.remote_addr
local key = "rate_limit:ip:" .. ip
local count = redis:incr(key)
redis:expire(key, 60)  -- 1-minute window

if count > 100 then  -- 100 requests per minute per IP
  return 429, "Too Many Requests"
end
```

### After Authentication

Rate limits by authenticated user:

```lua
-- Per magic_user_id (authenticated rate limit)
local user_key = "rate_limit:user:" .. sanitized_username
local user_count = redis:incr(user_key)
redis:expire(user_key, 60)

if user_count > 1000 then  -- 1000 requests per minute per user
  return 429, "Rate Limit Exceeded"
end
```

### Endpoint-Specific Limits

Different limits for different endpoint types:

```lua
-- Strict limits for sensitive operations
if request_path:match("/api/withdrawals") then
  limit = 10  -- 10 withdrawals per minute
elseif request_path:match("/api/orders") then
  limit = 100  -- 100 orders per minute
else
  limit = 1000  -- Default: 1000 requests per minute
end
```

### Implementation in APISIX

Add rate limiting as separate plugin in plugin chain:

```
Request → rate-limit (IP-based) → magic-did-auth → rate-limit (user-based) → keycloak-session → request-enrichment → Service
```

## Security Considerations

### Signature Recovery
- MUST use cryptographic signature recovery, not token claims
- The `add` field in token is UNVERIFIED - ignore it
- Only trust the recovered address from ECDSA recovery

### Username Sanitization
- MUST strip base64 padding characters (`=`) from Magic user ID before passing to Keycloak
- Apply sanitization in BOTH gateway and service layer (defense in depth)
- Sanitization must be identical in both places

### Token Caching
- Hash the token before using as cache key (don't store token as key)
- Never cache tokens longer than their expiration time
- Implement cache invalidation on token refresh

### Error Messages
- Return discreet error messages (e.g., "Invalid token") not implementation details
- Don't expose which validation step failed
- Log detailed errors server-side only

## Testing

### Unit Tests for magic-did-auth.lua
```lua
-- Test expired token rejection
-- Test invalid signature rejection
-- Test missing required claims
-- Test username sanitization (= removal)
-- Test issuer format validation
```

### Integration Tests
```lua
-- Test full flow: new user → password reset → token cache
-- Test flow: same user, same token → cache hit
-- Test flow: same user, different token → password reset
-- Test token expiration handling
-- Test Keycloak API failure handling
```

### Load Tests
- Verify rate limiting works correctly under load
- Verify cache hit performance (should be < 5ms)
- Verify Keycloak password reset under concurrent requests

## Debugging Tips

### Check Token Validity
```bash
# Decode token to verify structure
echo "WyIweGU2..." | base64 -d | jq .

# Check timestamps
echo "1700000000" | xargs date -d @
```

### Monitor Cache
```bash
redis-cli KEYS "session:*"
redis-cli GET "session:CQ0U5Pbimdu...:abc123def"
```

### Verify Signature Recovery
- Log intermediate values: proof hex, v/r/s bytes, recovered address
- Compare with service layer logs for consistency
- Test with known valid signature

## Deployment Steps

1. **Create Lua plugins** in APISIX plugins directory
2. **Configure Redis connection** in APISIX config
3. **Add routes** with plugin chain
4. **Enable rate limiting** per route
5. **Test with Magic SDK** to generate real tokens
6. **Monitor logs** for any authentication failures
7. **Validate** that service layer headers received correctly
8. **Run load tests** to verify performance

## Future Enhancements

- [ ] JWT token generation at gateway (cache as JWT for faster validation)
- [ ] WebAuthn support alongside Magic authentication
- [ ] OAuth2 device flow for CLI tools
- [ ] Conditional rate limiting based on user tier
- [ ] Distributed rate limiting across multiple APISIX instances
