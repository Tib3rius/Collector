package ui;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.params.HttpParameterType;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import model.ReplacementLocation;
import model.ReplacementRule;

import javax.swing.*;
import java.awt.*;

public class ReplacementRuleDialog extends JDialog {
    private ReplacementRule rule;
    private boolean confirmed = false;
    private MontoyaApi api;
    private final core.JavaScriptProcessor jsProcessor;
    private final boolean defaultApplyToRequests;
    private final boolean defaultApplyToResponses;

    private JComboBox<ReplacementLocation> locationCombo;
    private JLabel fieldNameLabel;
    private JTextField fieldNameField;
    private JLabel regexPatternLabel;
    private JTextField regexPatternField;
    private JLabel regexGroupLabel;
    private JSpinner regexGroupSpinner;
    private JLabel replaceAllLabel;
    private JCheckBox replaceAllCheckbox;
    private JCheckBox applyToRequestsCheckBox;
    private JCheckBox applyToResponsesCheckBox;
    private JTextArea preProcessingScriptArea;
    private JTextField scriptTestTokenField;
    private JTextArea scriptPreviewArea;
    private JComboBox<String> replacementTesterTypeCombo;
    private JTextField replacementTestTokenField;
    private JTextArea originalTextArea;
    private JTextArea modifiedTextArea;

    private static final String DUMMY_REQUEST = "POST /update?v=1 HTTP/2\r\n" +
            "Host: example.com\r\n" +
            "Content-Type: application/json\r\n" +
            "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36\r\n" +
            "Content-Length: 34\r\n" +
            "\r\n" +
            "{\n" +
            "  \"key1\": \"value1\",\n" +
            "  \"key2\": \"value2\"\n" +
            "}";

    private static final String DUMMY_RESPONSE = "HTTP/2 200 OK\r\n" +
            "Content-Type: text/html; charset=UTF-8\r\n" +
            "Server: server\r\n" +
            "Content-Length: 98\r\n" +
            "\r\n" +
            "<!DOCTYPE html>\n" +
            "<html>\n" +
            "    <title>Example</title>\n" +
            "    <head></head>\n" +
            "    <body></body>\n" +
            "</html>";

    public ReplacementRuleDialog(Frame owner, ReplacementRule existingRule, MontoyaApi api, boolean replaceInRequests, boolean replaceInResponses) {
        super(owner, "Replacement Rule", true);
        this.rule = existingRule != null ? existingRule : new ReplacementRule();
        this.api = api;
        this.jsProcessor = new core.JavaScriptProcessor(api.logging(), api);
        this.defaultApplyToRequests = replaceInRequests;
        this.defaultApplyToResponses = replaceInResponses;

        initComponents();
        if (existingRule != null) {
            loadFromRule();
        } else {
            // Set default values for new rules based on bucket replacement config
            applyToRequestsCheckBox.setSelected(replaceInRequests);
            applyToResponsesCheckBox.setSelected(replaceInResponses);
        }

        // Initialize the replacement tester constraints based on selected targets
        updateReplacementTesterConstraints();

        pack();
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Location
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Location:"), gbc);
        gbc.gridx = 1;
        locationCombo = new JComboBox<>(ReplacementLocation.values());
        // Custom renderer to show detailed descriptions in dropdown
        locationCombo.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ReplacementLocation) {
                    // Show detailed description in dropdown list
                    if (index >= 0) {
                        setText(((ReplacementLocation) value).getDescription());
                    } else {
                        // Show short name when selected/closed
                        setText(((ReplacementLocation) value).getShortName());
                    }
                }
                return this;
            }
        });
        locationCombo.addActionListener(e -> { updateFieldVisibility(); applyReplacement(); });
        formPanel.add(locationCombo, gbc);

        // Field Name
        gbc.gridx = 0; gbc.gridy = 1;
        fieldNameLabel = new JLabel("Field Name:");
        formPanel.add(fieldNameLabel, gbc);
        gbc.gridx = 1;
        fieldNameField = new JTextField(30);
        fieldNameField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { applyReplacement(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { applyReplacement(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { applyReplacement(); }
        });
        formPanel.add(fieldNameField, gbc);

        // Regex Pattern
        gbc.gridx = 0; gbc.gridy = 2;
        regexPatternLabel = new JLabel("Regex Pattern:");
        formPanel.add(regexPatternLabel, gbc);
        gbc.gridx = 1;
        regexPatternField = new JTextField(30);
        regexPatternField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { applyReplacement(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { applyReplacement(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { applyReplacement(); }
        });
        formPanel.add(regexPatternField, gbc);

        // Regex Group
        gbc.gridx = 0; gbc.gridy = 3;
        regexGroupLabel = new JLabel("Regex Group:");
        formPanel.add(regexGroupLabel, gbc);
        gbc.gridx = 1;
        regexGroupSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));
        regexGroupSpinner.addChangeListener(e -> applyReplacement());
        formPanel.add(regexGroupSpinner, gbc);

        // Replace All
        gbc.gridx = 0; gbc.gridy = 4;
        replaceAllLabel = new JLabel("Replace All:");
        formPanel.add(replaceAllLabel, gbc);
        gbc.gridx = 1;
        replaceAllCheckbox = new JCheckBox("Replace all occurrences (unchecked = replace first only)");
        replaceAllCheckbox.addActionListener(e -> applyReplacement());
        formPanel.add(replaceAllCheckbox, gbc);

        // Replacement sources section
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel sourcesSection = new JPanel(new GridBagLayout());
        GridBagConstraints sourcesGbc = new GridBagConstraints();
        sourcesGbc.insets = new Insets(5, 0, 5, 0);
        sourcesGbc.anchor = GridBagConstraints.WEST;
        sourcesGbc.fill = GridBagConstraints.HORIZONTAL;

        // Sources label
        sourcesGbc.gridx = 0; sourcesGbc.gridy = 0; sourcesGbc.gridwidth = 2; sourcesGbc.weightx = 1.0;
        JLabel sourcesLabel = new JLabel("Replacement Targets (where to apply this replacement):");
        sourcesLabel.setFont(sourcesLabel.getFont().deriveFont(Font.BOLD));
        sourcesSection.add(sourcesLabel, sourcesGbc);

        // Sources checkboxes
        sourcesGbc.gridy = 1; sourcesGbc.gridwidth = 1; sourcesGbc.insets = new Insets(0, 10, 5, 15);
        JPanel targetsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));

        applyToRequestsCheckBox = new JCheckBox("Requests");
        applyToRequestsCheckBox.setToolTipText("This replacement will be applied to HTTP requests");
        applyToRequestsCheckBox.addActionListener(e -> updateReplacementTesterConstraints());
        targetsPanel.add(applyToRequestsCheckBox);

        applyToResponsesCheckBox = new JCheckBox("Responses");
        applyToResponsesCheckBox.setToolTipText("This replacement will be applied to HTTP responses");
        applyToResponsesCheckBox.addActionListener(e -> updateReplacementTesterConstraints());
        targetsPanel.add(applyToResponsesCheckBox);

        sourcesSection.add(targetsPanel, sourcesGbc);
        formPanel.add(sourcesSection, gbc);

        // Pre-processing Script with preview
        gbc.gridx = 0; gbc.gridy = 6;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 0.5;
        JPanel scriptPanel = new JPanel(new BorderLayout(5, 5));

        // Header
        JPanel scriptHeaderPanel = new JPanel(new BorderLayout());
        JLabel scriptLabel = new JLabel("Pre-Replacement JavaScript");
        scriptLabel.setFont(scriptLabel.getFont().deriveFont(Font.BOLD, 14f));
        scriptHeaderPanel.add(scriptLabel, BorderLayout.NORTH);
        JLabel scriptDescLabel = new JLabel("Optional. Runs immediately before replacement. (variable: token, must return string)");
        scriptDescLabel.setFont(scriptDescLabel.getFont().deriveFont(Font.PLAIN, 11f));
        scriptHeaderPanel.add(scriptDescLabel, BorderLayout.CENTER);
        scriptPanel.add(scriptHeaderPanel, BorderLayout.NORTH);

        // Script and preview side by side
        JPanel scriptAndPreviewPanel = new JPanel(new GridLayout(1, 2, 10, 0));

        // Script area on left
        preProcessingScriptArea = new JTextArea(8, 40);
        preProcessingScriptArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        preProcessingScriptArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateScriptPreview(); applyReplacement(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateScriptPreview(); applyReplacement(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateScriptPreview(); applyReplacement(); }
        });
        scriptAndPreviewPanel.add(new JScrollPane(preProcessingScriptArea));

        // Preview area on right
        JPanel previewPanel = new JPanel(new BorderLayout(5, 5));

        JPanel testTokenPanel = new JPanel(new BorderLayout(5, 0));
        JLabel testTokenLabel = new JLabel("Test Token:");
        testTokenPanel.add(testTokenLabel, BorderLayout.WEST);
        scriptTestTokenField = new JTextField("testToken");
        scriptTestTokenField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateScriptPreview(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateScriptPreview(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateScriptPreview(); }
        });
        testTokenPanel.add(scriptTestTokenField, BorderLayout.CENTER);
        previewPanel.add(testTokenPanel, BorderLayout.NORTH);

        JLabel previewLabel = new JLabel("Preview:");
        previewPanel.add(previewLabel, BorderLayout.CENTER);

        scriptPreviewArea = new JTextArea(8, 40);
        scriptPreviewArea.setEditable(false);
        scriptPreviewArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane previewScrollPane = new JScrollPane(scriptPreviewArea);
        previewPanel.add(previewScrollPane, BorderLayout.SOUTH);

        scriptAndPreviewPanel.add(previewPanel);

        scriptPanel.add(scriptAndPreviewPanel, BorderLayout.CENTER);
        formPanel.add(scriptPanel, gbc);

        // Replacement Tester
        gbc.gridx = 0; gbc.gridy = 7;
        gbc.gridwidth = 2;
        gbc.weighty = 1.0;
        JPanel testerPanel = new JPanel(new BorderLayout(5, 5));

        // Header and controls
        JPanel testerTopPanel = new JPanel(new BorderLayout(5, 5));

        // Header
        JLabel testerLabel = new JLabel("Replacement Tester");
        testerLabel.setFont(testerLabel.getFont().deriveFont(Font.BOLD, 14f));
        testerTopPanel.add(testerLabel, BorderLayout.NORTH);

        // Type and Test Token on same line
        JPanel controlsPanel = new JPanel();
        controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.X_AXIS));

        JLabel typeLabel = new JLabel("Type:");
        controlsPanel.add(typeLabel);
        controlsPanel.add(Box.createRigidArea(new Dimension(5, 0)));
        replacementTesterTypeCombo = new JComboBox<>(new String[]{"Request", "Response"});
        replacementTesterTypeCombo.addActionListener(e -> loadDummyData());
        controlsPanel.add(replacementTesterTypeCombo);
        controlsPanel.add(Box.createRigidArea(new Dimension(20, 0)));

        JLabel replacementTestTokenLabel = new JLabel("Test Token:");
        controlsPanel.add(replacementTestTokenLabel);
        controlsPanel.add(Box.createRigidArea(new Dimension(5, 0)));
        replacementTestTokenField = new JTextField("testToken", 20);
        replacementTestTokenField.setMaximumSize(new Dimension(200, replacementTestTokenField.getPreferredSize().height));
        replacementTestTokenField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { applyReplacement(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { applyReplacement(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { applyReplacement(); }
        });
        controlsPanel.add(replacementTestTokenField);
        controlsPanel.add(Box.createHorizontalGlue());

        testerTopPanel.add(controlsPanel, BorderLayout.CENTER);
        testerPanel.add(testerTopPanel, BorderLayout.NORTH);

        // Original and Modified side by side
        JPanel originalModifiedPanel = new JPanel(new GridLayout(1, 2, 10, 0));

        // Original on left
        JPanel originalPanel = new JPanel(new BorderLayout(5, 5));
        JLabel originalLabel = new JLabel("Original:");
        originalPanel.add(originalLabel, BorderLayout.NORTH);
        originalTextArea = new JTextArea(10, 40);
        originalTextArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        originalTextArea.setLineWrap(false);
        originalTextArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { applyReplacement(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { applyReplacement(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { applyReplacement(); }
        });
        originalPanel.add(new JScrollPane(originalTextArea), BorderLayout.CENTER);
        originalModifiedPanel.add(originalPanel);

        // Modified on right
        JPanel modifiedPanel = new JPanel(new BorderLayout(5, 5));
        JLabel modifiedLabel = new JLabel("Modified:");
        modifiedPanel.add(modifiedLabel, BorderLayout.NORTH);
        modifiedTextArea = new JTextArea(10, 40);
        modifiedTextArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        modifiedTextArea.setEditable(false);
        modifiedTextArea.setLineWrap(false);
        modifiedPanel.add(new JScrollPane(modifiedTextArea), BorderLayout.CENTER);
        originalModifiedPanel.add(modifiedPanel);

        testerPanel.add(originalModifiedPanel, BorderLayout.CENTER);
        formPanel.add(testerPanel, gbc);

        add(formPanel, BorderLayout.CENTER);

        // Load initial dummy data
        loadDummyData();

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            saveToRule();
            confirmed = true;
            dispose();
        });
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);

        updateFieldVisibility();
    }

    private void updateFieldVisibility() {
        ReplacementLocation location = (ReplacementLocation) locationCombo.getSelectedItem();
        boolean isGenericRegex = location == ReplacementLocation.GENERIC_REGEX;

        // Show/hide Field Name (visible for Header, URL Parameter, Body Parameter, Cookie)
        fieldNameLabel.setVisible(!isGenericRegex);
        fieldNameField.setVisible(!isGenericRegex);

        // Show/hide Regex fields (visible only for Generic Regex)
        regexPatternLabel.setVisible(isGenericRegex);
        regexPatternField.setVisible(isGenericRegex);
        regexGroupLabel.setVisible(isGenericRegex);
        regexGroupSpinner.setVisible(isGenericRegex);
        replaceAllLabel.setVisible(isGenericRegex);
        replaceAllCheckbox.setVisible(isGenericRegex);

        // Repack to adjust dialog size
        pack();
    }

    private void updateReplacementTesterConstraints() {
        // Just refresh the preview - the applyReplacement method will check the constraints
        applyReplacement();
    }

    private void loadFromRule() {
        locationCombo.setSelectedItem(rule.getLocation());
        fieldNameField.setText(rule.getFieldName());
        regexPatternField.setText(rule.getRegexPattern());
        regexGroupSpinner.setValue(rule.getRegexGroup());
        replaceAllCheckbox.setSelected(rule.isReplaceAll());
        applyToRequestsCheckBox.setSelected(rule.isApplyToRequests());
        applyToResponsesCheckBox.setSelected(rule.isApplyToResponses());
        preProcessingScriptArea.setText(rule.getPreProcessingScript());
    }

    private void saveToRule() {
        rule.setLocation((ReplacementLocation) locationCombo.getSelectedItem());
        rule.setFieldName(fieldNameField.getText());
        rule.setRegexPattern(regexPatternField.getText());
        rule.setRegexGroup((Integer) regexGroupSpinner.getValue());
        rule.setReplaceAll(replaceAllCheckbox.isSelected());
        rule.setApplyToRequests(applyToRequestsCheckBox.isSelected());
        rule.setApplyToResponses(applyToResponsesCheckBox.isSelected());
        rule.setPreProcessingScript(preProcessingScriptArea.getText());
    }

    private void updateScriptPreview() {
        String script = preProcessingScriptArea.getText();
        String testToken = scriptTestTokenField.getText();

        try {
            String result = jsProcessor.processTokenWithException(testToken, script);
            scriptPreviewArea.setText(result);
        } catch (Exception e) {
            scriptPreviewArea.setText("Error: " + e.getMessage());
        }
    }

    private void loadDummyData() {
        String type = (String) replacementTesterTypeCombo.getSelectedItem();
        if ("Request".equals(type)) {
            originalTextArea.setText(DUMMY_REQUEST);
        } else {
            originalTextArea.setText(DUMMY_RESPONSE);
        }
    }

    private void applyReplacement() {
        try {
            String original = originalTextArea.getText();
            if (original == null || original.trim().isEmpty()) {
                modifiedTextArea.setText("");
                return;
            }

            // Normalize line endings to \r\n for proper HTTP parsing
            original = original.replace("\r\n", "\n").replace("\n", "\r\n");

            String testToken = replacementTestTokenField.getText();
            String type = (String) replacementTesterTypeCombo.getSelectedItem();

            if ("Request".equals(type)) {
                // Check if this rule applies to requests
                if (!applyToRequestsCheckBox.isSelected()) {
                    // Rule doesn't apply to requests - show original unchanged
                    modifiedTextArea.setText(original);
                    return;
                }
                HttpRequest request = HttpRequest.httpRequest(original);
                HttpRequest modified = applyReplacementToRequest(request, testToken);
                modifiedTextArea.setText(modified.toString());
            } else {
                // Check if this rule applies to responses
                if (!applyToResponsesCheckBox.isSelected()) {
                    // Rule doesn't apply to responses - show original unchanged
                    modifiedTextArea.setText(original);
                    return;
                }
                HttpResponse response = HttpResponse.httpResponse(original);
                HttpResponse modified = applyReplacementToResponse(response, testToken);
                modifiedTextArea.setText(modified.toString());
            }
        } catch (Exception e) {
            modifiedTextArea.setText("Error: " + e.getMessage());
        }
    }

    private HttpRequest applyReplacementToRequest(HttpRequest request, String tokenValue) {
        ReplacementLocation location = (ReplacementLocation) locationCombo.getSelectedItem();
        String fieldName = fieldNameField.getText();

        // Apply pre-processing script if defined
        String processedToken = tokenValue;
        String script = preProcessingScriptArea.getText();
        if (script != null && !script.trim().isEmpty()) {
            try {
                processedToken = jsProcessor.processTokenWithException(tokenValue, script);
            } catch (Exception e) {
                // On error, use original token
                processedToken = tokenValue;
            }
        }

        switch (location) {
            case HEADER:
                if (fieldName != null && !fieldName.trim().isEmpty()) {
                    // Remove existing header if present, then add new one
                    HttpRequest temp = request.withRemovedHeader(fieldName);
                    return temp.withAddedHeader(fieldName, processedToken);
                }
                break;
            case URL_PARAMETER:
                if (fieldName != null && !fieldName.trim().isEmpty()) {
                    return request.withParameter(burp.api.montoya.http.message.params.HttpParameter.parameter(fieldName, processedToken, HttpParameterType.URL));
                }
                break;
            case BODY_PARAMETER:
                if (fieldName != null && !fieldName.trim().isEmpty()) {
                    return request.withParameter(burp.api.montoya.http.message.params.HttpParameter.parameter(fieldName, processedToken, HttpParameterType.BODY));
                }
                break;
            case COOKIE:
                if (fieldName != null && !fieldName.trim().isEmpty()) {
                    // Get existing Cookie header
                    String existingCookies = request.headerValue("Cookie");
                    StringBuilder newCookieHeader = new StringBuilder();

                    if (existingCookies != null && !existingCookies.isEmpty()) {
                        // Parse existing cookies and update or add the new one
                        String[] cookies = existingCookies.split(";\\s*");
                        boolean found = false;

                        for (int i = 0; i < cookies.length; i++) {
                            String cookie = cookies[i].trim();
                            String cookieName = cookie.split("=")[0];

                            if (cookieName.equals(fieldName)) {
                                // Replace this cookie
                                if (newCookieHeader.length() > 0) newCookieHeader.append("; ");
                                newCookieHeader.append(fieldName).append("=").append(processedToken);
                                found = true;
                            } else {
                                // Keep existing cookie
                                if (newCookieHeader.length() > 0) newCookieHeader.append("; ");
                                newCookieHeader.append(cookie);
                            }
                        }

                        if (!found) {
                            // Add new cookie
                            if (newCookieHeader.length() > 0) newCookieHeader.append("; ");
                            newCookieHeader.append(fieldName).append("=").append(processedToken);
                        }

                        // Remove old Cookie header and add new one
                        HttpRequest temp = request.withRemovedHeader("Cookie");
                        return temp.withAddedHeader("Cookie", newCookieHeader.toString());
                    } else {
                        // No existing Cookie header, create new one
                        return request.withAddedHeader("Cookie", fieldName + "=" + processedToken);
                    }
                }
                break;
            case GENERIC_REGEX:
                String pattern = regexPatternField.getText();
                if (pattern != null && !pattern.trim().isEmpty()) {
                    int groupNum = (Integer) regexGroupSpinner.getValue();
                    String fullText = request.toString();
                    try {
                        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
                        java.util.regex.Matcher m = p.matcher(fullText);
                        if (replaceAllCheckbox.isSelected()) {
                            StringBuffer sb = new StringBuffer();
                            while (m.find()) {
                                m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(processedToken));
                            }
                            m.appendTail(sb);
                            return HttpRequest.httpRequest(sb.toString());
                        } else {
                            if (m.find()) {
                                String replaced = fullText.substring(0, m.start(groupNum)) + processedToken + fullText.substring(m.end(groupNum));
                                return HttpRequest.httpRequest(replaced);
                            }
                        }
                    } catch (Exception e) {
                        // Return original if regex fails
                    }
                }
                break;
        }
        return request;
    }

    private HttpResponse applyReplacementToResponse(HttpResponse response, String tokenValue) {
        ReplacementLocation location = (ReplacementLocation) locationCombo.getSelectedItem();
        String fieldName = fieldNameField.getText();

        // Apply pre-processing script if defined
        String processedToken = tokenValue;
        String script = preProcessingScriptArea.getText();
        if (script != null && !script.trim().isEmpty()) {
            try {
                processedToken = jsProcessor.processTokenWithException(tokenValue, script);
            } catch (Exception e) {
                // On error, use original token
                processedToken = tokenValue;
            }
        }

        switch (location) {
            case HEADER:
                if (fieldName != null && !fieldName.trim().isEmpty()) {
                    // Remove existing header if present, then add new one
                    HttpResponse temp = response.withRemovedHeader(fieldName);
                    return temp.withAddedHeader(fieldName, processedToken);
                }
                break;
            case GENERIC_REGEX:
                String pattern = regexPatternField.getText();
                if (pattern != null && !pattern.trim().isEmpty()) {
                    int groupNum = (Integer) regexGroupSpinner.getValue();
                    String fullText = response.toString();
                    try {
                        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
                        java.util.regex.Matcher m = p.matcher(fullText);
                        if (replaceAllCheckbox.isSelected()) {
                            StringBuffer sb = new StringBuffer();
                            while (m.find()) {
                                m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(processedToken));
                            }
                            m.appendTail(sb);
                            return HttpResponse.httpResponse(sb.toString());
                        } else {
                            if (m.find()) {
                                String replaced = fullText.substring(0, m.start(groupNum)) + processedToken + fullText.substring(m.end(groupNum));
                                return HttpResponse.httpResponse(replaced);
                            }
                        }
                    } catch (Exception e) {
                        // Return original if regex fails
                    }
                }
                break;
        }
        return response;
    }

    public ReplacementRule getRule() {
        return confirmed ? rule : null;
    }
}
