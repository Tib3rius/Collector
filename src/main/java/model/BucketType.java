package model;

public enum BucketType {
    FIFO("Queue (First In First Out)"),
    LIFO("Stack (Last In First Out)");

    private final String description;

    BucketType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return name() + " - " + description;
    }
}
