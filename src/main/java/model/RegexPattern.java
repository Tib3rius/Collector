package model;

import java.io.Serializable;

public class RegexPattern implements Serializable {
    private String pattern;
    private String comment;
    private String postProcessingScript;
    private boolean enabled;
    private boolean dotallMode;
    private boolean multilineMode;
    private boolean matchRequests;
    private boolean matchResponses;

    public RegexPattern() {
        this.pattern = "";
        this.comment = "";
        this.postProcessingScript = "";
        this.enabled = true;
        this.dotallMode = false;
        this.multilineMode = false;
        this.matchRequests = true;
        this.matchResponses = true;
    }

    public RegexPattern(String pattern, String comment) {
        this.pattern = pattern;
        this.comment = comment;
        this.postProcessingScript = "";
        this.enabled = true;
        this.dotallMode = false;
        this.multilineMode = false;
        this.matchRequests = true;
        this.matchResponses = true;
    }

    @Override
    public String toString() {
        if (comment != null && !comment.isEmpty()) {
            return pattern + " (" + comment + ")";
        }
        return pattern;
    }

    // Getters and setters
    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public String getPostProcessingScript() { return postProcessingScript; }
    public void setPostProcessingScript(String postProcessingScript) { this.postProcessingScript = postProcessingScript; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isDotallMode() { return dotallMode; }
    public void setDotallMode(boolean dotallMode) { this.dotallMode = dotallMode; }

    public boolean isMultilineMode() { return multilineMode; }
    public void setMultilineMode(boolean multilineMode) { this.multilineMode = multilineMode; }

    public boolean isMatchRequests() { return matchRequests; }
    public void setMatchRequests(boolean matchRequests) { this.matchRequests = matchRequests; }

    public boolean isMatchResponses() { return matchResponses; }
    public void setMatchResponses(boolean matchResponses) { this.matchResponses = matchResponses; }
}
