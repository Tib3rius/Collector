package ui;

import model.RegexPattern;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class RegexPatternDialog extends JDialog {
    private RegexPattern regexPattern;
    private boolean confirmed = false;
    private final core.JavaScriptProcessor jsProcessor;
    private final boolean defaultMatchRequests;
    private final boolean defaultMatchResponses;

    private JTextField patternField;
    private JTextField commentField;
    private JCheckBox dotallCheckBox;
    private JCheckBox multilineCheckBox;
    private JCheckBox matchRequestsCheckBox;
    private JCheckBox matchResponsesCheckBox;
    private JTextArea postProcessingScriptArea;
    private JTextField patternTestTokenField;
    private JTextArea patternPreviewArea;
    private JTextArea testInputArea;
    private JTextArea testOutputArea;

    public RegexPatternDialog(Frame owner, RegexPattern existingPattern, burp.api.montoya.MontoyaApi api, boolean collectFromRequests, boolean collectFromResponses) {
        super(owner, "Collection Pattern", true);
        this.regexPattern = existingPattern != null ? existingPattern : new RegexPattern();
        this.jsProcessor = new core.JavaScriptProcessor(api.logging(), api);
        this.defaultMatchRequests = collectFromRequests;
        this.defaultMatchResponses = collectFromResponses;

        initComponents();
        if (existingPattern != null) {
            loadFromPattern();
        } else {
            // Set default values for new patterns based on bucket collection sources
            matchRequestsCheckBox.setSelected(collectFromRequests);
            matchResponsesCheckBox.setSelected(collectFromResponses);
        }

        setSize(700, 650);
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top panel - Pattern and comment
        JPanel topPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Pattern
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1;
        topPanel.add(new JLabel("Pattern:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        patternField = new JTextField(40);
        patternField.setFont(new Font("Monospaced", Font.PLAIN, 12));
        patternField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { testPattern(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { testPattern(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { testPattern(); }
        });
        topPanel.add(patternField, gbc);

        // Info
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2; gbc.weighty = 0.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        JTextArea infoLabel = new JTextArea("Use capturing groups () to extract values. Group 1 will be extracted.\nUse non-capturing groups (?:) if necessary.");
        infoLabel.setEditable(false);
        infoLabel.setFocusable(false);
        infoLabel.setLineWrap(true);
        infoLabel.setWrapStyleWord(true);
        infoLabel.setOpaque(false);
        infoLabel.setBorder(null);
        infoLabel.setFont(UIManager.getFont("Label.font"));
        topPanel.add(infoLabel, gbc);

        // Regex flags section
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel flagsSection = new JPanel(new GridBagLayout());
        GridBagConstraints flagsGbc = new GridBagConstraints();
        flagsGbc.insets = new Insets(5, 0, 5, 0);
        flagsGbc.anchor = GridBagConstraints.WEST;
        flagsGbc.fill = GridBagConstraints.HORIZONTAL;

        // Regex flags label
        flagsGbc.gridx = 0; flagsGbc.gridy = 0; flagsGbc.gridwidth = 2; flagsGbc.weightx = 1.0;
        JLabel flagsLabel = new JLabel("Regex Flags:");
        flagsLabel.setFont(flagsLabel.getFont().deriveFont(Font.BOLD));
        flagsSection.add(flagsLabel, flagsGbc);

        // Regex flags checkboxes
        flagsGbc.gridy = 1; flagsGbc.gridwidth = 1; flagsGbc.insets = new Insets(0, 10, 5, 15);
        JPanel regexFlagsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));

        dotallCheckBox = new JCheckBox("DOTALL Mode");
        dotallCheckBox.setToolTipText("Makes . match any character including newlines (\\n, \\r)");
        dotallCheckBox.addActionListener(e -> testPattern());
        regexFlagsPanel.add(dotallCheckBox);

        multilineCheckBox = new JCheckBox("MULTILINE Mode");
        multilineCheckBox.setToolTipText("Makes ^ and $ match at line boundaries, not just string start/end");
        multilineCheckBox.addActionListener(e -> testPattern());
        regexFlagsPanel.add(multilineCheckBox);

        flagsSection.add(regexFlagsPanel, flagsGbc);

        // Match sources label
        flagsGbc.gridx = 0; flagsGbc.gridy = 2; flagsGbc.gridwidth = 2; flagsGbc.insets = new Insets(10, 0, 5, 0);
        JLabel sourcesLabel = new JLabel("Collection Sources (where to apply this pattern):");
        sourcesLabel.setFont(sourcesLabel.getFont().deriveFont(Font.BOLD));
        flagsSection.add(sourcesLabel, flagsGbc);

        // Match sources checkboxes
        flagsGbc.gridy = 3; flagsGbc.gridwidth = 1; flagsGbc.insets = new Insets(0, 10, 5, 15);
        JPanel matchSourcesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));

        matchRequestsCheckBox = new JCheckBox("Match Requests");
        matchRequestsCheckBox.setToolTipText("This pattern will be applied to HTTP requests");
        matchSourcesPanel.add(matchRequestsCheckBox);

        matchResponsesCheckBox = new JCheckBox("Match Responses");
        matchResponsesCheckBox.setToolTipText("This pattern will be applied to HTTP responses");
        matchSourcesPanel.add(matchResponsesCheckBox);

        flagsSection.add(matchSourcesPanel, flagsGbc);

        topPanel.add(flagsSection, gbc);

        // Comment
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1; gbc.weightx = 0.0;
        topPanel.add(new JLabel("Comment:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        commentField = new JTextField(40);
        topPanel.add(commentField, gbc);

        // Post-processing Script with preview
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2; gbc.weightx = 1.0; gbc.weighty = 0.3; gbc.fill = GridBagConstraints.BOTH;
        JPanel scriptPanel = new JPanel(new BorderLayout(5, 5));

        // Header
        JPanel scriptHeaderPanel = new JPanel(new BorderLayout());
        JLabel scriptLabel = new JLabel("Post-Collection JavaScript");
        scriptLabel.setFont(scriptLabel.getFont().deriveFont(Font.BOLD, 14f));
        scriptHeaderPanel.add(scriptLabel, BorderLayout.NORTH);
        JLabel scriptDescLabel = new JLabel("Optional. Runs immediately after extracting token. (variable: token, must return string)");
        scriptDescLabel.setFont(scriptDescLabel.getFont().deriveFont(Font.PLAIN, 11f));
        scriptHeaderPanel.add(scriptDescLabel, BorderLayout.CENTER);
        scriptPanel.add(scriptHeaderPanel, BorderLayout.NORTH);

        // Script and preview side by side
        JPanel scriptAndPreviewPanel = new JPanel(new GridLayout(1, 2, 10, 0));

        // Script area on left
        postProcessingScriptArea = new JTextArea(4, 40);
        postProcessingScriptArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        postProcessingScriptArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updatePatternPreview(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updatePatternPreview(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updatePatternPreview(); }
        });
        scriptAndPreviewPanel.add(new JScrollPane(postProcessingScriptArea));

        // Preview area on right
        JPanel previewPanel = new JPanel(new BorderLayout(5, 5));

        JPanel testTokenPanel = new JPanel(new BorderLayout(5, 0));
        JLabel testTokenLabel = new JLabel("Test Token:");
        testTokenPanel.add(testTokenLabel, BorderLayout.WEST);
        patternTestTokenField = new JTextField("testToken");
        patternTestTokenField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updatePatternPreview(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updatePatternPreview(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updatePatternPreview(); }
        });
        testTokenPanel.add(patternTestTokenField, BorderLayout.CENTER);
        previewPanel.add(testTokenPanel, BorderLayout.NORTH);

        JLabel previewLabel = new JLabel("Preview:");
        previewPanel.add(previewLabel, BorderLayout.CENTER);

        patternPreviewArea = new JTextArea(4, 40);
        patternPreviewArea.setEditable(false);
        patternPreviewArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        JScrollPane previewScrollPane = new JScrollPane(patternPreviewArea);
        previewPanel.add(previewScrollPane, BorderLayout.SOUTH);

        scriptAndPreviewPanel.add(previewPanel);

        scriptPanel.add(scriptAndPreviewPanel, BorderLayout.CENTER);
        topPanel.add(scriptPanel, gbc);

        add(topPanel, BorderLayout.NORTH);

        // Center panel - Regex tester
        JPanel testerPanel = new JPanel(new BorderLayout(5, 5));

        // Header
        JPanel testerHeaderPanel = new JPanel(new BorderLayout());
        JLabel testerLabel = new JLabel("Regex Tester");
        testerLabel.setFont(testerLabel.getFont().deriveFont(Font.BOLD, 14f));
        testerHeaderPanel.add(testerLabel, BorderLayout.NORTH);
        testerPanel.add(testerHeaderPanel, BorderLayout.NORTH);

        // Test input and output side by side
        JPanel inputOutputPanel = new JPanel(new GridLayout(1, 2, 10, 0));

        // Test input on left
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        JLabel inputLabel = new JLabel("Test Input:");
        inputPanel.add(inputLabel, BorderLayout.NORTH);
        testInputArea = new JTextArea(5, 50);
        testInputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        testInputArea.setLineWrap(true);
        testInputArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { testPattern(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { testPattern(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { testPattern(); }
        });
        inputPanel.add(new JScrollPane(testInputArea), BorderLayout.CENTER);
        inputOutputPanel.add(inputPanel);

        // Test output on right
        JPanel outputPanel = new JPanel(new BorderLayout(5, 5));
        JLabel outputLabel = new JLabel("Matches:");
        outputPanel.add(outputLabel, BorderLayout.NORTH);
        testOutputArea = new JTextArea(5, 50);
        testOutputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        testOutputArea.setEditable(false);
        testOutputArea.setLineWrap(true);
        outputPanel.add(new JScrollPane(testOutputArea), BorderLayout.CENTER);
        inputOutputPanel.add(outputPanel);

        testerPanel.add(inputOutputPanel, BorderLayout.CENTER);

        add(testerPanel, BorderLayout.CENTER);

        // Bottom panel - Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            if (validatePattern()) {
                saveToPattern();
                confirmed = true;
                dispose();
            }
        });
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void loadFromPattern() {
        patternField.setText(regexPattern.getPattern() != null ? regexPattern.getPattern() : "");
        commentField.setText(regexPattern.getComment() != null ? regexPattern.getComment() : "");
        dotallCheckBox.setSelected(regexPattern.isDotallMode());
        multilineCheckBox.setSelected(regexPattern.isMultilineMode());
        matchRequestsCheckBox.setSelected(regexPattern.isMatchRequests());
        matchResponsesCheckBox.setSelected(regexPattern.isMatchResponses());
        postProcessingScriptArea.setText(regexPattern.getPostProcessingScript() != null ? regexPattern.getPostProcessingScript() : "");
    }

    private void saveToPattern() {
        regexPattern.setPattern(patternField.getText().trim());
        regexPattern.setComment(commentField.getText().trim());
        regexPattern.setDotallMode(dotallCheckBox.isSelected());
        regexPattern.setMultilineMode(multilineCheckBox.isSelected());
        regexPattern.setMatchRequests(matchRequestsCheckBox.isSelected());
        regexPattern.setMatchResponses(matchResponsesCheckBox.isSelected());
        regexPattern.setPostProcessingScript(postProcessingScriptArea.getText());
    }

    private boolean validatePattern() {
        String pattern = patternField.getText().trim();
        if (pattern.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Pattern cannot be empty!", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        try {
            Pattern.compile(pattern);
            return true;
        } catch (PatternSyntaxException e) {
            JOptionPane.showMessageDialog(this,
                "Invalid regex pattern:\n" + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private void testPattern() {
        String pattern = patternField.getText().trim();
        String testInput = testInputArea.getText();

        if (pattern.isEmpty()) {
            testOutputArea.setText("Please enter a pattern.");
            return;
        }

        if (testInput.isEmpty()) {
            testOutputArea.setText("Please enter test input.");
            return;
        }

        try {
            // Build flags based on checkbox selections
            int flags = 0;
            if (dotallCheckBox.isSelected()) {
                flags |= Pattern.DOTALL;
            }
            if (multilineCheckBox.isSelected()) {
                flags |= Pattern.MULTILINE;
            }

            Pattern p = Pattern.compile(pattern, flags);
            Matcher m = p.matcher(testInput);

            StringBuilder result = new StringBuilder();
            int matchCount = 0;

            while (m.find()) {
                matchCount++;
                result.append("Match ").append(matchCount).append(":\n");
                result.append("  Full match: ").append(m.group(0)).append("\n");

                if (m.groupCount() > 0) {
                    result.append("  Captured groups:\n");
                    for (int i = 1; i <= m.groupCount(); i++) {
                        result.append("    Group ").append(i).append(": ").append(m.group(i)).append("\n");
                    }
                } else {
                    result.append("  No capturing groups defined.\n");
                }
                result.append("\n");
            }

            if (matchCount == 0) {
                testOutputArea.setText("No matches found.");
            } else {
                testOutputArea.setText(result.toString());
                testOutputArea.setCaretPosition(0);
            }

        } catch (PatternSyntaxException e) {
            testOutputArea.setText("Invalid regex pattern:\n" + e.getMessage());
        }
    }

    private void updatePatternPreview() {
        String script = postProcessingScriptArea.getText();
        String testToken = patternTestTokenField.getText();

        try {
            String result = jsProcessor.processTokenWithException(testToken, script);
            patternPreviewArea.setText(result);
        } catch (Exception e) {
            patternPreviewArea.setText("Error: " + e.getMessage());
        }
    }

    public RegexPattern getPattern() {
        return confirmed ? regexPattern : null;
    }
}
