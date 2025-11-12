package model;

import burp.api.montoya.core.ToolType;
import util.BurpEditionDetector;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class GlobalControls implements Serializable {
    private boolean bucketsEnabled;
    private Set<ToolType> collectionEnabledTools;
    private Set<ToolType> replacementEnabledTools;

    public GlobalControls() {
        this.bucketsEnabled = true; // Enabled by default
        this.collectionEnabledTools = new HashSet<>();
        this.replacementEnabledTools = new HashSet<>();

        // Enable common tools by default
        collectionEnabledTools.add(ToolType.TARGET);
        collectionEnabledTools.add(ToolType.PROXY);
        collectionEnabledTools.add(ToolType.INTRUDER);
        collectionEnabledTools.add(ToolType.REPEATER);
        collectionEnabledTools.add(ToolType.SCANNER);
        collectionEnabledTools.add(ToolType.SEQUENCER);
        collectionEnabledTools.add(ToolType.EXTENSIONS);

        // Only add BURP_AI if it's available (Pro edition only)
        if (BurpEditionDetector.isBurpAiAvailable()) {
            ToolType burpAi = BurpEditionDetector.getBurpAiToolType();
            if (burpAi != null) {
                collectionEnabledTools.add(burpAi);
            }
        }

        replacementEnabledTools.add(ToolType.TARGET);
        replacementEnabledTools.add(ToolType.PROXY);
        replacementEnabledTools.add(ToolType.INTRUDER);
        replacementEnabledTools.add(ToolType.REPEATER);
        replacementEnabledTools.add(ToolType.SCANNER);
        replacementEnabledTools.add(ToolType.SEQUENCER);
        replacementEnabledTools.add(ToolType.EXTENSIONS);

        // Only add BURP_AI if it's available (Pro edition only)
        if (BurpEditionDetector.isBurpAiAvailable()) {
            ToolType burpAi = BurpEditionDetector.getBurpAiToolType();
            if (burpAi != null) {
                replacementEnabledTools.add(burpAi);
            }
        }
    }

    public Set<ToolType> getCollectionEnabledTools() {
        return collectionEnabledTools;
    }

    public void setCollectionEnabledTools(Set<ToolType> collectionEnabledTools) {
        this.collectionEnabledTools = collectionEnabledTools;
    }

    public Set<ToolType> getReplacementEnabledTools() {
        return replacementEnabledTools;
    }

    public void setReplacementEnabledTools(Set<ToolType> replacementEnabledTools) {
        this.replacementEnabledTools = replacementEnabledTools;
    }

    public boolean isCollectionEnabledForTool(ToolType tool) {
        return collectionEnabledTools.contains(tool);
    }

    public boolean isReplacementEnabledForTool(ToolType tool) {
        return replacementEnabledTools.contains(tool);
    }

    public boolean isBucketsEnabled() {
        return bucketsEnabled;
    }

    public void setBucketsEnabled(boolean bucketsEnabled) {
        this.bucketsEnabled = bucketsEnabled;
    }
}
