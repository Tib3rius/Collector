package model;

import burp.api.montoya.core.ToolType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CollectionRule implements Serializable {
    private boolean collectFromRequests;
    private boolean collectFromResponses;
    private Set<ToolType> enabledTools;
    private boolean matchInScopeUrls;
    private List<UrlMatcher> urlMatchers;
    private List<RegexPattern> regexPatterns;
    private String postProcessingScript;

    public CollectionRule() {
        this.collectFromRequests = false;
        this.collectFromResponses = false;
        this.enabledTools = new HashSet<>();
        this.matchInScopeUrls = false;
        this.urlMatchers = new ArrayList<>();
        this.regexPatterns = new ArrayList<>();
        this.postProcessingScript = "";
    }

    // Getters and setters
    public boolean isCollectFromRequests() { return collectFromRequests; }
    public void setCollectFromRequests(boolean collectFromRequests) { this.collectFromRequests = collectFromRequests; }

    public boolean isCollectFromResponses() { return collectFromResponses; }
    public void setCollectFromResponses(boolean collectFromResponses) { this.collectFromResponses = collectFromResponses; }

    public Set<ToolType> getEnabledTools() { return enabledTools; }
    public void setEnabledTools(Set<ToolType> enabledTools) { this.enabledTools = enabledTools; }

    public boolean isMatchInScopeUrls() { return matchInScopeUrls; }
    public void setMatchInScopeUrls(boolean matchInScopeUrls) { this.matchInScopeUrls = matchInScopeUrls; }

    public List<UrlMatcher> getUrlMatchers() { return urlMatchers; }
    public void setUrlMatchers(List<UrlMatcher> urlMatchers) { this.urlMatchers = urlMatchers; }

    public List<RegexPattern> getRegexPatterns() { return regexPatterns; }
    public void setRegexPatterns(List<RegexPattern> regexPatterns) { this.regexPatterns = regexPatterns; }

    public String getPostProcessingScript() { return postProcessingScript; }
    public void setPostProcessingScript(String postProcessingScript) { this.postProcessingScript = postProcessingScript; }
}
