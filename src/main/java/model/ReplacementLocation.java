package model;

public enum ReplacementLocation {
    HEADER("Header", "Header (Requests & Responses)"),
    URL_PARAMETER("URL Parameter", "URL Parameter (Requests)"),
    BODY_PARAMETER("Body Parameter", "Body Parameter (Requests)"),
    COOKIE("Cookie", "Cookie (Requests)"),
    GENERIC_REGEX("Generic Regex", "Generic Regex (Requests & Responses)");

    private final String shortName;
    private final String description;

    ReplacementLocation(String shortName, String description) {
        this.shortName = shortName;
        this.description = description;
    }

    public String getShortName() {
        return shortName;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return shortName;
    }
}
