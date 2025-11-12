# Collector (Burp Suite Extension)

**Collector** is a mostly vibe-coded Burp Suite extension that enables automated collection and injection of dynamic tokens across HTTP requests and responses. It's designed to handle non-cookie session management, CSRF tokens, and any scenario where values need to be extracted from one request/response and used in subsequent ones.

Video demo available here: https://youtu.be/SsjlL1N1qgM

**DISCLAIMER:** This extension was mostly vibe-coded (including this README), so... use at your own risk.

## Table of Contents

- [Summary](#summary)
- [Key Features](#key-features)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [Detailed Feature Guide](#detailed-feature-guide)
  - [Global Controls](#global-controls)
  - [Bucket Defaults](#bucket-defaults)
  - [Import/Export](#importexport)
  - [Buckets (Token Collections)](#buckets-token-collections)
  - [Bucket Configuration](#bucket-configuration)
  - [Token Collection Rules](#token-collection-rules)
  - [Token Management](#token-management)
  - [Token Replacement Rules](#token-replacement-rules)
- [Building from Source](#building-from-source)

## Summary

Collector allows you to define "buckets" - independent token storage containers that can automatically:
- **Extract tokens** from HTTP requests/responses using regex patterns with optional JavaScript post-processing
- **Store tokens** using various strategies (FIFO, LIFO, unique sets)
- **Inject tokens** into subsequent requests/responses at specific locations (headers, parameters, cookies, or custom regex)
- **Transform tokens** before injection using JavaScript pre-processing
- **Handle edge cases** like empty buckets, full buckets, and missing tokens

All of this is controlled through an intuitive GUI with per-bucket and global controls, automatic persistence, and full import/export capabilities.

## Key Features

### Multi-Bucket Architecture
- Create unlimited independent token buckets
- Each bucket has its own collection and replacement rules
- Enable/disable buckets individually or globally

### Flexible Token Collection
- Extract tokens from requests, responses, or both
- Regex patterns with capturing groups for precise extraction
- **Per-pattern targeting**: Each pattern can independently match requests, responses, or both
- Pattern-level and bucket-level JavaScript post-processing
- DOTALL and MULTILINE regex flags support
- URL-based filtering (in-scope URLs, custom URL matchers)
- Tool-specific collection (Proxy, Repeater, Scanner, Intruder, etc.)
- Parse Proxy History to backfill tokens

### Powerful Token Replacement
- Replace tokens in requests, responses, or both
- **Per-rule targeting**: Each rule can independently apply to requests, responses, or both
- Multiple location types:
  - HTTP Headers (requests and responses)
  - URL Parameters (requests only)
  - Body Parameters (requests only)
  - Cookies (requests only)
  - Generic Regex (requests and responses, with capturing groups)
- Bucket-level and rule-level JavaScript pre-processing
- Replace-all or replace-once modes
- Tool-specific replacement controls (Proxy, Repeater, Scanner, Intruder, etc.)
- **Live testing**: Interactive preview of replacements before saving

### Advanced Bucket Management
- **Storage Types**: FIFO, LIFO, Unique Set
- **Bucket Size**: Unlimited or capped at specific size
- **Full Bucket Behavior**: Stop adding, replace oldest/newest
- **Empty Bucket Behavior**: Do nothing, use static value, generate from regex
- **Last Token Behavior**: Keep in bucket or remove when used

### Token Display & Management
- Configurable display length with center truncation
- Manual token addition with newline support
- Copy/paste tokens with encoding/decoding options (Base64, URL, Escape)
- Edit tokens with live preview
- Save/load tokens from files
- Refresh tokens display on demand

### Persistence & Portability
- Automatic project-level persistence for buckets
- Burp-level persistence for global settings and defaults
- Export entire configuration to JSON
- Import configuration from JSON

### Smart Content-Length Handling
- Automatically recalculates Content-Length headers after replacements
- Handles both requests and responses
- Supports standard HTTP formatting

## Installation

### From Release

1. Download the latest `Collector.jar` from the [Releases](../../releases) page
2. In Burp Suite, go to **Extensions** → **Installed** → **Add**
3. Select **Extension type**: Java
4. Click **Select file** and choose the downloaded JAR
5. Click **Next** to load the extension

### From Source

See [Building from Source](#building-from-source) below.

## Quick Start

### Example: Bearer Token Collection and Injection

1. **Create a Bucket**:
   - Go to the **Collector** tab in Burp
   - Click **Add Bucket**
   - Name it "Bearer Tokens"

2. **Configure Bucket**:
   - In the **Bucket Configuration** tab, set the following:
     - Max Size: `1`
     - When Bucket is full: **Replace latest token**
   - This ensures only the latest token is kept

2. **Configure Collection**:
   - In the **Token Collection** tab, check **Responses** in **Collection Sources**
   - Enable **Proxy** in the tools list
   - Add a URL matcher for your target application or use **All In-Scope URLs**
   - Add a Collection pattern: `"access_token":"([^"]+)"`

3. **Configure Replacement**:
   - In the **Token Replacement** tab, check **Requests** in **Replacement Sinks**
   - Enable **Repeater**, **Intruder**, **Scanner** etc. in the tools list
   - Add a replacement rule:
     - Location: **Header**
     - Field Name: `Authorization`
     - Pre-Replacement JavaScript: `return "Bearer " + token;`

5. **Test**:
   - Authenticate to the application in Burp's browser
   - Access tokens will be automatically extracted from JSON responses
   - Resend a request in Repeater (for example) with a removed or invalid Bearer token.
   - The latest token will be prefixed with "Bearer " and injected into the Authorization header

## Detailed Feature Guide

### Global Controls

Global Controls act as a master switch for the entire extension.

**Location**: Main Collector tab → Settings tab → Global Controls tab

**Features**:
- **Buckets Enabled**: Master on/off switch for all buckets
- **Collection Tools**: Enable/disable token collection per Burp tool (Proxy, Repeater, Scanner, Intruder, Sequencer, Decoder, Comparer, Extender)
- **Replacement Tools**: Enable/disable token replacement per Burp tool

**Note**: Both global AND bucket-level tools must be enabled for a tool to be active in a bucket.

### Bucket Defaults

Pre-configure settings for new buckets.

**Location**: Main Collector tab → Settings tab -> Defaults tab

**Configurable Defaults**:
- Storage Type
- Size Limit
- Full Bucket Behavior
- Empty Bucket Behavior
- Last Token Behavior
- Collect from Requests/Responses
- Replace in Requests/Responses
- URL Matchers
- Enabled Tools (Collection)
- Enabled Tools (Replacement)
- In-Scope URL matching

**Enable Defaults Checkbox**: When checked, new buckets inherit these settings instead of starting empty.

### Import/Export

**Location**: Main Collector tab → Import/Export buttons

#### Export Configuration
Exports everything to a JSON file:
- All buckets with their configurations
- Global controls settings
- Bucket defaults
- **DOES NOT EXPORT TOKENS**

#### Import Configuration
Imports from a JSON file:
- **Warning**: Overwrites current configuration
- Clears all existing buckets
- Loads bucket configurations from file
- Updates global controls
- Updates bucket defaults

#### Save All Tokens
Saves all tokens from each bucket to individual files (one per bucket), with optional encoding, to a designated folder.

### Buckets (Token Collections)

Buckets are independent token storage containers with their own collection and replacement rules.

**Bucket Properties**:
- **Name**: Unique identifier for the bucket (shown as tab label)
- **Enabled Checkbox**: Quick enable/disable without losing configuration
- **Bucket Type**: How tokens are stored and retrieved
- **Max Size**: Maximum number of tokens (0 = unlimited)

**Bucket Actions**:
- **Add Bucket**: Create a new bucket
- **Remove Bucket**: Delete the selected bucket (right-click tab)
- **Rename Bucket**: Change the bucket's name (right-click tab)
- **Duplicate Bucket**: Create a copy with same configuration (right-click tab)

### Bucket Configuration

**Location**: Bucket → Bucket Configuration

#### Enabled
Easily turn an individual bucket off or on.

#### Bucket Type
How tokens are stored and retrieved:
- **FIFO** (First In, First Out): Oldest token used first (queue)
- **LIFO** (Last In, First Out): Newest token used first (stack)

#### Max Size
- **-1**: Unlimited storage
- **Positive integer**: Maximum number of tokens

#### Full Bucket Behavior
What happens when bucket reaches size limit:
- **Reject new tokens**: New tokens are discarded
- **Replace latest token**: New token replaces latest token
- **Replace oldest token**: New token replaces oldest token

#### Unique Set
Check to only store unique tokens, duplicates ignored

### Token Collection Rules

Control how tokens are extracted from HTTP traffic.

**Location**: Bucket → Token Collection tab

**Configuration Options**:

#### Collection Sources
- **Requests**: Extract tokens from HTTP requests
- **Responses**: Extract tokens from HTTP responses

#### Collection Tools
Select which Burp tools will trigger token collection:
- Proxy, Repeater, Scanner, Intruder, Sequencer, Extensions, etc.

#### Collection URLs
Define which URLs will be processed:
- **All In-Scope URLs**: Match against Burp's scope
- **Custom URL Matchers**: Fine-grained control
  - Protocol (HTTP/HTTPS)
  - Host (exact match or regex)
  - Port (specific or any)
  - Path (exact match or regex)

#### Collection Patterns
Extract token values using regex patterns:
- **Pattern**: Regular expression with capturing group
- **Flags**:
  - DOTALL: `.` matches newlines
  - MULTILINE: `^` and `$` match line boundaries
- **Match Requests**: Enable to match this pattern against HTTP requests
- **Match Responses**: Enable to match this pattern against HTTP responses
- **Post-Collection JavaScript**: Optional JavaScript to transform the extracted value before it is stored
- **Enable/Disable**: Toggle patterns individually

**Note**: Each pattern can independently target requests, responses, or both. This allows fine-grained control when you have different patterns for extracting tokens from different message types within the same bucket.

**Example Patterns**:
```regex
"access_token":"([^"]+)"           # Extract JWT from JSON
Set-Cookie: session=([^;]+)        # Extract session cookie
<input name="csrf" value="(.+?)"> # Extract CSRF from HTML
```

#### Post-Collection JavaScript
JavaScript code that runs on ALL tokens collected by this bucket, after pattern-specific scripts.

**Example**:
```javascript
// URL decode token using MontoyaAPI function:
return utilities().urlUtils().decode(token);
```

#### Proxy History Parsing
Backfill tokens from existing proxy history:
- **Parse Proxy History**: Open dialog to scan historical traffic
- **Max Items**: Number of history items to process (or unlimited)
- **Order**: Newest first or Oldest first
- **Pause/Cancel**: Control long-running operations

### Token Management

**Location**: Bucket → Tokens tab

#### Display Options
- **Refresh**: Reload token list
- **Display Length**: Characters to show (0 = no truncation)
  - Uses center truncation: `start...end`
  - Auto-rounds to nearest 10 (except 0)

#### Token Actions
- **Add Token**: Manually add a token (supports multi-line)
- **Edit Token**: Modify selected token with live preview
- **Remove Token(s)**: Delete selected token(s)
- **Clear All**: Remove all tokens from bucket
- **Copy Token(s)**: Copy to clipboard with optional encoding
- **Paste Tokens**: Add from clipboard with optional decoding
- **Save Tokens**: Export to file with optional encoding
- **Load Tokens**: Import from file with optional decoding
- **Multi-Select**: Select multiple rows using Ctrl/Shift+Click

#### Encoding/Decoding
Handles tokens containing newlines:
- **Base64**: Standard Base64 encoding
- **URL Encoding**: Percent-encoding
- **Escape Encoding**: Backslash-escaped `\r` and `\n`

**Newline Warning Dialog**:
- **Encode Tokens**: Apply selected encoding
- **Do Nothing**: Save/copy as-is (may cause issues)
- **Skip Tokens with Newlines**: Only process tokens without newlines
- **Cancel**: Abort operation

### Token Replacement Rules

Control how tokens are injected into HTTP traffic.

**Location**: Bucket → Token Replacement tab

**Configuration Options**:

#### Replacement Sinks
- **Requests**: Inject tokens into HTTP requests
- **Responses**: Inject tokens into HTTP responses

#### Replacement Tools
Select which Burp tools will trigger token replacement (same options as collection).

#### Last Token Behavior
What to do when only one token remains:
- **Use and keep in bucket**: Use the token but don't remove it (infinite reuse)
- **Use and remove from bucket**: Use and remove (bucket becomes empty)

#### Empty Bucket Behavior
What to do when bucket has no tokens:
- **Do nothing**: Skip replacement rules
- **Use static value**: Use a predefined string
- **Generate value from regex**: Generate random value matching regex pattern

#### Replacement URLs
Define which URLs will have tokens replaced (same options as collection).

#### Pre-Replacement JavaScript
JavaScript code that runs on the token BEFORE all replacement rules in this bucket.

#### Replacement Rules
Define where and how tokens should be injected:

**Location Types**:
1. **Header**: Replace/add HTTP header
   - Field Name: Header name (e.g., `Authorization`)
   - Works for both requests and responses

2. **URL Parameter**: Replace/add query parameter
   - Field Name: Parameter name (e.g., `token`)
   - Only applies to requests

3. **Body Parameter**: Replace/add POST body parameter
   - Field Name: Parameter name (e.g., `csrf_token`)
   - Only applies to requests

4. **Cookie**: Replace/add cookie value in requests
   - Field Name: Cookie name (e.g., `session`)
   - Only applies to requests (modifies Cookie header)
   - For Set-Cookie in responses, use Header location with field name "Set-Cookie"

5. **Generic Regex**: Replace using regex pattern
   - Regex Pattern: Pattern with capturing group to replace
   - Group Number: Which capturing group to replace (0 = entire match)
   - Replace All: Replace all occurrences or just first
   - Works for both requests and responses

**Replacement Targets**:
- **Requests**: Enable to apply this rule to HTTP requests
- **Responses**: Enable to apply this rule to HTTP responses

**Note**: Each replacement rule can independently target requests, responses, or both. This allows different rules within the same bucket to apply to different message types. For example, one rule can inject into request headers while another modifies response headers.

**Pre-Replacement JavaScript**: Optional JavaScript to transform token before THIS specific rule.

**Example**:
```javascript
// Prepend "Bearer " to all tokens
return "Bearer " + token;
```

**Example Rules**:
```
Location: Header
Field Name: Authorization
Target: Requests
Pre-Replacemnent JS: return "Bearer " + token;

Location: Generic Regex
Pattern: "token"\s*:\s*"([^"]+)"
Group: 1
Targets: Requests and Responses
```

## Building from Source

### Prerequisites

- Java Development Kit (JDK) 21 or higher
- Git (optional, for cloning)

### Build Steps

1. **Clone or download the repository**:
   ```bash
   git clone <repository-url>
   cd Collector
   ```

2. **Build the JAR file**:

   **Windows**:
   ```bash
   gradlew.bat jar
   ```

   **Linux/Mac**:
   ```bash
   ./gradlew jar
   ```

3. **Locate the JAR file**:
   ```
   build/libs/Collector.jar
   ```

4. **Load into Burp Suite** (see [Installation](#installation) above)

### Development

To run all checks including tests:
```bash
./gradlew build
```

To clean build artifacts:
```bash
./gradlew clean
```

### Dependencies

- **Burp Montoya API** 2025.5 (compile-only)
- **Gson** 2.10.1 (JSON serialization)
- **GraalVM JavaScript** 23.0.1 (JavaScript engine)
- **rgxgen** 1.4 (regex-based string generation)