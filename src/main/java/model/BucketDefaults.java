package model;

import burp.api.montoya.core.ToolType;
import util.BurpEditionDetector;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class BucketDefaults implements Serializable {
    // Bucket Configuration defaults
    private boolean enabled;
    private BucketType bucketType;
    private int maxSize;
    private BucketFullBehavior fullBehavior;
    private boolean uniqueOnly;

    // Collection defaults
    private boolean collectFromRequests;
    private boolean collectFromResponses;
    private Set<ToolType> collectionEnabledTools;
    private boolean collectionMatchInScopeUrls;

    // Replacement defaults
    private boolean replaceInRequests;
    private boolean replaceInResponses;
    private Set<ToolType> replacementEnabledTools;
    private LastTokenBehavior lastTokenBehavior;
    private EmptyBucketBehavior emptyBucketBehavior;
    private String staticValue;
    private String generatorRegex;
    private boolean replacementMatchInScopeUrls;

    public BucketDefaults() {
        // Default values
        this.enabled = true;
        this.bucketType = BucketType.FIFO;
        this.maxSize = -1;
        this.fullBehavior = BucketFullBehavior.REJECT_NEW;
        this.uniqueOnly = true;

        this.collectFromRequests = true;
        this.collectFromResponses = true;
        this.collectionEnabledTools = new HashSet<>();
        // Enable all common tools by default
        this.collectionEnabledTools.add(ToolType.TARGET);
        this.collectionEnabledTools.add(ToolType.PROXY);
        this.collectionEnabledTools.add(ToolType.INTRUDER);
        this.collectionEnabledTools.add(ToolType.REPEATER);
        this.collectionEnabledTools.add(ToolType.SCANNER);
        this.collectionEnabledTools.add(ToolType.SEQUENCER);
        this.collectionEnabledTools.add(ToolType.EXTENSIONS);

        // Only add BURP_AI if it's available (Pro edition only)
        if (BurpEditionDetector.isBurpAiAvailable()) {
            ToolType burpAi = BurpEditionDetector.getBurpAiToolType();
            if (burpAi != null) {
                this.collectionEnabledTools.add(burpAi);
            }
        }
        this.collectionMatchInScopeUrls = true;

        this.replaceInRequests = true;
        this.replaceInResponses = true;
        this.replacementEnabledTools = new HashSet<>();
        // Enable all common tools by default
        this.replacementEnabledTools.add(ToolType.TARGET);
        this.replacementEnabledTools.add(ToolType.PROXY);
        this.replacementEnabledTools.add(ToolType.INTRUDER);
        this.replacementEnabledTools.add(ToolType.REPEATER);
        this.replacementEnabledTools.add(ToolType.SCANNER);
        this.replacementEnabledTools.add(ToolType.SEQUENCER);
        this.replacementEnabledTools.add(ToolType.EXTENSIONS);

        // Only add BURP_AI if it's available (Pro edition only)
        if (BurpEditionDetector.isBurpAiAvailable()) {
            ToolType burpAi = BurpEditionDetector.getBurpAiToolType();
            if (burpAi != null) {
                this.replacementEnabledTools.add(burpAi);
            }
        }
        this.lastTokenBehavior = LastTokenBehavior.KEEP_IN_BUCKET;
        this.emptyBucketBehavior = EmptyBucketBehavior.DO_NOTHING;
        this.staticValue = "";
        this.generatorRegex = "";
        this.replacementMatchInScopeUrls = true;
    }

    // Ensure sets are initialized after deserialization
    public void ensureInitialized() {
        if (collectionEnabledTools == null) {
            collectionEnabledTools = new HashSet<>();
        }
        if (replacementEnabledTools == null) {
            replacementEnabledTools = new HashSet<>();
        }
        if (staticValue == null) {
            staticValue = "";
        }
        if (generatorRegex == null) {
            generatorRegex = "";
        }
    }

    // Apply defaults to a bucket
    public void applyToBucket(Bucket bucket) {
        bucket.setEnabled(enabled);
        bucket.setBucketType(bucketType);
        bucket.setMaxSize(maxSize);
        bucket.setFullBehavior(fullBehavior);
        bucket.setUniqueOnly(uniqueOnly);

        CollectionRule collectionRule = bucket.getCollectionRule();
        collectionRule.setCollectFromRequests(collectFromRequests);
        collectionRule.setCollectFromResponses(collectFromResponses);
        collectionRule.getEnabledTools().clear();
        collectionRule.getEnabledTools().addAll(collectionEnabledTools);
        collectionRule.setMatchInScopeUrls(collectionMatchInScopeUrls);

        ReplacementConfig replacementConfig = bucket.getReplacementConfig();
        replacementConfig.setReplaceInRequests(replaceInRequests);
        replacementConfig.setReplaceInResponses(replaceInResponses);
        replacementConfig.getEnabledTools().clear();
        replacementConfig.getEnabledTools().addAll(replacementEnabledTools);
        replacementConfig.setLastTokenBehavior(lastTokenBehavior);
        replacementConfig.setEmptyBucketBehavior(emptyBucketBehavior);
        replacementConfig.setStaticValue(staticValue);
        replacementConfig.setGeneratorRegex(generatorRegex);
        replacementConfig.setMatchInScopeUrls(replacementMatchInScopeUrls);
    }

    // Getters and setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public BucketType getBucketType() { return bucketType; }
    public void setBucketType(BucketType bucketType) { this.bucketType = bucketType; }

    public int getMaxSize() { return maxSize; }
    public void setMaxSize(int maxSize) { this.maxSize = maxSize; }

    public BucketFullBehavior getFullBehavior() { return fullBehavior; }
    public void setFullBehavior(BucketFullBehavior fullBehavior) { this.fullBehavior = fullBehavior; }

    public boolean isUniqueOnly() { return uniqueOnly; }
    public void setUniqueOnly(boolean uniqueOnly) { this.uniqueOnly = uniqueOnly; }

    public boolean isCollectFromRequests() { return collectFromRequests; }
    public void setCollectFromRequests(boolean collectFromRequests) { this.collectFromRequests = collectFromRequests; }

    public boolean isCollectFromResponses() { return collectFromResponses; }
    public void setCollectFromResponses(boolean collectFromResponses) { this.collectFromResponses = collectFromResponses; }

    public Set<ToolType> getCollectionEnabledTools() { return collectionEnabledTools; }
    public void setCollectionEnabledTools(Set<ToolType> collectionEnabledTools) { this.collectionEnabledTools = collectionEnabledTools; }

    public boolean isCollectionMatchInScopeUrls() { return collectionMatchInScopeUrls; }
    public void setCollectionMatchInScopeUrls(boolean collectionMatchInScopeUrls) { this.collectionMatchInScopeUrls = collectionMatchInScopeUrls; }

    public boolean isReplaceInRequests() { return replaceInRequests; }
    public void setReplaceInRequests(boolean replaceInRequests) { this.replaceInRequests = replaceInRequests; }

    public boolean isReplaceInResponses() { return replaceInResponses; }
    public void setReplaceInResponses(boolean replaceInResponses) { this.replaceInResponses = replaceInResponses; }

    public Set<ToolType> getReplacementEnabledTools() { return replacementEnabledTools; }
    public void setReplacementEnabledTools(Set<ToolType> replacementEnabledTools) { this.replacementEnabledTools = replacementEnabledTools; }

    public LastTokenBehavior getLastTokenBehavior() { return lastTokenBehavior; }
    public void setLastTokenBehavior(LastTokenBehavior lastTokenBehavior) { this.lastTokenBehavior = lastTokenBehavior; }

    public EmptyBucketBehavior getEmptyBucketBehavior() { return emptyBucketBehavior; }
    public void setEmptyBucketBehavior(EmptyBucketBehavior emptyBucketBehavior) { this.emptyBucketBehavior = emptyBucketBehavior; }

    public String getStaticValue() { return staticValue; }
    public void setStaticValue(String staticValue) { this.staticValue = staticValue; }

    public String getGeneratorRegex() { return generatorRegex; }
    public void setGeneratorRegex(String generatorRegex) { this.generatorRegex = generatorRegex; }

    public boolean isReplacementMatchInScopeUrls() { return replacementMatchInScopeUrls; }
    public void setReplacementMatchInScopeUrls(boolean replacementMatchInScopeUrls) { this.replacementMatchInScopeUrls = replacementMatchInScopeUrls; }
}
