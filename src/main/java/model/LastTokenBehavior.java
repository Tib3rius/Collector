package model;

public enum LastTokenBehavior {
    KEEP_IN_BUCKET("Use and keep in bucket"),
    REMOVE_FROM_BUCKET("Use and remove from bucket");

    private final String description;

    LastTokenBehavior(String description) {
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
