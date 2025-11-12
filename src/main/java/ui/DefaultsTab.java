package ui;

import burp.api.montoya.core.ToolType;
import model.*;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class DefaultsTab extends JPanel {
    private final BucketDefaults defaults;
    private final Runnable onSaveCallback;
    private boolean isLoading = false;

    // Bucket Configuration
    private JCheckBox enabledCheck;
    private JComboBox<BucketType> bucketTypeCombo;
    private JSpinner maxSizeSpinner;
    private JComboBox<BucketFullBehavior> fullBehaviorCombo;
    private JCheckBox uniqueOnlyCheck;

    // Collection
    private JCheckBox collectFromRequestsCheck;
    private JCheckBox collectFromResponsesCheck;
    private Map<ToolType, JCheckBox> collectionToolCheckboxes;
    private JCheckBox collectionMatchInScopeUrlsCheck;

    // Replacement
    private JCheckBox replaceInRequestsCheck;
    private JCheckBox replaceInResponsesCheck;
    private Map<ToolType, JCheckBox> replacementToolCheckboxes;
    private JComboBox<LastTokenBehavior> lastTokenBehaviorCombo;
    private JComboBox<EmptyBucketBehavior> emptyBucketBehaviorCombo;
    private JTextField staticValueField;
    private JLabel staticValueLabel;
    private JTextField generatorRegexField;
    private JLabel generatorRegexLabel;
    private JCheckBox replacementMatchInScopeUrlsCheck;

    public DefaultsTab(BucketDefaults defaults, Runnable onSaveCallback) {
        this.defaults = defaults;
        this.onSaveCallback = onSaveCallback;
        this.collectionToolCheckboxes = new HashMap<>();
        this.replacementToolCheckboxes = new HashMap<>();

        setLayout(new BorderLayout());
        initComponents();
        loadFromDefaults();
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;

        // Description label at the top
        gbc.gridx = 0; gbc.gridy = 0;
        JTextArea descriptionArea = new JTextArea("These settings define the default configuration for newly created buckets. Changing these values will not affect existing buckets.");
        descriptionArea.setEditable(false);
        descriptionArea.setFocusable(false);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setOpaque(false);
        descriptionArea.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        descriptionArea.setFont(UIManager.getFont("Label.font"));
        mainPanel.add(descriptionArea, gbc);

        // Bucket Configuration section
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        mainPanel.add(createBucketConfigSection(), gbc);

        // Collection section
        gbc.gridy = 2;
        mainPanel.add(createCollectionSection(), gbc);

        // Replacement section
        gbc.gridy = 3;
        mainPanel.add(createReplacementSection(), gbc);

        // Filler to push content to top-left
        gbc.gridy = 4;
        gbc.weighty = 1.0;
        mainPanel.add(Box.createVerticalGlue(), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        mainPanel.add(Box.createHorizontalGlue(), gbc);

        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createBucketConfigSection() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;

        // Section heading
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        JLabel heading = new JLabel("Bucket Configuration");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 16f));
        heading.setForeground(new Color(0xd86633));
        panel.add(heading, gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
        enabledCheck = new JCheckBox("Enabled");
        enabledCheck.addActionListener(e -> autoSave());
        panel.add(enabledCheck, gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0;
        panel.add(new JLabel("Bucket Type:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.0;
        bucketTypeCombo = new JComboBox<>(BucketType.values());
        bucketTypeCombo.addActionListener(e -> autoSave());
        panel.add(bucketTypeCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.0;
        panel.add(new JLabel("Max Size (-1 = infinite):"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.0;
        maxSizeSpinner = new JSpinner(new SpinnerNumberModel(-1, -1, 100000, 1));
        maxSizeSpinner.addChangeListener(e -> autoSave());
        panel.add(maxSizeSpinner, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.0;
        panel.add(new JLabel("When Bucket is full:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.0;
        fullBehaviorCombo = new JComboBox<>(BucketFullBehavior.values());
        fullBehaviorCombo.addActionListener(e -> autoSave());
        panel.add(fullBehaviorCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        uniqueOnlyCheck = new JCheckBox("Only allow unique tokens (no duplicates)");
        uniqueOnlyCheck.addActionListener(e -> autoSave());
        panel.add(uniqueOnlyCheck, gbc);

        return panel;
    }

    private JPanel createCollectionSection() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;

        // Section heading
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel mainHeading = new JLabel("Token Collection");
        mainHeading.setFont(mainHeading.getFont().deriveFont(Font.BOLD, 16f));
        mainHeading.setForeground(new Color(0xd86633));
        panel.add(mainHeading, gbc);

        // Collection Sources
        gbc.gridy = 1;
        JLabel sourcesLabel = new JLabel("Collection Sources");
        sourcesLabel.setFont(sourcesLabel.getFont().deriveFont(Font.BOLD));
        panel.add(sourcesLabel, gbc);

        gbc.gridy = 2;
        JPanel sourcesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        // Add "All" checkbox for Collection Sources
        JCheckBox allCollectionSourcesCheck = new JCheckBox("All");
        allCollectionSourcesCheck.addActionListener(e -> {
            boolean selected = allCollectionSourcesCheck.isSelected();
            collectFromRequestsCheck.setSelected(selected);
            collectFromResponsesCheck.setSelected(selected);
            autoSave();
        });
        sourcesPanel.add(allCollectionSourcesCheck);

        // Add gap after "All" checkbox
        sourcesPanel.add(Box.createRigidArea(new Dimension(15, 0)));

        collectFromRequestsCheck = new JCheckBox("Requests");
        collectFromRequestsCheck.addActionListener(e -> autoSave());
        sourcesPanel.add(collectFromRequestsCheck);

        collectFromResponsesCheck = new JCheckBox("Responses");
        collectFromResponsesCheck.addActionListener(e -> autoSave());
        sourcesPanel.add(collectFromResponsesCheck);
        panel.add(sourcesPanel, gbc);

        // Collection Tools
        gbc.gridy = 3;
        JLabel toolsLabel = new JLabel("Collection Tools");
        toolsLabel.setFont(toolsLabel.getFont().deriveFont(Font.BOLD));
        panel.add(toolsLabel, gbc);

        gbc.gridy = 4;
        JPanel toolsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        // Add "All" checkbox for Collection Tools
        JCheckBox allCollectionToolsCheck = new JCheckBox("All");
        allCollectionToolsCheck.addActionListener(e -> {
            boolean selected = allCollectionToolsCheck.isSelected();
            for (ToolType toolType : getCommonToolTypes()) {
                JCheckBox checkbox = collectionToolCheckboxes.get(toolType);
                checkbox.setSelected(selected);
            }
            autoSave();
        });
        toolsPanel.add(allCollectionToolsCheck);

        // Add gap after "All" checkbox
        toolsPanel.add(Box.createRigidArea(new Dimension(15, 0)));

        for (ToolType toolType : getCommonToolTypes()) {
            JCheckBox checkbox = new JCheckBox(toolType.toolName());
            checkbox.addActionListener(e -> autoSave());
            collectionToolCheckboxes.put(toolType, checkbox);
            toolsPanel.add(checkbox);
        }
        panel.add(toolsPanel, gbc);

        // In-Scope URLs
        gbc.gridy = 5;
        JLabel urlsLabel = new JLabel("Collection URLs");
        urlsLabel.setFont(urlsLabel.getFont().deriveFont(Font.BOLD));
        panel.add(urlsLabel, gbc);

        gbc.gridy = 6;
        collectionMatchInScopeUrlsCheck = new JCheckBox("All In-Scope URLs");
        collectionMatchInScopeUrlsCheck.addActionListener(e -> autoSave());
        panel.add(collectionMatchInScopeUrlsCheck, gbc);

        return panel;
    }

    private JPanel createReplacementSection() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;

        // Section heading
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel mainHeading = new JLabel("Token Replacement");
        mainHeading.setFont(mainHeading.getFont().deriveFont(Font.BOLD, 16f));
        mainHeading.setForeground(new Color(0xd86633));
        panel.add(mainHeading, gbc);

        // Replacement Sinks
        gbc.gridy = 1;
        JLabel sinksLabel = new JLabel("Replacement Sinks");
        sinksLabel.setFont(sinksLabel.getFont().deriveFont(Font.BOLD));
        panel.add(sinksLabel, gbc);

        gbc.gridy = 2;
        JPanel sinksPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        // Add "All" checkbox for Replacement Sinks
        JCheckBox allReplacementSinksCheck = new JCheckBox("All");
        allReplacementSinksCheck.addActionListener(e -> {
            boolean selected = allReplacementSinksCheck.isSelected();
            replaceInRequestsCheck.setSelected(selected);
            replaceInResponsesCheck.setSelected(selected);
            autoSave();
        });
        sinksPanel.add(allReplacementSinksCheck);

        // Add gap after "All" checkbox
        sinksPanel.add(Box.createRigidArea(new Dimension(15, 0)));

        replaceInRequestsCheck = new JCheckBox("Requests");
        replaceInRequestsCheck.addActionListener(e -> autoSave());
        sinksPanel.add(replaceInRequestsCheck);

        replaceInResponsesCheck = new JCheckBox("Responses");
        replaceInResponsesCheck.addActionListener(e -> autoSave());
        sinksPanel.add(replaceInResponsesCheck);
        panel.add(sinksPanel, gbc);

        // Replacement Tools
        gbc.gridy = 3;
        JLabel toolsLabel = new JLabel("Replacement Tools");
        toolsLabel.setFont(toolsLabel.getFont().deriveFont(Font.BOLD));
        panel.add(toolsLabel, gbc);

        gbc.gridy = 4;
        JPanel toolsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        // Add "All" checkbox for Replacement Tools
        JCheckBox allReplacementToolsCheck = new JCheckBox("All");
        allReplacementToolsCheck.addActionListener(e -> {
            boolean selected = allReplacementToolsCheck.isSelected();
            for (ToolType toolType : getCommonToolTypes()) {
                JCheckBox checkbox = replacementToolCheckboxes.get(toolType);
                checkbox.setSelected(selected);
            }
            autoSave();
        });
        toolsPanel.add(allReplacementToolsCheck);

        // Add gap after "All" checkbox
        toolsPanel.add(Box.createRigidArea(new Dimension(15, 0)));

        for (ToolType toolType : getCommonToolTypes()) {
            JCheckBox checkbox = new JCheckBox(toolType.toolName());
            checkbox.addActionListener(e -> autoSave());
            replacementToolCheckboxes.put(toolType, checkbox);
            toolsPanel.add(checkbox);
        }
        panel.add(toolsPanel, gbc);

        // Last Token Behavior
        gbc.gridy = 5;
        gbc.gridx = 0;
        gbc.insets = new Insets(5, 5, 5, 5);
        JPanel lastTokenPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        lastTokenPanel.add(new JLabel("When using last token:"));
        lastTokenBehaviorCombo = new JComboBox<>(LastTokenBehavior.values());
        lastTokenBehaviorCombo.addActionListener(e -> autoSave());
        lastTokenPanel.add(lastTokenBehaviorCombo);
        panel.add(lastTokenPanel, gbc);

        // Empty Bucket Behavior
        gbc.gridy = 6;
        gbc.gridx = 0;
        JPanel emptyBucketPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        emptyBucketPanel.add(new JLabel("If bucket is empty:"));
        emptyBucketBehaviorCombo = new JComboBox<>(EmptyBucketBehavior.values());
        emptyBucketBehaviorCombo.addActionListener(e -> {
            updateEmptyBucketFieldsVisibility();
            autoSave();
        });
        emptyBucketPanel.add(emptyBucketBehaviorCombo);

        staticValueLabel = new JLabel("Value:");
        emptyBucketPanel.add(staticValueLabel);
        staticValueField = new JTextField(20);
        staticValueField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { autoSave(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { autoSave(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { autoSave(); }
        });
        emptyBucketPanel.add(staticValueField);

        generatorRegexLabel = new JLabel("Regex:");
        emptyBucketPanel.add(generatorRegexLabel);
        generatorRegexField = new JTextField(20);
        generatorRegexField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { autoSave(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { autoSave(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { autoSave(); }
        });
        emptyBucketPanel.add(generatorRegexField);

        panel.add(emptyBucketPanel, gbc);

        // In-Scope URLs
        gbc.gridy = 7;
        gbc.gridx = 0;
        gbc.insets = new Insets(5, 5, 5, 5);
        JLabel urlsLabel = new JLabel("Replacement URLs");
        urlsLabel.setFont(urlsLabel.getFont().deriveFont(Font.BOLD));
        panel.add(urlsLabel, gbc);

        gbc.gridy = 8;
        gbc.gridx = 0;
        replacementMatchInScopeUrlsCheck = new JCheckBox("All In-Scope URLs");
        replacementMatchInScopeUrlsCheck.addActionListener(e -> autoSave());
        panel.add(replacementMatchInScopeUrlsCheck, gbc);

        return panel;
    }

    private void updateEmptyBucketFieldsVisibility() {
        EmptyBucketBehavior behavior = (EmptyBucketBehavior) emptyBucketBehaviorCombo.getSelectedItem();
        boolean showStatic = behavior == EmptyBucketBehavior.USE_STATIC_VALUE;
        boolean showGenerator = behavior == EmptyBucketBehavior.GENERATE_FROM_REGEX;

        staticValueLabel.setVisible(showStatic);
        staticValueField.setVisible(showStatic);
        generatorRegexLabel.setVisible(showGenerator);
        generatorRegexField.setVisible(showGenerator);
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

    private void autoSave() {
        if (isLoading) {
            return; // Don't save while loading from model
        }
        saveToDefaults();
        if (onSaveCallback != null) {
            onSaveCallback.run();
        }
    }

    private void loadFromDefaults() {
        isLoading = true;
        try {
            enabledCheck.setSelected(defaults.isEnabled());
            bucketTypeCombo.setSelectedItem(defaults.getBucketType());
            maxSizeSpinner.setValue(defaults.getMaxSize());
            fullBehaviorCombo.setSelectedItem(defaults.getFullBehavior());
            uniqueOnlyCheck.setSelected(defaults.isUniqueOnly());

            collectFromRequestsCheck.setSelected(defaults.isCollectFromRequests());
            collectFromResponsesCheck.setSelected(defaults.isCollectFromResponses());
            for (Map.Entry<ToolType, JCheckBox> entry : collectionToolCheckboxes.entrySet()) {
                entry.getValue().setSelected(defaults.getCollectionEnabledTools().contains(entry.getKey()));
            }
            collectionMatchInScopeUrlsCheck.setSelected(defaults.isCollectionMatchInScopeUrls());

            replaceInRequestsCheck.setSelected(defaults.isReplaceInRequests());
            replaceInResponsesCheck.setSelected(defaults.isReplaceInResponses());
            for (Map.Entry<ToolType, JCheckBox> entry : replacementToolCheckboxes.entrySet()) {
                entry.getValue().setSelected(defaults.getReplacementEnabledTools().contains(entry.getKey()));
            }
            lastTokenBehaviorCombo.setSelectedItem(defaults.getLastTokenBehavior());
            emptyBucketBehaviorCombo.setSelectedItem(defaults.getEmptyBucketBehavior());
            staticValueField.setText(defaults.getStaticValue());
            generatorRegexField.setText(defaults.getGeneratorRegex());
            replacementMatchInScopeUrlsCheck.setSelected(defaults.isReplacementMatchInScopeUrls());

            // Update visibility based on empty bucket behavior
            updateEmptyBucketFieldsVisibility();
        } finally {
            isLoading = false;
        }
    }

    private void saveToDefaults() {
        defaults.setEnabled(enabledCheck.isSelected());
        defaults.setBucketType((BucketType) bucketTypeCombo.getSelectedItem());
        defaults.setMaxSize((Integer) maxSizeSpinner.getValue());
        defaults.setFullBehavior((BucketFullBehavior) fullBehaviorCombo.getSelectedItem());
        defaults.setUniqueOnly(uniqueOnlyCheck.isSelected());

        defaults.setCollectFromRequests(collectFromRequestsCheck.isSelected());
        defaults.setCollectFromResponses(collectFromResponsesCheck.isSelected());
        defaults.getCollectionEnabledTools().clear();
        for (Map.Entry<ToolType, JCheckBox> entry : collectionToolCheckboxes.entrySet()) {
            if (entry.getValue().isSelected()) {
                defaults.getCollectionEnabledTools().add(entry.getKey());
            }
        }
        defaults.setCollectionMatchInScopeUrls(collectionMatchInScopeUrlsCheck.isSelected());

        defaults.setReplaceInRequests(replaceInRequestsCheck.isSelected());
        defaults.setReplaceInResponses(replaceInResponsesCheck.isSelected());
        defaults.getReplacementEnabledTools().clear();
        for (Map.Entry<ToolType, JCheckBox> entry : replacementToolCheckboxes.entrySet()) {
            if (entry.getValue().isSelected()) {
                defaults.getReplacementEnabledTools().add(entry.getKey());
            }
        }
        defaults.setLastTokenBehavior((LastTokenBehavior) lastTokenBehaviorCombo.getSelectedItem());
        defaults.setEmptyBucketBehavior((EmptyBucketBehavior) emptyBucketBehaviorCombo.getSelectedItem());
        defaults.setStaticValue(staticValueField.getText());
        defaults.setGeneratorRegex(generatorRegexField.getText());
        defaults.setReplacementMatchInScopeUrls(replacementMatchInScopeUrlsCheck.isSelected());
    }

    public void updateFromModel() {
        loadFromDefaults();
    }
}
