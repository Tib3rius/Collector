package ui;

import model.UrlMatcher;

import javax.swing.*;
import java.awt.*;

public class UrlMatcherDialog extends JDialog {
    private UrlMatcher matcher;
    private boolean confirmed = false;

    private JComboBox<String> protocolCombo;
    private JTextField hostField;
    private JTextField portField;
    private JTextField pathField;

    public UrlMatcherDialog(Frame owner, UrlMatcher existingMatcher) {
        super(owner, "URL Matcher", true);
        this.matcher = existingMatcher != null ? existingMatcher : new UrlMatcher();

        initComponents();
        if (existingMatcher != null) {
            loadFromMatcher();
        }

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

        // Protocol
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Protocol:"), gbc);
        gbc.gridx = 1;
        protocolCombo = new JComboBox<>(new String[]{"Any", "HTTP", "HTTPS"});
        formPanel.add(protocolCombo, gbc);

        // Host
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Host:"), gbc);
        gbc.gridx = 1;
        hostField = new JTextField(30);
        formPanel.add(hostField, gbc);

        // Port
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Port:"), gbc);
        gbc.gridx = 1;
        portField = new JTextField(30);
        formPanel.add(portField, gbc);

        // Path
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Path:"), gbc);
        gbc.gridx = 1;
        pathField = new JTextField(30);
        formPanel.add(pathField, gbc);

        // Info label
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        JLabel infoLabel = new JLabel("All fields support regex patterns. Leave blank to match any.");
        infoLabel.setFont(infoLabel.getFont().deriveFont(Font.ITALIC, 11f));
        formPanel.add(infoLabel, gbc);

        add(formPanel, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            saveToMatcher();
            confirmed = true;
            dispose();
        });
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void loadFromMatcher() {
        protocolCombo.setSelectedItem(matcher.getProtocol() != null ? matcher.getProtocol() : "Any");
        hostField.setText(matcher.getHost() != null ? matcher.getHost() : "");
        portField.setText(matcher.getPort() != null ? matcher.getPort() : "");
        pathField.setText(matcher.getPath() != null ? matcher.getPath() : "");
    }

    private void saveToMatcher() {
        matcher.setProtocol((String) protocolCombo.getSelectedItem());
        matcher.setHost(hostField.getText().trim());
        matcher.setPort(portField.getText().trim());
        matcher.setPath(pathField.getText().trim());
    }

    public UrlMatcher getMatcher() {
        return confirmed ? matcher : null;
    }
}
