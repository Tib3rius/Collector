package model;

public enum BucketFullBehavior {
    REJECT_NEW("Reject new tokens"),
    REPLACE_LAST("Replace latest token"),
    REPLACE_OLDEST("Replace oldest token");

    private final String description;

    BucketFullBehavior(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return description;
    }
}
