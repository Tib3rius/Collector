package util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

public class TokenEncoder {

    public enum EncodingType {
        BASE64("Base64"),
        URL_ENCODING("URL Encoding"),
        ESCAPE_ENCODING("Escape Encoding (\\n, \\r)");

        private final String displayName;

        EncodingType(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    /**
     * Check if a token contains newline characters (\r or \n)
     */
    public static boolean containsNewline(String token) {
        return token != null && (token.contains("\n") || token.contains("\r"));
    }

    /**
     * Check if any token in the list contains newline characters
     */
    public static boolean hasTokensWithNewlines(List<String> tokens) {
        return tokens.stream().anyMatch(TokenEncoder::containsNewline);
    }

    /**
     * Encode a token using the specified encoding type
     */
    public static String encode(String token, EncodingType encodingType) {
        if (token == null) {
            return null;
        }

        switch (encodingType) {
            case BASE64:
                return Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));

            case URL_ENCODING:
                // Encode only characters that cause problems in newline-delimited files
                // Encode % first to avoid double-encoding the percent signs we add
                return token
                    .replace("%", "%25")   // Percent sign (must be first!)
                    .replace("\r", "%0D")  // Carriage return
                    .replace("\n", "%0A")  // Newline
                    .replace("\t", "%09"); // Tab

            case ESCAPE_ENCODING:
                // Escape backslashes first, then escape special characters
                // This ensures \n becomes \\n (literal backslash-n) when saved to file
                return token
                    .replace("\\", "\\\\")  // Backslash -> double backslash (must be first!)
                    .replace("\r", "\\r")   // Carriage return -> \r
                    .replace("\n", "\\n")   // Newline -> \n
                    .replace("\t", "\\t");  // Tab -> \t

            default:
                return token;
        }
    }

    /**
     * Encode all tokens in a list using the specified encoding type
     */
    public static List<String> encodeAll(List<String> tokens, EncodingType encodingType) {
        return tokens.stream()
                .map(token -> encode(token, encodingType))
                .collect(Collectors.toList());
    }

    /**
     * Filter out tokens that contain newlines
     */
    public static List<String> removeTokensWithNewlines(List<String> tokens) {
        return tokens.stream()
                .filter(token -> !containsNewline(token))
                .collect(Collectors.toList());
    }

    /**
     * Decode a token using the specified encoding type
     */
    public static String decode(String token, EncodingType encodingType) {
        if (token == null) {
            return null;
        }

        switch (encodingType) {
            case BASE64:
                try {
                    byte[] decoded = Base64.getDecoder().decode(token);
                    return new String(decoded, StandardCharsets.UTF_8);
                } catch (IllegalArgumentException e) {
                    // If decoding fails, return original token
                    return token;
                }

            case URL_ENCODING:
                // Decode in reverse order of encoding
                // Decode special characters first, then percent sign last
                return token
                    .replace("%09", "\t")   // Tab
                    .replace("%0A", "\n")   // Newline
                    .replace("%0D", "\r")   // Carriage return
                    .replace("%25", "%");   // Percent sign (must be last!)

            case ESCAPE_ENCODING:
                // Decode in reverse order of encoding
                // Decode special characters first, then backslash last
                return token
                    .replace("\\t", "\t")   // Tab
                    .replace("\\n", "\n")   // Newline
                    .replace("\\r", "\r")   // Carriage return
                    .replace("\\\\", "\\"); // Backslash (must be last!)

            default:
                return token;
        }
    }

    /**
     * Decode all tokens in a list using the specified encoding type
     */
    public static List<String> decodeAll(List<String> tokens, EncodingType encodingType) {
        return tokens.stream()
                .map(token -> decode(token, encodingType))
                .collect(Collectors.toList());
    }
}
