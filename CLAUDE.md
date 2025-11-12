# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

This is a Burp Suite Extension template project using the Montoya API (currently a minimal starter project).

## Architecture

- **Main Entry Point**: `src/main/java/Extension.java` - implements `BurpExtension` interface
- **Build System**: Gradle with Kotlin DSL, Java 21 compatibility
- **Dependencies**: Montoya API 2025.5 (compile-only), no runtime dependencies
- **Extension Pattern**: Single-class extension that initializes through `initialize(MontoyaApi montoyaApi)` method

## Key Development Commands

```bash
./gradlew build    # Build and test the extension
./gradlew jar      # Create the extension JAR file
./gradlew clean    # Clean build artifacts
```

The built JAR file will be in `build/libs/` and can be loaded directly into Burp Suite.

## Extension Loading in Burp

1. Build the JAR using `./gradlew jar`
2. In Burp: Extensions > Installed > Add > Select the JAR file
3. For quick reloading during development: Ctrl/⌘ + click the Loaded checkbox

## Documentation Structure

- See @docs/bapp-store-requirements.md for BApp Store submission requirements
- See @docs/montoya-api-examples.md for code patterns and extension structure  
- See @docs/development-best-practices.md for development guidelines
- See @docs/resources.md for external documentation and links

## Current State

This is a template project with minimal functionality. The main extension class currently only sets the extension name to "My Extension" and contains a TODO for actual implementation.

## INSTRUCTIONS

Build a Burp extension using the latest Montoya API which allows users to define "tokens" to collect from requests or responses in Burp and then use in subsequent requests or responses.

The extension should have a "Global Controls" tab for enabling which tools within Burp Suite tokens can be collected from, and separate controls (on the same tab) for enabling which tools within Burp Suite tokens can be used in again.

The extension should allow users to create a tab per token, with the tab being referred to as a "Bucket". Each tab / bucket is independent from each other, but abide by the rules of the global controls tab.

For each Bucket, users can define rules for how tokens are collected into the bucket. These rules should include:
1. Whether the token is collected from requests, responses (or both).
2. Which tools are monitored (just like the Global Controls tab).
3. An allow-list of URL matching options, letting users set a full URL, or parts of a URL (e.g. a host, port, path, or combination or any) to match.
4. A list of regex with capturing groups to extract the token value.
5. Dynamic post-processing code using custom JavaScript to transform the token value before it is stored (e.g. prepending, appending strings, etc.)
6. The type of bucket, i.e. how the bucket stores values (FIFO, LILO, etc.)
7. The bucket size (defaults to infinite, but can be set to any positive integer).
8. Options for what to do when a bucket is full (e.g. new tokens don't get added, new tokens replace the last token added, etc.)

For each Bucket, users can also define rules for how tokens are used in requests or responses, including:
1. Whether the token is used in requests, responses (or both).
2. Which tools are monitored (just like the Global Controls tab).
3. A list of replacement options, including the location of the replacement (e.g. Header, URL Parameter, Body Parameter, Cookie, or Generic Regex). For Header, URL Param, Body Param, Cookie, the name of the field determines the replacement. For Generic Regex, a regex string with a defined group determines the replacement.
4. Dynamic pre-processing code using custom JavaScript to transform the token value before it is used (e.g. prepending, appending strings, etc.)
5. Options for what to do when a bucket is down to its last item and needs it for replacement (keep it in bucket but still use it, or use it and remove it from bucket)

The application should autosave using Montoya API persistence, per-project. There should also be the option to export all options and buckets to a JSON file using Serialization, and an option to import a JSON file back into the extension, overwriting any current setup.