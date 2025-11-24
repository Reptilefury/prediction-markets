# Magic Node SDK for GraalVM

This directory contains the Magic Node SDK wrapper for validating DID tokens inside Java using GraalVM.

## Setup

### Option 1: Automatic Setup (Recommended)

From the project root:
```bash
./setup-magic-sdk.sh
```

### Option 2: Manual Setup

```bash
cd src/main/resources/magic-node
npm install
```

## Files

- **package.json** - NPM package definition with @magic-sdk/admin dependency
- **magic.js** - JavaScript wrapper that exposes Magic SDK functions to Java
- **node_modules/** - Installed npm dependencies (not committed to git)

## How It Works

1. **On startup**, `MagicGraalVMValidator` loads this magic.js script into GraalVM
2. **Initialization**, the Magic SDK is initialized with your API key
3. **Validation**, when a DID token arrives, `validateDIDToken()` is called
4. The Magic Node SDK validates the token and returns user metadata

## Configuration

Ensure `MAGIC_API_KEY` environment variable is set:
```bash
export MAGIC_API_KEY=sk_your_magic_secret_key
```

Or configure in `application.yml`:
```yaml
app:
  magic:
    api-key: ${MAGIC_API_KEY}
```

## Functions Exposed to Java

### initializeMagic(apiKey)
Initializes the Magic SDK with your API key. Called automatically on startup.

**Returns:**
```json
{ "success": true, "message": "Magic SDK initialized" }
```

### validateDIDToken(token)
Validates a DID token and returns user metadata.

**Returns:**
```json
{
  "success": true,
  "data": {
    "email": "user@example.com",
    "publicAddress": "0x...",
    "issuer": "https://api.magic.link",
    "phone": null
  }
}
```

On error:
```json
{
  "success": false,
  "error": "Invalid or expired DID token"
}
```

## Dependencies

- **@magic-sdk/admin** - Magic's official Node SDK for backend validation
- **Node.js** - Required for npm install (not required at runtime)
- **GraalVM** - Embedded in Java via spring-boot (Java 21+)

## Troubleshooting

### `node_modules` not found
Run the setup script:
```bash
./setup-magic-sdk.sh
```

### GraalVM context initialization fails
Check:
1. Ensure `MAGIC_API_KEY` is set
2. Verify `magic.js` exists in the correct location
3. Check application logs for detailed error messages

### npm install fails
```bash
cd src/main/resources/magic-node
npm install --legacy-peer-deps
```

## Development

To update the Magic SDK:
```bash
cd src/main/resources/magic-node
npm update @magic-sdk/admin
```

To test the magic.js script locally:
```bash
node magic.js  # Requires MAGIC_API_KEY env var
```
