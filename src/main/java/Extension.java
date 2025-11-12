import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import core.BucketManager;
import core.CollectorHttpHandler;
import core.PersistenceManager;
import model.BucketDefaults;
import ui.CollectorTab;

import javax.swing.*;

public class Extension implements BurpExtension {
    private MontoyaApi api;
    private BucketManager bucketManager;
    private BucketDefaults bucketDefaults;
    private PersistenceManager persistenceManager;
    private CollectorTab collectorTab;

    @Override
    public void initialize(MontoyaApi montoyaApi) {
        this.api = montoyaApi;

        // Set extension name
        api.extension().setName("Collector");

        // Initialize managers
        bucketManager = new BucketManager(api.logging(), api);
        persistenceManager = new PersistenceManager(api);

        // Load saved state
        persistenceManager.loadBucketManager(bucketManager);
        bucketDefaults = persistenceManager.loadBucketDefaults();

        // Register HTTP handler
        CollectorHttpHandler httpHandler = new CollectorHttpHandler(bucketManager);
        api.http().registerHttpHandler(httpHandler);

        // Create UI
        SwingUtilities.invokeLater(() -> {
            collectorTab = new CollectorTab(bucketManager, bucketDefaults, this::saveState, api);
            collectorTab.setPersistenceManager(persistenceManager);

            // Register the tab
            api.userInterface().registerSuiteTab("Collector", collectorTab);
        });

        // Log successful initialization
        api.logging().logToOutput("Collector extension loaded successfully");
    }

    private void saveState() {
        persistenceManager.saveBucketManager(bucketManager);
        persistenceManager.saveBucketDefaults(bucketDefaults);
    }
}
