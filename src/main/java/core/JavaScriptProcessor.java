package core;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.logging.Logging;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;

public class JavaScriptProcessor {
    private final Logging logging;
    private final ContextFactory contextFactory;
    private final MontoyaApi api;

    public JavaScriptProcessor(Logging logging, MontoyaApi api) {
        this.logging = logging;
        this.contextFactory = new ContextFactory();
        this.api = api;
    }

    /**
     * Process a token with a JavaScript script, catching exceptions and logging them.
     * This method is intended for runtime use where errors should not interrupt execution.
     *
     * @param token The token value to process
     * @param script The JavaScript code to execute
     * @return The processed token, or the original token if script is empty or an error occurs
     */
    public String processToken(String token, String script) {
        try {
            return processTokenWithException(token, script);
        } catch (Exception e) {
            logging.logToError("JavaScript processing error: " + e.getMessage());
            return token;
        }
    }

    /**
     * Process a token with a JavaScript script, throwing exceptions on error.
     * This method is intended for UI/preview use where errors should be displayed to the user.
     *
     * @param token The token value to process
     * @param script The JavaScript code to execute
     * @return The processed token, or the original token if script is empty
     * @throws Exception If script execution fails
     */
    public String processTokenWithException(String token, String script) throws Exception {
        if (script == null || script.trim().isEmpty()) {
            return token;
        }

        return executeScript(token, script);
    }

    /**
     * Core script execution logic shared by both public methods.
     */
    private String executeScript(String token, String script) throws Exception {
        Context cx = contextFactory.enterContext();
        try {
            // Set optimization level for better compatibility
            cx.setOptimizationLevel(-1);

            // Create a scope for the script execution
            Scriptable scope = cx.initStandardObjects();

            // Remove Java packages.
            scope.delete("Packages");
            scope.delete("java");
            scope.delete("javax");
            scope.delete("org");
            scope.delete("com");

            // Make the token available to the script
            scope.put("token", scope, token);

            // Make utilities available as a function
            String utilitiesFunction = "function utilities() { return __utilities; }";
            scope.put("__utilities", scope, api.utilities());
            cx.evaluateString(scope, utilitiesFunction, "utilitiesFunction", 1, null);

            // Make logging available as a function
            String loggingFunction = "function logging() { return __logging; }";
            scope.put("__logging", scope, api.logging());
            cx.evaluateString(scope, loggingFunction, "loggingFunction", 1, null);

            // Wrap the script in a function to allow return statements
            String wrappedScript = "(function() { " + script + " })()";

            // Execute the script - expect it to return the modified token
            Object result = cx.evaluateString(scope, wrappedScript, "JavaScriptProcessor", 1, null);

            if (result != null && result != org.mozilla.javascript.Undefined.instance) {
                return Context.toString(result);
            }

            return token;
        } finally {
            Context.exit();
        }
    }
}
