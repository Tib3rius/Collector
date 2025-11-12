package ui;

import burp.api.montoya.core.ToolType;
import model.GlobalControls;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GlobalControlsTab extends JPanel {
    private final GlobalControls globalControls;
    private final Map<ToolType, JCheckBox> collectionCheckboxes;
    private final Map<ToolType, JCheckBox> replacementCheckboxes;
    private final List<GlobalControlsChangeListener> listeners;
    private final Runnable onSaveCallback;
    private final Runnable onResetCallback;

    public interface GlobalControlsChangeListener {
        void onGlobalControlsChanged();
    }

    public GlobalControlsTab(GlobalControls globalControls, Runnable onSaveCallback, Runnable onResetCallback) {
        this.globalControls = globalControls;
        this.collectionCheckboxes = new HashMap<>();
        this.replacementCheckboxes = new HashMap<>();
        this.listeners = new ArrayList<>();
        this.onSaveCallback = onSaveCallback;
        this.onResetCallback = onResetCallback;

        setLayout(new BorderLayout());
        initComponents();
    }

    public void addChangeListener(GlobalControlsChangeListener listener) {
        listeners.add(listener);
    }

    private void notifyListeners() {
        for (GlobalControlsChangeListener listener : listeners) {
            listener.onGlobalControlsChanged();
        }
        // Auto-save after changes
        if (onSaveCallback != null) {
            onSaveCallback.run();
        }
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Buckets section
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel bucketsMainHeading = new JLabel("Buckets");
        bucketsMainHeading.setFont(bucketsMainHeading.getFont().deriveFont(Font.BOLD, 16f));
        bucketsMainHeading.setForeground(new Color(0xd86633));
        mainPanel.add(bucketsMainHeading, gbc);

        gbc.gridy = 1;
        JCheckBox bucketsEnabledCheck = new JCheckBox("Enabled");
        bucketsEnabledCheck.setSelected(globalControls.isBucketsEnabled());
        bucketsEnabledCheck.addActionListener(e -> {
            globalControls.setBucketsEnabled(bucketsEnabledCheck.isSelected());
            notifyListeners();
        });
        mainPanel.add(bucketsEnabledCheck, gbc);

        gbc.gridy = 2;
        JTextArea bucketsDesc = new JTextArea("Master control for all bucket functionality. Individual buckets must also be enabled to function.");
        bucketsDesc.setEditable(false);
        bucketsDesc.setFocusable(false);
        bucketsDesc.setLineWrap(true);
        bucketsDesc.setWrapStyleWord(true);
        bucketsDesc.setOpaque(false);
        bucketsDesc.setBorder(null);
        bucketsDesc.setFont(UIManager.getFont("Label.font"));
        mainPanel.add(bucketsDesc, gbc);

        // Collection panel
        gbc.gridx = 0; gbc.gridy = 3;
        JLabel collectionMainHeading = new JLabel("Token Collection");
        collectionMainHeading.setFont(collectionMainHeading.getFont().deriveFont(Font.BOLD, 16f));
        collectionMainHeading.setForeground(new Color(0xd86633));
        mainPanel.add(collectionMainHeading, gbc);

        gbc.gridy = 4;
        JLabel collectionLabel = new JLabel("Enabled Tools");
        collectionLabel.setFont(collectionLabel.getFont().deriveFont(Font.BOLD));
        mainPanel.add(collectionLabel, gbc);

        gbc.gridy = 5;
        JTextArea collectionDesc = new JTextArea("Global control for which Burp tools are monitored to collect tokens from. Individual buckets must also enable the tool.");
        collectionDesc.setEditable(false);
        collectionDesc.setFocusable(false);
        collectionDesc.setLineWrap(true);
        collectionDesc.setWrapStyleWord(true);
        collectionDesc.setOpaque(false);
        collectionDesc.setBorder(null);
        collectionDesc.setFont(UIManager.getFont("Label.font"));
        mainPanel.add(collectionDesc, gbc);

        gbc.gridy = 6;
        JPanel collectionToolsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        // Add "All" checkbox for Collection Tools
        JCheckBox allCollectionToolsCheck = new JCheckBox("All");
        allCollectionToolsCheck.addActionListener(e -> {
            boolean selected = allCollectionToolsCheck.isSelected();
            for (ToolType toolType : getCommonToolTypes()) {
                JCheckBox checkbox = collectionCheckboxes.get(toolType);
                checkbox.setSelected(selected);
                if (selected) {
                    globalControls.getCollectionEnabledTools().add(toolType);
                } else {
                    globalControls.getCollectionEnabledTools().remove(toolType);
                }
            }
            notifyListeners();
        });
        collectionToolsPanel.add(allCollectionToolsCheck);

        // Add gap after "All" checkbox
        collectionToolsPanel.add(Box.createRigidArea(new Dimension(15, 0)));

        for (ToolType toolType : getCommonToolTypes()) {
            JCheckBox checkbox = new JCheckBox(toolType.toolName());
            checkbox.setSelected(globalControls.getCollectionEnabledTools().contains(toolType));
            checkbox.addActionListener(e -> {
                if (checkbox.isSelected()) {
                    globalControls.getCollectionEnabledTools().add(toolType);
                } else {
                    globalControls.getCollectionEnabledTools().remove(toolType);
                }
                notifyListeners();
            });
            collectionCheckboxes.put(toolType, checkbox);
            collectionToolsPanel.add(checkbox);
        }
        mainPanel.add(collectionToolsPanel, gbc);

        // Replacement panel
        gbc.gridy = 7;
        JLabel replacementMainHeading = new JLabel("Token Replacement");
        replacementMainHeading.setFont(replacementMainHeading.getFont().deriveFont(Font.BOLD, 16f));
        replacementMainHeading.setForeground(new Color(0xd86633));
        mainPanel.add(replacementMainHeading, gbc);

        gbc.gridy = 8;
        JLabel replacementLabel = new JLabel("Enabled Tools");
        replacementLabel.setFont(replacementLabel.getFont().deriveFont(Font.BOLD));
        mainPanel.add(replacementLabel, gbc);

        gbc.gridy = 9;
        JTextArea replacementDesc = new JTextArea("Global control for which Burp tools tokens can be placed in. Individual buckets must also enable the tool.");
        replacementDesc.setEditable(false);
        replacementDesc.setFocusable(false);
        replacementDesc.setLineWrap(true);
        replacementDesc.setWrapStyleWord(true);
        replacementDesc.setOpaque(false);
        replacementDesc.setBorder(null);
        replacementDesc.setFont(UIManager.getFont("Label.font"));
        mainPanel.add(replacementDesc, gbc);

        gbc.gridy = 10;
        JPanel replacementToolsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        // Add "All" checkbox for Replacement Tools
        JCheckBox allReplacementToolsCheck = new JCheckBox("All");
        allReplacementToolsCheck.addActionListener(e -> {
            boolean selected = allReplacementToolsCheck.isSelected();
            for (ToolType toolType : getCommonToolTypes()) {
                JCheckBox checkbox = replacementCheckboxes.get(toolType);
                checkbox.setSelected(selected);
                if (selected) {
                    globalControls.getReplacementEnabledTools().add(toolType);
                } else {
                    globalControls.getReplacementEnabledTools().remove(toolType);
                }
            }
            notifyListeners();
        });
        replacementToolsPanel.add(allReplacementToolsCheck);

        // Add gap after "All" checkbox
        replacementToolsPanel.add(Box.createRigidArea(new Dimension(15, 0)));

        for (ToolType toolType : getCommonToolTypes()) {
            JCheckBox checkbox = new JCheckBox(toolType.toolName());
            checkbox.setSelected(globalControls.getReplacementEnabledTools().contains(toolType));
            checkbox.addActionListener(e -> {
                if (checkbox.isSelected()) {
                    globalControls.getReplacementEnabledTools().add(toolType);
                } else {
                    globalControls.getReplacementEnabledTools().remove(toolType);
                }
                notifyListeners();
            });
            replacementCheckboxes.put(toolType, checkbox);
            replacementToolsPanel.add(checkbox);
        }
        mainPanel.add(replacementToolsPanel, gbc);

        // Persistence section
        gbc.gridy = 11;
        JLabel persistenceLabel = new JLabel("Persistence");
        persistenceLabel.setFont(persistenceLabel.getFont().deriveFont(Font.BOLD));
        mainPanel.add(persistenceLabel, gbc);

        gbc.gridy = 12;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        JButton saveButton = new JButton("Save Configuration");
        saveButton.addActionListener(e -> {
            if (onSaveCallback != null) {
                onSaveCallback.run();
            }
            JOptionPane.showMessageDialog(this, "Configuration saved!");
        });

        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(e -> {
            if (onResetCallback != null) {
                onResetCallback.run();
            }
        });

        buttonPanel.add(saveButton);
        buttonPanel.add(resetButton);

        mainPanel.add(buttonPanel, gbc);

        // Wrapper panel to keep content at the top
        JPanel wrapperPanel = new JPanel(new BorderLayout());
        wrapperPanel.add(mainPanel, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(wrapperPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);
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

    public void updateFromModel() {
        for (Map.Entry<ToolType, JCheckBox> entry : collectionCheckboxes.entrySet()) {
            entry.getValue().setSelected(globalControls.getCollectionEnabledTools().contains(entry.getKey()));
        }
        for (Map.Entry<ToolType, JCheckBox> entry : replacementCheckboxes.entrySet()) {
            entry.getValue().setSelected(globalControls.getReplacementEnabledTools().contains(entry.getKey()));
        }
    }
}
