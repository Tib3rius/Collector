package ui;

import model.BucketDefaults;
import model.GlobalControls;

import javax.swing.*;
import java.awt.*;

public class SettingsTab extends JPanel {
    private final GlobalControlsTab globalControlsTab;
    private final DefaultsTab defaultsTab;
    private final ExportImportTab exportImportTab;

    public SettingsTab(GlobalControls globalControls, BucketDefaults bucketDefaults,
                       Runnable onSaveCallback, Runnable onExportCallback,
                       Runnable onImportCallback, Runnable onResetCallback,
                       Runnable onSaveAllTokensCallback) {
        setLayout(new BorderLayout());

        JTabbedPane tabbedPane = new JTabbedPane();

        // Create the three sub-tabs
        globalControlsTab = new GlobalControlsTab(globalControls, onSaveCallback, onResetCallback);
        defaultsTab = new DefaultsTab(bucketDefaults, onSaveCallback);
        exportImportTab = new ExportImportTab(onExportCallback, onImportCallback, onSaveAllTokensCallback);

        tabbedPane.addTab("Global Controls", globalControlsTab);
        tabbedPane.addTab("Defaults", defaultsTab);
        tabbedPane.addTab("Export / Import", exportImportTab);

        add(tabbedPane, BorderLayout.CENTER);
    }

    public void addGlobalControlsChangeListener(GlobalControlsTab.GlobalControlsChangeListener listener) {
        globalControlsTab.addChangeListener(listener);
    }

    public void updateFromModel() {
        globalControlsTab.updateFromModel();
        defaultsTab.updateFromModel();
    }
}
