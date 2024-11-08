package tukano.api;

public class ShortInfo {
    private String shortId;
    private long timestamp;

    public ShortInfo() {
    }

    public ShortInfo(String shortId, long timestamp) {
        this.shortId = shortId;
        this.timestamp = timestamp;
    }

    // Getters and setters
    public String getShortId() { return shortId; }
    public void setShortId(String shortId) { this.shortId = shortId; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "ShortInfo{" +
                "shortId='" + shortId + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
