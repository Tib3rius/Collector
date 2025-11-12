package model;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Bucket implements Serializable {
    private String name;
    private boolean enabled;
    private BucketType bucketType;
    private int maxSize; // -1 for infinite
    private BucketFullBehavior fullBehavior;
    private boolean uniqueOnly; // Only allow unique tokens
    private CollectionRule collectionRule;
    private ReplacementConfig replacementConfig;

    // Thread-safe token storage
    private transient Deque<String> tokens;
    private transient ReadWriteLock lock;

    public Bucket(String name) {
        this.name = name;
        this.enabled = true; // Enabled by default
        this.bucketType = BucketType.FIFO;
        this.maxSize = -1; // Infinite by default
        this.fullBehavior = BucketFullBehavior.REJECT_NEW;
        this.uniqueOnly = false;
        this.collectionRule = new CollectionRule();
        this.replacementConfig = new ReplacementConfig();
        this.tokens = new LinkedList<>();
        this.lock = new ReentrantReadWriteLock();
    }

    public void addToken(String token) {
        lock.writeLock().lock();
        try {
            // Check if unique only mode is enabled and token already exists
            if (uniqueOnly && tokens.contains(token)) {
                return; // Don't add duplicate token
            }

            if (maxSize > 0 && tokens.size() >= maxSize) {
                switch (fullBehavior) {
                    case REJECT_NEW:
                        return; // Don't add the token
                    case REPLACE_LAST:
                        if (!tokens.isEmpty()) {
                            tokens.removeLast();
                        }
                        break;
                    case REPLACE_OLDEST:
                        if (!tokens.isEmpty()) {
                            tokens.removeFirst();
                        }
                        break;
                }
            }
            tokens.addLast(token);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String getToken(boolean remove) {
        lock.writeLock().lock();
        try {
            if (tokens.isEmpty()) {
                return null;
            }

            // Respect last token behavior
            if (tokens.size() == 1 && replacementConfig.getLastTokenBehavior() == LastTokenBehavior.KEEP_IN_BUCKET) {
                return tokens.getFirst();
            }

            switch (bucketType) {
                case FIFO: // First In First Out - remove from front (queue behavior)
                    return remove ? tokens.removeFirst() : tokens.getFirst();
                case LIFO: // Last In First Out - remove from back (stack behavior)
                    return remove ? tokens.removeLast() : tokens.getLast();
                default:
                    return remove ? tokens.removeFirst() : tokens.getFirst();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int getTokenCount() {
        lock.readLock().lock();
        try {
            return tokens.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<String> getAllTokens() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(tokens);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void clearTokens() {
        lock.writeLock().lock();
        try {
            tokens.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void removeTokenAt(int index) {
        lock.writeLock().lock();
        try {
            if (index >= 0 && index < tokens.size()) {
                List<String> tokenList = new ArrayList<>(tokens);
                tokenList.remove(index);
                tokens.clear();
                tokens.addAll(tokenList);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void setTokenAt(int index, String newValue) {
        lock.writeLock().lock();
        try {
            if (index >= 0 && index < tokens.size()) {
                List<String> tokenList = new ArrayList<>(tokens);
                tokenList.set(index, newValue);
                tokens.clear();
                tokens.addAll(tokenList);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void insertTokenAt(int index, String token) {
        lock.writeLock().lock();
        try {
            List<String> tokenList = new ArrayList<>(tokens);
            if (index >= 0 && index <= tokenList.size()) {
                tokenList.add(index, token);
            } else {
                tokenList.add(token);
            }
            tokens.clear();
            tokens.addAll(tokenList);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void moveToken(int fromIndex, int toIndex) {
        lock.writeLock().lock();
        try {
            if (fromIndex >= 0 && fromIndex < tokens.size() && toIndex >= 0 && toIndex < tokens.size()) {
                List<String> tokenList = new ArrayList<>(tokens);
                String token = tokenList.remove(fromIndex);
                tokenList.add(toIndex, token);
                tokens.clear();
                tokens.addAll(tokenList);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int deduplicateTokens() {
        lock.writeLock().lock();
        try {
            List<String> tokenList = new ArrayList<>(tokens);
            List<String> uniqueTokens = new ArrayList<>();
            java.util.Set<String> seen = new java.util.LinkedHashSet<>();

            for (String token : tokenList) {
                if (seen.add(token)) {
                    uniqueTokens.add(token);
                }
            }

            int removedCount = tokenList.size() - uniqueTokens.size();
            tokens.clear();
            tokens.addAll(uniqueTokens);
            return removedCount;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean hasToken(String token) {
        lock.readLock().lock();
        try {
            return tokens.contains(token);
        } finally {
            lock.readLock().unlock();
        }
    }

    // Called after deserialization to reinitialize transient fields
    public void initializeTransientFields() {
        if (tokens == null) {
            tokens = new LinkedList<>();
        }
        if (lock == null) {
            lock = new ReentrantReadWriteLock();
        }
    }

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public BucketType getBucketType() { return bucketType; }
    public void setBucketType(BucketType bucketType) { this.bucketType = bucketType; }

    public int getMaxSize() { return maxSize; }
    public void setMaxSize(int maxSize) { this.maxSize = maxSize; }

    public BucketFullBehavior getFullBehavior() { return fullBehavior; }
    public void setFullBehavior(BucketFullBehavior fullBehavior) { this.fullBehavior = fullBehavior; }

    public boolean isUniqueOnly() { return uniqueOnly; }
    public void setUniqueOnly(boolean uniqueOnly) { this.uniqueOnly = uniqueOnly; }

    public CollectionRule getCollectionRule() { return collectionRule; }
    public void setCollectionRule(CollectionRule collectionRule) { this.collectionRule = collectionRule; }

    public ReplacementConfig getReplacementConfig() { return replacementConfig; }
    public void setReplacementConfig(ReplacementConfig replacementConfig) { this.replacementConfig = replacementConfig; }
}
