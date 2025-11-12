package util;

import burp.api.montoya.core.ToolType;

import java.lang.reflect.Field;

/**
 * Utility class to detect which edition of Burp Suite is being used.
 * Burp Suite Community Edition does not include the BURP_AI tool type,
 * which is only available in Burp Suite Professional.
 */
public class BurpEditionDetector {
    private static Boolean burpAiAvailable = null;

    /**
     * Checks if the BURP_AI ToolType is available in the current Burp Suite edition.
     * This method uses reflection to safely check for the field without causing
     * a NoSuchFieldError.
     *
     * @return true if BURP_AI is available (Pro edition), false otherwise (Community edition)
     */
    public static boolean isBurpAiAvailable() {
        if (burpAiAvailable == null) {
            try {
                // Try to access the BURP_AI field using reflection
                Field field = ToolType.class.getField("BURP_AI");
                burpAiAvailable = true;
            } catch (NoSuchFieldException e) {
                // BURP_AI field doesn't exist - Community Edition
                burpAiAvailable = false;
            }
        }
        return burpAiAvailable;
    }

    /**
     * Gets the BURP_AI ToolType if available, or null if not available.
     *
     * @return ToolType.BURP_AI if available, null otherwise
     */
    public static ToolType getBurpAiToolType() {
        if (!isBurpAiAvailable()) {
            return null;
        }
        try {
            Field field = ToolType.class.getField("BURP_AI");
            return (ToolType) field.get(null);
        } catch (Exception e) {
            return null;
        }
    }
}
