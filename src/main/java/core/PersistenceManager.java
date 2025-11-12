package core;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.persistence.PersistedObject;
import burp.api.montoya.persistence.Preferences;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import model.Bucket;
import model.BucketDefaults;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class PersistenceManager {
    private static final String BUCKETS_KEY = "collector_buckets";
    private static final String GLOBAL_CONTROLS_KEY = "collector_global_controls";
    private static final String BUCKET_DEFAULTS_KEY = "collector_bucket_defaults";

    private final Preferences preferences;
    private final PersistedObject extensionData;
    private final Gson gson;
    private final MontoyaApi api;

    public PersistenceManager(MontoyaApi api) {
        this.api = api;
        this.preferences = api.persistence().preferences();
        this.extensionData = api.persistence().extensionData();
        // Use LinkedHashMap to preserve insertion order during serialization
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .enableComplexMapKeySerialization()
                .create();
    }

    public void saveBucketManager(BucketManager bucketManager) {
        try {
            // Save buckets to project-specific storage (maintaining insertion order)
            List<Bucket> buckets = new ArrayList<>(bucketManager.getBuckets());
            String bucketsJson = gson.toJson(buckets);
            extensionData.setString(BUCKETS_KEY, bucketsJson);

            // Save global controls to Burp-level preferences
            String globalControlsJson = gson.toJson(bucketManager.getGlobalControls());
            preferences.setString(GLOBAL_CONTROLS_KEY, globalControlsJson);
        } catch (Exception e) {
            api.logging().logToError("Error saving bucket manager: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void saveBucketDefaults(BucketDefaults bucketDefaults) {
        try {
            String bucketDefaultsJson = gson.toJson(bucketDefaults);
            preferences.setString(BUCKET_DEFAULTS_KEY, bucketDefaultsJson);
        } catch (Exception e) {
            api.logging().logToError("Error saving bucket defaults: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void loadBucketManager(BucketManager bucketManager) {
        try {
            // Load global controls from Burp-level preferences
            String globalControlsJson = preferences.getString(GLOBAL_CONTROLS_KEY);
            if (globalControlsJson != null) {
                model.GlobalControls loadedControls = gson.fromJson(globalControlsJson, model.GlobalControls.class);
                if (loadedControls != null) {
                    bucketManager.getGlobalControls().setCollectionEnabledTools(loadedControls.getCollectionEnabledTools());
                    bucketManager.getGlobalControls().setReplacementEnabledTools(loadedControls.getReplacementEnabledTools());
                }
            }

            // Load buckets from project-specific storage
            String bucketsJson = extensionData.getString(BUCKETS_KEY);
            if (bucketsJson != null) {
                Bucket[] loadedBuckets = gson.fromJson(bucketsJson, Bucket[].class);
                if (loadedBuckets != null) {
                    for (Bucket bucket : loadedBuckets) {
                        // Initialize transient fields after deserialization
                        bucket.initializeTransientFields();
                        bucketManager.addBucket(bucket);
                    }
                }
            }
        } catch (Exception e) {
            api.logging().logToError("Error loading bucket manager: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public BucketDefaults loadBucketDefaults() {
        try {
            String bucketDefaultsJson = preferences.getString(BUCKET_DEFAULTS_KEY);
            if (bucketDefaultsJson != null) {
                BucketDefaults loaded = gson.fromJson(bucketDefaultsJson, BucketDefaults.class);
                if (loaded != null) {
                    loaded.ensureInitialized();
                    return loaded;
                }
            }
        } catch (Exception e) {
            api.logging().logToError("Error loading bucket defaults: " + e.getMessage());
            e.printStackTrace();
        }
        return new BucketDefaults(); // Return new defaults if loading fails
    }

    public void exportToFile(BucketManager bucketManager, BucketDefaults bucketDefaults, File file) throws IOException {
        ExportData data = new ExportData();
        data.buckets = bucketManager.getBuckets();
        data.globalControls = bucketManager.getGlobalControls();
        data.bucketDefaults = bucketDefaults;

        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(data, writer);
        }
    }

    public BucketDefaults importFromFile(BucketManager bucketManager, File file) throws IOException {
        try (FileReader reader = new FileReader(file)) {
            ExportData data = gson.fromJson(reader, ExportData.class);

            if (data != null) {
                // Clear existing buckets
                List<Bucket> existingBuckets = new ArrayList<>(bucketManager.getBuckets());
                for (Bucket bucket : existingBuckets) {
                    bucketManager.removeBucket(bucket);
                }

                // Import global controls
                if (data.globalControls != null) {
                    bucketManager.getGlobalControls().setCollectionEnabledTools(data.globalControls.getCollectionEnabledTools());
                    bucketManager.getGlobalControls().setReplacementEnabledTools(data.globalControls.getReplacementEnabledTools());
                }

                // Import buckets
                if (data.buckets != null) {
                    for (Bucket bucket : data.buckets) {
                        bucket.initializeTransientFields();
                        bucketManager.addBucket(bucket);
                    }
                }

                // Return imported defaults (or new defaults if none)
                return data.bucketDefaults != null ? data.bucketDefaults : new BucketDefaults();
            }
        }
        return new BucketDefaults();
    }

    public void clearAllData() {
        // Clear project-specific data
        extensionData.deleteString(BUCKETS_KEY);

        // Clear Burp-level preferences
        preferences.deleteString(GLOBAL_CONTROLS_KEY);
        preferences.deleteString(BUCKET_DEFAULTS_KEY);
    }

    private static class ExportData {
        List<Bucket> buckets;
        model.GlobalControls globalControls;
        BucketDefaults bucketDefaults;
    }
}
