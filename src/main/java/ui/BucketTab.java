package ui;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ToolType;
import model.*;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.util.HashMap;
import java.util.Map;
import javax.swing.SpinnerNumberModel;

public class BucketTab extends JPanel {
    private final Bucket bucket;
    private final Runnable onSaveCallback;
    private final MontoyaApi api;
    private final core.JavaScriptProcessor jsProcessor;
    private final core.BucketManager bucketManager;

    // Collection components
    private JCheckBox collectFromRequestsCheck;
    private JCheckBox collectFromResponsesCheck;
    private Map<ToolType, JCheckBox> collectionToolCheckboxes;
    private JCheckBox matchInScopeUrlsCheck;
    private UrlMatcherTableModel urlMatcherModel;
    private JTable urlMatcherTable;
    private RegexPatternTableModel regexPatternModel;
    private JTable regexPatternTable;
    private JTextArea postProcessingScriptArea;
    private JTextField postCollectionTestTokenField;
    private JTextArea postCollectionPreviewArea;

    // Bucket configuration
    private JCheckBox bucketEnabledCheck;
    private JComboBox<BucketType> bucketTypeCombo;
    private JSpinner maxSizeSpinner;
    private int lastCommittedMaxSize;
    private JComboBox<BucketFullBehavior> fullBehaviorCombo;
    private JCheckBox uniqueOnlyCheck;

    // Replacement components
    private JCheckBox replaceInRequestsCheck;
    private JCheckBox replaceInResponsesCheck;
    private Map<ToolType, JCheckBox> replacementToolCheckboxes;
    private JCheckBox replacementMatchInScopeUrlsCheck;
    private UrlMatcherTableModel replacementUrlMatcherModel;
    private JTable replacementUrlMatcherTable;
    private JTextArea preReplacementScriptArea;
    private JTextField preReplacementTestTokenField;
    private JTextArea preReplacementPreviewArea;
    private ReplacementRuleTableModel replacementRulesModel;
    private JTable replacementRulesTable;
    private JComboBox<LastTokenBehavior> lastTokenBehaviorCombo;
    private JComboBox<EmptyBucketBehavior> emptyBucketBehaviorCombo;
    private JLabel staticValueLabel;
    private JTextField staticValueField;
    private JLabel generatorRegexLabel;
    private JTextField generatorRegexField;

    // Token display
    private JTable tokenDisplayTable;
    private TokenTableModel tokenTableModel;
    private JLabel tokenCountLabel;
    private Timer tokenRefreshTimer;
    private int lastKnownTokenCount;
    private int tokenDisplayLength = 100; // 0 = no truncation

    public BucketTab(Bucket bucket, Runnable onSaveCallback, MontoyaApi api, core.BucketManager bucketManager) {
        this.bucket = bucket;
        this.lastKnownTokenCount = 0;
        this.onSaveCallback = onSaveCallback;
        this.api = api;
        this.jsProcessor = new core.JavaScriptProcessor(api.logging(), api);
        this.bucketManager = bucketManager;
        this.collectionToolCheckboxes = new HashMap<>();
        this.replacementToolCheckboxes = new HashMap<>();
        this.replacementRulesModel = new ReplacementRuleTableModel(bucket.getReplacementConfig().getReplacementRules());
        this.urlMatcherModel = new UrlMatcherTableModel(bucket.getCollectionRule().getUrlMatchers());
        this.replacementUrlMatcherModel = new UrlMatcherTableModel(bucket.getReplacementConfig().getUrlMatchers());
        this.regexPatternModel = new RegexPatternTableModel(bucket.getCollectionRule().getRegexPatterns());

        setLayout(new BorderLayout());
        initComponents();
        loadFromBucket();
        startTokenRefreshTimer();
    }

    private void initComponents() {
        JTabbedPane tabbedPane = new JTabbedPane();

        tabbedPane.addTab("Bucket Configuration", wrapInScrollPane(createBucketConfigPanel()));
        tabbedPane.addTab("Token Collection", wrapInScrollPane(createCollectionPanel()));
        tabbedPane.addTab("Tokens", wrapInScrollPane(createTokensPanel()));
        tabbedPane.addTab("Token Replacement", wrapInScrollPane(createReplacementPanel()));

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JScrollPane wrapInScrollPane(JPanel panel) {
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        return scrollPane;
    }

    private void autoSave() {
        if (onSaveCallback != null) {
            onSaveCallback.run();
        }
    }

    private JPanel createCollectionPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top section
        JPanel topPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Collection Sources label and Parse History button in top row
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel sourcesLabel = new JLabel("Collection Sources");
        sourcesLabel.setFont(sourcesLabel.getFont().deriveFont(Font.BOLD));
        topPanel.add(sourcesLabel, gbc);

        // Parse Proxy History button in upper right
        gbc.gridx = 1; gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.weightx = 0.0;
        JButton parseHistoryButton = new JButton("Parse Proxy History...");
        parseHistoryButton.addActionListener(e -> openProxyHistoryParser());
        topPanel.add(parseHistoryButton, gbc);

        // Reset for subsequent rows
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1.0;
        gbc.gridwidth = 2;

        gbc.gridy = 1;
        JPanel sourcesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        // Add "All" checkbox for Collection Sources
        JCheckBox allCollectionSourcesCheck = new JCheckBox("All");
        allCollectionSourcesCheck.addActionListener(e -> {
            boolean selected = allCollectionSourcesCheck.isSelected();
            collectFromRequestsCheck.setSelected(selected);
            collectFromResponsesCheck.setSelected(selected);
            bucket.getCollectionRule().setCollectFromRequests(selected);
            bucket.getCollectionRule().setCollectFromResponses(selected);
            autoSave();
        });
        sourcesPanel.add(allCollectionSourcesCheck);

        // Add gap after "All" checkbox
        sourcesPanel.add(Box.createRigidArea(new Dimension(15, 0)));

        collectFromRequestsCheck = new JCheckBox("Requests");
        collectFromRequestsCheck.addActionListener(e -> {
            bucket.getCollectionRule().setCollectFromRequests(collectFromRequestsCheck.isSelected());
            autoSave();
        });
        sourcesPanel.add(collectFromRequestsCheck);

        collectFromResponsesCheck = new JCheckBox("Responses");
        collectFromResponsesCheck.addActionListener(e -> {
            bucket.getCollectionRule().setCollectFromResponses(collectFromResponsesCheck.isSelected());
            autoSave();
        });
        sourcesPanel.add(collectFromResponsesCheck);
        topPanel.add(sourcesPanel, gbc);

        // Collection Tools
        gbc.gridy = 2;
        JLabel toolsLabel = new JLabel("Collection Tools");
        toolsLabel.setFont(toolsLabel.getFont().deriveFont(Font.BOLD));
        topPanel.add(toolsLabel, gbc);

        gbc.gridy = 3;
        JPanel toolsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        // Add "All" checkbox for Collection Tools
        JCheckBox allCollectionToolsCheck = new JCheckBox("All");
        allCollectionToolsCheck.addActionListener(e -> {
            boolean selected = allCollectionToolsCheck.isSelected();
            for (ToolType toolType : getCommonToolTypes()) {
                JCheckBox checkbox = collectionToolCheckboxes.get(toolType);
                checkbox.setSelected(selected);
                if (selected) {
                    bucket.getCollectionRule().getEnabledTools().add(toolType);
                } else {
                    bucket.getCollectionRule().getEnabledTools().remove(toolType);
                }
            }
            autoSave();
        });
        toolsPanel.add(allCollectionToolsCheck);

        // Add gap after "All" checkbox
        toolsPanel.add(Box.createRigidArea(new Dimension(15, 0)));

        for (ToolType toolType : getCommonToolTypes()) {
            JCheckBox checkbox = new JCheckBox(toolType.toolName());
            checkbox.addActionListener(e -> {
                if (checkbox.isSelected()) {
                    bucket.getCollectionRule().getEnabledTools().add(toolType);
                } else {
                    bucket.getCollectionRule().getEnabledTools().remove(toolType);
                }
                autoSave();
            });
            collectionToolCheckboxes.put(toolType, checkbox);
            toolsPanel.add(checkbox);
        }
        topPanel.add(toolsPanel, gbc);

        panel.add(topPanel, BorderLayout.NORTH);

        // Center section - tables
        JPanel centerPanel = new JPanel(new GridLayout(2, 1, 5, 5));

        // URL Matchers table
        centerPanel.add(createUrlMatcherPanel());

        // Regex Patterns table
        centerPanel.add(createRegexPatternPanel());

        panel.add(centerPanel, BorderLayout.CENTER);

        // Bottom section - post-processing script with preview
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));

        JPanel scriptHeaderPanel = new JPanel(new BorderLayout());
        JLabel scriptLabel = new JLabel("Post-Collection JavaScript");
        scriptLabel.setFont(scriptLabel.getFont().deriveFont(Font.BOLD, 14f));
        scriptHeaderPanel.add(scriptLabel, BorderLayout.NORTH);
        JLabel scriptDescLabel = new JLabel("Optional. Runs immediately before storing token. (variable: token, must return string)");
        scriptDescLabel.setFont(scriptDescLabel.getFont().deriveFont(Font.PLAIN, 11f));
        scriptHeaderPanel.add(scriptDescLabel, BorderLayout.CENTER);
        bottomPanel.add(scriptHeaderPanel, BorderLayout.NORTH);

        // Script and preview side by side
        JPanel scriptAndPreviewPanel = new JPanel(new GridLayout(1, 2, 10, 0));

        // Script area on left
        postProcessingScriptArea = new JTextArea(5, 40);
        postProcessingScriptArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        postProcessingScriptArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { save(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { save(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { save(); }
            private void save() {
                bucket.getCollectionRule().setPostProcessingScript(postProcessingScriptArea.getText());
                updatePostCollectionPreview();
                autoSave();
            }
        });
        scriptAndPreviewPanel.add(new JScrollPane(postProcessingScriptArea));

        // Preview area on right
        JPanel previewPanel = new JPanel(new BorderLayout(5, 5));

        JPanel testTokenPanel = new JPanel(new BorderLayout(5, 0));
        JLabel testTokenLabel = new JLabel("Test Token:");
        testTokenPanel.add(testTokenLabel, BorderLayout.WEST);
        postCollectionTestTokenField = new JTextField("testToken");
        postCollectionTestTokenField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updatePostCollectionPreview(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updatePostCollectionPreview(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updatePostCollectionPreview(); }
        });
        testTokenPanel.add(postCollectionTestTokenField, BorderLayout.CENTER);
        previewPanel.add(testTokenPanel, BorderLayout.NORTH);

        JLabel previewLabel = new JLabel("Preview:");
        previewPanel.add(previewLabel, BorderLayout.CENTER);

        postCollectionPreviewArea = new JTextArea(5, 40);
        postCollectionPreviewArea.setEditable(false);
        postCollectionPreviewArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane previewScrollPane = new JScrollPane(postCollectionPreviewArea);
        previewPanel.add(previewScrollPane, BorderLayout.SOUTH);

        scriptAndPreviewPanel.add(previewPanel);

        bottomPanel.add(scriptAndPreviewPanel, BorderLayout.CENTER);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createUrlMatcherPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        // Header with label
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel headerLabel = new JLabel("Collection URLs");
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 14f));
        headerPanel.add(headerLabel, BorderLayout.NORTH);

        // In-scope checkbox
        matchInScopeUrlsCheck = new JCheckBox("All In-Scope URLs");
        matchInScopeUrlsCheck.addActionListener(e -> { bucket.getCollectionRule().setMatchInScopeUrls(matchInScopeUrlsCheck.isSelected()); autoSave(); });
        headerPanel.add(matchInScopeUrlsCheck, BorderLayout.SOUTH);
        panel.add(headerPanel, BorderLayout.NORTH);

        // Content panel with buttons on left and table on right
        JPanel contentPanel = new JPanel(new BorderLayout(5, 0));

        // Button panel on left
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));

        JButton addButton = new JButton("Add");
        JButton pasteUrlButton = new JButton("Paste URL");
        JButton editButton = new JButton("Edit");
        JButton removeButton = new JButton("Remove");

        // Set all buttons to same size
        Dimension buttonSize = new Dimension(100, 25);
        addButton.setMaximumSize(buttonSize);
        pasteUrlButton.setMaximumSize(buttonSize);
        editButton.setMaximumSize(buttonSize);
        removeButton.setMaximumSize(buttonSize);

        addButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        pasteUrlButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        editButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        removeButton.setAlignmentX(Component.LEFT_ALIGNMENT);

        addButton.addActionListener(e -> addUrlMatcher());
        pasteUrlButton.addActionListener(e -> pasteUrlMatcher());
        editButton.addActionListener(e -> editUrlMatcher());
        removeButton.addActionListener(e -> removeUrlMatcher());

        buttonPanel.add(addButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        buttonPanel.add(pasteUrlButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        buttonPanel.add(editButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        buttonPanel.add(removeButton);
        buttonPanel.add(Box.createVerticalGlue());

        contentPanel.add(buttonPanel, BorderLayout.WEST);

        // Table on right
        urlMatcherTable = new JTable(urlMatcherModel);
        urlMatcherTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        urlMatcherModel.addTableModelListener(e -> autoSave());

        // Set column widths: Enabled, Protocol, Port should be minimal
        urlMatcherTable.getColumnModel().getColumn(0).setMaxWidth(60);  // Enabled
        urlMatcherTable.getColumnModel().getColumn(1).setMaxWidth(80);  // Protocol
        urlMatcherTable.getColumnModel().getColumn(3).setMaxWidth(60);  // Port

        JScrollPane scrollPane = new JScrollPane(urlMatcherTable);
        scrollPane.setPreferredSize(new Dimension(0, 150));
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        panel.add(contentPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createRegexPatternPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        // Header with label
        JLabel headerLabel = new JLabel("Collection Patterns");
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 14f));
        panel.add(headerLabel, BorderLayout.NORTH);

        // Content panel with buttons on left and table on right
        JPanel contentPanel = new JPanel(new BorderLayout(5, 0));

        // Button panel on left
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));

        JButton addButton = new JButton("Add");
        JButton editButton = new JButton("Edit");
        JButton removeButton = new JButton("Remove");
        JButton moveUpButton = new JButton("Up");
        JButton moveDownButton = new JButton("Down");

        // Set all buttons to same size
        Dimension buttonSize = new Dimension(100, 25);
        addButton.setMaximumSize(buttonSize);
        editButton.setMaximumSize(buttonSize);
        removeButton.setMaximumSize(buttonSize);
        moveUpButton.setMaximumSize(buttonSize);
        moveDownButton.setMaximumSize(buttonSize);

        addButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        editButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        removeButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        moveUpButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        moveDownButton.setAlignmentX(Component.LEFT_ALIGNMENT);

        addButton.addActionListener(e -> addRegexPattern());
        editButton.addActionListener(e -> editRegexPattern());
        removeButton.addActionListener(e -> removeRegexPattern());
        moveUpButton.addActionListener(e -> moveRegexPatternUp());
        moveDownButton.addActionListener(e -> moveRegexPatternDown());

        buttonPanel.add(addButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        buttonPanel.add(editButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        buttonPanel.add(removeButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        buttonPanel.add(moveUpButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        buttonPanel.add(moveDownButton);
        buttonPanel.add(Box.createVerticalGlue());

        contentPanel.add(buttonPanel, BorderLayout.WEST);

        // Table on right
        regexPatternTable = new JTable(regexPatternModel);
        regexPatternTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        regexPatternModel.addTableModelListener(e -> autoSave());

        // Set column widths
        // Columns: Enabled, Pattern, Match Requests, Match Responses, Comment, Has Script
        regexPatternTable.getColumnModel().getColumn(0).setPreferredWidth(60);  // Enabled
        regexPatternTable.getColumnModel().getColumn(0).setMaxWidth(80);
        regexPatternTable.getColumnModel().getColumn(1).setPreferredWidth(200); // Pattern
        regexPatternTable.getColumnModel().getColumn(2).setPreferredWidth(110); // Match Requests
        regexPatternTable.getColumnModel().getColumn(2).setMaxWidth(150);
        regexPatternTable.getColumnModel().getColumn(3).setPreferredWidth(120); // Match Responses
        regexPatternTable.getColumnModel().getColumn(3).setMaxWidth(150);
        regexPatternTable.getColumnModel().getColumn(4).setPreferredWidth(200); // Comment
        regexPatternTable.getColumnModel().getColumn(5).setPreferredWidth(80);  // Has Script
        regexPatternTable.getColumnModel().getColumn(5).setMaxWidth(100);

        JScrollPane scrollPane = new JScrollPane(regexPatternTable);
        scrollPane.setPreferredSize(new Dimension(0, 150));
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        panel.add(contentPanel, BorderLayout.CENTER);

        return panel;
    }

    private void addUrlMatcher() {
        UrlMatcherDialog dialog = new UrlMatcherDialog((Frame) SwingUtilities.getWindowAncestor(this), null);
        dialog.setVisible(true);
        UrlMatcher matcher = dialog.getMatcher();
        if (matcher != null) {
            urlMatcherModel.addMatcher(matcher);
            autoSave();
        }
    }

    private void editUrlMatcher() {
        int selected = urlMatcherTable.getSelectedRow();
        if (selected >= 0) {
            UrlMatcher matcher = urlMatcherModel.getMatcher(selected);
            UrlMatcherDialog dialog = new UrlMatcherDialog((Frame) SwingUtilities.getWindowAncestor(this), matcher);
            dialog.setVisible(true);
            urlMatcherModel.fireTableRowsUpdated(selected, selected);
            autoSave();
        }
    }

    private void removeUrlMatcher() {
        int selected = urlMatcherTable.getSelectedRow();
        if (selected >= 0) {
            urlMatcherModel.removeMatcher(selected);
            autoSave();
        }
    }

    private void pasteUrlMatcher() {
        try {
            // Get clipboard contents
            java.awt.datatransfer.Clipboard clipboard = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
            String clipboardText = (String) clipboard.getData(java.awt.datatransfer.DataFlavor.stringFlavor);

            if (clipboardText == null || clipboardText.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Clipboard is empty", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Parse URL
            java.net.URL url;
            try {
                url = new java.net.URL(clipboardText.trim());
            } catch (java.net.MalformedURLException e) {
                JOptionPane.showMessageDialog(this, "Invalid URL in clipboard:\n" + clipboardText, "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Create matcher from URL
            UrlMatcher matcher = new UrlMatcher();
            matcher.setEnabled(true);

            // Set protocol - just HTTP or HTTPS
            String protocol = url.getProtocol();
            if (protocol != null && !protocol.isEmpty()) {
                matcher.setProtocol(protocol.toUpperCase());
            }

            // Set host (escape and add anchors)
            String host = url.getHost();
            if (host != null && !host.isEmpty()) {
                matcher.setHost("^" + escapeRegex(host) + "$");
            }

            // Set port (escape and add anchors)
            int port = url.getPort();
            if (port != -1) {
                matcher.setPort("^" + escapeRegex(String.valueOf(port)) + "$");
            }

            // Set path (escape, add ^ prefix and .* suffix)
            String path = url.getPath();
            if (path != null && !path.isEmpty()) {
                matcher.setPath("^" + escapeRegex(path) + ".*");
            }

            // Add matcher to table
            urlMatcherModel.addMatcher(matcher);
            autoSave();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error pasting URL:\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String escapeRegex(String text) {
        // Escape special regex characters
        return text.replaceAll("([\\\\.*+?^${}()|\\[\\]])", "\\\\$1");
    }


    private JPanel createReplacementUrlMatcherPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        // Header with label
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel headerLabel = new JLabel("Replacement URLs");
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 14f));
        headerPanel.add(headerLabel, BorderLayout.NORTH);

        // In-scope checkbox
        replacementMatchInScopeUrlsCheck = new JCheckBox("All In-Scope URLs");
        replacementMatchInScopeUrlsCheck.addActionListener(e -> { bucket.getReplacementConfig().setMatchInScopeUrls(replacementMatchInScopeUrlsCheck.isSelected()); autoSave(); });
        headerPanel.add(replacementMatchInScopeUrlsCheck, BorderLayout.SOUTH);
        panel.add(headerPanel, BorderLayout.NORTH);

        // Content panel with buttons on left and table on right
        JPanel contentPanel = new JPanel(new BorderLayout(5, 0));

        // Button panel on left
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));

        JButton addButton = new JButton("Add");
        JButton pasteUrlButton = new JButton("Paste URL");
        JButton editButton = new JButton("Edit");
        JButton removeButton = new JButton("Remove");

        // Set all buttons to same size
        Dimension buttonSize = new Dimension(100, 25);
        addButton.setMaximumSize(buttonSize);
        pasteUrlButton.setMaximumSize(buttonSize);
        editButton.setMaximumSize(buttonSize);
        removeButton.setMaximumSize(buttonSize);

        addButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        pasteUrlButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        editButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        removeButton.setAlignmentX(Component.LEFT_ALIGNMENT);

        addButton.addActionListener(e -> addReplacementUrlMatcher());
        pasteUrlButton.addActionListener(e -> pasteReplacementUrlMatcher());
        editButton.addActionListener(e -> editReplacementUrlMatcher());
        removeButton.addActionListener(e -> removeReplacementUrlMatcher());

        buttonPanel.add(addButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        buttonPanel.add(pasteUrlButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        buttonPanel.add(editButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        buttonPanel.add(removeButton);
        buttonPanel.add(Box.createVerticalGlue());

        contentPanel.add(buttonPanel, BorderLayout.WEST);

        // Table on right
        replacementUrlMatcherTable = new JTable(replacementUrlMatcherModel);
        replacementUrlMatcherTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        replacementUrlMatcherModel.addTableModelListener(e -> autoSave());

        // Set column widths: Enabled, Protocol, Port should be minimal
        replacementUrlMatcherTable.getColumnModel().getColumn(0).setMaxWidth(60);  // Enabled
        replacementUrlMatcherTable.getColumnModel().getColumn(1).setMaxWidth(80);  // Protocol
        replacementUrlMatcherTable.getColumnModel().getColumn(3).setMaxWidth(60);  // Port

        JScrollPane scrollPane = new JScrollPane(replacementUrlMatcherTable);
        scrollPane.setPreferredSize(new Dimension(0, 150));
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        panel.add(contentPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createPreReplacementScriptPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        JPanel scriptHeaderPanel = new JPanel(new BorderLayout());
        JLabel scriptLabel = new JLabel("Pre-Replacement JavaScript");
        scriptLabel.setFont(scriptLabel.getFont().deriveFont(Font.BOLD, 14f));
        scriptHeaderPanel.add(scriptLabel, BorderLayout.NORTH);
        JLabel scriptDescLabel = new JLabel("Optional. Runs immediately before any replacement rules. (variable: token, must return string)");
        scriptDescLabel.setFont(scriptDescLabel.getFont().deriveFont(Font.PLAIN, 11f));
        scriptHeaderPanel.add(scriptDescLabel, BorderLayout.CENTER);
        panel.add(scriptHeaderPanel, BorderLayout.NORTH);

        // Script and preview side by side
        JPanel scriptAndPreviewPanel = new JPanel(new GridLayout(1, 2, 10, 0));

        // Script area on left
        preReplacementScriptArea = new JTextArea(5, 40);
        preReplacementScriptArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        preReplacementScriptArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { save(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { save(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { save(); }
            private void save() {
                bucket.getReplacementConfig().setPreReplacementScript(preReplacementScriptArea.getText());
                updatePreReplacementPreview();
                autoSave();
            }
        });
        scriptAndPreviewPanel.add(new JScrollPane(preReplacementScriptArea));

        // Preview area on right
        JPanel previewPanel = new JPanel(new BorderLayout(5, 5));

        JPanel testTokenPanel = new JPanel(new BorderLayout(5, 0));
        JLabel testTokenLabel = new JLabel("Test Token:");
        testTokenPanel.add(testTokenLabel, BorderLayout.WEST);
        preReplacementTestTokenField = new JTextField("testToken");
        preReplacementTestTokenField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updatePreReplacementPreview(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updatePreReplacementPreview(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updatePreReplacementPreview(); }
        });
        testTokenPanel.add(preReplacementTestTokenField, BorderLayout.CENTER);
        previewPanel.add(testTokenPanel, BorderLayout.NORTH);

        JLabel previewLabel = new JLabel("Preview:");
        previewPanel.add(previewLabel, BorderLayout.CENTER);

        preReplacementPreviewArea = new JTextArea(5, 40);
        preReplacementPreviewArea.setEditable(false);
        preReplacementPreviewArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane previewScrollPane = new JScrollPane(preReplacementPreviewArea);
        previewPanel.add(previewScrollPane, BorderLayout.SOUTH);

        scriptAndPreviewPanel.add(previewPanel);

        panel.add(scriptAndPreviewPanel, BorderLayout.CENTER);

        return panel;
    }

    private void addReplacementUrlMatcher() {
        UrlMatcherDialog dialog = new UrlMatcherDialog((Frame) SwingUtilities.getWindowAncestor(this), null);
        dialog.setVisible(true);
        UrlMatcher matcher = dialog.getMatcher();
        if (matcher != null) {
            replacementUrlMatcherModel.addMatcher(matcher);
            autoSave();
        }
    }

    private void editReplacementUrlMatcher() {
        int selected = replacementUrlMatcherTable.getSelectedRow();
        if (selected >= 0) {
            UrlMatcher matcher = replacementUrlMatcherModel.getMatcher(selected);
            UrlMatcherDialog dialog = new UrlMatcherDialog((Frame) SwingUtilities.getWindowAncestor(this), matcher);
            dialog.setVisible(true);
            replacementUrlMatcherModel.fireTableRowsUpdated(selected, selected);
            autoSave();
        }
    }

    private void removeReplacementUrlMatcher() {
        int selected = replacementUrlMatcherTable.getSelectedRow();
        if (selected >= 0) {
            replacementUrlMatcherModel.removeMatcher(selected);
            autoSave();
        }
    }

    private void pasteReplacementUrlMatcher() {
        try {
            // Get clipboard contents
            java.awt.datatransfer.Clipboard clipboard = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
            String clipboardText = (String) clipboard.getData(java.awt.datatransfer.DataFlavor.stringFlavor);

            if (clipboardText == null || clipboardText.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Clipboard is empty", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Parse URL
            java.net.URL url;
            try {
                url = new java.net.URL(clipboardText.trim());
            } catch (java.net.MalformedURLException e) {
                JOptionPane.showMessageDialog(this, "Invalid URL in clipboard:\n" + clipboardText, "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Create matcher from URL
            UrlMatcher matcher = new UrlMatcher();
            matcher.setEnabled(true);

            // Set protocol - just HTTP or HTTPS
            String protocol = url.getProtocol();
            if (protocol != null && !protocol.isEmpty()) {
                matcher.setProtocol(protocol.toUpperCase());
            }

            // Set host (escape and add anchors)
            String host = url.getHost();
            if (host != null && !host.isEmpty()) {
                matcher.setHost("^" + escapeRegex(host) + "$");
            }

            // Set port (escape and add anchors)
            int port = url.getPort();
            if (port != -1) {
                matcher.setPort("^" + escapeRegex(String.valueOf(port)) + "$");
            }

            // Set path (escape, add ^ prefix and .* suffix)
            String path = url.getPath();
            if (path != null && !path.isEmpty()) {
                matcher.setPath("^" + escapeRegex(path) + ".*");
            }

            // Add matcher to table
            replacementUrlMatcherModel.addMatcher(matcher);
            autoSave();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error pasting URL:\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    private void addRegexPattern() {
        RegexPatternDialog dialog = new RegexPatternDialog(
            (Frame) SwingUtilities.getWindowAncestor(this),
            null,
            api,
            bucket.getCollectionRule().isCollectFromRequests(),
            bucket.getCollectionRule().isCollectFromResponses()
        );
        dialog.setVisible(true);
        RegexPattern pattern = dialog.getPattern();
        if (pattern != null) {
            regexPatternModel.addPattern(pattern);
            autoSave();
        }
    }

    private void editRegexPattern() {
        int selected = regexPatternTable.getSelectedRow();
        if (selected >= 0) {
            RegexPattern pattern = regexPatternModel.getPattern(selected);
            RegexPatternDialog dialog = new RegexPatternDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                pattern,
                api,
                bucket.getCollectionRule().isCollectFromRequests(),
                bucket.getCollectionRule().isCollectFromResponses()
            );
            dialog.setVisible(true);
            regexPatternModel.fireTableRowsUpdated(selected, selected);
            autoSave();
        }
    }

    private void removeRegexPattern() {
        int selected = regexPatternTable.getSelectedRow();
        if (selected >= 0) {
            regexPatternModel.removePattern(selected);
            autoSave();
        }
    }

    private void openProxyHistoryParser() {
        ProxyHistoryParserDialog dialog = new ProxyHistoryParserDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                bucket,
                bucketManager,
                api
        );
        dialog.setVisible(true);
        // Refresh token display after dialog closes
        refreshTokenDisplay();
    }

    private void moveRegexPatternUp() {
        int selected = regexPatternTable.getSelectedRow();
        if (selected > 0) {
            regexPatternModel.moveUp(selected);
            regexPatternTable.setRowSelectionInterval(selected - 1, selected - 1);
            autoSave();
        }
    }

    private void moveRegexPatternDown() {
        int selected = regexPatternTable.getSelectedRow();
        if (selected >= 0 && selected < regexPatternModel.getRowCount() - 1) {
            regexPatternModel.moveDown(selected);
            regexPatternTable.setRowSelectionInterval(selected + 1, selected + 1);
            autoSave();
        }
    }

    private JPanel createBucketConfigPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.NORTHWEST;

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        bucketEnabledCheck = new JCheckBox("Enabled");
        bucketEnabledCheck.addActionListener(e -> { bucket.setEnabled(bucketEnabledCheck.isSelected()); autoSave(); });
        panel.add(bucketEnabledCheck, gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Bucket Type:"), gbc);
        gbc.gridx = 1;
        bucketTypeCombo = new JComboBox<>(BucketType.values());
        bucketTypeCombo.addActionListener(e -> { bucket.setBucketType((BucketType) bucketTypeCombo.getSelectedItem()); autoSave(); });
        panel.add(bucketTypeCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Max Size (-1 = infinite):"), gbc);
        gbc.gridx = 1;
        maxSizeSpinner = new JSpinner(new SpinnerNumberModel(-1, -1, 100000, 1));
        maxSizeSpinner.addChangeListener(e -> handleMaxSizeChange());

        // Add focus listener to the spinner's editor to handle manual typing
        JComponent editor = maxSizeSpinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField textField = ((JSpinner.DefaultEditor) editor).getTextField();
            textField.addFocusListener(new java.awt.event.FocusAdapter() {
                @Override
                public void focusLost(java.awt.event.FocusEvent e) {
                    // Commit the value when focus is lost
                    try {
                        maxSizeSpinner.commitEdit();
                    } catch (java.text.ParseException ex) {
                        // If parse fails, revert to current value
                        maxSizeSpinner.setValue(bucket.getMaxSize());
                    }
                }
            });
        }

        panel.add(maxSizeSpinner, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("When Bucket is full:"), gbc);
        gbc.gridx = 1;
        fullBehaviorCombo = new JComboBox<>(BucketFullBehavior.values());
        fullBehaviorCombo.addActionListener(e -> { bucket.setFullBehavior((BucketFullBehavior) fullBehaviorCombo.getSelectedItem()); autoSave(); });
        panel.add(fullBehaviorCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        uniqueOnlyCheck = new JCheckBox("Only allow unique tokens (no duplicates)");
        uniqueOnlyCheck.addActionListener(e -> {
            boolean newValue = uniqueOnlyCheck.isSelected();

            // If enabling unique only mode and bucket has tokens, ask about de-duplication
            if (newValue && bucket.getTokenCount() > 0) {
                int choice = JOptionPane.showConfirmDialog(this,
                    "De-duplicate existing tokens to remove duplicates?",
                    "Enable Unique Only Mode",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

                if (choice == JOptionPane.YES_OPTION) {
                    int removedCount = bucket.deduplicateTokens();
                    refreshTokenDisplay();
                    if (removedCount > 0) {
                        JOptionPane.showMessageDialog(this,
                            "Removed " + removedCount + " duplicate token(s).",
                            "De-duplication Complete",
                            JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            }

            bucket.setUniqueOnly(newValue);
            autoSave();
        });
        panel.add(uniqueOnlyCheck, gbc);

        // Add filler panels to push content to top-left
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 1; gbc.weighty = 1.0; gbc.weightx = 0.0;
        panel.add(new JPanel(), gbc);

        gbc.gridx = 2; gbc.gridy = 0; gbc.gridheight = 5; gbc.weighty = 0.0; gbc.weightx = 1.0;
        panel.add(new JPanel(), gbc);

        return panel;
    }

    private void handleMaxSizeChange() {
        int newSize = (Integer) maxSizeSpinner.getValue();
        int currentTokenCount = bucket.getTokenCount();

        // Only prompt if reducing size and bucket has more tokens than new size
        if (newSize > 0 && newSize < lastCommittedMaxSize && currentTokenCount > newSize) {
            int tokensToRemove = currentTokenCount - newSize;

            String[] options = {"Remove Newest", "Remove Oldest", "Leave Alone"};
            int choice = JOptionPane.showOptionDialog(this,
                "The bucket currently contains " + currentTokenCount + " tokens, which is more than the new size of " + newSize + ".\n\n" +
                "Would you like to remove " + tokensToRemove + " token(s)?",
                "Bucket Size Reduced",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[2]);

            if (choice == 0) { // Remove Newest
                // Remove from the end
                for (int i = 0; i < tokensToRemove; i++) {
                    bucket.removeTokenAt(currentTokenCount - 1 - i);
                }
                refreshTokenDisplay();
                lastCommittedMaxSize = newSize; // Update only when tokens are removed
            } else if (choice == 1) { // Remove Oldest
                // Remove from the beginning
                for (int i = 0; i < tokensToRemove; i++) {
                    bucket.removeTokenAt(0);
                }
                refreshTokenDisplay();
                lastCommittedMaxSize = newSize; // Update only when tokens are removed
            }
            // choice == 2 or closed dialog: Leave Alone, don't update lastCommittedMaxSize
        } else if (newSize >= 0 && currentTokenCount <= newSize) {
            // Size increased or matches token count, update the baseline
            lastCommittedMaxSize = newSize;
        }

        bucket.setMaxSize(newSize);
        autoSave();
    }

    private JPanel createReplacementPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Replacement Sinks
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel sourcesLabel = new JLabel("Replacement Sinks");
        sourcesLabel.setFont(sourcesLabel.getFont().deriveFont(Font.BOLD));
        topPanel.add(sourcesLabel, gbc);

        gbc.gridy = 1;
        JPanel sourcesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        // Add "All" checkbox for Replacement Sinks
        JCheckBox allReplacementSinksCheck = new JCheckBox("All");
        allReplacementSinksCheck.addActionListener(e -> {
            boolean selected = allReplacementSinksCheck.isSelected();
            replaceInRequestsCheck.setSelected(selected);
            replaceInResponsesCheck.setSelected(selected);
            bucket.getReplacementConfig().setReplaceInRequests(selected);
            bucket.getReplacementConfig().setReplaceInResponses(selected);
            autoSave();
        });
        sourcesPanel.add(allReplacementSinksCheck);

        // Add gap after "All" checkbox
        sourcesPanel.add(Box.createRigidArea(new Dimension(15, 0)));

        replaceInRequestsCheck = new JCheckBox("Requests");
        replaceInRequestsCheck.addActionListener(e -> { bucket.getReplacementConfig().setReplaceInRequests(replaceInRequestsCheck.isSelected()); autoSave(); });
        sourcesPanel.add(replaceInRequestsCheck);

        replaceInResponsesCheck = new JCheckBox("Responses");
        replaceInResponsesCheck.addActionListener(e -> { bucket.getReplacementConfig().setReplaceInResponses(replaceInResponsesCheck.isSelected()); autoSave(); });
        sourcesPanel.add(replaceInResponsesCheck);
        topPanel.add(sourcesPanel, gbc);

        // Replacement Tools
        gbc.gridy = 2;
        JLabel toolsLabel = new JLabel("Replacement Tools");
        toolsLabel.setFont(toolsLabel.getFont().deriveFont(Font.BOLD));
        topPanel.add(toolsLabel, gbc);

        gbc.gridy = 3;
        JPanel toolsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        // Add "All" checkbox for Replacement Tools
        JCheckBox allReplacementToolsCheck = new JCheckBox("All");
        allReplacementToolsCheck.addActionListener(e -> {
            boolean selected = allReplacementToolsCheck.isSelected();
            for (ToolType toolType : getCommonToolTypes()) {
                JCheckBox checkbox = replacementToolCheckboxes.get(toolType);
                checkbox.setSelected(selected);
                if (selected) {
                    bucket.getReplacementConfig().getEnabledTools().add(toolType);
                } else {
                    bucket.getReplacementConfig().getEnabledTools().remove(toolType);
                }
            }
            autoSave();
        });
        toolsPanel.add(allReplacementToolsCheck);

        // Add gap after "All" checkbox
        toolsPanel.add(Box.createRigidArea(new Dimension(15, 0)));

        for (ToolType toolType : getCommonToolTypes()) {
            JCheckBox checkbox = new JCheckBox(toolType.toolName());
            checkbox.addActionListener(e -> {
                if (checkbox.isSelected()) {
                    bucket.getReplacementConfig().getEnabledTools().add(toolType);
                } else {
                    bucket.getReplacementConfig().getEnabledTools().remove(toolType);
                }
                autoSave();
            });
            replacementToolCheckboxes.put(toolType, checkbox);
            toolsPanel.add(checkbox);
        }
        topPanel.add(toolsPanel, gbc);

        // Last token behavior
        gbc.gridy = 4;
        JPanel lastTokenPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lastTokenPanel.add(new JLabel("When using last token:"));
        lastTokenBehaviorCombo = new JComboBox<>(LastTokenBehavior.values());
        lastTokenBehaviorCombo.addActionListener(e -> { bucket.getReplacementConfig().setLastTokenBehavior((LastTokenBehavior) lastTokenBehaviorCombo.getSelectedItem()); autoSave(); });
        lastTokenPanel.add(lastTokenBehaviorCombo);
        topPanel.add(lastTokenPanel, gbc);

        // Empty bucket behavior
        gbc.gridy = 5;
        JPanel emptyBucketPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        emptyBucketPanel.add(new JLabel("If bucket is empty:"));
        emptyBucketBehaviorCombo = new JComboBox<>(EmptyBucketBehavior.values());
        emptyBucketBehaviorCombo.addActionListener(e -> {
            bucket.getReplacementConfig().setEmptyBucketBehavior((EmptyBucketBehavior) emptyBucketBehaviorCombo.getSelectedItem());
            updateEmptyBucketFieldsVisibility();
            autoSave();
        });
        emptyBucketPanel.add(emptyBucketBehaviorCombo);

        staticValueLabel = new JLabel(" Value:");
        staticValueField = new JTextField(20);
        staticValueField.addActionListener(e -> bucket.getReplacementConfig().setStaticValue(staticValueField.getText()));
        staticValueField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { update(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { update(); }
            private void update() { bucket.getReplacementConfig().setStaticValue(staticValueField.getText()); autoSave(); }
        });
        emptyBucketPanel.add(staticValueLabel);
        emptyBucketPanel.add(staticValueField);

        generatorRegexLabel = new JLabel(" Regex:");
        generatorRegexField = new JTextField(20);
        generatorRegexField.addActionListener(e -> bucket.getReplacementConfig().setGeneratorRegex(generatorRegexField.getText()));
        generatorRegexField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { update(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { update(); }
            private void update() { bucket.getReplacementConfig().setGeneratorRegex(generatorRegexField.getText()); autoSave(); }
        });
        emptyBucketPanel.add(generatorRegexLabel);
        emptyBucketPanel.add(generatorRegexField);

        topPanel.add(emptyBucketPanel, gbc);

        panel.add(topPanel, BorderLayout.NORTH);

        // Center section with URL Matchers, Pre-Replacement Script, and Replacement Rules
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.add(createReplacementUrlMatcherPanel());
        centerPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        centerPanel.add(createPreReplacementScriptPanel());
        centerPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        JPanel rulesPanel = new JPanel(new BorderLayout(5, 5));

        // Header with label
        JLabel rulesHeaderLabel = new JLabel("Replacement Rules");
        rulesHeaderLabel.setFont(rulesHeaderLabel.getFont().deriveFont(Font.BOLD, 14f));
        rulesPanel.add(rulesHeaderLabel, BorderLayout.NORTH);

        // Content panel with buttons on left and table on right
        JPanel rulesContentPanel = new JPanel(new BorderLayout(5, 0));

        // Button panel on left
        JPanel rulesButtonPanel = new JPanel();
        rulesButtonPanel.setLayout(new BoxLayout(rulesButtonPanel, BoxLayout.Y_AXIS));

        JButton addRuleButton = new JButton("Add");
        JButton editRuleButton = new JButton("Edit");
        JButton removeRuleButton = new JButton("Remove");
        JButton moveUpButton = new JButton("Up");
        JButton moveDownButton = new JButton("Down");

        // Set all buttons to same size
        Dimension buttonSize = new Dimension(100, 25);
        addRuleButton.setMaximumSize(buttonSize);
        editRuleButton.setMaximumSize(buttonSize);
        removeRuleButton.setMaximumSize(buttonSize);
        moveUpButton.setMaximumSize(buttonSize);
        moveDownButton.setMaximumSize(buttonSize);

        addRuleButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        editRuleButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        removeRuleButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        moveUpButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        moveDownButton.setAlignmentX(Component.LEFT_ALIGNMENT);

        addRuleButton.addActionListener(e -> addReplacementRule());
        editRuleButton.addActionListener(e -> editReplacementRule());
        removeRuleButton.addActionListener(e -> removeReplacementRule());
        moveUpButton.addActionListener(e -> moveReplacementRuleUp());
        moveDownButton.addActionListener(e -> moveReplacementRuleDown());

        rulesButtonPanel.add(addRuleButton);
        rulesButtonPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        rulesButtonPanel.add(editRuleButton);
        rulesButtonPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        rulesButtonPanel.add(removeRuleButton);
        rulesButtonPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        rulesButtonPanel.add(moveUpButton);
        rulesButtonPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        rulesButtonPanel.add(moveDownButton);
        rulesButtonPanel.add(Box.createVerticalGlue());

        rulesContentPanel.add(rulesButtonPanel, BorderLayout.WEST);

        // Table on right
        replacementRulesTable = new JTable(replacementRulesModel);
        replacementRulesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        replacementRulesModel.addTableModelListener(e -> autoSave());

        // Set column widths
        // Columns: Enabled, Location, Field/Pattern, Apply to Requests, Apply to Responses, Has Script
        replacementRulesTable.getColumnModel().getColumn(0).setPreferredWidth(60);  // Enabled
        replacementRulesTable.getColumnModel().getColumn(0).setMaxWidth(80);
        replacementRulesTable.getColumnModel().getColumn(1).setPreferredWidth(140); // Location
        replacementRulesTable.getColumnModel().getColumn(1).setMinWidth(140);
        replacementRulesTable.getColumnModel().getColumn(1).setMaxWidth(160);
        replacementRulesTable.getColumnModel().getColumn(2).setPreferredWidth(200); // Field/Pattern
        replacementRulesTable.getColumnModel().getColumn(3).setPreferredWidth(120); // Apply to Requests
        replacementRulesTable.getColumnModel().getColumn(3).setMaxWidth(150);
        replacementRulesTable.getColumnModel().getColumn(4).setPreferredWidth(130); // Apply to Responses
        replacementRulesTable.getColumnModel().getColumn(4).setMaxWidth(160);
        replacementRulesTable.getColumnModel().getColumn(5).setPreferredWidth(80);  // Has Script
        replacementRulesTable.getColumnModel().getColumn(5).setMaxWidth(100);

        JScrollPane rulesScrollPane = new JScrollPane(replacementRulesTable);
        rulesScrollPane.setPreferredSize(new Dimension(0, 150));
        rulesContentPanel.add(rulesScrollPane, BorderLayout.CENTER);

        rulesPanel.add(rulesContentPanel, BorderLayout.CENTER);

        centerPanel.add(rulesPanel);
        panel.add(centerPanel, BorderLayout.CENTER);

        return panel;
    }

    private void addReplacementRule() {
        ReplacementRuleDialog dialog = new ReplacementRuleDialog(
            (Frame) SwingUtilities.getWindowAncestor(this),
            null,
            api,
            bucket.getReplacementConfig().isReplaceInRequests(),
            bucket.getReplacementConfig().isReplaceInResponses()
        );
        dialog.setVisible(true);
        ReplacementRule rule = dialog.getRule();
        if (rule != null) {
            replacementRulesModel.addRule(rule);
            autoSave();
        }
    }

    private void editReplacementRule() {
        int selected = replacementRulesTable.getSelectedRow();
        if (selected >= 0) {
            ReplacementRule rule = replacementRulesModel.getRule(selected);
            ReplacementRuleDialog dialog = new ReplacementRuleDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                rule,
                api,
                bucket.getReplacementConfig().isReplaceInRequests(),
                bucket.getReplacementConfig().isReplaceInResponses()
            );
            dialog.setVisible(true);
            replacementRulesModel.fireTableRowsUpdated(selected, selected);
            autoSave();
        }
    }

    private void removeReplacementRule() {
        int selected = replacementRulesTable.getSelectedRow();
        if (selected >= 0) {
            replacementRulesModel.removeRule(selected);
            autoSave();
        }
    }

    private void moveReplacementRuleUp() {
        int selected = replacementRulesTable.getSelectedRow();
        if (selected > 0) {
            replacementRulesModel.moveUp(selected);
            replacementRulesTable.setRowSelectionInterval(selected - 1, selected - 1);
            autoSave();
        }
    }

    private void moveReplacementRuleDown() {
        int selected = replacementRulesTable.getSelectedRow();
        if (selected >= 0 && selected < replacementRulesModel.getRowCount() - 1) {
            replacementRulesModel.moveDown(selected);
            replacementRulesTable.setRowSelectionInterval(selected + 1, selected + 1);
            autoSave();
        }
    }

    private void updateEmptyBucketFieldsVisibility() {
        EmptyBucketBehavior behavior = (EmptyBucketBehavior) emptyBucketBehaviorCombo.getSelectedItem();
        boolean showStatic = behavior == EmptyBucketBehavior.USE_STATIC_VALUE;
        boolean showRegex = behavior == EmptyBucketBehavior.GENERATE_FROM_REGEX;

        staticValueLabel.setVisible(showStatic);
        staticValueField.setVisible(showStatic);
        generatorRegexLabel.setVisible(showRegex);
        generatorRegexField.setVisible(showRegex);
    }

    private JPanel createTokensPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top panel with token count, refresh button, and display length setting
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        tokenCountLabel = new JLabel("Token count: 0");
        topPanel.add(tokenCountLabel);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshTokenDisplay());
        topPanel.add(refreshButton);

        // Add display length control
        topPanel.add(new JLabel("Display Length:"));
        SpinnerNumberModel displayLengthModel = new SpinnerNumberModel(100, 0, 1000, 10);
        JSpinner displayLengthSpinner = new JSpinner(displayLengthModel);
        displayLengthSpinner.setPreferredSize(new Dimension(70, displayLengthSpinner.getPreferredSize().height));
        displayLengthSpinner.setToolTipText("Maximum characters to display (0 = no limit)");

        // Add change listener to model for immediate updates
        displayLengthModel.addChangeListener(e -> {
            int newValue = displayLengthModel.getNumber().intValue();

            // Round up to nearest 10 (except 0)
            if (newValue > 0 && newValue % 10 != 0) {
                newValue = ((newValue / 10) + 1) * 10;
                // Avoid infinite loop by only setting if different
                if (newValue != displayLengthModel.getNumber().intValue()) {
                    displayLengthModel.setValue(newValue);
                    return; // The setValue will trigger this listener again with the rounded value
                }
            }

            tokenDisplayLength = newValue;
            refreshTokenDisplay();
        });

        topPanel.add(displayLengthSpinner);

        panel.add(topPanel, BorderLayout.NORTH);

        // Left panel with vertical buttons
        JPanel leftButtonPanel = new JPanel();
        leftButtonPanel.setLayout(new BoxLayout(leftButtonPanel, BoxLayout.Y_AXIS));
        leftButtonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));

        JButton addTokenButton = new JButton("Add Token");
        addTokenButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, addTokenButton.getPreferredSize().height));
        addTokenButton.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        addTokenButton.addActionListener(e -> addToken());
        leftButtonPanel.add(addTokenButton);
        leftButtonPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        JButton copyAllButton = new JButton("Copy Tokens");
        copyAllButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, copyAllButton.getPreferredSize().height));
        copyAllButton.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        copyAllButton.addActionListener(e -> copyAllTokens());
        leftButtonPanel.add(copyAllButton);
        leftButtonPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        JButton pasteButton = new JButton("Paste Tokens");
        pasteButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, pasteButton.getPreferredSize().height));
        pasteButton.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        pasteButton.addActionListener(e -> pasteTokens());
        leftButtonPanel.add(pasteButton);
        leftButtonPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        JButton deduplicateButton = new JButton("De-Duplicate Tokens");
        deduplicateButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, deduplicateButton.getPreferredSize().height));
        deduplicateButton.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        deduplicateButton.addActionListener(e -> {
            int removedCount = bucket.deduplicateTokens();
            refreshTokenDisplay();
            if (removedCount > 0) {
                JOptionPane.showMessageDialog(this,
                    "Removed " + removedCount + " duplicate token(s).",
                    "De-duplication Complete",
                    JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                    "No duplicate tokens found.",
                    "De-duplication Complete",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        });
        leftButtonPanel.add(deduplicateButton);
        leftButtonPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        JButton clearButton = new JButton("Clear Tokens");
        clearButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, clearButton.getPreferredSize().height));
        clearButton.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        clearButton.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this, "Clear all tokens from this bucket?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                bucket.clearTokens();
                refreshTokenDisplay();
            }
        });
        leftButtonPanel.add(clearButton);

        // Add gap
        leftButtonPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        JButton saveButton = new JButton("Save Tokens");
        saveButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, saveButton.getPreferredSize().height));
        saveButton.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        saveButton.addActionListener(e -> saveTokens());
        leftButtonPanel.add(saveButton);
        leftButtonPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        JButton loadButton = new JButton("Load Tokens");
        loadButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, loadButton.getPreferredSize().height));
        loadButton.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        loadButton.addActionListener(e -> loadTokens());
        leftButtonPanel.add(loadButton);

        // Add glue to push buttons to the top
        leftButtonPanel.add(Box.createVerticalGlue());

        panel.add(leftButtonPanel, BorderLayout.WEST);

        tokenTableModel = new TokenTableModel(bucket);
        tokenDisplayTable = new JTable(tokenTableModel);
        tokenDisplayTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        tokenDisplayTable.setFont(new Font("Monospaced", Font.PLAIN, 12));
        tokenDisplayTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        tokenDisplayTable.getColumnModel().getColumn(0).setMaxWidth(100);
        tokenDisplayTable.getColumnModel().getColumn(1).setPreferredWidth(500);

        // Set custom cell renderer for Token column to display newlines properly
        tokenDisplayTable.getColumnModel().getColumn(1).setCellRenderer(new TokenCellRenderer());

        // Set custom cell editor for Token column to support newline editing
        tokenDisplayTable.getColumnModel().getColumn(1).setCellEditor(new TokenCellEditor());

        DefaultTableCellRenderer renderer = (DefaultTableCellRenderer) tokenDisplayTable.getTableHeader().getDefaultRenderer();
        renderer.setHorizontalAlignment(SwingConstants.LEFT);
        tokenDisplayTable.getColumnModel().getColumn(1).setHeaderRenderer(renderer);

        // Enable drag and drop reordering
        tokenDisplayTable.setDragEnabled(true);
        tokenDisplayTable.setDropMode(DropMode.INSERT_ROWS);
        tokenDisplayTable.setTransferHandler(new TokenTableTransferHandler(tokenTableModel, this));

        // Add right-click context menu
        java.awt.event.MouseAdapter contextMenuListener = new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    // Convert point to table coordinates if coming from scroll pane
                    Point point = e.getPoint();
                    if (e.getSource() != tokenDisplayTable) {
                        point = SwingUtilities.convertPoint(e.getComponent(), point, tokenDisplayTable);
                    }

                    int row = tokenDisplayTable.rowAtPoint(point);
                    if (row >= 0) {
                        // If the clicked row is not in the current selection, change selection to just this row
                        if (!tokenDisplayTable.isRowSelected(row)) {
                            tokenDisplayTable.setRowSelectionInterval(row, row);
                        }
                        // Otherwise keep the current selection (which includes multiple rows)
                    } else {
                        tokenDisplayTable.clearSelection();
                    }
                    showTokenContextMenu(e);
                }
            }
        };

        tokenDisplayTable.addMouseListener(contextMenuListener);

        JScrollPane tokenScrollPane = new JScrollPane(tokenDisplayTable);
        tokenScrollPane.addMouseListener(contextMenuListener);
        panel.add(tokenScrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void showTokenContextMenu(java.awt.event.MouseEvent e) {
        JPopupMenu contextMenu = new JPopupMenu();
        int[] selectedRows = tokenDisplayTable.getSelectedRows();

        JMenuItem addItem = new JMenuItem("Add Token");
        addItem.addActionListener(event -> {
            TokenEditorDialog dialog = new TokenEditorDialog((Frame) SwingUtilities.getWindowAncestor(this), "");
            dialog.setVisible(true);

            String newToken = dialog.getEditedToken();
            if (newToken != null && !newToken.isEmpty()) {
                // Check if unique only mode is enabled and token already exists
                if (bucket.isUniqueOnly() && bucket.hasToken(newToken)) {
                    JOptionPane.showMessageDialog(this,
                        "Token already exists! This bucket only allows unique tokens.",
                        "Duplicate Token",
                        JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // Insert after the last selected row, or at the end if nothing selected
                int insertIndex = selectedRows.length > 0 ? selectedRows[selectedRows.length - 1] + 1 : bucket.getTokenCount();
                tokenTableModel.insertRow(insertIndex, newToken);
                refreshTokenDisplay();
            }
        });
        contextMenu.add(addItem);

        if (selectedRows.length > 0) {
            // Add Copy menu item
            String copyLabel = selectedRows.length == 1 ? "Copy Token" : "Copy Tokens (" + selectedRows.length + ")";
            JMenuItem copyItem = new JMenuItem(copyLabel);
            copyItem.addActionListener(event -> {
                java.util.List<String> selectedTokens = new java.util.ArrayList<>();
                java.util.List<String> allTokens = bucket.getAllTokens();
                for (int row : selectedRows) {
                    if (row < allTokens.size()) {
                        selectedTokens.add(allTokens.get(row));
                    }
                }
                String tokensText = String.join("\n", selectedTokens);
                java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(tokensText);
                java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
            });
            contextMenu.add(copyItem);

            // Add Delete menu item
            String deleteLabel = selectedRows.length == 1 ? "Delete Token" : "Delete Tokens (" + selectedRows.length + ")";
            JMenuItem deleteItem = new JMenuItem(deleteLabel);
            deleteItem.addActionListener(event -> {
                // Delete in reverse order to maintain correct indices
                for (int i = selectedRows.length - 1; i >= 0; i--) {
                    tokenTableModel.removeRow(selectedRows[i]);
                }
                refreshTokenDisplay();
            });
            contextMenu.add(deleteItem);
        }

        contextMenu.show(e.getComponent(), e.getX(), e.getY());
    }

    public void refreshTokenDisplay() {
        java.util.List<String> tokens = bucket.getAllTokens();
        tokenCountLabel.setText("Token count: " + tokens.size());
        tokenTableModel.fireTableDataChanged();
        lastKnownTokenCount = tokens.size();
    }

    private void startTokenRefreshTimer() {
        // Create a timer that checks for token changes every 1 second
        tokenRefreshTimer = new Timer(1000, e -> {
            int currentTokenCount = bucket.getTokenCount();
            if (currentTokenCount != lastKnownTokenCount) {
                refreshTokenDisplay();
            }
        });
        tokenRefreshTimer.start();
    }

    private void stopTokenRefreshTimer() {
        if (tokenRefreshTimer != null) {
            tokenRefreshTimer.stop();
            tokenRefreshTimer = null;
        }
    }

    private void copyAllTokens() {
        java.util.List<String> tokens = bucket.getAllTokens();
        java.util.List<String> tokensToWrite = tokens;

        // Check for newlines in tokens
        if (util.TokenEncoder.hasTokensWithNewlines(tokens)) {
            // Show newline warning dialog
            NewlineTokenWarningDialog dialog = new NewlineTokenWarningDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                NewlineTokenWarningDialog.OperationType.COPY
            );
            dialog.setVisible(true);

            NewlineTokenWarningDialog.Action action = dialog.getSelectedAction();

            switch (action) {
                case ENCODE_TOKENS:
                    util.TokenEncoder.EncodingType encoding = dialog.getSelectedEncoding();
                    tokensToWrite = util.TokenEncoder.encodeAll(tokens, encoding);
                    break;
                case SKIP_TOKENS:
                    tokensToWrite = util.TokenEncoder.removeTokensWithNewlines(tokens);
                    break;
                case CANCEL:
                    return;
                case DO_NOTHING:
                    // Continue with original tokens
                    break;
            }
        }

        String tokensText = String.join("\n", tokensToWrite);
        java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(tokensText);
        java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
        JOptionPane.showMessageDialog(this, "Copied " + tokensToWrite.size() + " tokens to clipboard", "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    private void addToken() {
        TokenEditorDialog dialog = new TokenEditorDialog((Frame) SwingUtilities.getWindowAncestor(this), "");
        dialog.setVisible(true);

        String token = dialog.getEditedToken();

        if (token != null && !token.isEmpty()) {
            // Apply bucket rules: unique check
            if (bucket.isUniqueOnly() && bucket.getAllTokens().contains(token)) {
                JOptionPane.showMessageDialog(this,
                    "Token already exists in bucket (unique only is enabled)",
                    "Duplicate Token",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Check if bucket is full
            int maxSize = bucket.getMaxSize();
            if (maxSize > 0 && bucket.getTokenCount() >= maxSize) {
                BucketFullBehavior fullBehavior = bucket.getFullBehavior();
                if (fullBehavior == BucketFullBehavior.REJECT_NEW) {
                    JOptionPane.showMessageDialog(this,
                        "Bucket is full. Cannot add new token.",
                        "Bucket Full",
                        JOptionPane.WARNING_MESSAGE);
                    return;
                }
                // For REPLACE_LAST and REPLACE_OLDEST, bucket.addToken() will handle it automatically
            }

            bucket.addToken(token);
            refreshTokenDisplay();
        }
    }

    private void pasteTokens() {
        try {
            java.awt.datatransfer.Clipboard clipboard = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
            String clipboardText = (String) clipboard.getData(java.awt.datatransfer.DataFlavor.stringFlavor);

            if (clipboardText == null || clipboardText.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Clipboard is empty", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Show decoding dialog
            TokenDecodingDialog decodingDialog = new TokenDecodingDialog((Frame) SwingUtilities.getWindowAncestor(this), false);
            decodingDialog.setVisible(true);

            TokenDecodingDialog.Action decodingAction = decodingDialog.getSelectedAction();

            if (decodingAction == TokenDecodingDialog.Action.CANCEL) {
                return;
            }

            String[] newTokens = clipboardText.split("\n");

            // Apply decoding if selected
            if (decodingAction == TokenDecodingDialog.Action.DECODE) {
                util.TokenEncoder.EncodingType encoding = decodingDialog.getSelectedEncoding();
                java.util.List<String> tokensList = java.util.Arrays.asList(newTokens);
                tokensList = util.TokenEncoder.decodeAll(tokensList, encoding);
                newTokens = tokensList.toArray(new String[0]);
            }
            int tokenCount = bucket.getTokenCount();
            int tokensBeforeAdd = 0;
            int nonEmptyTokenCount = 0;

            // Count non-empty tokens
            for (String token : newTokens) {
                if (!token.trim().isEmpty()) {
                    nonEmptyTokenCount++;
                }
            }

            if (tokenCount > 0) {
                int choice = JOptionPane.showOptionDialog(this,
                    "Bucket already contains tokens. What would you like to do?",
                    "Paste Tokens",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    new String[]{"Replace", "Add", "Cancel"},
                    "Cancel");

                if (choice == 0) { // Replace
                    bucket.clearTokens();
                    tokensBeforeAdd = 0;
                    for (String token : newTokens) {
                        if (!token.trim().isEmpty()) {
                            bucket.addToken(token.trim());
                        }
                    }
                } else if (choice == 1) { // Add
                    tokensBeforeAdd = bucket.getTokenCount();
                    for (String token : newTokens) {
                        if (!token.trim().isEmpty()) {
                            bucket.addToken(token.trim());
                        }
                    }
                } else { // Cancel
                    return;
                }
            } else {
                tokensBeforeAdd = 0;
                for (String token : newTokens) {
                    if (!token.trim().isEmpty()) {
                        bucket.addToken(token.trim());
                    }
                }
            }

            refreshTokenDisplay();

            int tokensAfterAdd = bucket.getTokenCount();
            int tokensActuallyAdded = tokensAfterAdd - tokensBeforeAdd;

            String message;
            if (tokensActuallyAdded == nonEmptyTokenCount) {
                message = "Pasted " + nonEmptyTokenCount + " tokens";
            } else {
                message = "Pasted " + nonEmptyTokenCount + " tokens (" + tokensActuallyAdded + " added, " +
                         (nonEmptyTokenCount - tokensActuallyAdded) + " duplicates removed)";
            }

            JOptionPane.showMessageDialog(this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error pasting tokens: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveTokens() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Tokens");
        fileChooser.setSelectedFile(new java.io.File(bucket.getName() + "_tokens.txt"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                java.io.File file = fileChooser.getSelectedFile();

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

                java.util.List<String> tokens = bucket.getAllTokens();

                // Check for newlines in tokens
                if (util.TokenEncoder.hasTokensWithNewlines(tokens)) {
                    NewlineTokenWarningDialog dialog = new NewlineTokenWarningDialog(
                        (Frame) SwingUtilities.getWindowAncestor(this),
                        NewlineTokenWarningDialog.OperationType.SAVE
                    );
                    dialog.setVisible(true);

                    NewlineTokenWarningDialog.Action action = dialog.getSelectedAction();

                    switch (action) {
                        case ENCODE_TOKENS:
                            util.TokenEncoder.EncodingType encoding = dialog.getSelectedEncoding();
                            tokens = util.TokenEncoder.encodeAll(tokens, encoding);
                            break;
                        case SKIP_TOKENS:
                            tokens = util.TokenEncoder.removeTokensWithNewlines(tokens);
                            break;
                        case CANCEL:
                            return;
                        case DO_NOTHING:
                            // Continue with original tokens
                            break;
                    }
                }

                java.nio.file.Files.write(file.toPath(), tokens);
                JOptionPane.showMessageDialog(this, "Saved " + tokens.size() + " tokens to " + file.getName(), "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error saving tokens: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void loadTokens() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Load Tokens");

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                java.io.File file = fileChooser.getSelectedFile();
                java.util.List<String> newTokens = java.nio.file.Files.readAllLines(file.toPath());

                // Ask user if tokens need to be decoded
                TokenDecodingDialog decodingDialog = new TokenDecodingDialog((Frame) SwingUtilities.getWindowAncestor(this));
                decodingDialog.setVisible(true);

                TokenDecodingDialog.Action decodingAction = decodingDialog.getSelectedAction();

                // If user cancels, abort the load
                if (decodingAction == TokenDecodingDialog.Action.CANCEL) {
                    return;
                }

                // Apply decoding if requested
                if (decodingAction == TokenDecodingDialog.Action.DECODE) {
                    util.TokenEncoder.EncodingType encoding = decodingDialog.getSelectedEncoding();
                    newTokens = util.TokenEncoder.decodeAll(newTokens, encoding);
                }
                // If DO_NOTHING, leave newTokens as-is

                int tokenCount = bucket.getTokenCount();

                if (tokenCount > 0) {
                    int choice = JOptionPane.showOptionDialog(this,
                        "Bucket already contains tokens. What would you like to do?",
                        "Load Tokens",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        new String[]{"Replace", "Add", "Cancel"},
                        "Cancel");

                    if (choice == 0) { // Replace
                        bucket.clearTokens();
                        for (String token : newTokens) {
                            if (!token.trim().isEmpty()) {
                                bucket.addToken(token.trim());
                            }
                        }
                    } else if (choice == 1) { // Add
                        for (String token : newTokens) {
                            if (!token.trim().isEmpty()) {
                                bucket.addToken(token.trim());
                            }
                        }
                    } else { // Cancel
                        return;
                    }
                } else {
                    for (String token : newTokens) {
                        if (!token.trim().isEmpty()) {
                            bucket.addToken(token.trim());
                        }
                    }
                }

                refreshTokenDisplay();
                JOptionPane.showMessageDialog(this, "Loaded " + newTokens.size() + " tokens from " + file.getName(), "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error loading tokens: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void loadFromBucket() {
        CollectionRule collectionRule = bucket.getCollectionRule();
        collectFromRequestsCheck.setSelected(collectionRule.isCollectFromRequests());
        collectFromResponsesCheck.setSelected(collectionRule.isCollectFromResponses());

        for (Map.Entry<ToolType, JCheckBox> entry : collectionToolCheckboxes.entrySet()) {
            entry.getValue().setSelected(collectionRule.getEnabledTools().contains(entry.getKey()));
        }

        matchInScopeUrlsCheck.setSelected(collectionRule.isMatchInScopeUrls());

        postProcessingScriptArea.setText(collectionRule.getPostProcessingScript());

        bucketEnabledCheck.setSelected(bucket.isEnabled());
        bucketTypeCombo.setSelectedItem(bucket.getBucketType());
        maxSizeSpinner.setValue(bucket.getMaxSize());
        lastCommittedMaxSize = bucket.getMaxSize(); // Initialize the tracked value
        fullBehaviorCombo.setSelectedItem(bucket.getFullBehavior());
        uniqueOnlyCheck.setSelected(bucket.isUniqueOnly());

        ReplacementConfig replacementConfig = bucket.getReplacementConfig();
        replaceInRequestsCheck.setSelected(replacementConfig.isReplaceInRequests());
        replaceInResponsesCheck.setSelected(replacementConfig.isReplaceInResponses());

        for (Map.Entry<ToolType, JCheckBox> entry : replacementToolCheckboxes.entrySet()) {
            entry.getValue().setSelected(replacementConfig.getEnabledTools().contains(entry.getKey()));
        }

        lastTokenBehaviorCombo.setSelectedItem(replacementConfig.getLastTokenBehavior());

        emptyBucketBehaviorCombo.setSelectedItem(replacementConfig.getEmptyBucketBehavior());
        staticValueField.setText(replacementConfig.getStaticValue());
        generatorRegexField.setText(replacementConfig.getGeneratorRegex());
        updateEmptyBucketFieldsVisibility();

        replacementMatchInScopeUrlsCheck.setSelected(replacementConfig.isMatchInScopeUrls());
        preReplacementScriptArea.setText(replacementConfig.getPreReplacementScript());

        // Replacement rules table is already initialized with the bucket's list
        replacementRulesModel.fireTableDataChanged();
        replacementUrlMatcherModel.fireTableDataChanged();

        refreshTokenDisplay();
    }

    public void updateToolCheckboxesFromGlobalControls(GlobalControls globalControls) {
        // Update collection tool checkboxes
        for (Map.Entry<ToolType, JCheckBox> entry : collectionToolCheckboxes.entrySet()) {
            ToolType toolType = entry.getKey();
            JCheckBox checkbox = entry.getValue();
            boolean globallyEnabled = globalControls.isCollectionEnabledForTool(toolType);
            checkbox.setEnabled(globallyEnabled);
        }

        // Update replacement tool checkboxes
        for (Map.Entry<ToolType, JCheckBox> entry : replacementToolCheckboxes.entrySet()) {
            ToolType toolType = entry.getKey();
            JCheckBox checkbox = entry.getValue();
            boolean globallyEnabled = globalControls.isReplacementEnabledForTool(toolType);
            checkbox.setEnabled(globallyEnabled);
        }
    }

    private void updatePostCollectionPreview() {
        String script = postProcessingScriptArea.getText();
        String testToken = postCollectionTestTokenField.getText();

        try {
            String result = jsProcessor.processTokenWithException(testToken, script);
            postCollectionPreviewArea.setText(result);
        } catch (Exception e) {
            postCollectionPreviewArea.setText("Error: " + e.getMessage());
        }
    }

    private void updatePreReplacementPreview() {
        String script = preReplacementScriptArea.getText();
        String testToken = preReplacementTestTokenField.getText();

        try {
            String result = jsProcessor.processTokenWithException(testToken, script);
            preReplacementPreviewArea.setText(result);
        } catch (Exception e) {
            preReplacementPreviewArea.setText("Error: " + e.getMessage());
        }
    }

    private ToolType[] getCommonToolTypes() {
        java.util.List<ToolType> toolTypes = new java.util.ArrayList<>();
        toolTypes.add(ToolType.TARGET);
        toolTypes.add(ToolType.PROXY);
        toolTypes.add(ToolType.INTRUDER);
        toolTypes.add(ToolType.REPEATER);
        toolTypes.add(ToolType.SCANNER);
        toolTypes.add(ToolType.SEQUENCER);
        toolTypes.add(ToolType.EXTENSIONS);

        // Only add BURP_AI if it's available (Pro edition only)
        if (util.BurpEditionDetector.isBurpAiAvailable()) {
            ToolType burpAi = util.BurpEditionDetector.getBurpAiToolType();
            if (burpAi != null) {
                toolTypes.add(burpAi);
            }
        }

        return toolTypes.toArray(new ToolType[0]);
    }

    // Table models
    private static class UrlMatcherTableModel extends AbstractTableModel {
        private final java.util.List<UrlMatcher> matchers;
        private final String[] columnNames = {"Enabled", "Protocol", "Host", "Port", "Path"};

        public UrlMatcherTableModel(java.util.List<UrlMatcher> matchers) {
            this.matchers = matchers;
        }

        @Override
        public int getRowCount() { return matchers.size(); }

        @Override
        public int getColumnCount() { return columnNames.length; }

        @Override
        public String getColumnName(int column) { return columnNames[column]; }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0) {
                return Boolean.class;
            }
            return String.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 0;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            UrlMatcher matcher = matchers.get(rowIndex);
            switch (columnIndex) {
                case 0: return matcher.isEnabled();
                case 1: return matcher.getProtocol();
                case 2: return matcher.getHost();
                case 3: return matcher.getPort();
                case 4: return matcher.getPath();
                default: return null;
            }
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            if (columnIndex == 0 && value instanceof Boolean) {
                matchers.get(rowIndex).setEnabled((Boolean) value);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }

        public void addMatcher(UrlMatcher matcher) {
            matchers.add(matcher);
            fireTableRowsInserted(matchers.size() - 1, matchers.size() - 1);
        }

        public void removeMatcher(int index) {
            matchers.remove(index);
            fireTableRowsDeleted(index, index);
        }

        public UrlMatcher getMatcher(int index) {
            return matchers.get(index);
        }

        public void moveUp(int index) {
            if (index > 0) {
                UrlMatcher temp = matchers.get(index);
                matchers.set(index, matchers.get(index - 1));
                matchers.set(index - 1, temp);
                fireTableRowsUpdated(index - 1, index);
            }
        }

        public void moveDown(int index) {
            if (index < matchers.size() - 1) {
                UrlMatcher temp = matchers.get(index);
                matchers.set(index, matchers.get(index + 1));
                matchers.set(index + 1, temp);
                fireTableRowsUpdated(index, index + 1);
            }
        }
    }

    private static class RegexPatternTableModel extends AbstractTableModel {
        private final java.util.List<RegexPattern> patterns;
        private final String[] columnNames = {"Enabled", "Pattern", "Match Requests", "Match Responses", "Comment", "Has Script"};

        public RegexPatternTableModel(java.util.List<RegexPattern> patterns) {
            this.patterns = patterns;
        }

        @Override
        public int getRowCount() { return patterns.size(); }

        @Override
        public int getColumnCount() { return columnNames.length; }

        @Override
        public String getColumnName(int column) { return columnNames[column]; }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0 || columnIndex == 2 || columnIndex == 3) {
                return Boolean.class;
            }
            return String.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 0;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            RegexPattern pattern = patterns.get(rowIndex);
            switch (columnIndex) {
                case 0: return pattern.isEnabled();
                case 1: return pattern.getPattern();
                case 2: return pattern.isMatchRequests();
                case 3: return pattern.isMatchResponses();
                case 4: return pattern.getComment();
                case 5: return (pattern.getPostProcessingScript() != null && !pattern.getPostProcessingScript().isEmpty()) ? "Yes" : "No";
                default: return null;
            }
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            if (columnIndex == 0 && value instanceof Boolean) {
                patterns.get(rowIndex).setEnabled((Boolean) value);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }

        public void addPattern(RegexPattern pattern) {
            patterns.add(pattern);
            fireTableRowsInserted(patterns.size() - 1, patterns.size() - 1);
        }

        public void removePattern(int index) {
            patterns.remove(index);
            fireTableRowsDeleted(index, index);
        }

        public RegexPattern getPattern(int index) {
            return patterns.get(index);
        }

        public void moveUp(int index) {
            if (index > 0) {
                RegexPattern temp = patterns.get(index);
                patterns.set(index, patterns.get(index - 1));
                patterns.set(index - 1, temp);
                fireTableRowsUpdated(index - 1, index);
            }
        }

        public void moveDown(int index) {
            if (index < patterns.size() - 1) {
                RegexPattern temp = patterns.get(index);
                patterns.set(index, patterns.get(index + 1));
                patterns.set(index + 1, temp);
                fireTableRowsUpdated(index, index + 1);
            }
        }
    }

    private static class ReplacementRuleTableModel extends AbstractTableModel {
        private final java.util.List<ReplacementRule> rules;
        private final String[] columnNames = {"Enabled", "Location", "Field/Pattern", "Apply to Requests", "Apply to Responses", "Has Script"};

        public ReplacementRuleTableModel(java.util.List<ReplacementRule> rules) {
            this.rules = rules;
        }

        @Override
        public int getRowCount() { return rules.size(); }

        @Override
        public int getColumnCount() { return columnNames.length; }

        @Override
        public String getColumnName(int column) { return columnNames[column]; }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0 || columnIndex == 3 || columnIndex == 4) {
                return Boolean.class;
            }
            return String.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 0;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            ReplacementRule rule = rules.get(rowIndex);
            switch (columnIndex) {
                case 0: return rule.isEnabled();
                case 1: return rule.getLocation().toString();
                case 2:
                    if (rule.getLocation() == ReplacementLocation.GENERIC_REGEX) {
                        return rule.getRegexPattern();
                    } else {
                        return rule.getFieldName();
                    }
                case 3: return rule.isApplyToRequests();
                case 4: return rule.isApplyToResponses();
                case 5: return (rule.getPreProcessingScript() != null && !rule.getPreProcessingScript().isEmpty()) ? "Yes" : "No";
                default: return null;
            }
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            if (columnIndex == 0 && value instanceof Boolean) {
                rules.get(rowIndex).setEnabled((Boolean) value);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }

        public void addRule(ReplacementRule rule) {
            rules.add(rule);
            fireTableRowsInserted(rules.size() - 1, rules.size() - 1);
        }

        public void removeRule(int index) {
            rules.remove(index);
            fireTableRowsDeleted(index, index);
        }

        public ReplacementRule getRule(int index) {
            return rules.get(index);
        }

        public void moveUp(int index) {
            if (index > 0) {
                ReplacementRule temp = rules.get(index);
                rules.set(index, rules.get(index - 1));
                rules.set(index - 1, temp);
                fireTableRowsUpdated(index - 1, index);
            }
        }

        public void moveDown(int index) {
            if (index < rules.size() - 1) {
                ReplacementRule temp = rules.get(index);
                rules.set(index, rules.get(index + 1));
                rules.set(index + 1, temp);
                fireTableRowsUpdated(index, index + 1);
            }
        }
    }

    private static class TokenTableModel extends AbstractTableModel {
        private final Bucket bucket;
        private final String[] columnNames = {"Index", "Token"};

        public TokenTableModel(Bucket bucket) {
            this.bucket = bucket;
        }

        @Override
        public int getRowCount() {
            return bucket.getTokenCount();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            java.util.List<String> tokens = bucket.getAllTokens();
            if (rowIndex >= tokens.size()) {
                return null;
            }

            switch (columnIndex) {
                case 0: return rowIndex;
                case 1: return tokens.get(rowIndex);
                default: return null;
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            // Only the Token column (column 1) is editable
            return columnIndex == 1;
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            if (columnIndex == 1 && value != null) {
                bucket.setTokenAt(rowIndex, value.toString());
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }

        public void removeRow(int rowIndex) {
            bucket.removeTokenAt(rowIndex);
            fireTableRowsDeleted(rowIndex, rowIndex);
        }

        public void insertRow(int rowIndex, String token) {
            bucket.insertTokenAt(rowIndex, token);
            fireTableRowsInserted(rowIndex, rowIndex);
        }

        public void moveRow(int fromIndex, int toIndex) {
            bucket.moveToken(fromIndex, toIndex);
            if (fromIndex < toIndex) {
                fireTableRowsUpdated(fromIndex, toIndex);
            } else {
                fireTableRowsUpdated(toIndex, fromIndex);
            }
        }
    }

    private static class TokenTableTransferHandler extends TransferHandler {
        private final TokenTableModel model;
        private final BucketTab parent;
        private int draggedRowIndex = -1;

        public TokenTableTransferHandler(TokenTableModel model, BucketTab parent) {
            this.model = model;
            this.parent = parent;
        }

        @Override
        public int getSourceActions(JComponent c) {
            return MOVE;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            JTable table = (JTable) c;
            draggedRowIndex = table.getSelectedRow();
            if (draggedRowIndex >= 0) {
                String token = (String) model.getValueAt(draggedRowIndex, 1);
                return new StringSelection(token);
            }
            return null;
        }

        @Override
        public boolean canImport(TransferSupport support) {
            return support.isDataFlavorSupported(DataFlavor.stringFlavor);
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }

            JTable.DropLocation dropLocation = (JTable.DropLocation) support.getDropLocation();
            int dropRowIndex = dropLocation.getRow();

            if (draggedRowIndex >= 0 && dropRowIndex >= 0 && draggedRowIndex != dropRowIndex) {
                // Adjust drop index if dragging down
                if (draggedRowIndex < dropRowIndex) {
                    dropRowIndex--;
                }

                model.moveRow(draggedRowIndex, dropRowIndex);
                parent.refreshTokenDisplay();
                return true;
            }

            return false;
        }

        @Override
        protected void exportDone(JComponent source, Transferable data, int action) {
            draggedRowIndex = -1;
        }
    }

    /**
     * Custom cell renderer for displaying tokens with newlines.
     * Shows newlines as ↵ symbol and provides tooltip with escaped token.
     */
    private class TokenCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (value != null && comp instanceof JLabel) {
                String token = value.toString();
                JLabel label = (JLabel) comp;

                // Replace newlines and carriage returns with visible symbols for display
                String displayText = token
                    .replace("\r\n", "↵")
                    .replace("\n", "↵")
                    .replace("\r", "↵")
                    .replace("\t", "→");

                // Truncate if configured (center truncation)
                if (tokenDisplayLength > 0 && displayText.length() > tokenDisplayLength) {
                    int halfLength = tokenDisplayLength / 2;
                    int startEnd = halfLength - 1; // -1 for the ellipsis character
                    if (startEnd > 0) {
                        displayText = displayText.substring(0, startEnd) + "…" +
                                    displayText.substring(displayText.length() - startEnd);
                    } else {
                        // If display length is very small, just show ellipsis
                        displayText = "…";
                    }
                }

                label.setText(displayText);

                // Set tooltip to show full escaped token representation (no truncation)
                String tooltipText = token
                    .replace("\r", "\\r")
                    .replace("\n", "\\n")
                    .replace("\t", "\\t");

                label.setToolTipText(tooltipText);
            }

            return comp;
        }
    }

    /**
     * Custom cell editor that opens a dialog for editing tokens with newline support.
     */
    private class TokenCellEditor extends AbstractCellEditor implements TableCellEditor {
        private String currentValue;

        @Override
        public boolean isCellEditable(java.util.EventObject e) {
            // Require double-click to start editing (like standard JTable behavior)
            if (e instanceof java.awt.event.MouseEvent) {
                return ((java.awt.event.MouseEvent) e).getClickCount() >= 2;
            }
            return true; // Allow other events (e.g., keyboard)
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            currentValue = value != null ? value.toString() : "";

            // Open the editor dialog
            SwingUtilities.invokeLater(() -> {
                TokenEditorDialog dialog = new TokenEditorDialog(
                    (Frame) SwingUtilities.getWindowAncestor(BucketTab.this),
                    currentValue
                );
                dialog.setVisible(true);

                if (dialog.isConfirmed()) {
                    currentValue = dialog.getEditedToken();
                    fireEditingStopped();
                } else {
                    fireEditingCanceled();
                }
            });

            // Return a temporary label for display during editing
            JLabel label = new JLabel(currentValue);
            label.setFont(table.getFont());
            return label;
        }

        @Override
        public Object getCellEditorValue() {
            return currentValue;
        }
    }
}
