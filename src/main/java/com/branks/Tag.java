package com.branks;

public class Tag {
    public final String id;
    public volatile String text;
    /** Permission that unlocks the tag. Empty = no permission requirement. */
    public volatile String permission = "";
    /** Lowest rank that unlocks the tag (every higher rank unlocks it too). Empty = no rank requirement. */
    public volatile String rank = "";

    public Tag(String id, String text) {
        this.id = id;
        this.text = text;
    }

    public String coloredText() {
        return Colors.translate(text);
    }

    /** True when anybody can use the tag (no permission and no rank requirement). */
    public boolean isFree() {
        return permission.isEmpty() && rank.isEmpty();
    }
}
