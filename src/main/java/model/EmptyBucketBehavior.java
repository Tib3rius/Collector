package model;

public enum EmptyBucketBehavior {
    DO_NOTHING("Do nothing"),
    USE_STATIC_VALUE("Use static value"),
    GENERATE_FROM_REGEX("Generate value from regex");

    private final String displayName;

    EmptyBucketBehavior(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
