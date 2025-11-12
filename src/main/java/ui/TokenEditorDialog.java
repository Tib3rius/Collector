package ui;

import javax.swing.*;
import java.awt.*;

public class TokenEditorDialog extends JDialog {
    private String editedToken;
    private boolean confirmed = false;

    private JTextArea tokenArea;
    private JTextArea previewArea;
    private JLabel charCountLabel;
    private JLabel lineCountLabel;

    public TokenEditorDialog(Frame parent, String initialToken) {
        this(parent, initialToken, initialToken != null && !initialToken.isEmpty() ? "Edit Token" : "Add Token");
    }

    public TokenEditorDialog(Frame parent, String initialToken, String title) {
        super(parent, title, true);
        this.editedToken = initialToken != null ? initialToken : "";

        initComponents();
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Info panel
        JPanel infoPanel = new JPanel(new BorderLayout(5, 5));
        JTextArea infoArea = new JTextArea(
            "Use the buttons below to insert special characters."
        );
        infoArea.setEditable(false);
        infoArea.setFocusable(false);
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        infoArea.setOpaque(false);
        infoArea.setBorder(null);
        infoArea.setFont(UIManager.getFont("Label.font"));
        infoPanel.add(infoArea, BorderLayout.NORTH);

        add(infoPanel, BorderLayout.NORTH);

        // Text editor panel
        JPanel editorPanel = new JPanel(new BorderLayout(5, 5));

        tokenArea = new JTextArea(editedToken, 10, 50);
        tokenArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        tokenArea.setLineWrap(true);
        tokenArea.setWrapStyleWord(false);

        // Add document listener to update counts and preview
        tokenArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateCounts(); updatePreview(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateCounts(); updatePreview(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateCounts(); updatePreview(); }
        });

        // Add Ctrl+Enter key binding for newline insertion
        InputMap inputMap = tokenArea.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = tokenArea.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke("control ENTER"), "insert-newline");
        actionMap.put("insert-newline", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                insertAtCaret("\n");
            }
        });

        // Create preview area - same size as token area
        previewArea = new JTextArea();
        previewArea.setRows(tokenArea.getRows());
        previewArea.setColumns(tokenArea.getColumns());
        previewArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        previewArea.setEditable(false);
        previewArea.setLineWrap(true);
        previewArea.setWrapStyleWord(false);
        // Don't set background color - let it inherit from the theme for dark mode compatibility

        // Create scroll panes
        JScrollPane tokenScrollPane = new JScrollPane(tokenArea);
        JScrollPane previewScrollPane = new JScrollPane(previewArea);

        // Use GridLayout with 1 row and 2 columns for truly equal sizing
        JPanel gridPanel = new JPanel(new GridLayout(1, 2, 10, 0));

        // Left panel (editable)
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.add(new JLabel("Token:"), BorderLayout.NORTH);
        leftPanel.add(tokenScrollPane, BorderLayout.CENTER);

        // Right panel (preview)
        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
        rightPanel.add(new JLabel("Preview (Escaped):"), BorderLayout.NORTH);
        rightPanel.add(previewScrollPane, BorderLayout.CENTER);

        gridPanel.add(leftPanel);
        gridPanel.add(rightPanel);

        editorPanel.add(gridPanel, BorderLayout.CENTER);

        add(editorPanel, BorderLayout.CENTER);

        // Initialize preview
        updatePreview();

        // Control buttons panel
        JPanel controlPanel = new JPanel(new BorderLayout(5, 5));

        // Insert special characters buttons
        JPanel insertPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        JLabel insertLabel = new JLabel("Insert:");
        insertPanel.add(insertLabel);

        JButton insertNewlineButton = new JButton("Newline (\\n)");
        insertNewlineButton.setToolTipText("Insert a newline character");
        insertNewlineButton.addActionListener(e -> insertAtCaret("\n"));
        insertPanel.add(insertNewlineButton);

        JButton insertCarriageReturnButton = new JButton("Carriage Return (\\r)");
        insertCarriageReturnButton.setToolTipText("Insert a carriage return character");
        insertCarriageReturnButton.addActionListener(e -> insertAtCaret("\r"));
        insertPanel.add(insertCarriageReturnButton);

        JButton insertTabButton = new JButton("Tab (\\t)");
        insertTabButton.setToolTipText("Insert a tab character");
        insertTabButton.addActionListener(e -> insertAtCaret("\t"));
        insertPanel.add(insertTabButton);

        JButton insertCRLFButton = new JButton("CRLF (\\r\\n)");
        insertCRLFButton.setToolTipText("Insert Windows-style line ending (carriage return + line feed)");
        insertCRLFButton.addActionListener(e -> insertAtCaret("\r\n"));
        insertPanel.add(insertCRLFButton);

        controlPanel.add(insertPanel, BorderLayout.NORTH);

        // Stats panel
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        charCountLabel = new JLabel();
        lineCountLabel = new JLabel();
        statsPanel.add(charCountLabel);
        statsPanel.add(lineCountLabel);
        updateCounts();

        controlPanel.add(statsPanel, BorderLayout.CENTER);

        add(controlPanel, BorderLayout.SOUTH);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));

        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            editedToken = tokenArea.getText();
            confirmed = true;
            dispose();
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        // Add button panel to control panel
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(controlPanel, BorderLayout.NORTH);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);

        setSize(700, 500);
    }

    private void insertAtCaret(String text) {
        int caretPos = tokenArea.getCaretPosition();
        try {
            tokenArea.getDocument().insertString(caretPos, text, null);
        } catch (Exception ex) {
            // Ignore
        }
    }

    private void updateCounts() {
        String text = tokenArea.getText();
        int charCount = text.length();
        int lineCount = text.split("\n", -1).length;

        charCountLabel.setText("Characters: " + charCount);
        lineCountLabel.setText("Lines: " + lineCount);
    }

    private void updatePreview() {
        String text = tokenArea.getText();

        // Convert actual special characters to escaped representation
        // IMPORTANT: Check \r\n first before individual \r and \n
        String escaped = text
            .replace("\r\n", "\\r\\n")  // Windows line ending (must be first!)
            .replace("\r", "\\r")        // Carriage return
            .replace("\n", "\\n")        // Newline
            .replace("\t", "\\t");       // Tab

        previewArea.setText(escaped);
        previewArea.setCaretPosition(0);
    }

    public String getEditedToken() {
        return confirmed ? editedToken : null;
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}
