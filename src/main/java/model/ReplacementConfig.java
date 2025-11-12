package model;

import burp.api.montoya.core.ToolType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ReplacementConfig implements Serializable {
    private boolean replaceInRequests;
    private boolean replaceInResponses;
    private Set<ToolType> enabledTools;
    private boolean matchInScopeUrls;
    private List<UrlMatcher> urlMatchers;
    private List<ReplacementRule> replacementRules;
    private LastTokenBehavior lastTokenBehavior;
    private EmptyBucketBehavior emptyBucketBehavior;
    private String staticValue;
    private String generatorRegex;
    private String preReplacementScript;

    public ReplacementConfig() {
        this.replaceInRequests = false;
        this.replaceInResponses = false;
        this.enabledTools = new HashSet<>();
        this.matchInScopeUrls = false;
        this.urlMatchers = new ArrayList<>();
        this.replacementRules = new ArrayList<>();
        this.lastTokenBehavior = LastTokenBehavior.KEEP_IN_BUCKET;
        this.emptyBucketBehavior = EmptyBucketBehavior.DO_NOTHING;
        this.staticValue = "";
        this.generatorRegex = "";
        this.preReplacementScript = "";
    }

    // Getters and setters
    public boolean isReplaceInRequests() { return replaceInRequests; }
    public void setReplaceInRequests(boolean replaceInRequests) { this.replaceInRequests = replaceInRequests; }

    public boolean isReplaceInResponses() { return replaceInResponses; }
    public void setReplaceInResponses(boolean replaceInResponses) { this.replaceInResponses = replaceInResponses; }

    public Set<ToolType> getEnabledTools() { return enabledTools; }
    public void setEnabledTools(Set<ToolType> enabledTools) { this.enabledTools = enabledTools; }

    public boolean isMatchInScopeUrls() { return matchInScopeUrls; }
    public void setMatchInScopeUrls(boolean matchInScopeUrls) { this.matchInScopeUrls = matchInScopeUrls; }

    public List<UrlMatcher> getUrlMatchers() { return urlMatchers; }
    public void setUrlMatchers(List<UrlMatcher> urlMatchers) { this.urlMatchers = urlMatchers; }

    public List<ReplacementRule> getReplacementRules() { return replacementRules; }
    public void setReplacementRules(List<ReplacementRule> replacementRules) { this.replacementRules = replacementRules; }

    public LastTokenBehavior getLastTokenBehavior() { return lastTokenBehavior; }
    public void setLastTokenBehavior(LastTokenBehavior lastTokenBehavior) { this.lastTokenBehavior = lastTokenBehavior; }

    public EmptyBucketBehavior getEmptyBucketBehavior() { return emptyBucketBehavior; }
    public void setEmptyBucketBehavior(EmptyBucketBehavior emptyBucketBehavior) { this.emptyBucketBehavior = emptyBucketBehavior; }

    public String getStaticValue() { return staticValue; }
    public void setStaticValue(String staticValue) { this.staticValue = staticValue; }

    public String getGeneratorRegex() { return generatorRegex; }
    public void setGeneratorRegex(String generatorRegex) { this.generatorRegex = generatorRegex; }

    public String getPreReplacementScript() { return preReplacementScript; }
    public void setPreReplacementScript(String preReplacementScript) { this.preReplacementScript = preReplacementScript; }
}
