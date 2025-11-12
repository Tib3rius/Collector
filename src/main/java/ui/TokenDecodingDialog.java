package ui;

import util.TokenEncoder;

import javax.swing.*;
import java.awt.*;

public class TokenDecodingDialog extends JDialog {

    public enum Action {
        DECODE,
        DO_NOTHING,
        CANCEL
    }

    private Action selectedAction = Action.CANCEL;
    private TokenEncoder.EncodingType selectedEncoding = TokenEncoder.EncodingType.BASE64;

    public TokenDecodingDialog(Frame parent) {
        this(parent, true);
    }

    public TokenDecodingDialog(Frame parent, boolean isLoadingFromFile) {
        super(parent, "Decode Tokens", true);
        initComponents(isLoadingFromFile);
        setLocationRelativeTo(parent);
    }

    private void initComponents(boolean isLoadingFromFile) {
        setLayout(new BorderLayout(10, 10));
        setSize(450, 220);
        setMinimumSize(new Dimension(450, 220));

        // Message panel
        JPanel messagePanel = new JPanel(new BorderLayout(5, 5));
        messagePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));

        JLabel questionIcon = new JLabel(UIManager.getIcon("OptionPane.questionIcon"));
        messagePanel.add(questionIcon, BorderLayout.WEST);

        String message;
        if (isLoadingFromFile) {
            message = "If the tokens in this file are encoded to handle newlines, select the decoding type. " +
                     "Otherwise, select 'Do Nothing' to load tokens as-is.";
        } else {
            message = "If the tokens being pasted are encoded to handle newlines, select the decoding type. " +
                     "Otherwise, select 'Do Nothing' to paste tokens as-is.";
        }

        JTextArea messageArea = new JTextArea(message);
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

        JLabel encodeLabel = new JLabel("Decoding Type:");
        JComboBox<TokenEncoder.EncodingType> encodingCombo = new JComboBox<>(TokenEncoder.EncodingType.values());
        encodingCombo.addActionListener(e -> selectedEncoding = (TokenEncoder.EncodingType) encodingCombo.getSelectedItem());

        optionsPanel.add(encodeLabel);
        optionsPanel.add(encodingCombo);

        add(optionsPanel, BorderLayout.CENTER);

        // Buttons panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        JButton decodeButton = new JButton("Decode Tokens");
        decodeButton.addActionListener(e -> {
            selectedAction = Action.DECODE;
            dispose();
        });

        JButton doNothingButton = new JButton("Do Nothing");
        doNothingButton.addActionListener(e -> {
            selectedAction = Action.DO_NOTHING;
            dispose();
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            selectedAction = Action.CANCEL;
            dispose();
        });

        buttonsPanel.add(decodeButton);
        buttonsPanel.add(doNothingButton);
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
