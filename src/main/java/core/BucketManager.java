package core;

import burp.api.montoya.http.message.params.HttpParameter;
import burp.api.montoya.http.message.params.HttpParameterType;
import burp.api.montoya.http.message.params.ParsedHttpParameter;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.logging.Logging;
import com.github.curiousoddman.rgxgen.RgxGen;
import model.*;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BucketManager {
    private final List<Bucket> buckets;
    private final GlobalControls globalControls;
    private final JavaScriptProcessor jsProcessor;
    private final Logging logging;
    private final burp.api.montoya.MontoyaApi api;

    public BucketManager(Logging logging, burp.api.montoya.MontoyaApi api) {
        this.buckets = new CopyOnWriteArrayList<>();
        this.globalControls = new GlobalControls();
        this.jsProcessor = new JavaScriptProcessor(logging, api);
        this.logging = logging;
        this.api = api;
    }

    public void addBucket(Bucket bucket) {
        buckets.add(bucket);
    }

    public void removeBucket(Bucket bucket) {
        buckets.remove(bucket);
    }

    public List<Bucket> getBuckets() {
        return new ArrayList<>(buckets);
    }

    public Bucket getBucketByName(String name) {
        return buckets.stream()
                .filter(b -> b.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    public GlobalControls getGlobalControls() {
        return globalControls;
    }

    public void collectTokens(String content, String url, burp.api.montoya.core.ToolType toolType, boolean isRequest) {
        // Check if buckets are globally enabled
        if (!globalControls.isBucketsEnabled()) {
            return;
        }

        // Global controls are a master switch - if disabled globally, skip entirely
        if (!globalControls.isCollectionEnabledForTool(toolType)) {
            return;
        }

        for (Bucket bucket : buckets) {
            // Check if bucket is enabled (both global buckets enabled AND individual bucket enabled)
            if (!bucket.isEnabled()) continue;

            CollectionRule rule = bucket.getCollectionRule();

            // Check if this bucket is configured to collect from this type
            if (isRequest && !rule.isCollectFromRequests()) continue;
            if (!isRequest && !rule.isCollectFromResponses()) continue;

            // Bucket must explicitly enable the tool (global is already checked above)
            // Both global AND bucket must be checked for the tool to be processed
            if (!rule.getEnabledTools().contains(toolType)) {
                continue; // Tool not enabled at bucket level
            }

            // Check URL matchers - always act as allow-list
            // At least one of: in-scope match OR URL matcher match required
            boolean urlMatches = false;

            // Check in-scope first if enabled
            if (rule.isMatchInScopeUrls() && api.scope().isInScope(url)) {
                urlMatches = true;
            }

            // If not matched yet, check URL matchers
            if (!urlMatches && !rule.getUrlMatchers().isEmpty()) {
                for (UrlMatcher matcher : rule.getUrlMatchers()) {
                    if (matcher.isEnabled() && matcher.matches(url)) {
                        urlMatches = true;
                        break;
                    }
                }
            }

            // If no match found, skip this bucket
            if (!urlMatches) {
                continue;
            }

            // Try each regex pattern
            for (model.RegexPattern regexPattern : rule.getRegexPatterns()) {
                // Skip disabled patterns
                if (!regexPattern.isEnabled()) {
                    continue;
                }

                // Check if pattern should match this message type
                if (isRequest && !regexPattern.isMatchRequests()) {
                    continue;
                }
                if (!isRequest && !regexPattern.isMatchResponses()) {
                    continue;
                }

                try {
                    // Build flags based on pattern settings
                    int flags = 0;
                    if (regexPattern.isDotallMode()) {
                        flags |= Pattern.DOTALL;
                    }
                    if (regexPattern.isMultilineMode()) {
                        flags |= Pattern.MULTILINE;
                    }

                    Pattern pattern = Pattern.compile(regexPattern.getPattern(), flags);
                    Matcher matcher = pattern.matcher(content);

                    while (matcher.find()) {
                        // Extract the first capturing group, or the whole match if no groups
                        String token = matcher.groupCount() > 0 ? matcher.group(1) : matcher.group(0);

                        // Apply pattern-specific post-processing script first
                        if (regexPattern.getPostProcessingScript() != null && !regexPattern.getPostProcessingScript().isEmpty()) {
                            token = jsProcessor.processToken(token, regexPattern.getPostProcessingScript());
                        }

                        // Then apply collection-level post-processing script
                        if (rule.getPostProcessingScript() != null && !rule.getPostProcessingScript().isEmpty()) {
                            token = jsProcessor.processToken(token, rule.getPostProcessingScript());
                        }

                        bucket.addToken(token);
                    }
                } catch (Exception e) {
                    logging.logToError("Error processing regex for bucket " + bucket.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    private String performRegexReplacement(String content, String regex, int group, String value, boolean replaceAll) {
        try {
            if (regex == null || regex.isEmpty() || content == null) {
                return content;
            }

            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(content);
            StringBuffer result = new StringBuffer();

            while (matcher.find()) {
                // Validate group index
                if (group < 0 || group > matcher.groupCount()) {
                    logging.logToError("Invalid regex group " + group + " for pattern: " + regex);
                    return content;
                }

                String groupContent = matcher.group(group);

                if (groupContent == null) {
                    logging.logToError("Regex group " + group + " is null for pattern: " + regex);
                    continue;
                }

                String fullMatch = matcher.group(0);

                // Replace the captured group within the full match
                // Use replaceFirst with Pattern.quote to avoid replacing all occurrences
                String replaced = fullMatch.replaceFirst(Pattern.quote(groupContent), Matcher.quoteReplacement(value));

                matcher.appendReplacement(result, Matcher.quoteReplacement(replaced));

                // If not replaceAll mode, break after first match
                if (!replaceAll) {
                    break;
                }
            }

            matcher.appendTail(result);
            return result.toString();
        } catch (Exception e) {
            logging.logToError("Error applying regex replacement: " + e.getMessage());
            e.printStackTrace();
            return content;
        }
    }

    // New methods using Montoya API for proper HTTP message manipulation
    public HttpRequest applyReplacementsToRequest(HttpRequest request, burp.api.montoya.core.ToolType toolType) {
        // Check if buckets are globally enabled
        if (!globalControls.isBucketsEnabled()) {
            return request;
        }

        // Global controls are a master switch - if disabled globally, skip entirely
        if (!globalControls.isReplacementEnabledForTool(toolType)) {
            return request;
        }

        HttpRequest modifiedRequest = request;

        for (Bucket bucket : buckets) {
            // Check if bucket is enabled (both global buckets enabled AND individual bucket enabled)
            if (!bucket.isEnabled()) continue;

            ReplacementConfig config = bucket.getReplacementConfig();

            // Check if this bucket is configured to replace in requests
            if (!config.isReplaceInRequests()) continue;

            // Bucket must explicitly enable the tool (global is already checked above)
            // Both global AND bucket must be checked for the tool to be processed
            if (!config.getEnabledTools().contains(toolType)) {
                continue; // Tool not enabled at bucket level
            }

            // Check URL matchers - always act as allow-list
            // At least one of: in-scope match OR URL matcher match required
            String url = request.url();
            boolean urlMatches = false;

            // Check in-scope first if enabled
            if (config.isMatchInScopeUrls() && api.scope().isInScope(url)) {
                urlMatches = true;
            }

            // If not matched yet, check URL matchers
            if (!urlMatches && !config.getUrlMatchers().isEmpty()) {
                for (UrlMatcher matcher : config.getUrlMatchers()) {
                    if (matcher.isEnabled() && matcher.matches(url)) {
                        urlMatches = true;
                        break;
                    }
                }
            }

            // If no match found, skip this bucket
            if (!urlMatches) {
                continue;
            }

            // Check if there are any enabled replacement rules
            boolean hasEnabledRules = false;
            for (ReplacementRule rule : config.getReplacementRules()) {
                if (rule.isEnabled()) {
                    hasEnabledRules = true;
                    break;
                }
            }

            // Skip this bucket if no enabled rules
            if (!hasEnabledRules) {
                continue;
            }

            // Get a token from the bucket
            boolean removeToken = config.getLastTokenBehavior() == LastTokenBehavior.REMOVE_FROM_BUCKET || bucket.getTokenCount() > 1;
            String token = bucket.getToken(removeToken);

            // Handle empty bucket scenarios
            if (token == null) {
                switch (config.getEmptyBucketBehavior()) {
                    case DO_NOTHING:
                        continue; // Skip this bucket
                    case USE_STATIC_VALUE:
                        token = config.getStaticValue();
                        if (token == null || token.isEmpty()) continue;
                        break;
                    case GENERATE_FROM_REGEX:
                        try {
                            String regex = config.getGeneratorRegex();
                            if (regex == null || regex.isEmpty()) continue;
                            RgxGen rgxGen = RgxGen.parse(regex);
                            token = rgxGen.generate();
                        } catch (Exception e) {
                            logging.logToError("Error generating string from regex: " + e.getMessage());
                            continue;
                        }
                        break;
                }
            }

            // Apply pre-replacement script if defined (runs before all replacement rules)
            if (config.getPreReplacementScript() != null && !config.getPreReplacementScript().isEmpty()) {
                token = jsProcessor.processToken(token, config.getPreReplacementScript());
            }

            // Apply each replacement rule
            for (ReplacementRule rule : config.getReplacementRules()) {
                // Skip disabled rules
                if (!rule.isEnabled()) {
                    continue;
                }

                // Check if rule should apply to requests
                if (!rule.isApplyToRequests()) {
                    continue;
                }

                // Apply pre-processing script if defined (runs for each individual rule)
                String processedToken = token;
                if (rule.getPreProcessingScript() != null && !rule.getPreProcessingScript().isEmpty()) {
                    processedToken = jsProcessor.processToken(token, rule.getPreProcessingScript());
                }

                modifiedRequest = applyReplacementRuleToRequest(modifiedRequest, processedToken, rule);
            }
        }

        return modifiedRequest;
    }

    public HttpResponse applyReplacementsToResponse(HttpResponse response, burp.api.montoya.core.ToolType toolType) {
        // Check if buckets are globally enabled
        if (!globalControls.isBucketsEnabled()) {
            return response;
        }

        // Global controls are a master switch - if disabled globally, skip entirely
        if (!globalControls.isReplacementEnabledForTool(toolType)) {
            return response;
        }

        HttpResponse modifiedResponse = response;

        for (Bucket bucket : buckets) {
            // Check if bucket is enabled (both global buckets enabled AND individual bucket enabled)
            if (!bucket.isEnabled()) continue;

            ReplacementConfig config = bucket.getReplacementConfig();

            // Check if this bucket is configured to replace in responses
            if (!config.isReplaceInResponses()) continue;

            // Bucket must explicitly enable the tool (global is already checked above)
            // Both global AND bucket must be checked for the tool to be processed
            if (!config.getEnabledTools().contains(toolType)) {
                continue; // Tool not enabled at bucket level
            }

            // Check if there are any enabled replacement rules
            boolean hasEnabledRules = false;
            for (ReplacementRule rule : config.getReplacementRules()) {
                if (rule.isEnabled()) {
                    hasEnabledRules = true;
                    break;
                }
            }

            // Skip this bucket if no enabled rules
            if (!hasEnabledRules) {
                continue;
            }

            // Get a token from the bucket
            boolean removeToken = config.getLastTokenBehavior() == LastTokenBehavior.REMOVE_FROM_BUCKET || bucket.getTokenCount() > 1;
            String token = bucket.getToken(removeToken);

            // Handle empty bucket scenarios
            if (token == null) {
                switch (config.getEmptyBucketBehavior()) {
                    case DO_NOTHING:
                        continue; // Skip this bucket
                    case USE_STATIC_VALUE:
                        token = config.getStaticValue();
                        if (token == null || token.isEmpty()) continue;
                        break;
                    case GENERATE_FROM_REGEX:
                        try {
                            String regex = config.getGeneratorRegex();
                            if (regex == null || regex.isEmpty()) continue;
                            RgxGen rgxGen = RgxGen.parse(regex);
                            token = rgxGen.generate();
                        } catch (Exception e) {
                            logging.logToError("Error generating string from regex: " + e.getMessage());
                            continue;
                        }
                        break;
                }
            }

            // Apply pre-replacement script if defined (runs before all replacement rules)
            if (config.getPreReplacementScript() != null && !config.getPreReplacementScript().isEmpty()) {
                token = jsProcessor.processToken(token, config.getPreReplacementScript());
            }

            // Apply each replacement rule
            for (ReplacementRule rule : config.getReplacementRules()) {
                // Skip disabled rules
                if (!rule.isEnabled()) {
                    continue;
                }

                // Check if rule should apply to responses
                if (!rule.isApplyToResponses()) {
                    continue;
                }

                // Apply pre-processing script if defined (runs for each individual rule)
                String processedToken = token;
                if (rule.getPreProcessingScript() != null && !rule.getPreProcessingScript().isEmpty()) {
                    processedToken = jsProcessor.processToken(token, rule.getPreProcessingScript());
                }

                modifiedResponse = applyReplacementRuleToResponse(modifiedResponse, processedToken, rule);
            }
        }

        return modifiedResponse;
    }

    private HttpRequest applyReplacementRuleToRequest(HttpRequest request, String token, ReplacementRule rule) {
        switch (rule.getLocation()) {
            case HEADER:
                return replaceOrAddHeader(request, rule.getFieldName(), token);
            case URL_PARAMETER:
                return replaceOrAddUrlParameter(request, rule.getFieldName(), token);
            case BODY_PARAMETER:
                return replaceOrAddBodyParameter(request, rule.getFieldName(), token);
            case COOKIE:
                return replaceOrAddCookie(request, rule.getFieldName(), token);
            case GENERIC_REGEX:
                // Apply regex to individual parts of the request
                return applyRegexToRequest(request, rule.getRegexPattern(), rule.getRegexGroup(), token, rule.isReplaceAll());
            default:
                return request;
        }
    }

    private HttpResponse applyReplacementRuleToResponse(HttpResponse response, String token, ReplacementRule rule) {
        switch (rule.getLocation()) {
            case HEADER:
                return replaceOrAddHeaderInResponse(response, rule.getFieldName(), token);
            case GENERIC_REGEX:
                // Apply regex to individual parts of the response
                return applyRegexToResponse(response, rule.getRegexPattern(), rule.getRegexGroup(), token, rule.isReplaceAll());
            default:
                // URL_PARAMETER, BODY_PARAMETER, COOKIE don't apply to responses
                return response;
        }
    }

    private HttpRequest replaceOrAddHeader(HttpRequest request, String headerName, String value) {
        // Check if header exists
        if (request.hasHeader(headerName)) {
            // Replace existing header
            return request.withUpdatedHeader(headerName, value);
        } else {
            // Add new header
            return request.withAddedHeader(headerName, value);
        }
    }

    private HttpResponse replaceOrAddHeaderInResponse(HttpResponse response, String headerName, String value) {
        // Check if header exists
        if (response.hasHeader(headerName)) {
            // Replace existing header
            return response.withUpdatedHeader(headerName, value);
        } else {
            // Add new header
            return response.withAddedHeader(headerName, value);
        }
    }

    private HttpRequest replaceOrAddUrlParameter(HttpRequest request, String paramName, String value) {
        // withParameter() adds the parameter if it doesn't exist, or updates it if it does
        return request.withParameter(HttpParameter.urlParameter(paramName, value));
    }

    private HttpRequest replaceOrAddBodyParameter(HttpRequest request, String paramName, String value) {
        // withParameter() adds the parameter if it doesn't exist, or updates it if it does
        return request.withParameter(HttpParameter.bodyParameter(paramName, value));
    }

    private HttpRequest replaceOrAddCookie(HttpRequest request, String cookieName, String value) {
        // withParameter() adds the parameter if it doesn't exist, or updates it if it does
        return request.withParameter(HttpParameter.cookieParameter(cookieName, value));
    }

    /**
     * Updates the Content-Length header in an HTTP message to match the actual body length.
     * Handles both \r\n\r\n and \n\n separators between headers and body.
     *
     * @param httpMessage The HTTP request or response as a string
     * @return The HTTP message with corrected Content-Length header
     */
    private String updateContentLength(String httpMessage) {
        try {
            // Find the separator between headers and body
            int separatorIndex = -1;
            String separator = null;

            // Try \r\n\r\n first (standard HTTP)
            separatorIndex = httpMessage.indexOf("\r\n\r\n");
            if (separatorIndex != -1) {
                separator = "\r\n\r\n";
            } else {
                // Try \n\n (some non-standard cases)
                separatorIndex = httpMessage.indexOf("\n\n");
                if (separatorIndex != -1) {
                    separator = "\n\n";
                }
            }

            // If no separator found, return as-is (no body)
            if (separatorIndex == -1) {
                return httpMessage;
            }

            // Split headers and body
            String headersSection = httpMessage.substring(0, separatorIndex);
            String body = httpMessage.substring(separatorIndex + separator.length());

            // Calculate actual body length in bytes (UTF-8)
            int actualBodyLength = body.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;

            // Determine line separator used in headers
            String lineSeparator = headersSection.contains("\r\n") ? "\r\n" : "\n";

            // Split headers into individual lines
            String[] headerLines = headersSection.split(lineSeparator);
            StringBuilder updatedHeaders = new StringBuilder();

            boolean contentLengthFound = false;

            // Process each header line
            for (int i = 0; i < headerLines.length; i++) {
                String line = headerLines[i];

                // Check if this is a Content-Length header (case-insensitive)
                if (line.toLowerCase().startsWith("content-length:")) {
                    // Replace with correct Content-Length
                    updatedHeaders.append("Content-Length: ").append(actualBodyLength);
                    contentLengthFound = true;
                } else {
                    updatedHeaders.append(line);
                }

                // Add line separator after each line except the last
                if (i < headerLines.length - 1) {
                    updatedHeaders.append(lineSeparator);
                }
            }

            // If Content-Length wasn't found but there's a body, add it
            if (!contentLengthFound && actualBodyLength > 0) {
                updatedHeaders.append(lineSeparator);
                updatedHeaders.append("Content-Length: ").append(actualBodyLength);
            }

            // Reconstruct the HTTP message
            return updatedHeaders.toString() + separator + body;

        } catch (Exception e) {
            logging.logToError("Error updating Content-Length: " + e.getMessage());
            e.printStackTrace();
            return httpMessage; // Return original on error
        }
    }

    private HttpRequest applyRegexToRequest(HttpRequest request, String regex, int group, String value, boolean replaceAll) {
        try {
            // Get the entire request as a string
            String fullRequest = request.toString();

            // Apply regex replacement to the entire string
            String modifiedRequestString = performRegexReplacement(fullRequest, regex, group, value, replaceAll);

            // If no replacement was made, return original
            if (modifiedRequestString.equals(fullRequest)) {
                return request;
            }

            // Update Content-Length header if body was modified
            modifiedRequestString = updateContentLength(modifiedRequestString);

            // Rebuild the request from the modified string using Montoya API
            // IMPORTANT: Preserve the original httpService so Burp knows where to send the request
            return HttpRequest.httpRequest(request.httpService(), modifiedRequestString);
        } catch (Exception e) {
            logging.logToError("Error applying regex to request: " + e.getMessage());
            e.printStackTrace();
            return request;
        }
    }

    private HttpResponse applyRegexToResponse(HttpResponse response, String regex, int group, String value, boolean replaceAll) {
        try {
            // Get the entire response as a string
            String fullResponse = response.toString();

            // Apply regex replacement to the entire string
            String modifiedResponseString = performRegexReplacement(fullResponse, regex, group, value, replaceAll);

            // If no replacement was made, return original
            if (modifiedResponseString.equals(fullResponse)) {
                return response;
            }

            // Update Content-Length header if body was modified
            modifiedResponseString = updateContentLength(modifiedResponseString);

            // Rebuild the response from the modified string using Montoya API
            return HttpResponse.httpResponse(modifiedResponseString);
        } catch (Exception e) {
            logging.logToError("Error applying regex to response: " + e.getMessage());
            e.printStackTrace();
            return response;
        }
    }

    /**
     * Collect tokens from content for a specific bucket only.
     * This is used by parseProxyHistory to target a single bucket.
     *
     * @param bypassRestrictions If true, bypasses bucket enabled and tool enabled checks
     */
    private void collectTokensForBucket(Bucket bucket, String content, String url, burp.api.montoya.core.ToolType toolType, boolean isRequest, boolean bypassRestrictions) {
        // Check if bucket is enabled (unless bypassing restrictions)
        if (!bypassRestrictions && !bucket.isEnabled()) return;

        CollectionRule rule = bucket.getCollectionRule();

        // Check if this bucket is configured to collect from this type
        if (isRequest && !rule.isCollectFromRequests()) return;
        if (!isRequest && !rule.isCollectFromResponses()) return;

        // Bucket must explicitly enable the tool (unless bypassing restrictions)
        if (!bypassRestrictions && !rule.getEnabledTools().contains(toolType)) {
            return;
        }

        // Check URL matchers - always act as allow-list
        // At least one of: in-scope match OR URL matcher match required
        boolean urlMatches = false;

        // Check in-scope first if enabled
        if (rule.isMatchInScopeUrls() && api.scope().isInScope(url)) {
            urlMatches = true;
        }

        // If not matched yet, check URL matchers
        if (!urlMatches && !rule.getUrlMatchers().isEmpty()) {
            for (UrlMatcher matcher : rule.getUrlMatchers()) {
                if (matcher.isEnabled() && matcher.matches(url)) {
                    urlMatches = true;
                    break;
                }
            }
        }

        // If no match found, skip
        if (!urlMatches) {
            return;
        }

        // Try each regex pattern
        for (model.RegexPattern regexPattern : rule.getRegexPatterns()) {
            // Skip disabled patterns
            if (!regexPattern.isEnabled()) {
                continue;
            }

            // Check if pattern should match this message type
            if (isRequest && !regexPattern.isMatchRequests()) {
                continue;
            }
            if (!isRequest && !regexPattern.isMatchResponses()) {
                continue;
            }

            try {
                // Build flags based on pattern settings
                int flags = 0;
                if (regexPattern.isDotallMode()) {
                    flags |= Pattern.DOTALL;
                }
                if (regexPattern.isMultilineMode()) {
                    flags |= Pattern.MULTILINE;
                }

                Pattern pattern = Pattern.compile(regexPattern.getPattern(), flags);
                Matcher matcher = pattern.matcher(content);

                while (matcher.find()) {
                    // Extract the first capturing group, or the whole match if no groups
                    String token = matcher.groupCount() > 0 ? matcher.group(1) : matcher.group(0);

                    // Apply pattern-specific post-processing script first
                    if (regexPattern.getPostProcessingScript() != null && !regexPattern.getPostProcessingScript().isEmpty()) {
                        token = jsProcessor.processToken(token, regexPattern.getPostProcessingScript());
                    }

                    // Then apply collection-level post-processing script
                    if (rule.getPostProcessingScript() != null && !rule.getPostProcessingScript().isEmpty()) {
                        token = jsProcessor.processToken(token, rule.getPostProcessingScript());
                    }

                    bucket.addToken(token);
                }
            } catch (Exception e) {
                logging.logToError("Error processing regex for bucket " + bucket.getName() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Parse proxy history and collect tokens for a specific bucket.
     *
     * @param bucket The bucket to collect tokens for
     * @param maxItems Maximum number of history items to process (0 or negative = unlimited)
     * @param newestFirst If true, process newest items first; if false, process oldest first
     * @param progressCallback Callback to report progress (current item number)
     * @param cancelCheck Function to check if operation should be cancelled
     * @param pauseCheck Function to check if operation should be paused
     * @return Number of tokens collected
     */
    public int parseProxyHistory(Bucket bucket, int maxItems, boolean newestFirst,
                                  java.util.function.Consumer<Integer> progressCallback,
                                  java.util.function.BooleanSupplier cancelCheck,
                                  java.util.function.BooleanSupplier pauseCheck) {
        int initialTokenCount = bucket.getTokenCount();

        try {
            // Get all proxy history items
            java.util.List<burp.api.montoya.proxy.ProxyHttpRequestResponse> historyItems = api.proxy().history();

            // Determine items to process
            int itemsToProcess = (maxItems > 0) ? Math.min(maxItems, historyItems.size()) : historyItems.size();

            // Create list in the desired order
            java.util.List<burp.api.montoya.proxy.ProxyHttpRequestResponse> itemsToScan = new java.util.ArrayList<>();
            if (newestFirst) {
                // Take from end of list (newest)
                for (int i = historyItems.size() - 1; i >= Math.max(0, historyItems.size() - itemsToProcess); i--) {
                    itemsToScan.add(historyItems.get(i));
                }
            } else {
                // Take from beginning of list (oldest)
                for (int i = 0; i < itemsToProcess; i++) {
                    itemsToScan.add(historyItems.get(i));
                }
            }

            // Process each item
            for (int i = 0; i < itemsToScan.size(); i++) {
                // Check for pause
                while (pauseCheck != null && pauseCheck.getAsBoolean()) {
                    // Sleep briefly while paused to avoid busy-waiting
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    // Check for cancellation during pause
                    if (cancelCheck != null && cancelCheck.getAsBoolean()) {
                        logging.logToOutput("Proxy history parsing cancelled by user");
                        return bucket.getTokenCount() - initialTokenCount;
                    }
                }

                // Check for cancellation
                if (cancelCheck != null && cancelCheck.getAsBoolean()) {
                    logging.logToOutput("Proxy history parsing cancelled by user");
                    break;
                }

                // Report progress
                if (progressCallback != null) {
                    progressCallback.accept(i + 1);
                }

                burp.api.montoya.proxy.ProxyHttpRequestResponse item = itemsToScan.get(i);
                HttpRequest request = item.finalRequest();
                HttpResponse response = item.originalResponse();
                String url = request.url();

                // Process request if bucket collects from requests
                // Pass true for bypassRestrictions to ignore bucket enabled and tool enabled checks
                if (bucket.getCollectionRule().isCollectFromRequests()) {
                    collectTokensForBucket(bucket, request.toString(), url, burp.api.montoya.core.ToolType.PROXY, true, true);
                }

                // Process response if bucket collects from responses and response exists
                // Pass true for bypassRestrictions to ignore bucket enabled and tool enabled checks
                if (response != null && bucket.getCollectionRule().isCollectFromResponses()) {
                    collectTokensForBucket(bucket, response.toString(), url, burp.api.montoya.core.ToolType.PROXY, false, true);
                }
            }

            return bucket.getTokenCount() - initialTokenCount;

        } catch (Exception e) {
            logging.logToError("Error parsing proxy history: " + e.getMessage());
            e.printStackTrace();
            return bucket.getTokenCount() - initialTokenCount;
        }
    }
}
