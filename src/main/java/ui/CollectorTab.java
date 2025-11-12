package ui;

import burp.api.montoya.MontoyaApi;
import core.BucketManager;
import core.PersistenceManager;
import model.*;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CollectorTab extends JPanel {
    private final BucketManager bucketManager;
    private final BucketDefaults bucketDefaults;
    private final JTabbedPane tabbedPane;
    private final SettingsTab settingsTab;
    private final Map<Bucket, BucketTab> bucketTabs;
    private final Runnable onSaveCallback;
    private final MontoyaApi api;
    private PersistenceManager persistenceManager;
    private boolean isAddingBucket = false;
    private boolean isRemovingBucket = false;
    private boolean isReorderingTabs = false;

    public CollectorTab(BucketManager bucketManager, BucketDefaults bucketDefaults, Runnable onSaveCallback, MontoyaApi api) {
        this.bucketManager = bucketManager;
        this.bucketDefaults = bucketDefaults;
        this.bucketTabs = new HashMap<>();
        this.onSaveCallback = onSaveCallback;
        this.api = api;

        setLayout(new BorderLayout());

        // Create tabbed pane
        tabbedPane = new JTabbedPane();

        // Add mouse listener for double-click and right-click on tabs
        tabbedPane.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int tabIndex = tabbedPane.indexAtLocation(e.getX(), e.getY());

                if (tabIndex == -1) return; // Not on a tab

                if (SwingUtilities.isRightMouseButton(e)) {
                    // Right-click - show context menu
                    if (tabIndex > 0 && tabIndex < tabbedPane.getTabCount() - 1) { // Only for bucket tabs
                        showTabContextMenu(e, tabIndex);
                    }
                } else if (e.getClickCount() == 2) {
                    // Double-click - rename tab
                    if (tabIndex > 0 && tabIndex < tabbedPane.getTabCount() - 1) { // Don't allow renaming Global Controls or "+" tab
                        renameBucketTab(tabIndex);
                    }
                }
            }
        });

        // Add mouse motion listener to initiate drag for tab reordering
        tabbedPane.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseDragged(java.awt.event.MouseEvent e) {
                int tabIndex = tabbedPane.indexAtLocation(e.getX(), e.getY());
                // Only allow dragging bucket tabs (not Global Controls or "+")
                if (tabIndex > 0 && tabIndex < tabbedPane.getTabCount() - 1) {
                    tabbedPane.getTransferHandler().exportAsDrag(tabbedPane, e, javax.swing.TransferHandler.MOVE);
                }
            }
        });

        // Add change listener to detect when "+" tab is clicked
        tabbedPane.addChangeListener(e -> {
            int selectedIndex = tabbedPane.getSelectedIndex();
            int plusTabIndex = tabbedPane.getTabCount() - 1;

            // Only trigger if we're on the "+" tab and not currently adding, removing, or reordering
            if (selectedIndex == plusTabIndex && !isAddingBucket && !isRemovingBucket && !isReorderingTabs && plusTabIndex > 0) {
                addNewBucket();
            }
        });

        // Enable tab reordering via drag and drop
        tabbedPane.setTransferHandler(new TabTransferHandler());

        // Add Settings tab
        settingsTab = new SettingsTab(bucketManager.getGlobalControls(), bucketDefaults, onSaveCallback, this::exportToJson, this::importFromJson, this::resetToDefaults, this::saveAllTokens);
        tabbedPane.addTab("Settings", settingsTab);

        // Register listener to update bucket tabs when global controls change
        settingsTab.addGlobalControlsChangeListener(this::updateAllBucketTabsFromGlobalControls);

        // Add existing buckets
        for (Bucket bucket : bucketManager.getBuckets()) {
            addBucketTab(bucket);
        }

        // Add the "+" tab at the end
        addPlusTab();

        // Initialize bucket tab checkbox states based on global controls
        updateAllBucketTabsFromGlobalControls();

        add(tabbedPane, BorderLayout.CENTER);
    }

    private void addNewBucket() {
        // Set flag to prevent re-entry
        isAddingBucket = true;

        try {
            String name = JOptionPane.showInputDialog(this, "Enter bucket name:");
            if (name != null && !name.trim().isEmpty()) {
                // Check if bucket with this name already exists
                if (bucketManager.getBucketByName(name) != null) {
                    JOptionPane.showMessageDialog(this, "A bucket with this name already exists!", "Error", JOptionPane.ERROR_MESSAGE);
                    // Switch back to Global Controls or first bucket tab
                    int fallbackTab = tabbedPane.getTabCount() > 2 ? tabbedPane.getTabCount() - 2 : 0;
                    tabbedPane.setSelectedIndex(fallbackTab);
                    return;
                }

                Bucket bucket = new Bucket(name);

                // Apply defaults to new bucket
                bucketDefaults.applyToBucket(bucket);

                bucketManager.addBucket(bucket);

                // Get the index where the new tab will be inserted (before the "+" tab)
                int newTabIndex = tabbedPane.getTabCount() - 1;

                addBucketTab(bucket);

                // Switch to the newly created tab
                tabbedPane.setSelectedIndex(newTabIndex);

                if (onSaveCallback != null) {
                    onSaveCallback.run();
                }
            } else {
                // User cancelled or entered empty name, switch back to a valid tab
                int fallbackTab = tabbedPane.getTabCount() > 2 ? tabbedPane.getTabCount() - 2 : 0;
                tabbedPane.setSelectedIndex(fallbackTab);
            }
        } finally {
            // Always reset flag
            isAddingBucket = false;
        }
    }

    private void addBucketTab(Bucket bucket) {
        BucketTab tab = new BucketTab(bucket, onSaveCallback, api, bucketManager);
        bucketTabs.put(bucket, tab);

        // Insert before the "+" tab if it exists, otherwise at the end
        int insertIndex;
        int tabCount = tabbedPane.getTabCount();

        // Check if the "+" tab exists (it's always last if it exists)
        if (tabCount > 0 && "+".equals(tabbedPane.getTitleAt(tabCount - 1))) {
            // Insert before the "+" tab
            insertIndex = tabCount - 1;
        } else {
            // No "+" tab yet, insert at the end (after Global Controls)
            insertIndex = tabCount;
        }

        if (insertIndex < 1) insertIndex = 1; // After Global Controls at minimum

        // Format tab title with index prefix
        String tabTitle = insertIndex + ": " + bucket.getName();
        tabbedPane.insertTab(tabTitle, null, tab, null, insertIndex);

        // Update all subsequent token tab titles with new indices
        updateTokenTabTitles();
    }

    private void addPlusTab() {
        // Add an empty panel for the "+" tab
        JPanel emptyPanel = new JPanel();
        tabbedPane.addTab("+", emptyPanel);
    }

    private void removePlusTab() {
        // Remove the last tab if it's the "+" tab
        int lastIndex = tabbedPane.getTabCount() - 1;
        if (lastIndex >= 0 && "+".equals(tabbedPane.getTitleAt(lastIndex))) {
            tabbedPane.removeTabAt(lastIndex);
        }
    }

    private void removeCurrentBucket() {
        int selectedIndex = tabbedPane.getSelectedIndex();
        if (selectedIndex <= 0 || selectedIndex == tabbedPane.getTabCount() - 1) {
            JOptionPane.showMessageDialog(this, "Please select a bucket tab to remove (not Global Controls or +).", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Remove this bucket?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            isRemovingBucket = true;
            try {
                String tabTitle = tabbedPane.getTitleAt(selectedIndex);
                String bucketName = extractBucketNameFromTitle(tabTitle);
                Bucket bucket = bucketManager.getBucketByName(bucketName);

                if (bucket != null) {
                    bucketManager.removeBucket(bucket);
                    bucketTabs.remove(bucket);
                }

                // Check if we're removing the right-most bucket (just before the "+" tab)
                boolean isRightMostBucket = selectedIndex == tabbedPane.getTabCount() - 2;

                tabbedPane.removeTabAt(selectedIndex);

                // Update remaining token tab titles with new indices
                updateTokenTabTitles();

                // If this was the last bucket (only Global Controls and + tab remain), switch to Global Controls
                if (tabbedPane.getTabCount() == 2) {
                    tabbedPane.setSelectedIndex(0);
                } else if (isRightMostBucket) {
                    // If we removed the right-most bucket, select the one to its left (avoid selecting "+")
                    tabbedPane.setSelectedIndex(selectedIndex - 1);
                }

                if (onSaveCallback != null) {
                    onSaveCallback.run();
                }
            } finally {
                isRemovingBucket = false;
            }
        }
    }

    public void refreshAllTokenDisplays() {
        for (BucketTab tab : bucketTabs.values()) {
            tab.refreshTokenDisplay();
        }
    }

    private void updateAllBucketTabsFromGlobalControls() {
        for (BucketTab tab : bucketTabs.values()) {
            tab.updateToolCheckboxesFromGlobalControls(bucketManager.getGlobalControls());
        }
    }

    private void showTabContextMenu(java.awt.event.MouseEvent e, int tabIndex) {
        // Get the bucket and tab title at the time of right-click
        Bucket bucket = getBucketAtTabIndex(tabIndex);
        String tabTitle = tabbedPane.getTitleAt(tabIndex);

        if (bucket == null) {
            return;
        }

        JPopupMenu contextMenu = new JPopupMenu();

        JMenuItem renameItem = new JMenuItem("Rename");
        renameItem.addActionListener(event -> renameBucket(bucket, tabIndex));
        contextMenu.add(renameItem);

        JMenuItem duplicateItem = new JMenuItem("Duplicate");
        duplicateItem.addActionListener(event -> duplicateBucket(bucket));
        contextMenu.add(duplicateItem);

        JMenuItem deleteItem = new JMenuItem("Delete");
        deleteItem.addActionListener(event -> deleteBucket(bucket, tabTitle, tabIndex));
        contextMenu.add(deleteItem);

        contextMenu.show(e.getComponent(), e.getX(), e.getY());
    }

    private void renameBucketTab(int tabIndex) {
        // Get the bucket for this tab (tabIndex - 1 because Global Controls is at index 0)
        Bucket bucket = getBucketAtTabIndex(tabIndex);
        if (bucket == null) {
            return;
        }

        renameBucket(bucket, tabIndex);
    }

    private void renameBucket(Bucket bucket, int tabIndex) {
        String currentName = bucket.getName();
        String newName = JOptionPane.showInputDialog(this, "Enter new bucket name:", currentName);

        if (newName != null && !newName.trim().isEmpty() && !newName.equals(currentName)) {
            // Check if name already exists
            if (bucketManager.getBucketByName(newName) != null) {
                JOptionPane.showMessageDialog(this, "A bucket with that name already exists!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Update bucket name
            bucket.setName(newName.trim());

            // Update tab title with index prefix
            String newTitle = tabIndex + ": " + newName.trim();
            tabbedPane.setTitleAt(tabIndex, newTitle);

            // Save changes
            if (onSaveCallback != null) {
                onSaveCallback.run();
            }
        }
    }

    private void deleteBucket(Bucket bucket, String tabTitle, int tabIndex) {
        // Extract bucket name from display title
        String bucketName = extractBucketNameFromTitle(tabTitle);

        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete bucket '" + bucketName + "'?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            isRemovingBucket = true;
            try {
                bucketManager.removeBucket(bucket);
                bucketTabs.remove(bucket);

                // Check if we're removing the right-most bucket (just before the "+" tab)
                boolean isRightMostBucket = tabIndex == tabbedPane.getTabCount() - 2;

                tabbedPane.removeTabAt(tabIndex);

                // Update remaining token tab titles with new indices
                updateTokenTabTitles();

                // If this was the last bucket (only Global Controls and + tab remain), switch to Global Controls
                if (tabbedPane.getTabCount() == 2) {
                    tabbedPane.setSelectedIndex(0);
                } else if (isRightMostBucket) {
                    // If we removed the right-most bucket, select the one to its left (avoid selecting "+")
                    tabbedPane.setSelectedIndex(tabIndex - 1);
                }

                if (onSaveCallback != null) {
                    onSaveCallback.run();
                }
            } finally {
                isRemovingBucket = false;
            }
        }
    }

    private Bucket getBucketAtTabIndex(int tabIndex) {
        // Get the bucket by looking up the tab title (extract name from "N: Name" format)
        if (tabIndex > 0 && tabIndex < tabbedPane.getTabCount()) {
            String tabTitle = tabbedPane.getTitleAt(tabIndex);
            String bucketName = extractBucketNameFromTitle(tabTitle);
            return bucketManager.getBucketByName(bucketName);
        }
        return null;
    }

    public void updateFromModel() {
        // Update settings
        settingsTab.updateFromModel();

        // Clear existing bucket tabs (except Settings)
        while (tabbedPane.getTabCount() > 1) {
            tabbedPane.removeTabAt(1);
        }
        bucketTabs.clear();

        // Re-add all buckets
        for (Bucket bucket : bucketManager.getBuckets()) {
            addBucketTab(bucket);
        }

        // Re-add the "+" tab
        addPlusTab();

        // Update bucket tab checkbox states based on global controls
        updateAllBucketTabsFromGlobalControls();
    }

    public void setPersistenceManager(PersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
    }

    private void exportToJson() {
        if (persistenceManager == null) {
            JOptionPane.showMessageDialog(this, "Persistence manager not initialized!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Configuration");
        fileChooser.setSelectedFile(new File("collector-config.json"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File file = fileChooser.getSelectedFile();

                // Check if file exists and prompt for overwrite
                if (file.exists()) {
                    int choice = JOptionPane.showConfirmDialog(this,
                        "File '" + file.getName() + "' already exists. Overwrite?",
                        "File Exists",
                        JOptionPane.YES_NO_OPTION);

                    if (choice != JOptionPane.YES_OPTION) {
                        return;
                    }
                }

                persistenceManager.exportToFile(bucketManager, bucketDefaults, file);
                JOptionPane.showMessageDialog(this, "Configuration exported successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void importFromJson() {
        if (persistenceManager == null) {
            JOptionPane.showMessageDialog(this, "Persistence manager not initialized!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "Importing will replace your current configuration. Continue?",
            "Confirm Import",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Import Configuration");

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File file = fileChooser.getSelectedFile();
                BucketDefaults importedDefaults = persistenceManager.importFromFile(bucketManager, file);

                // Update bucketDefaults with imported values
                if (importedDefaults != null) {
                    importedDefaults.applyToBucket(new model.Bucket("temp")); // Validate it can be applied
                    // Copy all fields from importedDefaults to bucketDefaults
                    bucketDefaults.setEnabled(importedDefaults.isEnabled());
                    bucketDefaults.setBucketType(importedDefaults.getBucketType());
                    bucketDefaults.setMaxSize(importedDefaults.getMaxSize());
                    bucketDefaults.setFullBehavior(importedDefaults.getFullBehavior());
                    bucketDefaults.setUniqueOnly(importedDefaults.isUniqueOnly());
                    bucketDefaults.setCollectFromRequests(importedDefaults.isCollectFromRequests());
                    bucketDefaults.setCollectFromResponses(importedDefaults.isCollectFromResponses());
                    bucketDefaults.getCollectionEnabledTools().clear();
                    bucketDefaults.getCollectionEnabledTools().addAll(importedDefaults.getCollectionEnabledTools());
                    bucketDefaults.setCollectionMatchInScopeUrls(importedDefaults.isCollectionMatchInScopeUrls());
                    bucketDefaults.setReplaceInRequests(importedDefaults.isReplaceInRequests());
                    bucketDefaults.setReplaceInResponses(importedDefaults.isReplaceInResponses());
                    bucketDefaults.getReplacementEnabledTools().clear();
                    bucketDefaults.getReplacementEnabledTools().addAll(importedDefaults.getReplacementEnabledTools());
                    bucketDefaults.setLastTokenBehavior(importedDefaults.getLastTokenBehavior());
                    bucketDefaults.setEmptyBucketBehavior(importedDefaults.getEmptyBucketBehavior());
                    bucketDefaults.setStaticValue(importedDefaults.getStaticValue());
                    bucketDefaults.setGeneratorRegex(importedDefaults.getGeneratorRegex());
                    bucketDefaults.setReplacementMatchInScopeUrls(importedDefaults.isReplacementMatchInScopeUrls());
                }

                updateFromModel();

                if (onSaveCallback != null) {
                    onSaveCallback.run();
                }

                JOptionPane.showMessageDialog(this, "Configuration imported successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Import failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void resetToDefaults() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Reset all settings to defaults and clear all tokens?\nThis cannot be undone.",
            "Confirm Reset",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        if (persistenceManager != null) {
            // Clear all persisted data
            persistenceManager.clearAllData();

            // Remove all buckets from manager
            List<Bucket> existingBuckets = new ArrayList<>(bucketManager.getBuckets());
            for (Bucket bucket : existingBuckets) {
                bucketManager.removeBucket(bucket);
            }

            // Reset global controls to defaults
            bucketManager.getGlobalControls().setCollectionEnabledTools(new java.util.HashSet<>());
            bucketManager.getGlobalControls().setReplacementEnabledTools(new java.util.HashSet<>());
            bucketManager.getGlobalControls().getCollectionEnabledTools().add(burp.api.montoya.core.ToolType.TARGET);
            bucketManager.getGlobalControls().getCollectionEnabledTools().add(burp.api.montoya.core.ToolType.PROXY);
            bucketManager.getGlobalControls().getCollectionEnabledTools().add(burp.api.montoya.core.ToolType.INTRUDER);
            bucketManager.getGlobalControls().getCollectionEnabledTools().add(burp.api.montoya.core.ToolType.REPEATER);
            bucketManager.getGlobalControls().getCollectionEnabledTools().add(burp.api.montoya.core.ToolType.SCANNER);
            bucketManager.getGlobalControls().getCollectionEnabledTools().add(burp.api.montoya.core.ToolType.SEQUENCER);
            bucketManager.getGlobalControls().getCollectionEnabledTools().add(burp.api.montoya.core.ToolType.EXTENSIONS);

            // Only add BURP_AI if it's available (Pro edition only)
            if (util.BurpEditionDetector.isBurpAiAvailable()) {
                burp.api.montoya.core.ToolType burpAi = util.BurpEditionDetector.getBurpAiToolType();
                if (burpAi != null) {
                    bucketManager.getGlobalControls().getCollectionEnabledTools().add(burpAi);
                }
            }

            bucketManager.getGlobalControls().getReplacementEnabledTools().add(burp.api.montoya.core.ToolType.TARGET);
            bucketManager.getGlobalControls().getReplacementEnabledTools().add(burp.api.montoya.core.ToolType.PROXY);
            bucketManager.getGlobalControls().getReplacementEnabledTools().add(burp.api.montoya.core.ToolType.INTRUDER);
            bucketManager.getGlobalControls().getReplacementEnabledTools().add(burp.api.montoya.core.ToolType.REPEATER);
            bucketManager.getGlobalControls().getReplacementEnabledTools().add(burp.api.montoya.core.ToolType.SCANNER);
            bucketManager.getGlobalControls().getReplacementEnabledTools().add(burp.api.montoya.core.ToolType.SEQUENCER);
            bucketManager.getGlobalControls().getReplacementEnabledTools().add(burp.api.montoya.core.ToolType.EXTENSIONS);

            // Only add BURP_AI if it's available (Pro edition only)
            if (util.BurpEditionDetector.isBurpAiAvailable()) {
                burp.api.montoya.core.ToolType burpAi = util.BurpEditionDetector.getBurpAiToolType();
                if (burpAi != null) {
                    bucketManager.getGlobalControls().getReplacementEnabledTools().add(burpAi);
                }
            }

            // Reset bucket defaults to defaults
            BucketDefaults freshDefaults = new BucketDefaults();
            bucketDefaults.setEnabled(freshDefaults.isEnabled());
            bucketDefaults.setBucketType(freshDefaults.getBucketType());
            bucketDefaults.setMaxSize(freshDefaults.getMaxSize());
            bucketDefaults.setFullBehavior(freshDefaults.getFullBehavior());
            bucketDefaults.setUniqueOnly(freshDefaults.isUniqueOnly());
            bucketDefaults.setCollectFromRequests(freshDefaults.isCollectFromRequests());
            bucketDefaults.setCollectFromResponses(freshDefaults.isCollectFromResponses());
            bucketDefaults.getCollectionEnabledTools().clear();
            bucketDefaults.getCollectionEnabledTools().addAll(freshDefaults.getCollectionEnabledTools());
            bucketDefaults.setCollectionMatchInScopeUrls(freshDefaults.isCollectionMatchInScopeUrls());
            bucketDefaults.setReplaceInRequests(freshDefaults.isReplaceInRequests());
            bucketDefaults.setReplaceInResponses(freshDefaults.isReplaceInResponses());
            bucketDefaults.getReplacementEnabledTools().clear();
            bucketDefaults.getReplacementEnabledTools().addAll(freshDefaults.getReplacementEnabledTools());
            bucketDefaults.setLastTokenBehavior(freshDefaults.getLastTokenBehavior());
            bucketDefaults.setEmptyBucketBehavior(freshDefaults.getEmptyBucketBehavior());
            bucketDefaults.setStaticValue(freshDefaults.getStaticValue());
            bucketDefaults.setGeneratorRegex(freshDefaults.getGeneratorRegex());
            bucketDefaults.setReplacementMatchInScopeUrls(freshDefaults.isReplacementMatchInScopeUrls());

            // Update UI
            updateFromModel();

            // Save the reset state
            if (onSaveCallback != null) {
                onSaveCallback.run();
            }

            JOptionPane.showMessageDialog(this, "Settings reset to defaults successfully!", "Reset Complete", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void saveAllTokens() {
        List<Bucket> buckets = bucketManager.getBuckets();

        if (buckets.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No token buckets to save.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Choose directory to save all token files
        JFileChooser directoryChooser = new JFileChooser();
        directoryChooser.setDialogTitle("Select Directory to Save All Tokens");
        directoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        if (directoryChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File directory = directoryChooser.getSelectedFile();

        // Check if ANY bucket contains tokens with newlines
        boolean hasNewlines = false;
        for (Bucket bucket : buckets) {
            java.util.List<String> tokens = bucket.getAllTokens();
            if (!tokens.isEmpty() && util.TokenEncoder.hasTokensWithNewlines(tokens)) {
                hasNewlines = true;
                break;
            }
        }

        // If any bucket has newlines, show the warning dialog
        NewlineTokenWarningDialog.Action action = NewlineTokenWarningDialog.Action.DO_NOTHING;
        util.TokenEncoder.EncodingType encoding = util.TokenEncoder.EncodingType.BASE64;

        if (hasNewlines) {
            NewlineTokenWarningDialog dialog = new NewlineTokenWarningDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                NewlineTokenWarningDialog.OperationType.SAVE_ALL
            );
            dialog.setVisible(true);

            action = dialog.getSelectedAction();
            encoding = dialog.getSelectedEncoding();

            // If user cancels, abort the save operation
            if (action == NewlineTokenWarningDialog.Action.CANCEL) {
                return;
            }
        }

        int savedCount = 0;
        int skippedCount = 0;
        StringBuilder messages = new StringBuilder();

        for (Bucket bucket : buckets) {
            java.util.List<String> tokens = bucket.getAllTokens();
            if (tokens.isEmpty()) {
                skippedCount++;
                continue;
            }

            // Apply the action only to buckets with newlines
            java.util.List<String> tokensToSave = tokens;
            if (util.TokenEncoder.hasTokensWithNewlines(tokens)) {
                switch (action) {
                    case ENCODE_TOKENS:
                        tokensToSave = util.TokenEncoder.encodeAll(tokens, encoding);
                        break;
                    case SKIP_TOKENS:
                        tokensToSave = util.TokenEncoder.removeTokensWithNewlines(tokens);
                        // If all tokens were skipped, skip this bucket
                        if (tokensToSave.isEmpty()) {
                            skippedCount++;
                            continue;
                        }
                        break;
                    case DO_NOTHING:
                        // Continue with original tokens
                        break;
                    case CANCEL:
                        // Already handled above
                        break;
                }
            }

            File file = new File(directory, bucket.getName() + "_tokens.txt");

            // Check if file exists and prompt for overwrite
            if (file.exists()) {
                int choice = JOptionPane.showConfirmDialog(this,
                    "File '" + file.getName() + "' already exists. Overwrite?",
                    "File Exists",
                    JOptionPane.YES_NO_OPTION);

                if (choice != JOptionPane.YES_OPTION) {
                    skippedCount++;
                    continue;
                }
            }

            try {
                java.nio.file.Files.write(file.toPath(), tokensToSave);
                savedCount++;
            } catch (Exception ex) {
                messages.append("\nFailed to save '").append(bucket.getName()).append("': ").append(ex.getMessage());
            }
        }

        // Show summary
        String summary = "Saved " + savedCount + " bucket(s)";
        if (skippedCount > 0) {
            summary += ", skipped " + skippedCount + " bucket(s)";
        }
        if (messages.length() > 0) {
            summary += messages.toString();
            JOptionPane.showMessageDialog(this, summary, "Save All Tokens Complete", JOptionPane.WARNING_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, summary, "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // Inner class for tab reordering via drag and drop
    private class TabTransferHandler extends javax.swing.TransferHandler {
        private int draggedTabIndex = -1;
        private JPanel dropIndicator;

        public TabTransferHandler() {
            // Create visual drop indicator
            dropIndicator = new JPanel();
            dropIndicator.setBackground(new Color(0, 100, 255, 150));
            dropIndicator.setPreferredSize(new Dimension(3, 20));
        }

        @Override
        public int getSourceActions(JComponent c) {
            return MOVE;
        }

        @Override
        protected java.awt.datatransfer.Transferable createTransferable(JComponent c) {
            JTabbedPane pane = (JTabbedPane) c;
            draggedTabIndex = pane.getSelectedIndex();

            // Don't allow dragging Global Controls or "+" tab
            if (draggedTabIndex == 0 || draggedTabIndex == pane.getTabCount() - 1) {
                return null;
            }

            return new java.awt.datatransfer.StringSelection(String.valueOf(draggedTabIndex));
        }

        private int getDropIndex(JTabbedPane pane, Point dropPoint) {
            int tabIndex = pane.indexAtLocation(dropPoint.x, dropPoint.y);

            if (tabIndex == -1) {
                return -1;
            }

            // Get the bounds of the tab under the cursor
            Rectangle tabBounds = pane.getBoundsAt(tabIndex);

            // Calculate if drop point is in left or right half of the tab
            int tabCenter = tabBounds.x + (tabBounds.width / 2);

            if (dropPoint.x < tabCenter) {
                // Drop to the left of this tab
                return tabIndex;
            } else {
                // Drop to the right of this tab
                return tabIndex + 1;
            }
        }

        @Override
        public boolean canImport(TransferSupport support) {
            if (!support.isDrop()) {
                return false;
            }

            JTabbedPane pane = (JTabbedPane) support.getComponent();
            Point dropPoint = support.getDropLocation().getDropPoint();
            int dropIndex = getDropIndex(pane, dropPoint);

            // Don't allow dropping at position 0 (before Global Controls) or at the last position (after last bucket, at "+")
            if (dropIndex == -1 || dropIndex == 0 || dropIndex == pane.getTabCount()) {
                return false;
            }

            return true;
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }

            JTabbedPane pane = (JTabbedPane) support.getComponent();
            Point dropPoint = support.getDropLocation().getDropPoint();
            int dropIndex = getDropIndex(pane, dropPoint);

            if (draggedTabIndex == -1) {
                return false;
            }

            // Get the bucket being moved
            String tabTitle = pane.getTitleAt(draggedTabIndex);
            java.awt.Component tabComponent = pane.getComponentAt(draggedTabIndex);
            Bucket movedBucket = getBucketAtTabIndex(draggedTabIndex);

            if (movedBucket == null) {
                return false;
            }

            // Set flag to prevent triggering add bucket dialog
            isReorderingTabs = true;

            try {
                // Remove tab at old position
                pane.removeTabAt(draggedTabIndex);

                // Adjust drop index if necessary (if we removed a tab before the drop position)
                if (dropIndex > draggedTabIndex) {
                    dropIndex--;
                }

                // Insert at new position
                pane.insertTab(tabTitle, null, tabComponent, null, dropIndex);
                pane.setSelectedIndex(dropIndex);

                // Update bucket order in manager
                updateBucketOrder();

                // Update all token tab titles with new indices
                updateTokenTabTitles();

                // Save the new order
                if (onSaveCallback != null) {
                    onSaveCallback.run();
                }

                return true;
            } finally {
                isReorderingTabs = false;
            }
        }

        @Override
        protected void exportDone(JComponent c, java.awt.datatransfer.Transferable data, int action) {
            draggedTabIndex = -1;
        }
    }

    private void updateBucketOrder() {
        // Rebuild the bucket list in the manager based on current tab order
        List<Bucket> orderedBuckets = new ArrayList<>();

        for (int i = 1; i < tabbedPane.getTabCount() - 1; i++) { // Skip Global Controls (0) and "+" (last)
            String tabTitle = tabbedPane.getTitleAt(i);
            String bucketName = extractBucketNameFromTitle(tabTitle);
            Bucket bucket = bucketManager.getBucketByName(bucketName);
            if (bucket != null) {
                orderedBuckets.add(bucket);
            }
        }

        // Clear and re-add buckets in new order
        List<Bucket> existingBuckets = new ArrayList<>(bucketManager.getBuckets());
        for (Bucket bucket : existingBuckets) {
            bucketManager.removeBucket(bucket);
        }
        for (Bucket bucket : orderedBuckets) {
            bucketManager.addBucket(bucket);
        }
    }

    private void updateTokenTabTitles() {
        // Update all token tab titles with their current index
        for (int i = 1; i < tabbedPane.getTabCount(); i++) {
            String currentTitle = tabbedPane.getTitleAt(i);

            // Skip the "+" tab
            if ("+".equals(currentTitle)) {
                break;
            }

            // Extract bucket name (remove any existing index prefix)
            String bucketName = extractBucketNameFromTitle(currentTitle);

            // Set new title with current index
            String newTitle = i + ": " + bucketName;
            tabbedPane.setTitleAt(i, newTitle);
        }
    }

    private String extractBucketNameFromTitle(String tabTitle) {
        // Extract bucket name from "N: BucketName" format
        // If no prefix exists, return the title as-is
        if (tabTitle.contains(": ")) {
            int colonIndex = tabTitle.indexOf(": ");
            return tabTitle.substring(colonIndex + 2);
        }
        return tabTitle;
    }

    private void duplicateBucket(Bucket sourceBucket) {
        // Generate a default name for the duplicate
        String defaultName = generateCopyName(sourceBucket.getName());

        // Create custom dialog with options
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Name field
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("New Bucket Name:"), gbc);

        gbc.gridx = 1;
        JTextField nameField = new JTextField(defaultName, 20);
        panel.add(nameField, gbc);

        // Copy tokens checkbox
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
        JCheckBox copyTokensCheck = new JCheckBox("Copy Current Tokens", true);
        panel.add(copyTokensCheck, gbc);

        // Enable checkbox
        gbc.gridy = 2;
        JCheckBox enableCheck = new JCheckBox("Enable", false);
        panel.add(enableCheck, gbc);

        int result = JOptionPane.showConfirmDialog(this, panel, "Duplicate Bucket",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String newName = nameField.getText().trim();

            if (newName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Bucket name cannot be empty!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Check if bucket with this name already exists
            if (bucketManager.getBucketByName(newName) != null) {
                JOptionPane.showMessageDialog(this, "A bucket with this name already exists!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Create duplicate bucket
            Bucket newBucket = duplicateBucketDeep(sourceBucket, newName, copyTokensCheck.isSelected());
            newBucket.setEnabled(enableCheck.isSelected());

            // Add to manager
            bucketManager.addBucket(newBucket);

            // Get the index where the new tab will be inserted (before the "+" tab)
            int newTabIndex = tabbedPane.getTabCount() - 1;

            // Add tab
            addBucketTab(newBucket);

            // Switch to the newly created tab
            tabbedPane.setSelectedIndex(newTabIndex);

            // Update bucket tabs from global controls
            BucketTab newBucketTab = bucketTabs.get(newBucket);
            if (newBucketTab != null) {
                newBucketTab.updateToolCheckboxesFromGlobalControls(bucketManager.getGlobalControls());
            }

            if (onSaveCallback != null) {
                onSaveCallback.run();
            }
        }
    }

    private String generateCopyName(String originalName) {
        // Check if name already has (Copy N) suffix
        java.util.regex.Pattern copyPattern = java.util.regex.Pattern.compile("^(.+) \\(Copy(?: (\\d+))?\\)$");
        java.util.regex.Matcher matcher = copyPattern.matcher(originalName);

        if (matcher.matches()) {
            // Name has (Copy N) suffix
            String baseName = matcher.group(1);
            String numberStr = matcher.group(2);
            int copyNumber = numberStr != null ? Integer.parseInt(numberStr) : 1;
            int nextNumber = copyNumber + 1;

            // Find next available number
            String candidateName;
            do {
                candidateName = baseName + " (Copy " + nextNumber + ")";
                nextNumber++;
            } while (bucketManager.getBucketByName(candidateName) != null);

            return candidateName;
        } else {
            // Name doesn't have (Copy) suffix yet
            String candidateName = originalName + " (Copy)";

            // If (Copy) already exists, find next available number
            if (bucketManager.getBucketByName(candidateName) != null) {
                int copyNumber = 2;
                do {
                    candidateName = originalName + " (Copy " + copyNumber + ")";
                    copyNumber++;
                } while (bucketManager.getBucketByName(candidateName) != null);
            }

            return candidateName;
        }
    }

    private Bucket duplicateBucketDeep(Bucket source, String newName, boolean copyTokens) {
        Bucket newBucket = new Bucket(newName);

        // Copy basic properties
        newBucket.setEnabled(source.isEnabled());
        newBucket.setBucketType(source.getBucketType());
        newBucket.setMaxSize(source.getMaxSize());
        newBucket.setFullBehavior(source.getFullBehavior());
        newBucket.setUniqueOnly(source.isUniqueOnly());

        // Deep copy CollectionRule
        CollectionRule sourceCollectionRule = source.getCollectionRule();
        CollectionRule newCollectionRule = newBucket.getCollectionRule();

        newCollectionRule.setCollectFromRequests(sourceCollectionRule.isCollectFromRequests());
        newCollectionRule.setCollectFromResponses(sourceCollectionRule.isCollectFromResponses());
        newCollectionRule.getEnabledTools().clear();
        newCollectionRule.getEnabledTools().addAll(sourceCollectionRule.getEnabledTools());
        newCollectionRule.setMatchInScopeUrls(sourceCollectionRule.isMatchInScopeUrls());
        newCollectionRule.setPostProcessingScript(sourceCollectionRule.getPostProcessingScript());

        // Deep copy URL matchers
        for (UrlMatcher sourceUrlMatcher : sourceCollectionRule.getUrlMatchers()) {
            UrlMatcher newUrlMatcher = new UrlMatcher();
            newUrlMatcher.setProtocol(sourceUrlMatcher.getProtocol());
            newUrlMatcher.setHost(sourceUrlMatcher.getHost());
            newUrlMatcher.setPort(sourceUrlMatcher.getPort());
            newUrlMatcher.setPath(sourceUrlMatcher.getPath());
            newUrlMatcher.setEnabled(sourceUrlMatcher.isEnabled());
            newCollectionRule.getUrlMatchers().add(newUrlMatcher);
        }

        // Deep copy Regex patterns
        for (RegexPattern sourcePattern : sourceCollectionRule.getRegexPatterns()) {
            RegexPattern newPattern = new RegexPattern();
            newPattern.setPattern(sourcePattern.getPattern());
            newPattern.setComment(sourcePattern.getComment());
            newPattern.setPostProcessingScript(sourcePattern.getPostProcessingScript());
            newPattern.setEnabled(sourcePattern.isEnabled());
            newCollectionRule.getRegexPatterns().add(newPattern);
        }

        // Deep copy ReplacementConfig
        ReplacementConfig sourceReplacementConfig = source.getReplacementConfig();
        ReplacementConfig newReplacementConfig = newBucket.getReplacementConfig();

        newReplacementConfig.setReplaceInRequests(sourceReplacementConfig.isReplaceInRequests());
        newReplacementConfig.setReplaceInResponses(sourceReplacementConfig.isReplaceInResponses());
        newReplacementConfig.getEnabledTools().clear();
        newReplacementConfig.getEnabledTools().addAll(sourceReplacementConfig.getEnabledTools());
        newReplacementConfig.setMatchInScopeUrls(sourceReplacementConfig.isMatchInScopeUrls());
        newReplacementConfig.setLastTokenBehavior(sourceReplacementConfig.getLastTokenBehavior());
        newReplacementConfig.setEmptyBucketBehavior(sourceReplacementConfig.getEmptyBucketBehavior());
        newReplacementConfig.setStaticValue(sourceReplacementConfig.getStaticValue());
        newReplacementConfig.setGeneratorRegex(sourceReplacementConfig.getGeneratorRegex());
        newReplacementConfig.setPreReplacementScript(sourceReplacementConfig.getPreReplacementScript());

        // Deep copy replacement URL matchers
        for (UrlMatcher sourceUrlMatcher : sourceReplacementConfig.getUrlMatchers()) {
            UrlMatcher newUrlMatcher = new UrlMatcher();
            newUrlMatcher.setProtocol(sourceUrlMatcher.getProtocol());
            newUrlMatcher.setHost(sourceUrlMatcher.getHost());
            newUrlMatcher.setPort(sourceUrlMatcher.getPort());
            newUrlMatcher.setPath(sourceUrlMatcher.getPath());
            newUrlMatcher.setEnabled(sourceUrlMatcher.isEnabled());
            newReplacementConfig.getUrlMatchers().add(newUrlMatcher);
        }

        // Deep copy replacement rules
        for (ReplacementRule sourceRule : sourceReplacementConfig.getReplacementRules()) {
            ReplacementRule newRule = new ReplacementRule();
            newRule.setLocation(sourceRule.getLocation());
            newRule.setFieldName(sourceRule.getFieldName());
            newRule.setRegexPattern(sourceRule.getRegexPattern());
            newRule.setRegexGroup(sourceRule.getRegexGroup());
            newRule.setReplaceAll(sourceRule.isReplaceAll());
            newRule.setPreProcessingScript(sourceRule.getPreProcessingScript());
            newRule.setEnabled(sourceRule.isEnabled());
            newReplacementConfig.getReplacementRules().add(newRule);
        }

        // Copy tokens if requested
        if (copyTokens) {
            for (String token : source.getAllTokens()) {
                newBucket.addToken(token);
            }
        }

        return newBucket;
    }
}
