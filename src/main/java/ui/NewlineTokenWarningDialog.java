package ui;

import util.TokenEncoder;

import javax.swing.*;
import java.awt.*;

public class NewlineTokenWarningDialog extends JDialog {

    public enum Action {
        ENCODE_TOKENS,
        DO_NOTHING,
        SKIP_TOKENS,
        CANCEL
    }

    public enum OperationType {
        SAVE,
        COPY,
        SAVE_ALL
    }

    private Action selectedAction = Action.CANCEL;
    private TokenEncoder.EncodingType selectedEncoding = TokenEncoder.EncodingType.BASE64;

    public NewlineTokenWarningDialog(Frame parent) {
        this(parent, OperationType.SAVE);
    }

    public NewlineTokenWarningDialog(Frame parent, OperationType operationType) {
        super(parent, "Newline Characters Detected", true);
        initComponents(operationType);
        setLocationRelativeTo(parent);
    }

    private void initComponents(OperationType operationType) {
        setLayout(new BorderLayout(10, 10));
        setSize(500, 300);

        // Warning message
        JPanel messagePanel = new JPanel(new BorderLayout(5, 5));
        messagePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel warningIcon = new JLabel(UIManager.getIcon("OptionPane.warningIcon"));
        messagePanel.add(warningIcon, BorderLayout.WEST);

        String firstSentence;
        String operationText;
        String cancelButtonText;

        if (operationType == OperationType.SAVE_ALL) {
            firstSentence = "One or more token lists contain tokens with newline characters (\\r or \\n).";
            operationText = "Saving these tokens to files may be inaccurate since tokens are separated by newlines.";
            cancelButtonText = "Cancel Save";
        } else if (operationType == OperationType.SAVE) {
            firstSentence = "One or more tokens contain newline characters (\\r or \\n).";
            operationText = "Saving these tokens to a file may be inaccurate since tokens are separated by newlines.";
            cancelButtonText = "Cancel Save";
        } else { // COPY
            firstSentence = "One or more tokens contain newline characters (\\r or \\n).";
            operationText = "Copying these tokens to clipboard may be inaccurate since tokens are separated by newlines.";
            cancelButtonText = "Cancel Copy";
        }

        JTextArea messageArea = new JTextArea(
            firstSentence + "\n\n" +
            operationText + "\n\n" +
            "Please choose how to handle these tokens:"
        );
        messageArea.setEditable(false);
        messageArea.setFocusable(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setOpaque(false);
        messageArea.setFont(UIManager.getFont("Label.font"));
        messagePanel.add(messageArea, BorderLayout.CENTER);

        add(messagePanel, BorderLayout.NORTH);

        // Options panel - center-aligned with natural sizing
        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        optionsPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

        JLabel encodeLabel = new JLabel("Encoding Type:");
        JComboBox<TokenEncoder.EncodingType> encodingCombo = new JComboBox<>(TokenEncoder.EncodingType.values());
        encodingCombo.addActionListener(e -> selectedEncoding = (TokenEncoder.EncodingType) encodingCombo.getSelectedItem());

        optionsPanel.add(encodeLabel);
        optionsPanel.add(encodingCombo);

        add(optionsPanel, BorderLayout.CENTER);

        // Buttons panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        JButton encodeButton = new JButton("Encode Tokens");
        encodeButton.addActionListener(e -> {
            selectedAction = Action.ENCODE_TOKENS;
            dispose();
        });

        JButton doNothingButton = new JButton("Do Nothing");
        doNothingButton.addActionListener(e -> {
            selectedAction = Action.DO_NOTHING;
            dispose();
        });

        JButton skipButton = new JButton("Skip Tokens with Newlines");
        skipButton.addActionListener(e -> {
            selectedAction = Action.SKIP_TOKENS;
            dispose();
        });

        JButton cancelButton = new JButton(cancelButtonText);
        cancelButton.addActionListener(e -> {
            selectedAction = Action.CANCEL;
            dispose();
        });

        buttonsPanel.add(encodeButton);
        buttonsPanel.add(doNothingButton);
        buttonsPanel.add(skipButton);
        buttonsPanel.add(cancelButton);

        add(buttonsPanel, BorderLayout.SOUTH);
    }

    public Action getSelectedAction() {
        return selectedAction;
    }

    public TokenEncoder.EncodingType getSelectedEncoding() {
        return selectedEncoding;
    }
}
