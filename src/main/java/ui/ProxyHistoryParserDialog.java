package ui;

import core.BucketManager;
import model.Bucket;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ProxyHistoryParserDialog extends JDialog {
    private final Bucket bucket;
    private final BucketManager bucketManager;
    private final burp.api.montoya.MontoyaApi api;

    private JSpinner maxItemsSpinner;
    private JCheckBox unlimitedCheckbox;
    private ButtonGroup directionGroup;
    private JRadioButton oldestFirstRadio;
    private JRadioButton newestFirstRadio;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JButton parseButton;
    private JButton cancelButton;

    private SwingWorker<Integer, Integer> parserWorker;
    private AtomicBoolean cancelFlag;
    private AtomicBoolean pauseFlag;

    public ProxyHistoryParserDialog(Frame owner, Bucket bucket, BucketManager bucketManager, burp.api.montoya.MontoyaApi api) {
        super(owner, "Parse Proxy History - " + bucket.getName(), true);
        this.bucket = bucket;
        this.bucketManager = bucketManager;
        this.api = api;
        this.cancelFlag = new AtomicBoolean(false);
        this.pauseFlag = new AtomicBoolean(false);

        initComponents();
        setSize(500, 300);
        setMinimumSize(new Dimension(450, 250));
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Options panel
        JPanel optionsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Max items
        gbc.gridx = 0; gbc.gridy = 0;
        optionsPanel.add(new JLabel("Max Items:"), gbc);
        gbc.gridx = 1;
        maxItemsSpinner = new JSpinner(new SpinnerNumberModel(100, 1, 999999, 100));
        maxItemsSpinner.setPreferredSize(new Dimension(100, 25));
        maxItemsSpinner.setEnabled(false); // Start disabled since unlimited is default
        optionsPanel.add(maxItemsSpinner, gbc);
        gbc.gridx = 2;
        unlimitedCheckbox = new JCheckBox("Unlimited", true); // Default to checked
        unlimitedCheckbox.addActionListener(e -> {
            maxItemsSpinner.setEnabled(!unlimitedCheckbox.isSelected());
        });
        optionsPanel.add(unlimitedCheckbox, gbc);

        // Direction
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        optionsPanel.add(new JLabel("Direction:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        JPanel directionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        directionGroup = new ButtonGroup();
        oldestFirstRadio = new JRadioButton("Oldest First");
        newestFirstRadio = new JRadioButton("Newest First", true);
        directionGroup.add(oldestFirstRadio);
        directionGroup.add(newestFirstRadio);
        directionPanel.add(oldestFirstRadio);
        directionPanel.add(newestFirstRadio);
        optionsPanel.add(directionPanel, gbc);

        add(optionsPanel, BorderLayout.NORTH);

        // Progress panel
        JPanel progressPanel = new JPanel(new GridBagLayout());
        GridBagConstraints progressGbc = new GridBagConstraints();
        progressGbc.insets = new Insets(10, 10, 10, 10);
        progressGbc.fill = GridBagConstraints.HORIZONTAL;
        progressGbc.weightx = 1.0;

        progressGbc.gridx = 0;
        progressGbc.gridy = 0;
        statusLabel = new JLabel("Ready to parse");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        progressPanel.add(statusLabel, progressGbc);

        progressGbc.gridy = 1;
        progressGbc.ipady = 0; // Reset internal padding
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(0, 20)); // Width will be controlled by fill constraint
        progressPanel.add(progressBar, progressGbc);

        add(progressPanel, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        parseButton = new JButton("Parse");
        parseButton.addActionListener(e -> handleParseButtonClick());
        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> cancelParsing());
        cancelButton.setEnabled(false);

        buttonPanel.add(parseButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void handleParseButtonClick() {
        String buttonText = parseButton.getText();

        if ("Parse".equals(buttonText)) {
            startParsing();
        } else if ("Pause".equals(buttonText)) {
            pauseParsing();
        } else if ("Resume".equals(buttonText)) {
            resumeParsing();
        }
    }

    private void pauseParsing() {
        pauseFlag.set(true);
        parseButton.setText("Resume");
        statusLabel.setText("Paused - " + statusLabel.getText());
    }

    private void resumeParsing() {
        pauseFlag.set(false);
        parseButton.setText("Pause");
        // Remove "Paused - " prefix from status if present
        String currentStatus = statusLabel.getText();
        if (currentStatus.startsWith("Paused - ")) {
            statusLabel.setText(currentStatus.substring(9));
        }
    }

    private void startParsing() {
        // Check if bucket is enabled and Proxy tool is enabled for collection
        boolean bucketEnabled = bucket.isEnabled();
        boolean proxyToolEnabled = bucket.getCollectionRule().getEnabledTools().contains(burp.api.montoya.core.ToolType.PROXY);

        if (!bucketEnabled || !proxyToolEnabled) {
            // Build warning message
            StringBuilder message = new StringBuilder("This bucket would not normally collect tokens from the Proxy:\n\n");

            if (!bucketEnabled) {
                message.append("• The bucket is currently DISABLED\n");
            }

            if (!proxyToolEnabled) {
                message.append("• The PROXY tool is not enabled for collection\n");
            }

            message.append("\nDo you want to parse the Proxy history and collect tokens anyway?");

            int choice = JOptionPane.showConfirmDialog(
                    this,
                    message.toString(),
                    "Parse Proxy History",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );

            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
        }

        // Update button states
        parseButton.setText("Pause");
        parseButton.setEnabled(true);
        cancelButton.setEnabled(true);
        maxItemsSpinner.setEnabled(false);
        unlimitedCheckbox.setEnabled(false);
        oldestFirstRadio.setEnabled(false);
        newestFirstRadio.setEnabled(false);

        // Reset flags
        cancelFlag.set(false);
        pauseFlag.set(false);

        // Get options
        int maxItems = unlimitedCheckbox.isSelected() ? 0 : (Integer) maxItemsSpinner.getValue();
        boolean newestFirst = newestFirstRadio.isSelected();

        // Get total history size for progress bar
        int totalItems = api.proxy().history().size();
        int itemsToProcess = (maxItems > 0) ? Math.min(maxItems, totalItems) : totalItems;
        progressBar.setMaximum(itemsToProcess);
        progressBar.setValue(0);
        statusLabel.setText("Processing item 0 of " + itemsToProcess);

        // Create and execute worker
        parserWorker = new SwingWorker<Integer, Integer>() {
            @Override
            protected Integer doInBackground() throws Exception {
                return bucketManager.parseProxyHistory(
                        bucket,
                        maxItems,
                        newestFirst,
                        progress -> publish(progress),
                        () -> cancelFlag.get(),
                        () -> pauseFlag.get()
                );
            }

            @Override
            protected void process(java.util.List<Integer> chunks) {
                if (!chunks.isEmpty()) {
                    int progress = chunks.get(chunks.size() - 1);
                    progressBar.setValue(progress);
                    statusLabel.setText("Processing item " + progress + " of " + itemsToProcess);
                }
            }

            @Override
            protected void done() {
                try {
                    int tokensCollected = get();
                    // Use current progress bar value to show actual items processed
                    int itemsProcessed = progressBar.getValue();

                    // Check if cancelled
                    if (cancelFlag.get()) {
                        statusLabel.setText("Cancelled - " + tokensCollected + " tokens collected from " + itemsProcessed + " items");

                        // Show results dialog for cancelled operation
                        JOptionPane.showMessageDialog(
                                ProxyHistoryParserDialog.this,
                                "Parsing cancelled!\n\n" +
                                        "Items processed: " + itemsProcessed + " of " + itemsToProcess + "\n" +
                                        "Tokens collected: " + tokensCollected,
                                "Parsing Cancelled",
                                JOptionPane.INFORMATION_MESSAGE
                        );

                        dispose();
                    } else {
                        // Completed successfully
                        progressBar.setValue(itemsToProcess);
                        statusLabel.setText("Completed - " + tokensCollected + " tokens collected");

                        // Show results dialog
                        JOptionPane.showMessageDialog(
                                ProxyHistoryParserDialog.this,
                                "Parsing complete!\n\n" +
                                        "Items processed: " + itemsToProcess + "\n" +
                                        "Tokens collected: " + tokensCollected,
                                "Parsing Complete",
                                JOptionPane.INFORMATION_MESSAGE
                        );

                        dispose();
                    }
                } catch (Exception e) {
                    statusLabel.setText("Error: " + e.getMessage());
                    JOptionPane.showMessageDialog(
                            ProxyHistoryParserDialog.this,
                            "Error parsing proxy history:\n" + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                } finally {
                    // Re-enable controls and reset button text
                    parseButton.setText("Parse");
                    parseButton.setEnabled(true);
                    cancelButton.setEnabled(false);
                    maxItemsSpinner.setEnabled(!unlimitedCheckbox.isSelected());
                    unlimitedCheckbox.setEnabled(true);
                    oldestFirstRadio.setEnabled(true);
                    newestFirstRadio.setEnabled(true);
                }
            }
        };

        parserWorker.execute();
    }

    private void cancelParsing() {
        if (parserWorker != null && !parserWorker.isDone()) {
            cancelFlag.set(true);
            statusLabel.setText("Cancelling...");
            cancelButton.setEnabled(false);
        }
    }
}
