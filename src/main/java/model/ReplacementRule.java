package model;

import java.io.Serializable;

public class ReplacementRule implements Serializable {
    private ReplacementLocation location;
    private String fieldName; // For Header, URL Param, Body Param, Cookie
    private String regexPattern; // For Generic Regex
    private int regexGroup; // Which group to replace in regex
    private boolean replaceAll; // true = replaceAll, false = replaceFirst
    private String preProcessingScript;
    private boolean enabled;
    private boolean applyToRequests;
    private boolean applyToResponses;

    public ReplacementRule() {
        this.location = ReplacementLocation.HEADER;
        this.fieldName = "";
        this.regexPattern = "";
        this.regexGroup = 1;
        this.replaceAll = false;
        this.preProcessingScript = "";
        this.enabled = true;
        this.applyToRequests = true;
        this.applyToResponses = true;
    }

    public ReplacementRule(ReplacementLocation location, String fieldName, String regexPattern, int regexGroup, boolean replaceAll, String preProcessingScript) {
        this.location = location;
        this.fieldName = fieldName;
        this.regexPattern = regexPattern;
        this.regexGroup = regexGroup;
        this.replaceAll = replaceAll;
        this.preProcessingScript = preProcessingScript;
        this.enabled = true;
        this.applyToRequests = true;
        this.applyToResponses = true;
    }

    // Getters and setters
    public ReplacementLocation getLocation() { return location; }
    public void setLocation(ReplacementLocation location) { this.location = location; }

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public String getRegexPattern() { return regexPattern; }
    public void setRegexPattern(String regexPattern) { this.regexPattern = regexPattern; }

    public int getRegexGroup() { return regexGroup; }
    public void setRegexGroup(int regexGroup) { this.regexGroup = regexGroup; }

    public boolean isReplaceAll() { return replaceAll; }
    public void setReplaceAll(boolean replaceAll) { this.replaceAll = replaceAll; }

    public String getPreProcessingScript() { return preProcessingScript; }
    public void setPreProcessingScript(String preProcessingScript) { this.preProcessingScript = preProcessingScript; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isApplyToRequests() { return applyToRequests; }
    public void setApplyToRequests(boolean applyToRequests) { this.applyToRequests = applyToRequests; }

    public boolean isApplyToResponses() { return applyToResponses; }
    public void setApplyToResponses(boolean applyToResponses) { this.applyToResponses = applyToResponses; }
}
