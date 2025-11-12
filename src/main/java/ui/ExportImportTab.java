package ui;

import javax.swing.*;
import java.awt.*;

public class ExportImportTab extends JPanel {
    private final Runnable onExportCallback;
    private final Runnable onImportCallback;
    private final Runnable onSaveAllTokensCallback;

    public ExportImportTab(Runnable onExportCallback, Runnable onImportCallback, Runnable onSaveAllTokensCallback) {
        this.onExportCallback = onExportCallback;
        this.onImportCallback = onImportCallback;
        this.onSaveAllTokensCallback = onSaveAllTokensCallback;

        setLayout(new BorderLayout());
        initComponents();
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Export/Import section
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel exportImportLabel = new JLabel("Configuration");
        exportImportLabel.setFont(exportImportLabel.getFont().deriveFont(Font.BOLD, 16f));
        exportImportLabel.setForeground(new Color(0xd86633));
        mainPanel.add(exportImportLabel, gbc);

        gbc.gridy = 1;
        JTextArea exportImportDesc = new JTextArea("Export or import the entire extension configuration including global controls, defaults, and all token buckets.");
        exportImportDesc.setEditable(false);
        exportImportDesc.setFocusable(false);
        exportImportDesc.setLineWrap(true);
        exportImportDesc.setWrapStyleWord(true);
        exportImportDesc.setOpaque(false);
        exportImportDesc.setBorder(null);
        exportImportDesc.setFont(UIManager.getFont("Label.font"));
        mainPanel.add(exportImportDesc, gbc);

        gbc.gridy = 2;
        JPanel configButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        JButton exportButton = new JButton("Export to JSON");
        exportButton.addActionListener(e -> {
            if (onExportCallback != null) {
                onExportCallback.run();
            }
        });

        JButton importButton = new JButton("Import from JSON");
        importButton.addActionListener(e -> {
            if (onImportCallback != null) {
                onImportCallback.run();
            }
        });

        configButtonPanel.add(exportButton);
        configButtonPanel.add(importButton);
        mainPanel.add(configButtonPanel, gbc);

        // Save All Tokens section
        gbc.gridy = 3;
        JLabel tokensLabel = new JLabel("Tokens");
        tokensLabel.setFont(tokensLabel.getFont().deriveFont(Font.BOLD, 16f));
        tokensLabel.setForeground(new Color(0xd86633));
        mainPanel.add(tokensLabel, gbc);

        gbc.gridy = 4;
        JTextArea tokensDesc = new JTextArea("Save tokens from all buckets to individual text files.");
        tokensDesc.setEditable(false);
        tokensDesc.setFocusable(false);
        tokensDesc.setLineWrap(true);
        tokensDesc.setWrapStyleWord(true);
        tokensDesc.setOpaque(false);
        tokensDesc.setBorder(null);
        tokensDesc.setFont(UIManager.getFont("Label.font"));
        mainPanel.add(tokensDesc, gbc);

        gbc.gridy = 5;
        JPanel tokensButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        JButton saveAllTokensButton = new JButton("Save All Tokens");
        saveAllTokensButton.addActionListener(e -> {
            if (onSaveAllTokensCallback != null) {
                onSaveAllTokensCallback.run();
            }
        });

        tokensButtonPanel.add(saveAllTokensButton);
        mainPanel.add(tokensButtonPanel, gbc);

        // Wrapper panel to keep content at the top
        JPanel wrapperPanel = new JPanel(new BorderLayout());
        wrapperPanel.add(mainPanel, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(wrapperPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);
    }
}
